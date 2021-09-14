package com.example.aurora

import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Delay
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    private var deviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //handle bundle
        val deviceName: String? = intent.getStringExtra("DEVICE_NAME")
        //val deviceSelected: Boolean = intent.getBooleanExtra("DEVICE_SELECTED",
            //false)
        setContentView(R.layout.activity_main)
        setupWiFiDirect()
        initListeners()
        handleConnection(deviceName)
    }

    private fun setupWiFiDirect() {
        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
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
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }
        groupInfoListener = WifiP2pManager.GroupInfoListener {
            onGroupAvailable(it)
        }
    }

    private fun onGroupAvailable(group: WifiP2pGroup) {
        /*Get name of connected device. At this time - we're only going to be connected
        to one device. We know this device is the owner, so we just need to get the first
        client.*/
        val clientDevice: WifiP2pDevice = group.clientList.elementAt(0)
        deviceName = clientDevice.deviceName
        handleConnection(deviceName)
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo) {
        if (groupInfo.groupFormed) {
            //set connected device details
        }
    }

    private fun transmitMessage() {
        //val UDPClient: UDPClient = UDPClient()
    }

    private fun handleConnection(deviceName: String?){
        if (deviceName != null) {
            val devNameTextView: TextView = findViewById(R.id.device_name_textview)
            devNameTextView.text = deviceName
        }
    }
}