package com.example.aurora

import android.net.wifi.p2p.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    lateinit var peerListener: WifiP2pManager.PeerListListener
    lateinit var udpConnection: UDPConnection
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        initListeners()
        //udpConnection = UDPConnection()
    }

    private fun initListeners() {
        val findDevicesButton: Button = findViewById(R.id.find_devices_button)

        findDevicesButton.setOnClickListener {
            findDevices()
        }
        val transmitButton: ImageButton = findViewById(R.id.speak_image_button)
        transmitButton.setOnClickListener {
            udpConnection.transmit()
        }
        val listView: ListView = findViewById(R.id.search_listview)
        listView.setOnItemClickListener { parent, view, position, _ ->
            listView.setSelector(R.color.item_selected_amber);
            handleSelect(parent.getItemAtPosition(position) as WifiP2pDevice, view)
        }
        peerListener = WifiP2pManager.PeerListListener() {
            onPeersAvailable(it)
        }
        /*groupInfoListener = WifiP2pManager.GroupInfoListener {
            //onGroupAvailable(it)
        }*/
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            //onConnectionAvailable(it)
        }
    }

    private fun findDevices() {
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
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
            val listView: ListView = findViewById(R.id.search_listview)
            listView.adapter = ListViewAdapter(this, p2pDeviceList)
        }
    }

    private fun handleSelect(selectedItem: WifiP2pDevice, view: View) {
        val listView: ListView = findViewById(R.id.search_listview)
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(String.format(getString(R.string.dialog_message1), selectedItem.deviceName))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                    val progressBar: ProgressBar = findViewById(R.id.progressBar)
                    progressBar.visibility = View.GONE
                    listView.setSelector(R.color.item_selected_celadon_green)
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
        peerDevice.initConnection()
        val peerIPAddress = peerDevice.getRemoteIPAddress()
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

