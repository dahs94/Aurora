package com.example.aurora

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import timber.log.Timber

/**
 * Class to simplify config & management of Bluetooth API:
 * https://developer.android.com/guide/topics/connectivity/bluetooth
 */
class BluetoothUtils(
    private val context: Context,
    private val activity: Activity
) {
    private var bluetoothAdapter: BluetoothAdapter? = null

    fun initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Timber.i("T_Debug: initBluetooth() >> ERROR. Bluetooth not supported.")
        }
        else {
            checkEnabled()
        }
    }

    private fun checkEnabled() {
        if (bluetoothAdapter?.isEnabled == false) {
            Timber.i("T_Debug: checkEnabled() >> Bluetooth not enabled.")
            TODO("Enable Bluetooth")
        }
        else {
            Timber.i("T_Debug: checkEnabled() >> Bluetooth is enabled.")
        }
    }

    fun discoverDevices() {
        //TO DO - query paired devices first?
        if (bluetoothAdapter == null) {
            Timber.i("T_Debug: discoverDevices() >> BluetoothAdapter is null, please call" +
                    "'initBluetooth()' and try again.")
        }
        else {
            Timber.i("T_Debug: discoverDevices() >> device discovery started.")
            bluetoothAdapter?.startDiscovery()
        }

    }
}