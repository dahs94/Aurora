package com.example.aurora

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothUtils: BluetoothUtils
    private lateinit var audioRecorderUtils: AudioRecorderUtils
    private val p2pDeviceList: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var tipTextView: TextView
    private lateinit var speakingTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var listView: ListView
    private lateinit var peerName: String
    private lateinit var findDevicesButton: Button
    private lateinit var microphoneIB: ImageButton
    private lateinit var imageView: ImageView
    var connectionFormed: Boolean = false
    private var socket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        peerName = ""
        initResources()
        progressBar.visibility = View.GONE
        imageView.visibility = View.GONE
        speakingTextView.visibility = View.GONE
        bluetoothUtils = BluetoothUtils(this)
        bluetoothUtils.initBluetooth()
        bluetoothUtils.listenForPeer()
        //BluetoothUtils.disconnectGroup() //disconnect any existing groups on startup
        audioRecorderUtils = AudioRecorderUtils()
        audioRecorderUtils.initAudioRecording()
        initListeners()
    }

    private fun initResources() {
        tipTextView = findViewById(R.id.TipTextView)
        speakingTextView = findViewById(R.id.speakingTextView)
        progressBar = findViewById(R.id.progressBar)
        listView = findViewById(R.id.search_listview)
        findDevicesButton = findViewById(R.id.find_devices_button)
        microphoneIB = findViewById(R.id.speak_image_button)
        imageView = findViewById(R.id.conversation_IV)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        findDevicesButton.setOnClickListener {
            findDevices()
        }
        microphoneIB.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    handleIBPress()
                    return true
                }
                else if (event?.action == MotionEvent.ACTION_UP) {
                    handleIBRelease()
                    return true
                }
                else {
                    return false
                }
            }

        })
        listView.setOnItemClickListener { parent, view, position, _ ->
            handleSelect(parent.getItemAtPosition(position) as BluetoothDevice)
        }
    }

    private fun findDevices() {
        listView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(getString(R.string.dialog_message2))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                bluetoothUtils.cancelDiscovery()
                //wifiDirectUtils.disconnectGroup()
                connectionFormed = false
                tipTextView.text = getString(R.string.discovery_tip)
                p2pDeviceList.clear()
                bluetoothUtils.discoverDevices()
            }
            setNegativeButton("No") { dialog, _ ->
                progressBar.visibility = View.GONE
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

     fun onPeersAvailable(device: BluetoothDevice?) {
        if(device == null) {
            Timber.i("T_Debug: onPeersAvailable() >> p2pDeviceList is empty.")
        }
        else {
            if (device !in p2pDeviceList)
            {
                Timber.i("T_Debug: onPeersAvailable() >> updating ListView with new Bluetooth peers.")
                p2pDeviceList.add(device)
                listView.adapter = ListViewAdapter(this, p2pDeviceList)
            }

        }
    }

    private fun handleIBPress() {
        if (connectionFormed && socket != null) {
            speakingTextView.text = getString(R.string.transmit_audio)
            speakingTextView.visibility = View.VISIBLE
            imageView.visibility = View.VISIBLE
            //audioRecorderUtils.startRecording(socket!!)
        }
    }

    private fun handleIBRelease() {
        if (connectionFormed && socket != null) {
            speakingTextView.text = ""
            speakingTextView.visibility = View.GONE
            imageView.visibility = View.GONE
            audioRecorderUtils.stopRecording()
            audioRecorderUtils.getRecording(socket!!)
        }
    }

    private fun handleSelect(selectedItem: BluetoothDevice) {
        peerName = selectedItem.name
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(String.format(getString(R.string.dialog_message1), peerName))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                //Initiate connection to device as Bluetooth client.
                bluetoothUtils.cancelListening()
                bluetoothUtils.connectPeer(selectedItem)
                }

            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

     fun onConnectionAvailable(mySocket: BluetoothSocket) {
         Timber.i("T_Debug: onConnectionAvailable() >> connection succeeded.")
         connectionFormed = true
         listView.visibility = View.GONE
         progressBar.visibility = View.GONE
         val message: String = "\nYou are connected to: ${mySocket.remoteDevice.name}"
         tipTextView.text = String.format(getString(R.string.device_connected), message)
         tipTextView.text = String.format(getString(R.string.device_connected), message)
         //audioRecorderUtils.getRecording(mySocket)
     }

    override fun onResume() {
        super.onResume()
        registerReceiver(bluetoothUtils.bReceiver, bluetoothUtils.intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothUtils.bReceiver)
    }
}

