package com.example.aurora

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
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
 *
 * Template for this class provided by Android documentation, please see here:
 * https://developer.android.com/guide/topics/connectivity/wifip2p
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
                if (wifiState != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    val toast: Toast = Toast.makeText(context, "Wi-Fi Direct is disabled. " +
                            "Please enable it in settings.", Toast.LENGTH_LONG)
                    toast.show()
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                //different listener is called, depending on the Activity constructor.
                //included to make it easier to add more activities in the future.
                if (activity is MainActivity) {
                    wManager.requestPeers(wChannel, activity.peerListener)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                /*Check what kind of connection change this is. 'NetworkInfo' is depreciated, but
                I could not find an alternative that worked correctly.*/
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                when (networkInfo != null && networkInfo.isConnected()) {
                    true -> {
                        Timber.i("T_Debug: onReceive() >> connection change: device connected.")
                        if (activity is MainActivity) {
                            wManager.requestConnectionInfo(wChannel, activity.connectionListener)
                            wManager.requestGroupInfo(wChannel, activity.groupInfoListener)
                        }
                    }
                    false -> {
                        Timber.i("T_Debug: onReceive() >> connection change: device disconnected.")
                    }
                    null -> {
                        Timber.i("T_Debug: onReceive() >> connection change: null.")
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                //respond to this device's wifi state changing
            }

        }
    }
}