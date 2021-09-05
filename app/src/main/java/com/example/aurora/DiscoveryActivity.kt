package com.example.aurora

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber


class DiscoveryActivity : AppCompatActivity() {

    private lateinit var wManager: WifiP2pManager
    private lateinit var wChannel: WifiP2pManager.Channel
    private lateinit var wReceiver: BroadcastReceiver
    lateinit var peerListener: WifiP2pManager.PeerListListener
    private val intentFilter: IntentFilter = IntentFilter()
    private val p2pDeviceList: MutableList<WifiP2pDevice> = mutableListOf()

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

        val listView: ListView = findViewById(R.id.search_listview)
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem: WifiP2pDevice = parent.getItemAtPosition(position) as WifiP2pDevice
            var bundle: Bundle = Bundle()
            bundle.putString("DEVICE_NAME", selectedItem.deviceName)
            bundle.putString("DEVICE_ADDRESS", selectedItem.deviceAddress)
            //bundle.putBoolean("DEVICE_SELECTED", true)
            val intent: Intent = Intent(this, MainActivity::class.java)
            intent.putExtras(bundle)
            startActivity(Intent(intent))
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

    override fun onResume() {
        super.onResume()
        registerReceiver(wReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wReceiver)
    }
}