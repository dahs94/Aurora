package com.example.aurora

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.w3c.dom.Text
import timber.log.Timber

class ListViewAdapter(context: Context, deviceList: MutableList<BluetoothDevice>) : BaseAdapter() {

    private val mContext: Context = context
    private val mDeviceList: MutableList<BluetoothDevice> = deviceList

    /**Sets number of rows**/
    override fun getCount(): Int {
        return mDeviceList.size
    }

    /**Sets view for each row**/
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater: LayoutInflater = LayoutInflater.from(mContext)
        val rowView: View = layoutInflater.inflate(R.layout.listview_row, parent, false)

        val nameTextView: TextView = rowView.findViewById(R.id.device_name_header)
        val typeTextView: TextView = rowView.findViewById(R.id.device_type_header)

        nameTextView.text = mDeviceList[position].name
        typeTextView.text = mDeviceList[position].address

        return rowView
    }

    /**Returns list item at selected position**/
    override fun getItem(position: Int): Any {
        return mDeviceList[position]
    }

    /**Gets the ID for the item, e.g. 1, 2, 3 etc.**/
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}