package games.bigoud.yrobotcontroller.adapter

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import games.bigoud.yrobotcontroller.R

class BluetoothDevicesAdapter(context: Context) : ArrayAdapter<BluetoothDevice?>(context, 0) {
    // View lookup cache
    internal class ViewHolder(view: View?) {

        var name = view?.findViewById<TextView>(R.id.device_name)
        var address = view?.findViewById<TextView>(R.id.device_address)

    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View { // Get the data item for this position
        // Get the data item for this position
        var view = convertView
        val device = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.item_device, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        // Populate the data into the template view using the data object
        viewHolder.name?.text = device!!.name
        viewHolder.address?.text = device.address
        // Return the completed to render on screen
        return view!!
    }
}