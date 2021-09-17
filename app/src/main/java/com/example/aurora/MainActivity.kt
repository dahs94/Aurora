package com.example.aurora

import android.content.Intent
import android.net.wifi.p2p.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import timber.log.Timber
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    lateinit var udpConnection: UDPConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        initListeners()
        udpConnection = UDPConnection()

        if (this.intent.extras != null) {
            handleBundle(
                intent.getStringExtra("DEVICE_NAME"),
                intent.getBooleanExtra("GROUP_FORMED", false),
                intent.getStringExtra("DEVICE_IP"),
                intent.getBooleanExtra("GROUP_OWNER", false)
            )
        }
    }

    //access resource, create intent & start activity using intent
    private fun initListeners() {
        val findDevicesButton: Button = findViewById(R.id.find_devices_button)
        findDevicesButton.setOnClickListener {
            startActivity(Intent(this, DiscoveryActivity::class.java))
        }
        val disconnectButton: Button = findViewById(R.id.disconnect_button)
        disconnectButton.setOnClickListener {
            wifiDirectUtils.disconnect()
            val devNameTextView: TextView = findViewById(R.id.device_name_textview)
            devNameTextView.text = ""
        }
        val transmitButton: ImageButton = findViewById(R.id.transmit_button)
        transmitButton.setOnClickListener {
            udpConnection.transmit()
        }
        groupInfoListener = WifiP2pManager.GroupInfoListener {
            onGroupAvailable(it)
        }
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }
    }

    private fun onGroupAvailable(group: WifiP2pGroup) {
        udpConnection.getPeerName(group)
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo) {
        udpConnection.getDeviceRole(groupInfo)
        udpConnection.getPeerAddress(groupInfo)
        if (udpConnection.groupOwner) udpConnection.receive()
    }

    private fun handleBundle(
        peerName: String?,
        groupFormed: Boolean,
        ipAddress: String?,
        isGroupOwner: Boolean
    ){
        val devNameTextView: TextView = findViewById(R.id.device_name_textview)
        if (groupFormed) {
            udpConnection.groupFormed = true
            udpConnection.peerName = peerName
            udpConnection.peerAddress = ipAddress
            udpConnection.groupOwner = isGroupOwner
            devNameTextView.text = getString(
                R.string.peer_details, peerName,
                isGroupOwner.toString()
            )
            if (isGroupOwner) {
                udpConnection.receive()
            }
        }
    }
}