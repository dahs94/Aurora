package com.example.aurora

import timber.log.Timber
import java.io.IOException
import java.net.*
import kotlin.concurrent.thread

/**class UDPClient(
    private val destAddress: InetAddress,
) {
    private val port: Int = 4540
    private val dSocket: DatagramSocket = DatagramSocket()
    private val message: ByteArray = ("Hello World").toByteArray()
    private val dPacket: DatagramPacket = DatagramPacket(message, message.size, destAddress, port)

    fun send() {
        thread() {
            Timber.i("T_Debug: send() >> ${Thread.currentThread()} started")
            try {
                Timber.i("T_Debug: send() >> sending packet " +
                        "from localhost to $destAddress.")
                dSocket.send(dPacket)
            }
            catch (exception: IOException) {
                Timber.i("T_Debug: send() >> $exception.")
            }
            catch (exception: SecurityException) {
                Timber.i("T_Debug: send() >> $exception.")
            }
        }
    }
}**/