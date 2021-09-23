package com.example.aurora

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import timber.log.Timber
import java.net.InetAddress

class UDPConnection() {

    var groupOwner: Boolean = false
    var peerAddress: String? = "None"
    var peerName: String? = "None"
    var groupFormed: Boolean = false
    var client: UDPClient = UDPClient(InetAddress.getByName("127.0.0.1"))
    var server: UDPServer = UDPServer()

    fun getPeerName(group: WifiP2pGroup) {
        /*Get name of connected device. At this time - we're only going to be connected
        to one device. We know that this device is either the GO or the client*/
        peerName = if (group.isGroupOwner) {
            //get the one any only client in the list
            val clientDevice: WifiP2pDevice = group.clientList.elementAt(0)
            clientDevice.deviceName
        } else
        {
            group.owner.deviceName
        }
        Timber.i("T_Debug: getPeerName() >> result: $peerName")
    }

    fun receive() {
        if (groupOwner) {
            server = UDPServer()
            server.receive()
        }
        else Timber.i("T_Debug: receive() >> cannot receive, is client")
    }

    fun transmit() {
        if (groupFormed) {
            if (!groupOwner) {
                client = UDPClient(InetAddress.getByName(peerAddress))
                client.send()
            }
            else Timber.i("T_Debug: transmit() >> cannot transmit, is client")
        }
        else Timber.i("T_Debug: transmit() >> cannot transmit, group not formed")
    }
}