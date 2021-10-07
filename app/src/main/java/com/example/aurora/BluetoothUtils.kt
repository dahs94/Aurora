package com.example.aurora

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import timber.log.Timber

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
}