package com.example.aurora

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
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

    private var groupFormed: Boolean = false
    private var discoveryRunning: Boolean = false

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
                    throw Exception("initWiFiDiscovery() >> " +
                        "Discover devices failed: $reasonCode")
                }
            })
        }
        else {
             throw Exception("initWiFiDiscovery() >>" +
                     "discovery failed: fine location not granted")
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

    @RequiresApi(Build.VERSION_CODES.Q) //do something about this
    fun refreshConnectionDetails() {
        discoveryRunning()
        groupFormed()
    }

    /**
     * For both discoveryRunning & groupFormed, need a way to wait for the async listener result
     * before running the rest of the code. Otherwise, it gives false results.
     **/

    @RequiresApi(Build.VERSION_CODES.Q) //do something about this
    fun discoveryRunning() {
        wManager.requestDiscoveryState(wChannel) { state ->
            if (state == 1) discoveryRunning = true }
        Timber.i("T_Debug: discoveryRunning >> discovery running: $discoveryRunning")
    }

    @RequiresApi(Build.VERSION_CODES.Q) //do something about this
    fun groupFormed() {
        //Permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Timber.i("T_Debug: groupFormed() >> location permission already granted")
            wManager.requestGroupInfo(wChannel) { group ->
                Timber.i("T_Debug: groupFormed >> ${group.toString()}")
                if (group != null) {
                    Timber.i("T_Debug: groupFormed >> true")
                    groupFormed = true
                }
            }
        }
        else {
            throw Exception("groupFormed() >>" +
                    "failed: fine location permission not granted")
        }
    }

    fun stopDiscovery() {
        wManager.stopPeerDiscovery(wChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("T_Debug: stopDiscovery >> Discovery stopped")
            }

            override fun onFailure(reason: Int) {
                throw Exception("stopPeerDiscovery() >>" +
                        "could not stop discovery: $reason")
            }
        })
    }

    fun disconnect() {
        wManager.removeGroup(wChannel, object : WifiP2pManager.ActionListener  {
            override fun onSuccess() {
                Timber.i("T_Debug: stopDiscovery >> group removed")
            }

            override fun onFailure(reason: Int) {
                throw Exception("disconnect() >> could not disconnect: $reason")
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
                    throw Exception("connectPeer() >>" +
                            "connection to $deviceName failed: $reason")
                }
            })
        }
        else {
            throw Exception("connectPeer() >>" +
                    "connection to $deviceName failed: fine location permission not granted")
        }
    }
}