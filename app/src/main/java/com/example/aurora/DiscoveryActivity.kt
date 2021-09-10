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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class DiscoveryActivity : AppCompatActivity() {

    private lateinit var wManager: WifiP2pManager
    private lateinit var wChannel: WifiP2pManager.Channel
    private lateinit var wReceiver: BroadcastReceiver
    lateinit var peerListener: WifiP2pManager.PeerListListener
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    private lateinit var peerName: String
    private val intentFilter: IntentFilter = IntentFilter()
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()
    private var groupCreated: Boolean = false
    private val bundle: Bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discovery)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
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
            val progressBar: ProgressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.VISIBLE
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

        groupInfoListener = WifiP2pManager.GroupInfoListener {
            onGroupAvailable(it)
        }

        val listView: ListView = findViewById(R.id.search_listview)
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem: WifiP2pDevice = parent.getItemAtPosition(position) as WifiP2pDevice
            connectPeer(selectedItem)
        }
    }

    private fun discoverWiFiDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Timber.i("T_Debug: DiscoverWifiDevices() >> Location permission already granted")
            val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
            wManager.discoverPeers(wChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    discoveryTipTextView.text = getString(R.string.peer_discovery)
                    CoroutineScope(Default).launch {
                        discoveryTimer()
                    }
                }
                override fun onFailure(reasonCode: Int) {
                    discoveryTipTextView.text = String.format(getString(R.string.peer_discovery_failed),
                        reasonCode)
                }
            })
        }
        else {
            Timber.i("T_Debug: DiscoverWifiDevices() >> Location permission missing")
            val toastMessage: String = String.format(getString(R.string.enable_location))
            val toast: Toast = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    suspend fun discoveryTimer() {
        withContext(Default) {
            val timeout: Long = 15000 //two minute timeout
            delay(timeout)
            //after delay, stop searching
            wManager.stopPeerDiscovery(wChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Timber.i("T_Debug: discoverPeer >> Discovery stopped")
                }

                override fun onFailure(reason: Int) {
                    Timber.i("T_Debug: discoverPeer >> Discovery stop failed")
                }
            })
        }
    }

    /**This will be called each time a new device is found, so we need to handle
     * the list correctly when a new device is added, this is why we set p2p device
     * list as a field in the class and not as a local variable in the function**/
    private fun onPeersAvailable(deviceList: WifiP2pDeviceList) {
        if(deviceList.deviceList.isEmpty() ) {
            Timber.i("T_Debug: onPeersAvailable() >> p2pDeviceList is empty")
            val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
            val progressBar: ProgressBar = findViewById(R.id.progressBar)

            discoveryTipTextView.text = getString(R.string.discovery_tip_fin)
            progressBar.visibility = View.GONE
        }
        else {
            if (deviceList != p2pDeviceList)
            {
                p2pDeviceList.clear()
                for (device in deviceList.deviceList) {
                    p2pDeviceList.add(device)
                }
            }

            val listView: ListView = findViewById(R.id.search_listview)
            Timber.i("T_Debug: onPeersAvailable() >> updating ListView with new peers")
            listView.adapter = ListViewAdapter(this, p2pDeviceList)
        }
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo){
        if (groupInfo.groupFormed) groupCreated = true

        //Navigate to MainActivity & pass device details if connection successful
        if (groupCreated){
            val intent: Intent = Intent(this, MainActivity::class.java)
            bundle.putString("DEVICE_NAME", peerName)
            intent.putExtras(bundle)
            startActivity(Intent(intent))
        }
        else {
            val toastMessage: String = String.format(getString(R.string.wd_connection_failed))
            val toast: Toast = Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG)
            toast.show()

            val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
            discoveryTipTextView.text = getString(R.string.discovery_tip)
        }
    }

    private fun onGroupAvailable(group: WifiP2pGroup) {
        /*At this time - we're only going to be connected to one device.
        We know this device is the owner, so we just need to get the first client.*/

        val clientDevice: WifiP2pDevice = group.clientList.elementAt(0)
        peerName = clientDevice.deviceName
    }

    private fun connectPeer(deviceSelected: WifiP2pDevice) {
        Timber.i("T_Debug: connectPeer() >> connecting to ${deviceSelected.deviceName} " +
                " - ${deviceSelected.deviceAddress}")
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
            Timber.i("T_Debug: connectPeer() >> location permission already granted")
            //Connect to peer
            wManager.connect(wChannel, wifiPeerConfig, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
                    discoveryTipTextView.text = "Connecting to ${deviceName.lowercase()}..."
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
            Timber.i("T_Debug: connectPeer() >> request location permission")
            val toastMessage: String = String.format(getString(R.string.enable_location))
            val toast: Toast = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG)
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