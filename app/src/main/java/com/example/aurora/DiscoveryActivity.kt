package com.example.aurora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DiscoveryActivity : AppCompatActivity() {

    private val wManager: WifiP2pManager =
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private lateinit var wChannel: WifiP2pManager.Channel
    private lateinit var wReceiver: BroadcastReceiver
    private val intentFilter: IntentFilter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discovery)
        setupWiFiDirect()

        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupWiFiDirect() {
        wChannel = wManager.initialize(this,mainLooper, null)
        wReceiver = WiFiDirectBroadcastReceiver(wManager, wChannel, this)

        intentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
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