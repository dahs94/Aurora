package com.example.aurora

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Class to simplify config & management of Bluetooth API:
 * This documentation was used as guidance:
 * https://developer.android.com/guide/topics/connectivity/bluetooth
 */
class BluetoothUtils(
    private val activity: MainActivity
) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var bReceiver: BroadcastReceiver
    val intentFilter: IntentFilter = IntentFilter()
    private val id: String = "d7515482-2ac4-11ec-8d3d-0242ac130003"
    lateinit var serverSocket: BluetoothServerSocket
    private var socket: BluetoothSocket? = null

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
            makeDiscoverable()
            bluetoothAdapter?.startDiscovery()
        }
    }

    private fun makeDiscoverable() {
        //code from here (Google): https://developer.android.com/guide/topics/connectivity/bluetooth/find-bluetooth-devices
        Timber.i("T_Debug: makeDiscoverable() >> device is now discoverable for 5 minutes.")
        val requestCode = 1;
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivityForResult(activity, discoverableIntent, requestCode, null)
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
                /*code for socket connection originates from here:
                https://developerndroid.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices
                */
                Timber.i("T_Debug: listenForPeer() >> listening as Bluetooth server.")
                serverSocket = bluetoothAdapter!!.
                listenUsingRfcommWithServiceRecord("Aurora", UUID.fromString(id))
                var loop = true
                while(loop) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        Timber.i("T_Debug: listenForPeer() >> socket accept failed $e")
                        loop = false
                        null
                    }
                    socket?.also {
                        withContext(Dispatchers.Main) {
                            handleSocket(it)
                        }
                        serverSocket?.close()
                        loop = false
                    }
                }
            }
        }
    }

    fun cancelListening(){
        try {
            serverSocket.close()
            Timber.i("T_Debug: cancelListening() >> listening for peers stopped.")
        } catch (e: IOException) {
            Timber.i("T_Debug: cancelListening() >> could not cancel, $e.")
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
            Timber.i(
                "T_Debug: connectPeer() >> ERROR, BluetoothAdapter is null, please call" +
                        "'initBluetooth()' and try again."
            )
        } else {
            cancelDiscovery() //recommended to cancel any ongoing discovery before connecting
            val clientSocket: BluetoothSocket = peerDevice.createRfcommSocketToServiceRecord(
                UUID.fromString(id)
            )
            Timber.i("T_Debug: connectPeer() >> connecting to peer as client.")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    clientSocket.connect()
                    withContext(Dispatchers.Main) {
                        handleSocket(clientSocket)
                    }
                } catch (e: Exception) {
                    Timber.i("T_Debug: connectPeer() >> connection failed: $e.")
                }
            }
        }
    }

     private fun handleSocket (clientSocket: BluetoothSocket) {
        activity.onConnectionAvailable(clientSocket)
    }

    fun closeSocket(socket: BluetoothSocket?) {
        try {
            socket?.close()
            Timber.i("T_Debug: closeSocket() >> Bluetooth socket closed")
        }
        catch(e: IOException) {
            Timber.i("T_Debug: closeSocket() >> could not close the Bluetooth socket: $e.")
        }

    }
}