package com.example.aurora

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothBroadcastReceiver(
    private val activity: Activity
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                if (activity is MainActivity) {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    activity.onPeersAvailable(device)
                }
            }
        }
    }
}