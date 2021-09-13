package com.example.aurora

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class AudioStreamSender(
    val host: String,
    val len: Int
) {
    private val port: Int = 4540
    private val socket: Socket = Socket()
    val buffer: ByteArray = ByteArray(1024)

    fun initSocket() {
        try {
            //Client socket
            socket.bind(null)
            socket.connect((InetSocketAddress(host, port)), 500)


        }
        catch (e: IOException) {

        }
    }

}