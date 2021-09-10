package com.example.aurora

import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Delay
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //handle bundle
        val deviceName: String? = intent.getStringExtra("DEVICE_NAME")
        //val deviceSelected: Boolean = intent.getBooleanExtra("DEVICE_SELECTED",
            //false)

        setContentView(R.layout.activity_main)
        initListeners()
        checkForPermissions()
        handleConnectedDevice(deviceName)
    }

    //access resource, create intent & start activity using intent
    private fun initListeners() {
        val findDevicesButton: Button = findViewById(R.id.find_devices_button)
        findDevicesButton.setOnClickListener {
            startActivity(Intent(this, DiscoveryActivity::class.java))
        }
    }

    /**
     * Request runtime permissions
     */
    private fun checkForPermissions() {
        //VERSION_CODES.M == SDK 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                PackageManager.PERMISSION_GRANTED -> {
                    Timber.i("T_Debug: checkForPermissions() >> location permission already granted")
                }
                else -> {
                    Timber.i("T_Debug: checkForPermissions() >> request location permission")
                    ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 0)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Timber.i("T_Debug: onRequestPermissionsResult() >> location permission denied")
            val toastMessage: String = String.format(getString(R.string.enable_location))
            val toast: Toast = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG)
            toast.show()
        }
        else {
            Timber.i("T_Debug: onRequestPermissionsResult() >> location permission granted")
        }
    }

    private fun handleConnectedDevice(deviceName: String?){
        if (deviceName != null) {
            val devNameTextView: TextView = findViewById(R.id.device_name_textview)
            devNameTextView.text = deviceName
        }
    }
}