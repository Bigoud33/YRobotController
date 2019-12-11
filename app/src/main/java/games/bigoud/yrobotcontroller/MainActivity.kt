package games.bigoud.yrobotcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import games.bigoud.yrobotcontroller.adapter.BluetoothDevicesAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.empty_list_item.*

class MainActivity : AppCompatActivity() {

    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothDevicesAdapter: BluetoothDevicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        setStatus("None")

        bluetoothDevicesAdapter = BluetoothDevicesAdapter(this)

        devices_list_view.adapter = bluetoothDevicesAdapter
        devices_list_view.emptyView = empty_list_item

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.e(Constants.TAG, "Device has no bluetooth")
            AlertDialog.Builder(this@MainActivity)
                .setCancelable(false)
                .setTitle("No Bluetooth")
                .setMessage("Your device has no bluetooth")
                .setPositiveButton("Close app"
                ) { _, _ ->
                    Log.d(Constants.TAG, "App closed")
                    finish()
                }.show()
        }

        search_button.setOnClickListener {
            if (bluetoothAdapter.isEnabled) { // Bluetooth enabled
                startSearching()
            } else {
                enableBluetooth()
            }
        }

        devices_list_view.setOnItemClickListener { parent, view, position, id ->
            setStatus("Asking to connect")
            val device = bluetoothDevicesAdapter.getItem(position)

            AlertDialog.Builder(this@MainActivity)
                .setCancelable(false)
                .setTitle("Connect")
                .setMessage("Do you want to connect to: " + device!!.name + " - " + device.address)
                .setPositiveButton("Connect"
                ) { dialog, which ->
                    Log.d(Constants.TAG, "Opening new Activity")
                    bluetoothAdapter.cancelDiscovery()
                    toolbar_progress_bar.visibility = View.INVISIBLE
                    val intent =
                        Intent(this@MainActivity, BluetoothActivity::class.java)
                    intent.putExtra(Constants.EXTRA_DEVICE, device)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel"
                ) { dialog, which ->
                    setStatus("Cancelled connection")
                    Log.d(Constants.TAG, "Cancelled ")
                }.show()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(Constants.TAG, "Registering receiver")
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        Log.d(Constants.TAG, "Receiver unregistered")
        unregisterReceiver(mReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startSearching()
            } else {
                setStatus("Error")
                Snackbar.make(
                    coordinator_layout_main,
                    "Failed to enable bluetooth",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Try Again") { enableBluetooth() }.show()
            }
        }
    }

    private fun setStatus(status: String) {
        toolbar.subtitle = status
    }

    private fun startSearching() {
        if (bluetoothAdapter.startDiscovery()) {
            toolbar_progress_bar.visibility = View.VISIBLE
            setStatus("Searching for devices")
        } else {
            setStatus("Error")
            Snackbar.make(
                coordinator_layout_main,
                "Failed to start searching",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("Try Again") { startSearching() }.show()
        }
    }

    private fun enableBluetooth() {
        setStatus("Enabling Bluetooth")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT)
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) { // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device.fetchUuidsWithSdp()
                if (bluetoothDevicesAdapter.getPosition(device) == -1) { // -1 is returned when the item is not in the adapter
                    bluetoothDevicesAdapter.add(device)
                    bluetoothDevicesAdapter.notifyDataSetChanged()
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                toolbar_progress_bar.visibility = View.INVISIBLE
                setStatus("None")
            } else if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> Snackbar.make(
                        coordinator_layout_main,
                        "Bluetooth turned off",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("Turn on"
                        ) { enableBluetooth() }.show()
                }
            }
        }
    }

}
