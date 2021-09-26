package com.example.aurora

import android.net.wifi.p2p.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var peerListener: WifiP2pManager.PeerListListener
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()
    private lateinit var tipTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        initResources()
        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        initListeners()
    }

    private fun initResources() {
        tipTextView = findViewById(R.id.TipTextView)
        progressBar = findViewById(R.id.progressBar)
        listView = findViewById(R.id.search_listview)
    }

    private fun initListeners() {
        val findDevicesButton: Button = findViewById(R.id.find_devices_button)

        findDevicesButton.setOnClickListener {
            findDevices()
        }
        val transmitButton: ImageButton = findViewById(R.id.speak_image_button)
        transmitButton.setOnClickListener {

        }
        listView.setOnItemClickListener { parent, view, position, _ ->
            listView.setSelector(R.color.item_selected_amber);
            handleSelect(parent.getItemAtPosition(position) as WifiP2pDevice, view)
        }
        peerListener = WifiP2pManager.PeerListListener() {
            onPeersAvailable(it)
        }
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }
    }

    private fun findDevices() {
        progressBar.visibility = View.VISIBLE
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(getString(R.string.dialog_message2))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                wifiDirectUtils.stopDiscoveryIfRunning()
                wifiDirectUtils.disconnectIfGroupFormed()
                p2pDeviceList.clear()
                wifiDirectUtils.initWiFiDiscovery()
            }
            setNegativeButton("No") { dialog, _ ->
                progressBar.visibility = View.GONE
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        if(deviceList.deviceList.isEmpty() ) {
            Timber.i("T_Debug: onPeersAvailable() >> p2pDeviceList is empty")
        }
        else {
            if (deviceList != p2pDeviceList)
            {
                p2pDeviceList.clear()
                for (device in deviceList.deviceList) {
                    p2pDeviceList.add(device)
                }
            }
            Timber.i("T_Debug: onPeersAvailable() >> updating ListView with new peers")
            listView.adapter = ListViewAdapter(this, p2pDeviceList)
        }
    }

    private fun handleSelect(selectedItem: WifiP2pDevice, view: View) {
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(String.format(getString(R.string.dialog_message1), selectedItem.deviceName))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                    wifiDirectUtils.stopDiscoveryIfRunning()
                    listView.setSelector(R.color.item_selected_celadon_green)
                    progressBar.visibility = View.GONE
                    //Connect to selected device
                    try {
                        wifiDirectUtils.connectPeer(selectedItem)
                    }
                    catch (e: Exception) {
                        Timber.i("T_Debug: handleSelect() >> error connecting to peer: " +
                                "$e")

                        listView.setSelector(android.R.color.transparent)
                    }
                }
            setNegativeButton("No") { dialog, _ ->
                listView.setSelector(android.R.color.transparent)
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo) {
        val peerDevice: PeerDevice = PeerDevice(groupInfo)
        tipTextView.text = getString(R.string.onConnectedDevice)
        progressBar.visibility = View.VISIBLE
        /*
            Launch operation in a separate thread. That thread then blocks & waits for
            IO operations to complete. We then handle the result in the main thread.
            we update the main thread first so give the user appropriate feedback.
        */
        CoroutineScope(Dispatchers.Default).launch {
            CoroutineScope(Dispatchers.Default).async {
                peerDevice.initConnection()
            }.await()
            withContext(Dispatchers.Main) {
                Timber.i("T_Debug: onConnectionAvailable() >> all jobs finished.")
                val peerIPAddress: String = peerDevice.getRemoteIPAddress()
                var toastMessage: String = ""
                if (peerIPAddress == "9.9.9.9") {
                    /*
                     PeerDevice 'remoteIPAddress' property on default & has not been updated.
                     Issue with device connectivity.
                    */
                    toastMessage = "Getting remote peer details failed. Disconnecting from remote peer, " +
                            "please try again."
                    Timber.i("T_Debug: onConnectionAvailable() >> remote peer details: $toastMessage")
                    wifiDirectUtils.disconnectIfGroupFormed()
                }
                else {
                    Timber.i("T_Debug: onConnectionAvailable() >> connected to remote peer")
                    var role: String = "Client"
                    if (peerDevice.getRole(groupInfo) == 1) {
                        role = "Group owner"
                    }
                    toastMessage = "Connected to $peerIPAddress.\nThis device is the $role."
                }
                Toast.makeText(applicationContext,toastMessage,Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiDirectUtils.wReceiver, wifiDirectUtils.intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiDirectUtils.wReceiver)
    }
}

