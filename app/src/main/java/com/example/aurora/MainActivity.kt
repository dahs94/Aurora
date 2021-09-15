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
    lateinit var peerName: String
    lateinit var peerAddress: String
    private lateinit var client: UDPClient
    private lateinit var server: UDPServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        initListeners()

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
        val makeVisibleButton: Button = findViewById(R.id.make_visible_button)
        makeVisibleButton.setOnClickListener {
            startActivity(Intent(this, MakeVisibleActivity::class.java))
        }
        val disconnectButton: Button = findViewById(R.id.disconnect_button)
        disconnectButton.setOnClickListener {
            wifiDirectUtils.disconnect()
            val devNameTextView: TextView = findViewById(R.id.device_name_textview)
            devNameTextView.text = ""
        }
        val transmitButton: ImageButton = findViewById(R.id.transmit_button)
        transmitButton.setOnClickListener {
            transmitMessage()
        }
        groupInfoListener = WifiP2pManager.GroupInfoListener {
            onGroupAvailable(it)
        }
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }
    }

    private fun onGroupAvailable(group: WifiP2pGroup) {
        /*Get name of connected device. At this time - we're only going to be connected
        to one device. We know that this device is either the GO or the client*/
        peerName = if (group.isGroupOwner) {
            //get the one any only client in the list
            val clientDevice: WifiP2pDevice = group.clientList.elementAt(0)
            clientDevice.deviceName
        } else
        {
            group.owner.deviceName
        }
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo) {
        if (groupInfo.groupFormed) {
            val devNameTextView: TextView = findViewById(R.id.device_name_textview)
            var groupOwner: Boolean = false
            if (groupInfo.isGroupOwner) {
                //Is group owner, so listen for socket connection
                server = UDPServer()
                server.receive()
                groupOwner = true
            }
            else {
                //Is group client, so initiate socket connection
               peerAddress = (groupInfo.groupOwnerAddress).toString()
               peerAddress = peerAddress.substring(1)
               client = UDPClient(InetAddress.getByName(peerAddress))
            }
            devNameTextView.text = getString(R.string.peer_details, peerName,
                groupOwner.toString())
        }
        else {
            Timber.i("T_Debug: onConnectionAvailable() >> group formation failed")
        }
    }

    private fun handleBundle(
        peerName: String?,
        groupFormed: Boolean,
        ipAddress: String?,
        isGroupOwner: Boolean
    ){
        val devNameTextView: TextView = findViewById(R.id.device_name_textview)
        if (groupFormed) {
            devNameTextView.text = getString(R.string.peer_details, peerName,
                isGroupOwner.toString())
            if (isGroupOwner) {
                server = UDPServer()
                server.receive()
            }
            else {
                client = UDPClient(InetAddress.getByName(ipAddress))
            }
        }
        else {
            Timber.i("T_Debug: handleBundle() >> discarding bundle, group not formed.")
        }
    }

    private fun transmitMessage() {
        client.send()
    }
}