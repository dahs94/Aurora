package com.example.aurora

import android.bluetooth.BluetoothSocket
import android.media.*
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Class to simplify config & management of AudioRecord class & utility functions:
 * https://developer.android.com/reference/kotlin/android/media/AudioRecord
 * The utilities in this class need to be run on a separate thread.
 */
class AudioRecorderUtils() {

    /*AudioRecord constructors*/
    private val audioSource: Int = MediaRecorder.AudioSource.MIC

    /* The the next three constructors are set for compatibility. All three options
       (44100Hz, mono, 16-bit encoding) are listed as supported by all Android
       devices in the developer documentation for AudioRecord. */

    private val sampleRateInHz: Int = 44100
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes: Int = 30000 //
    //AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat) * 100
    private lateinit var recorder: AudioRecord
    private var isRecording: Boolean = false

    fun initAudioRecording() {
        recorder = AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
    }

    /**
     * starts audio recording & transmits recording to remote peer
     * @Param socket, the already established Bluetooth socket.
     */
    fun startRecording(socket: BluetoothSocket) {
        CoroutineScope(Dispatchers.IO).launch {
            val audioData: ByteArray = ByteArray(bufferSizeInBytes)
            isRecording = true
            Timber.i("T_Debug: startRecording() >> recording started.")
            recorder.startRecording()
            while (isRecording) {
               /*read data from recorder into ByteArray, set the offset to 0. This overload method
                 supports 16 bit encoding */
               recorder.read(audioData, 0, audioData.size)
               //transmit array to remote peer, commonly accepted to use UDP to voice transmission
               Timber.i("T_Debug: startRecording() >> transmitting audio packet to peer, " +
                        "${socket.remoteDevice.name}.")
                try {
                    socket.outputStream.write(audioData)
                }
                catch (e: IOException) {
                    Timber.i("T_Debug: startRecording() >> $e")
                }
            }
        }
    }

    private fun talkingStick() {
        TODO("Implement talking stick: only one device can transmit at a time")
    }

    fun stopRecording(){
        if (isRecording) {
            Timber.i("T_Debug: stopRecording() >> recording stopped.")
            isRecording = false
            recorder.stop()
        }
        else Timber.i("T_Debug: stopRecording() >> could not stop recording, no recording in progress.")
    }

    /**
     * Retrieves the received recording from the Bluetooth socket and sends it to the speakers.
     * Guidance taken from here:
     * https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-data
     * @Param socket, the already established Bluetooth socket.
     */
    fun getRecording(socket: BluetoothSocket) {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.i("T_Debug: getRecording() >> listening for audio from remote peer...")
            var audioData: ByteArray = ByteArray(bufferSizeInBytes)
            val audioAttributesBuilder: AudioAttributes.Builder = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)

            val audioFormatBuilder: AudioFormat.Builder = AudioFormat.Builder()
                .setSampleRate(sampleRateInHz)
                .setEncoding(audioFormat)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)

            val audioAttributes: AudioAttributes = audioAttributesBuilder.build()
            val audioFormat: AudioFormat = audioFormatBuilder.build()

            val recording: AudioTrack = AudioTrack(
                audioAttributes, audioFormat, bufferSizeInBytes,
                AudioTrack.MODE_STREAM, 1
            )
            if (socket.isConnected) {
                recording.play()
                while (!isRecording) {
                    try {
                        socket.inputStream.read(audioData)
                    }
                    catch (e: IOException) {
                        Timber.i("T_Debug: startRecording() >> $e")
                    }
                    Timber.i("T_Debug: getRecording() >> playing audio received from ${socket.remoteDevice.name}.")
                    recording.write(audioData, 0, audioData.size)
                }
            }
            else {
                recording.stop()
            }
        }
    }
}