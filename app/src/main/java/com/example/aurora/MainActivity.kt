package com.example.aurora

import android.Manifest
import android.annotation.SuppressLint
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

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    private lateinit var audioRecorderUtils: AudioRecorderUtils
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    lateinit var peerListener: WifiP2pManager.PeerListListener
    private var peerDevice: PeerDevice? = null
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()
    private lateinit var tipTextView: TextView
    private lateinit var speakingTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var listView: ListView
    private lateinit var peerName: String
    private lateinit var findDevicesButton: Button
    private lateinit var microphoneIB: ImageButton
    private lateinit var imageView: ImageView
    var groupFormed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        peerName = ""
        initResources()
        progressBar.visibility = View.GONE
        imageView.visibility = View.GONE
        speakingTextView.visibility = View.GONE
        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        wifiDirectUtils.disconnectGroup() //disconnect any existing groups on startup
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
            handleSelect(parent.getItemAtPosition(position) as WifiP2pDevice)
        }
        peerListener = WifiP2pManager.PeerListListener() {
            onPeersAvailable(it)
        }
        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }
        groupInfoListener = WifiP2pManager.GroupInfoListener {
            //val clientDevice: WifiP2pDevice = it.clientList.elementAt(0)
            //clientDevice.deviceName
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
                wifiDirectUtils.stopDiscovery()
                wifiDirectUtils.disconnectGroup()
                groupFormed = false
                tipTextView.text = getString(R.string.discovery_tip)
                p2pDeviceList.clear()
                wifiDirectUtils.initWiFiDiscovery()
            }
            setNegativeButton("No") { dialog, _ ->
                progressBar.visibility = View.GONE
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        if(deviceList.deviceList.isEmpty() ) {
            Timber.i("T_Debug: onPeersAvailable() >> p2pDeviceList is empty.")
        }
        else {
            if (deviceList != p2pDeviceList)
            {
                p2pDeviceList.clear()
                for (device in deviceList.deviceList) {
                    p2pDeviceList.add(device)
                }
            }
            Timber.i("T_Debug: onPeersAvailable() >> updating ListView with new peers.")
            listView.adapter = ListViewAdapter(this, p2pDeviceList)
        }
    }

    private fun handleIBPress() {
        if (groupFormed && peerDevice != null) {
            speakingTextView.text = getString(R.string.transmit_audio)
            speakingTextView.visibility = View.VISIBLE
            imageView.visibility = View.VISIBLE
            audioRecorderUtils.startRecording(peerDevice!!)
        }
    }

    private fun handleIBRelease() {
        if (groupFormed && peerDevice != null) {
            speakingTextView.text = ""
            speakingTextView.visibility = View.GONE
            imageView.visibility = View.GONE
            audioRecorderUtils.stopRecording()
            audioRecorderUtils.getRecording()
        }
    }

    private fun handleSelect(selectedItem: WifiP2pDevice) {
        peerName = selectedItem.deviceName
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.apply {
            setMessage(String.format(getString(R.string.dialog_message1), peerName))
            setCancelable(true)
            setPositiveButton("Yes") { dialog, _ ->
                    //Connect to selected device
                    wifiDirectUtils.connectPeer(selectedItem)
                }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        (dialog.create()).show()
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo) {
        peerDevice = PeerDevice(groupInfo)
        groupFormed = true
        tipTextView.text = getString(R.string.onConnectedDevice)
        progressBar.visibility = View.VISIBLE
        /*
            Launch operation in a separate job. That job then blocks & waits for
            IO jobs to complete. We then handle the result in the main thread.
            we update the main thread first so give the user appropriate feedback
            during operation.
        */
        CoroutineScope(Dispatchers.Default).launch {
            CoroutineScope(Dispatchers.Default).async {
                peerDevice?.handleConnection()
                /*
                  delay() = delay job enough to get network details before changing
                  context before doing this, 'all jobs finished' log would always trigger
                  before handleConnection() finished, which would lead to an error. Not got
                  to grips with how to fix this properly. Perhaps adding another await()?
                */
                delay(5000)
            }.await()
            withContext(Dispatchers.Main) {
                Timber.i("T_Debug: onConnectionAvailable() >> handleConnection(): all jobs finished.")
                if (peerDevice != null) {
                    val peerIPAddress: String = peerDevice!!.getRemoteIPAddressString()
                    if (peerIPAddress == "9.9.9.9") {
                        /*
                         PeerDevice 'remoteIPAddress' property on default & has not been updated.
                         Issue with device connectivity.
                        */
                        var toastMessage: String = "Getting remote peer details failed, disconnecting from remote peer, " +
                                "please try again."
                        Timber.i("T_Debug: onConnectionAvailable() >> handleConnection() details " +
                                "not received, aborting.")
                        Toast.makeText(applicationContext,toastMessage,Toast.LENGTH_LONG).show()
                        wifiDirectUtils.disconnectGroup()
                        tipTextView.text = getString(R.string.discovery_tip)
                        groupFormed = false
                        progressBar.visibility = View.VISIBLE
                        tipTextView.text = getString(R.string.discovery_tip)
                    }
                    else {
                        Timber.i("T_Debug: onConnectionAvailable() >> connected to remote peer.")
                        var role: String = "Client"
                        if (peerDevice!!.getRole(groupInfo) == 1) {
                            role = "Group owner"
                        }
                        var message: String = "\nYou are connected to $peerIPAddress.\nThis device is the $role"
                        listView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        tipTextView.text = String.format(getString(R.string.device_connected), message)
                        audioRecorderUtils.getRecording()
                    }
                }
                else {
                    Timber.i("T_Debug: onConnectionAvailable() >> ERROR, peerDevice is null.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiDirectUtils.wReceiver, wifiDirectUtils.intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiDirectUtils.wReceiver)
    }
}

