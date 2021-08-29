package com.example.aurora

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import timber.log.Timber

/**
 * Inherits from the 'Broadcast Receiver' class. Listens for intents broadcast by the OS.
 * Allows app to respond to certain events on the device.
 *
 * Broadcast in android is the system-wide events that can occur when the device starts,
 * when a message is received on the device or when incoming calls are received, or when
 * a device goes to airplane mode, etc. Broadcast Receivers are used to respond to these
 * system-wide events
 */
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: DiscoveryActivity
) : BroadcastReceiver() {
    /**we must override this, because BroadcastReceiver is an abstract class**/

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                //Check to see if Wi-Fi is enabled and notify appropriate activity
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Timber.i("P2P_CHANGED intent triggered")
                manager?.requestPeers(channel) {
                        peers: WifiP2pDeviceList? -> Timber.i("$peers")
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
            }
        }
    }
}