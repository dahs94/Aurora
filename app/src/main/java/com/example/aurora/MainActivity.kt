package com.example.aurora

import android.graphics.Color
import android.net.wifi.p2p.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    lateinit var peerListener: WifiP2pManager.PeerListListener
    lateinit var udpConnection: UDPConnection
    private lateinit var listView: ListView
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()

    @RequiresApi(Build.VERSION_CODES.Q) //do something about this: i.e. increase minimum API
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        initListeners()
        udpConnection = UDPConnection()
        listView = findViewById(R.id.search_listview)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
        groupInfoListener = WifiP2pManager.GroupInfoListener {
            //onGroupAvailable(it)
        }
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            //onConnectionAvailable(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findDevices() {
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(getString(R.string.dialog_message2))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                if (wifiDirectUtils.groupFormed()) {
                    try {
                        wifiDirectUtils.disconnect()
                    }
                    catch (e: Exception) {
                        Timber.i("T_Debug: findDevices() >> error starting discovery: " +
                                "$e")
                        progressBar.visibility = View.GONE
                    }
                }
                if (wifiDirectUtils.discoveryRunning()) {
                    try {
                        wifiDirectUtils.stopDiscovery()
                    }
                    catch (e: Exception) {
                        Timber.i("T_Debug: findDevices() >> error starting discovery: " +
                                "$e")
                        progressBar.visibility = View.GONE
                    }
                }
                try {
                    wifiDirectUtils.initWiFiDiscovery()
                }
                catch (e: Exception) {
                    Timber.i("T_Debug: findDevices() >> error starting discovery: " +
                            "$e")
                    progressBar.visibility = View.GONE
                }
                p2pDeviceList.clear()
                progressBar.visibility = View.VISIBLE
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        if(deviceList.deviceList.isEmpty() ) {
            Timber.i("T_Debug: onPeersAvailable() >> p2pDeviceList is empty")
            //val progressBar: ProgressBar = findViewById(R.id.progressBar)
            //progressBar.visibility = View.GONE
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
            var listView: ListView = findViewById(R.id.search_listview)
            listView.adapter = ListViewAdapter(this, p2pDeviceList)
        }
    }

    private fun handleSelect(selectedItem: WifiP2pDevice, view: View) {
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

    private fun onGroupAvailable(group: WifiP2pGroup) {
        //udpConnection.getPeerName(group)
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo) {
        //udpConnection.getDeviceRole(groupInfo)
        //udpConnection.getPeerAddress(groupInfo)
        //if (udpConnection.groupOwner) udpConnection.receive()
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

