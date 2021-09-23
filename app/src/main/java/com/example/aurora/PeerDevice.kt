package com.example.aurora

import android.net.wifi.p2p.WifiP2pInfo
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import java.net.*

/**
 * Represents the connected peer device that the user initiated or received a connection to/from.
 * N.B. the group only ever consists of two devices in this application.
 */
class PeerDevice(private val groupInfo: WifiP2pInfo) {

     private var remoteIPAddress: InetAddress? = InetAddress.getByName("9.9.9.9")

    /**
     * Does work to make sure both devices know the IP address of the other device. In the
     * Wi-Fi Direct API, devices can only get the GO address when the group is formed. This
     * means that if the device is a GO, it relies on clients sending it a packet before
     * it knows their assigned P2p Ip address.
     * @param role the WifiP2p role of the device, e.g. 1 for client
     */

    fun getRemoteIPAddress(): String {
        return remoteIPAddress.toString()
    }

    fun initConnection() {
        when (getRole(groupInfo)) {
            1 -> {
                /*
                the peer is the client, it will transmit it's address to us
                so we need to listen for this transmission
                */
                receiveClientIP()
            }
            2 -> {
                /*
                the peer is the GO, and is unaware of our IP address
                we need to get it's IP address, and then transmit our own
                */
                remoteIPAddress = getAddressIsGroupOwner(groupInfo)
                transmitClientIP()
            }
            else -> {
                Timber.i("T_Debug: getPeerAddress() >> failed" +
                        "unknown role integer")
            }
        }
    }

    /**
     * Get whether the remote peer is a client or a GO.
     * @param groupInfo the groupInfo broadcast object received after connection
     * @returns either 1 or 2.
     *          1 = peer is the client
     *          2 = peer is the GO
     */
    private fun getRole(groupInfo: WifiP2pInfo): Int {
        return when (groupInfo.isGroupOwner) {
            true -> 1
            false -> 2
        }
    }

    /**
     * Get the IP address of the device if it is the group owner
     * @param groupInfo the groupInfo broadcast object received after connection
     * @param role the WifiP2p role of the device, e.g. 1 for client
     * @returns the IP address of the remote peer.
     */
    private fun getAddressIsGroupOwner(groupInfo: WifiP2pInfo): InetAddress {
        var address: String = (groupInfo.groupOwnerAddress).toString()
        address = address.substring(1)
        Timber.i("T_Debug: getGOIPAddress() >> result: $address")
        return InetAddress.getByName(address)
    }

    /**
     * Transmits the client's IP address to the GO (runs if client)
     */
    private fun transmitClientIP() {
        if (remoteIPAddress.toString() == "9.9.9.9") {
            Timber.i("T_Debug: transmitClientIP() >> remote IP address invalid")
            return
        }
        val port: Int = 4540
        val socket: Socket = Socket(remoteIPAddress, port)
        val message = "Echo 123"
        try {
            Timber.i("T_Debug: transmitClientIP() >> sending '$message' " +
                    "from localhost to GO: $remoteIPAddress")
            socket.getOutputStream().write((message).toByteArray())
        }
        catch (exception: IOException) {
            Timber.i("T_Debug: transmitClientIP() >> $exception")
        }
        catch (exception: SecurityException) {
            Timber.i("T_Debug: transmitClientIP() >> $exception")
        }
        finally {
            socket.close()
        }
    }

    /**
     * Waits to receive the client's IP address (runs if GO)
     */
    private fun receiveClientIP() {
        val port: Int = 4540
        val socket: ServerSocket = ServerSocket(port)
        try {
            val clientTransmission = socket.accept()
            remoteIPAddress = clientTransmission.inetAddress
            Timber.i("T_Debug: receiveClientIP() >> client IP is ${remoteIPAddress.toString()}")
        }
        catch (exception: Exception) {
            Timber.i("T_Debug: receiveClientIP() >> $exception")
        }
        finally {
            socket.close()
        }
    }
}