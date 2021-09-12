package com.example.aurora

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
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
    private val wManager: WifiP2pManager,
    private val wChannel: WifiP2pManager.Channel,
    private val activity: Activity
) : BroadcastReceiver() {
    /**we must override this, because BroadcastReceiver is an abstract class**/

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val wifiState: Int = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (wifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    //Wi-Fi Direct is enabled. Nothing to do here.
                }
                else {
                    val toast: Toast = Toast.makeText(context, "Wi-Fi Direct is disabled. " +
                            "Please enable it in settings.", Toast.LENGTH_LONG)
                    toast.show()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                if (activity is DiscoveryActivity) {
                    wManager.requestPeers(wChannel, activity.peerListener)
                }
                if (activity is MakeVisibleActivity) {
                    //do nothing -- we're just advertising ourself
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                if (activity is DiscoveryActivity) {
                    wManager.requestGroupInfo(wChannel, activity.groupInfoListener)
                    wManager.requestConnectionInfo(wChannel, activity.connectionListener)
                }
                if (activity is MakeVisibleActivity) {
                    //do nothing -- we're just advertising ourself
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                //respond to this device's wifi state changing
            }
        }
    }
}