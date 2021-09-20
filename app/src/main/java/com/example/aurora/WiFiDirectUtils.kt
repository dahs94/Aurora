package com.example.aurora

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import timber.log.Timber

class WiFiDirectUtils(
    private val context: Context,
    private val activity: Activity
) {

    private lateinit var wManager: WifiP2pManager
    lateinit var wChannel: WifiP2pManager.Channel
    lateinit var wReceiver: BroadcastReceiver
    val intentFilter: IntentFilter = IntentFilter()

    fun initWiFiDirect() {
        wManager = context.applicationContext.getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wChannel = wManager.initialize(context,context.mainLooper, null)
        wReceiver = WiFiDirectBroadcastReceiver(wManager, wChannel, activity)

        intentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }

     fun initWiFiDiscovery() {
         if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Timber.i("T_Debug: initWiFiDiscovery() >> Location permission already granted")
            wManager.discoverPeers(wChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Timber.i("T_Debug: initWiFiDiscovery() >> " +
                                "Discover devices initiated")
                }
                override fun onFailure(reasonCode: Int) {
                    Timber.i("T_Debug: initWiFiDiscovery() >> " +
                            "Discover devices failed: $reasonCode")
                }
            })
        }
        else {
            Timber.i("T_Debug: initWiFiDiscovery() >> " +
                    "Location permission missing")
        }
    }

    /**private suspend fun discoveryTimer() {
        withContext(Dispatchers.Default) {
            val timeout: Long = 120000
            delay(timeout)
            //after delay, stop searching
            stopDiscovery()
        }
    }**/

    fun stopDiscovery() {
        wManager.stopPeerDiscovery(wChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("T_Debug: stopDiscovery >> Discovery stopped")
            }

            override fun onFailure(reason: Int) {
                Timber.i("T_Debug: stopDiscovery >> Discovery stop failed")
            }
        })
    }

    fun disconnect() {
        wManager.removeGroup(wChannel, object : WifiP2pManager.ActionListener  {
            override fun onSuccess() {
                Timber.i("T_Debug: stopDiscovery >> Group removed")
            }

            override fun onFailure(reason: Int) {
                Timber.i("T_Debug: stopDiscovery >> Cannot remove group")
            }
        })
    }

    fun connectPeer(deviceSelected: WifiP2pDevice) {
        Timber.i("T_Debug: connectPeer() >> connecting to ${deviceSelected.deviceName} " +
                " - ${deviceSelected.deviceAddress}")
        val deviceName: String = deviceSelected.deviceName
        val wifiPeerConfig: WifiP2pConfig = WifiP2pConfig()
        wifiPeerConfig.deviceAddress = deviceSelected.deviceAddress

        /*for now, make this device GO. Studies show that autonomous mode is faster
        for group creation (Oide et al). Right now, we're imitating a point to point
        connection with one device*/
        wifiPeerConfig.groupOwnerIntent = 15

        //Permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Timber.i("T_Debug: connectPeer() >> location permission already granted")
            //Connect to peer
            wManager.connect(wChannel, wifiPeerConfig, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Timber.i("T_Debug: connectPeer() >>" +
                            "initiating connection to $deviceName")
                }

                override fun onFailure(reason: Int) {
                    Timber.i("T_Debug: connectPeer() >>" +
                            "connection to $deviceName failed")
                }
            })
        }
        else {
            Timber.i("T_Debug: connectPeer() >> " +
                    "Location permission missing")
        }
    }
}