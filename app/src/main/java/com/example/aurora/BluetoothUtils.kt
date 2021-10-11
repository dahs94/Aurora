package com.example.aurora

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Class to simplify config & management of Bluetooth API:
 * This documentation was used as guidance:
 * https://developer.android.com/guide/topics/connectivity/bluetooth
 */
class BluetoothUtils(
    private val activity: Activity
) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var bReceiver: BroadcastReceiver
    val intentFilter: IntentFilter = IntentFilter()

    /**
     * Initial work to enabled Bluetooth and make sure the device supports it.
     */
    fun initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Timber.i("T_Debug: initBluetooth() >> ERROR. Bluetooth not supported.")
        }
        else {
            if (checkEnabled()) {
                intentFilter.apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                }
                bReceiver = BluetoothBroadcastReceiver(activity)
                activity.registerReceiver(bReceiver, intentFilter)
            }
            else
            {
                TODO("Enable Bluetooth")
            }
        }
    }

    private fun checkEnabled(): Boolean {
        if (bluetoothAdapter?.isEnabled == false) {
            Timber.i("T_Debug: checkEnabled() >> Bluetooth not enabled.")
            return false
        }
        else {
            Timber.i("T_Debug: checkEnabled() >> Bluetooth is enabled.")
            return true
        }
    }

    fun discoverDevices() {
        //query paired devices first?
        if (bluetoothAdapter == null) {
            Timber.i("T_Debug: discoverDevices() >> BluetoothAdapter is null, please call" +
                    "'initBluetooth()' and try again.")
        }
        else {
            Timber.i("T_Debug: discoverDevices() >> Bluetooth device discovery started.")
            bluetoothAdapter?.startDiscovery()
        }
    }

    fun cancelDiscovery() {
        Timber.i("T_Debug: cancelDiscovery() >> device discovery stopped.")
        bluetoothAdapter?.cancelDiscovery()
    }

    /**
     * Creates a BluetoothServerSocket and listens for incoming connection from client. Once
     * a connection is received, the server socket is closed to stop any other devices from
     * connecting. The created BluetoothSocket is passed to AudioRecorderUtils for use in
     * transmitting data.
     */
    fun listenForPeer() {
        if (bluetoothAdapter?.isEnabled == false) {
            Timber.i("T_Debug: listenForPeer() >> ERROR, BluetoothAdapter is null, please call" +
                    "'initBluetooth()' and try again.")
        }
        else {
            CoroutineScope(Dispatchers.IO).launch {
                val id: String = "d7515482-2ac4-11ec-8d3d-0242ac130003"
                val bluetoothServerSocket: BluetoothServerSocket = bluetoothAdapter!!.
                listenUsingInsecureRfcommWithServiceRecord("Aurora", java.util.UUID.fromString(id))
                /*code for socket connection originates from here:
                https://developerndroid.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices
                */
                var loop = true
                while(loop) {
                    val socket: BluetoothSocket? = bluetoothServerSocket.accept()
                    socket.also {
                        //handleSocket(it)
                        socket?.close()
                        loop = false
                    }
                }
            }
        }
    }

    /**
     * Connects to the peer device acting as the Bluetooth server socket.
     * @param peerDevice a Bluetooth device found and selected during device discovery
     * Once again, this page used for API guidance:
     * https://developer.android.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices
     */
    fun connectPeer(peerDevice: BluetoothDevice) {
        if (bluetoothAdapter?.isEnabled == false) {
            Timber.i("T_Debug: connectPeer() >> ERROR, BluetoothAdapter is null, please call" +
                    "'initBluetooth()' and try again.")
        }
        else {
            CoroutineScope(Dispatchers.IO).launch {
                cancelDiscovery() //recommended to cancel any ongoing discovery before connecting
                val id: String = "d7515482-2ac4-11ec-8d3d-0242ac130003"
                val bluetoothClientSocket: BluetoothSocket = peerDevice.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(id))
                bluetoothClientSocket.let {
                    bluetoothSocket -> bluetoothSocket.connect()
                    //handleSocket(bluetoothSocket)
                }
            }
        }
    }
}