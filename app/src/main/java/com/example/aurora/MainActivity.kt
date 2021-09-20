package com.example.aurora

import android.graphics.Color
import android.net.wifi.p2p.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    lateinit var peerListener: WifiP2pManager.PeerListListener
    lateinit var udpConnection: UDPConnection
    lateinit var listView: ListView
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()

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

    private fun initListeners() {
        val findDevicesButton: Button = findViewById(R.id.find_devices_button)
        findDevicesButton.setOnClickListener {
            val progressBar: ProgressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.VISIBLE

            //Stop any previous discovery and start a new one.
            wifiDirectUtils.stopDiscovery()
            p2pDeviceList.clear()
            wifiDirectUtils.initWiFiDiscovery()
        }
        val transmitButton: ImageButton = findViewById(R.id.speak_image_button)
        transmitButton.setOnClickListener {
            udpConnection.transmit()
        }
        val listView: ListView = findViewById(R.id.search_listview)
        listView.setOnItemClickListener { parent, view, position, _ ->
            listView.setSelector(R.color.item_selected_celadon_green);
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

    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        if(deviceList.deviceList.isEmpty() ) {
            Timber.i("T_Debug: onPeersAvailable() >> p2pDeviceList is empty")
            val progressBar: ProgressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.GONE
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
            setMessage(String.format(getString(R.string.dialog_message), selectedItem.deviceName))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                if (udpConnection.groupFormed) {
                    Timber.i("T_Debug: handleSelect() >> disconnecting from current group")
                    val progressBar: ProgressBar = findViewById(R.id.progressBar)
                    progressBar.visibility = View.GONE
                    // wifiDirectUtils.disconnect()
                   // wifiDirectUtils.connectPeer(selectedItem)

                }
                else {
                   // wifiDirectUtils.connectPeer(selectedItem)
                }

            }
            setNegativeButton("No") { dialog, _ ->
                listView.setSelector(android.R.color.transparent);
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

    /**
     * Really need to have a think about how we handle device connection.
     **/

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

