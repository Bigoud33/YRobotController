package games.bigoud.yrobotcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_device_list.*


class DeviceListActivity : AppCompatActivity() {

    var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var pairedDevices: Set<BluetoothDevice>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth Device Not Available", Toast.LENGTH_LONG).show()

            finish()
        } else if (!bluetoothAdapter!!.isEnabled) {
            var turnBTon = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(turnBTon, 1)
        }

        button.setOnClickListener {
            pairedDevicesList()
        }
    }

    fun pairedDevicesList() {
        pairedDevices = bluetoothAdapter!!.bondedDevices
        var list = ArrayList<String>()

        if (pairedDevices.isNotEmpty()) {
            for (bt in pairedDevices) {
                list.add(bt.name +"\n" + bt.address)
            }
        } else {
            Toast.makeText(this, "No paired Bluetooth Devices Found", Toast.LENGTH_LONG).show()
        }

        var adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        listView.adapter = adapter
        listView.setOnItemClickListener { av, v, arg2, arg3 ->
            val info = (v as TextView).text.toString()
            val address = info.substring(info.length - 17)

            // Make an intent to start next activity.
            val i = Intent(this, ControlListActivity::class.java)

            //Change the activity.
            i.putExtra(
                EXTRA_ADRESS,
                address
            ) //this will be received at ledControl (class) Activity
            startActivity(i)
        }
    }

    companion object {
        const val EXTRA_ADRESS = "device_adress"
    }
}

