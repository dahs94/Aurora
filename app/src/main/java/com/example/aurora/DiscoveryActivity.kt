package com.example.aurora

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

class DiscoveryActivity : AppCompatActivity() {

    private lateinit var wManager: WifiP2pManager
    private lateinit var wChannel: WifiP2pManager.Channel
    private lateinit var wReceiver: BroadcastReceiver
    lateinit var peerListener: WifiP2pManager.PeerListListener
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    private val intentFilter: IntentFilter = IntentFilter()
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()
    private var groupCreated: Boolean = false
    private val bundle: Bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discovery)
        setupWiFiDirect()
        initListeners()
    }

    private fun setupWiFiDirect() {
        wManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wChannel = wManager.initialize(this,mainLooper, null)
        wReceiver = WiFiDirectBroadcastReceiver(wManager, wChannel, this)

        intentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }

    private fun initListeners() {
        val findDevicesButton: ImageButton = findViewById(R.id.find_devices_magnifying_glass)
        findDevicesButton.setOnClickListener {
            p2pDeviceList.clear()
            discoverWiFiDevices()
        }

        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        peerListener = WifiP2pManager.PeerListListener() {
            onPeersAvailable(it)
        }

        connectionListener = WifiP2pManager.ConnectionInfoListener {
            onConnectionAvailable(it)
        }

        val listView: ListView = findViewById(R.id.search_listview)
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem: WifiP2pDevice = parent.getItemAtPosition(position) as WifiP2pDevice
            onDeviceSelected(selectedItem)
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverWiFiDevices() {
        val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
        wManager.discoverPeers(wChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                discoveryTipTextView.text = getString(R.string.peer_discovery)

            }
            override fun onFailure(reasonCode: Int) {
                discoveryTipTextView.text = String.format(getString(R.string.peer_discovery_failed),
                    reasonCode)
            }
        })
    }

    /**This will be called each time a new device is found, so we need to handle
     * the list correctly when a new device is added, this is why we set p2p device
     * list as a field in the class and not as a local variable in the function**/
    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        if (deviceList != p2pDeviceList)
        {
            p2pDeviceList.clear()
            for (device in deviceList.deviceList) {
                p2pDeviceList.add(device)
            }
        }

        val listView: ListView = findViewById(R.id.search_listview)
        listView.adapter = ListViewAdapter(this, p2pDeviceList)

        if(p2pDeviceList.isEmpty()) {
           val noDevicesTextView: TextView = findViewById(R.id.noDevicesTextView)
           val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
           listView.removeAllViews()
           noDevicesTextView.visibility = View.VISIBLE
           discoveryTipTextView.text = getString(R.string.discovery_tip_fin)
        }
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo){
        if (groupInfo.groupFormed) groupCreated = true

        //Navigate to MainActivity & pass device details if connection successful
        if (groupCreated){
            val intent: Intent = Intent(this, MainActivity::class.java)
            intent.putExtras(bundle)
            startActivity(Intent(intent))
        }
        else {
            val toast: Toast = Toast.makeText(applicationContext,
                "Connection failed, could not form group", Toast.LENGTH_LONG)
            toast.show()
            val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
            discoveryTipTextView.text = getString(R.string.discovery_tip)
        }
    }

    private fun onDeviceSelected(selectedItem: WifiP2pDevice) {
        //Connect to device
        bundle.putString("DEVICE_NAME", selectedItem.deviceName)
        //bundle.putBoolean("DEVICE_SELECTED", true)
        connectPeer(selectedItem)
    }

    private fun connectPeer(deviceSelected: WifiP2pDevice) {
        val deviceName: String = deviceSelected.deviceName
        val wifiPeerConfig: WifiP2pConfig = WifiP2pConfig()
        wifiPeerConfig.deviceAddress = deviceSelected.deviceAddress

        /*for now, make this device GO. Studies show that autonomous mode is faster
        for group creation (Oide et al). Right now, we're imitating a point to point
        connection with one device*/
        wifiPeerConfig.groupOwnerIntent = 15

        //Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            //Connect to peer
            wManager.connect(wChannel, wifiPeerConfig, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
                    discoveryTipTextView.text = "Connecting to $deviceName..."
                }

                override fun onFailure(reason: Int) {
                    val toast: Toast = Toast.makeText(DiscoveryActivity(),
                        "Connection to $deviceName failed: $reason",
                        Toast.LENGTH_LONG)
                    toast.show()
                    }
                })
        }
        else {
            val toast: Toast = Toast.makeText(this,
                "The LOCATION permission must be granted for Aurora to work.",
                Toast.LENGTH_LONG)
            toast.show()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wReceiver)
    }
}