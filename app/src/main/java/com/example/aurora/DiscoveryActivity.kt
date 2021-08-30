package com.example.aurora

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class DiscoveryActivity : AppCompatActivity() {

    private lateinit var wManager: WifiP2pManager
    private lateinit var wChannel: WifiP2pManager.Channel
    private lateinit var wReceiver: BroadcastReceiver
    lateinit var peerListener: WifiP2pManager.PeerListListener
    private val intentFilter: IntentFilter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discovery)
        setupWiFiDirect()
        initListeners()
    }

    private fun setupWiFiDirect() {
        wManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wChannel = wManager.initialize(this,mainLooper, null)
        wReceiver = WiFiDirectBroadcastReceiver(wManager, wChannel, this)

        intentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }

    private fun initListeners() {
        val findDevicesButton: ImageButton = findViewById(R.id.find_devices_magnifying_glass)
        findDevicesButton.setOnClickListener {
            discoverWiFiDevices()
        }

        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        peerListener = WifiP2pManager.PeerListListener() {
            onPeersAvailable(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverWiFiDevices() {
        val textView: TextView = findViewById(R.id.discoveryTipTextView)
        wManager.discoverPeers(wChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                textView.text = "Peer discovery started..."

            }
            override fun onFailure(reasonCode: Int) {
                textView.text = "Peer discovery failed: $reasonCode"
            }
        })
    }

    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        for (device in deviceList.deviceList) {
            //to do
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wReceiver)
    }

}