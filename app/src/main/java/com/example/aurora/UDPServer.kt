package com.example.aurora

import timber.log.Timber
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket

class UDPServer() {
    private val port: Int = 4540
    private val dSocket: DatagramSocket = DatagramSocket(port)
    private val buffer: ByteArray = ByteArray(1024)
    private val dPacket: DatagramPacket = DatagramPacket(buffer, buffer.size)

    fun receive () {
        Thread() {
            Timber.i("T_Debug receive() >> ${Thread.currentThread()} started")
            while (true) {
                try {
                    dSocket.receive(dPacket)
                    Timber.i("T_Debug: receive() >> ${dPacket.data} from ${dPacket.address}.")
                } catch (exception: Exception) {
                    Timber.i("T_Debug: receive() >> $exception")
                }
            }
        }
    }
}