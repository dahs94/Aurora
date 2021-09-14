package com.example.aurora

import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MakeVisibleActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    private lateinit var deviceName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_visible)
        initListeners()
        makeDeviceVisible()
    }

    private fun initListeners(){
        val backButton: Button = findViewById(R.id.make_visible_back_button)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            wifiDirectUtils.stopDiscovery()
        }

        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }
        groupInfoListener = WifiP2pManager.GroupInfoListener {
            onGroupAvailable(it)
        }
    }

    private fun makeDeviceVisible(){
        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        wifiDirectUtils.initWiFiDiscovery()
    }


    /**
     * BUG: Not working, not passing name/moving to ActivityMain when a device connects.
     */

    private fun onGroupAvailable(group: WifiP2pGroup){
        /*Get name of connected device. At this time - we're only going to be connected
        to one device. We know this device is the owner, so we just need to get the first
        client.*/
        val clientDevice: WifiP2pDevice = group.clientList.elementAt(0)
        deviceName = clientDevice.deviceName
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo){
        var groupCreated: Boolean = false
        val bundle: Bundle = Bundle()

        if (groupInfo.groupFormed) groupCreated = true

        //Navigate to MainActivity & pass device details if connection successful
        if (groupCreated){
            val intent: Intent = Intent(this, MainActivity::class.java)
            bundle.putString("DEVICE_NAME", deviceName)
            intent.putExtras(bundle)
            startActivity(Intent(intent))
        }
        else {
            val toastMessage: String = String.format(getString(R.string.wd_connection_failed))
            val toast: Toast = Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG)
            toast.show()
        }
    }
}