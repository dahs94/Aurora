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
    private lateinit var peerName: String

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

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo){
        if (groupInfo.groupFormed){
            val intent: Intent = Intent(this, MainActivity::class.java)
            val bundle: Bundle = Bundle()
            bundle.putString("DEVICE_NAME", peerName)
            bundle.putBoolean("GROUP_FORMED", true)
            bundle.putString("DEVICE_IP",groupInfo.groupOwnerAddress.toString())
            if (groupInfo.isGroupOwner) {
                bundle.putBoolean("GROUP_OWNER", true)
            }
            else bundle.putBoolean("GROUP_OWNER", false)
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