package com.example.aurora

import android.net.wifi.p2p.WifiP2pInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*
import java.lang.Exception
import java.net.*
import java.util.*
import java.util.stream.Collectors

/**
 * Represents the connected peer device that the user initiated or received a connection to/from.
 * N.B. the group only ever consists of two devices in this application.
 * Needs to be ran on a separate thread.
 */
class PeerDevice(private val groupInfo: WifiP2pInfo) {

     private var remoteIPAddress: InetAddress = InetAddress.getByName("9.9.9.9")

    /**
     * E.g.
     *      getRemoteIPAddress() = "192.168.49.150"
     * @returns the IP address of the remote peer in string format, with leading '/' removed.
     */
    //could improve this by just overriding toString() method: TO DO
    fun getRemoteIPAddressString(): String {
        return (remoteIPAddress.toString()).substring(1)
    }

    fun getRemoteIPAddress(): InetAddress {
        return remoteIPAddress
    }

    /**
     * Sets device to transmit or receive remote peer address based on role
     */
    fun handleConnection() {
        when (getRole(groupInfo)) {
            1 -> {
                /*
                the peer is the client, it will transmit it's address to us
                so we need to listen for this transmission
                */
                Timber.i("T_Debug: handleConnection() >> peer device is the client.")
                receiveClientIP()
            }
            2 -> {
                /*
                the peer is the GO, and is unaware of our IP address
                we need to get it's IP address, and then transmit our own
                */
                Timber.i("T_Debug: handleConnection() >> peer device is the group owner.")
                remoteIPAddress = getAddressIsGroupOwner(groupInfo)
                transmitClientIP()
            }
            else -> {
                Timber.i("T_Debug: handleConnection() >> getPeerAddress() failed" +
                        "unknown role integer.")
            }
        }
    }

    /**
     * Get whether the remote peer is a client or a GO.
     * @param groupInfo the groupInfo broadcast object received after connection
     * E.g. getRole(groupInfo) = 1 if GO
     * @returns either 1 or 2.
     *          1 = peer is the client
     *          2 = peer is the GO
     */
    fun getRole(groupInfo: WifiP2pInfo): Int {
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
        Timber.i("T_Debug: getGOIPAddress() >> result: $address.")
        return InetAddress.getByName(address)
    }

    /**
     * Transmits the client's IP address to the GO (runs if client)
     */
    private fun transmitClientIP() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.i("T_Debug transmitClientIP() >> ${Thread.currentThread()} started.")
            if (getRemoteIPAddressString() == "9.9.9.9") {
                Timber.i("T_Debug: transmitClientIP() >> remote IP address invalid.")

            }
            val port: Int = 4540
            val socket: Socket = Socket(remoteIPAddress, port)
            val message = "capuchin"
            try {
                Timber.i("T_Debug: transmitClientIP() >> sending '$message' " +
                        "to peer device (GO) with connection details.")
                socket.getOutputStream().write((message).toByteArray())
            }
            catch (exception: IOException) {
                Timber.i("T_Debug: transmitClientIP() >> $exception.")
            }
            catch (exception: SecurityException) {
                Timber.i("T_Debug: transmitClientIP() >> $exception.")
            }
            finally {
                socket.close()
        }}
    }

    /**
     * Waits to receive the client's IP address (runs if GO)
     */
    private fun receiveClientIP() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.i("T_Debug receiveClientIP() >> ${Thread.currentThread()} started.")
            val port: Int = 4540
            val socket: ServerSocket = ServerSocket(port)
            try {
                val clientTransmission = socket.accept()
                remoteIPAddress = clientTransmission.inetAddress
                var inputStream: InputStream = clientTransmission.getInputStream()
                var message: String = "capuchin"
                    //BufferedReader(InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"))
                    //temporarily disabled BufferedReader, because need Nougat for it to work
                Timber.i("T_Debug: receiveClientIP() >> " +
                        "$message " +
                        "received from client. Client IP is " +
                        remoteIPAddress.toString().substring(1) +
                        ", my IP address is 192.168.49.1.") //group owner address is always the same.
            } catch (exception: Exception) {
                Timber.i("T_Debug: receiveClientIP() >> $exception.")
            } finally {
                socket.close()
            }
        }
    }
}