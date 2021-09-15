package com.example.aurora

import android.content.Intent
import android.net.wifi.p2p.*
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class DiscoveryActivity : AppCompatActivity() {

    private lateinit var wifiDirectUtils: WiFiDirectUtils
    lateinit var peerListener: WifiP2pManager.PeerListListener
    lateinit var connectionListener: WifiP2pManager.ConnectionInfoListener
    lateinit var groupInfoListener: WifiP2pManager.GroupInfoListener
    private lateinit var peerName: String
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discovery)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        wifiDirectUtils = WiFiDirectUtils(this, this)
        wifiDirectUtils.initWiFiDirect()
        initListeners()
    }

    private fun initListeners() {
        val findDevicesButton: ImageButton = findViewById(R.id.find_devices_magnifying_glass)
        findDevicesButton.setOnClickListener {
            val progressBar: ProgressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.VISIBLE
            p2pDeviceList.clear()
            wifiDirectUtils.stopDiscovery()
            discoverWiFiDevices()
        }

        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            wifiDirectUtils.stopDiscovery()
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
            connectToSelectedPeer(selectedItem)
        }
    }

    private fun discoverWiFiDevices() {
        val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
        wifiDirectUtils.initWiFiDiscovery()
        discoveryTipTextView.text = getString(R.string.peer_discovery)
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

    private fun onGroupAvailable(group: WifiP2pGroup) {
        /*Get name of connected device. At this time - we're only going to be connected
        to one device. We know that this device is either the GO or the client*/
        peerName = if (group.isGroupOwner) {
            //get the one any only client in the list
            val clientDevice: WifiP2pDevice = group.clientList.elementAt(0)
            clientDevice.deviceName
        } else
        {
            group.owner.deviceName
        }
    }

    private fun onConnectionAvailable(groupInfo: WifiP2pInfo){
        //Navigate to MainActivity & pass device details if connection successful
        if (groupInfo.groupFormed){
            val intent: Intent = Intent(this, MainActivity::class.java)
            val bundle: Bundle = Bundle()
            bundle.putString("DEVICE_NAME", peerName)
            bundle.putBoolean("GROUP_FORMED", true)
            if (groupInfo.isGroupOwner) {
                bundle.putBoolean("GROUP_OWNER", true)
            }
            else {
                bundle.putBoolean("GROUP_OWNER", false)
                var ipAddress: String = (groupInfo.groupOwnerAddress).toString()
                ipAddress = ipAddress.substring(1)
                bundle.putString("DEVICE_IP",ipAddress)
            }
            Timber.i("T_Debug: onConnectionAvailable() >> bundle details:" +
                    bundle.toString())
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

    private fun connectToSelectedPeer(deviceSelected: WifiP2pDevice) {
        wifiDirectUtils.connectPeer(deviceSelected)
        val deviceName: String = deviceSelected.deviceName

        val discoveryTipTextView: TextView = findViewById(R.id.discoveryTipTextView)
        discoveryTipTextView.text = "Connecting to ${deviceName.lowercase()}..."
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