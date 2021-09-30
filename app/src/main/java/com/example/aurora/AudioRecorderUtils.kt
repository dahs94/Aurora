package com.example.aurora

import android.media.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * Class to simplify config & management of AudioRecord class & utility functions:
 * https://developer.android.com/reference/kotlin/android/media/AudioRecord
 * The utilities in this class need to be run on a separate thread.
 */
class AudioRecorderUtils {

    /*AudioRecord constructors*/
    private val audioSource: Int = MediaRecorder.AudioSource.MIC

    /* The the next three constructors are set for compatibility. All three options
       (44100Hz, mono, 16-bit encoding) are listed as supported by all Android
       devices in the developer documentation for AudioRecord. */

    private val sampleRateInHz: Int = 44100
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes: Int = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat)
    private lateinit var recorder: AudioRecord
    private var isRecording: Boolean = false

    fun initAudioRecording() {
        recorder = AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
    }

    fun startRecording(peerDevice: PeerDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            val audioData: ByteArray = ByteArray(bufferSizeInBytes)
            var validRemoteIP: Boolean = false
            if (!isRecording && peerDevice.getRemoteIPAddressString() != "9.9.9.9") {
                isRecording = true
                validRemoteIP = true
                Timber.i("T_Debug: startRecording() >> recording started.")
                recorder.startRecording()
                //we need to start transmitting this recording here
            }
            else Timber.i("T_Debug: startRecording() >> cannot start recording, " +
                    "previous recording in progress or invalid remote IP.")
            while (isRecording && validRemoteIP) {
               /*read data from recorder into ByteArray, set the offset to 0. This overload method
                 supports 16 bit encoding */
               recorder.read(audioData, 0, audioData.size)
               //transmit array to remote peer, commonly accepted to use UDP to voice transmission
                var udpSocket: DatagramSocket = DatagramSocket()
                var udpPacket: DatagramPacket = DatagramPacket(audioData, audioData.size,
                    peerDevice.getRemoteIPAddress(), 4540)
                Timber.i("T_Debug: startRecording() >> transmitting audio packet to peer, " +
                        "${peerDevice.getRemoteIPAddressString()}.")
                udpSocket.send(udpPacket)
            }
        }
    }

    fun stopRecording(){
        if (isRecording) {
            Timber.i("T_Debug: stopRecording() >> recording stopped.")
            isRecording = false
            recorder.stop()
            //we need to stop transmitting the recording here
        }
        else Timber.i("T_Debug: stopRecording() >> could not stop recording, no recording in progress.")
    }










    /**
     * Retrieves the received recording and plays it. Uses the AudioTrack class.
     */
    fun getRecording() {
        val audioAttributesBuilder: AudioAttributes.Builder = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)

        val audioFormatBuilder: AudioFormat.Builder = AudioFormat.Builder()
            .setSampleRate(sampleRateInHz)
            .setEncoding(audioFormat)
            .setChannelMask(channelConfig)

        val audioAttributes: AudioAttributes = audioAttributesBuilder.build()
        val audioFormat: AudioFormat = audioFormatBuilder.build()

        val recording: AudioTrack = AudioTrack(audioAttributes, audioFormat, bufferSizeInBytes,
            AudioTrack.MODE_STREAM, 1)

        recording.play()
    }
}