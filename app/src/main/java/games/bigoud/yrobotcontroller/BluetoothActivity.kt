package games.bigoud.yrobotcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.android.material.snackbar.Snackbar
import games.bigoud.yrobotcontroller.adapter.ChatAdapter
import kotlinx.android.synthetic.main.activity_bluetooth.*
import kotlinx.android.synthetic.main.content_bluetooth.*
import kotlinx.android.synthetic.main.empty_list_item.*
import java.lang.ref.WeakReference

class BluetoothActivity : AppCompatActivity() {
    var bluetoothService: BluetoothService? = null
    var device: BluetoothDevice? = null
    var reconnectButton: MenuItem? = null
    var chatAdapter: ChatAdapter? = null
    var snackTurnOn: Snackbar? = null
    private var showMessagesIsChecked = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        edit_text!!.error = "Enter text first"
        edit_text!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send_button.performClick()
                return@OnEditorActionListener true
            }
            false
        })
        snackTurnOn = Snackbar.make(coordinator_layout_bluetooth, "Bluetooth turned off", Snackbar.LENGTH_INDEFINITE)
            .setAction("Turn On") { enableBluetooth() }
        chatAdapter = ChatAdapter(this)
        chat_list_view!!.emptyView = empty_list_item
        chat_list_view!!.adapter = chatAdapter
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(toolbar)
        val handler =
            MyHandler(this@BluetoothActivity)
        assert(
            supportActionBar != null // won't be null, lint error
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        device = intent.extras.getParcelable(Constants.EXTRA_DEVICE)
        bluetoothService = BluetoothService(handler, device!!)
        title = device!!.name
        send_button.setOnClickListener {
            val message = edit_text!!.text.toString()
            if (message.trim { it <= ' ' }.isEmpty()) {
                edit_text!!.error = "Enter text first"
            } else {
                sendMessage(message)
                edit_text!!.setText("")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mReceiver, filter)
        bluetoothService?.connect()
        Log.d(Constants.TAG, "Connecting")
    }

    override fun onStop() {
        super.onStop()
        if (bluetoothService != null) {
            bluetoothService?.stop()
            Log.d(Constants.TAG, "Stopping")
        }
        unregisterReceiver(mReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setStatus("None")
            } else {
                setStatus("Error")
                Snackbar.make(
                    coordinator_layout_bluetooth,
                    "Failed to enable bluetooth",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Try Again") { enableBluetooth() }.show()
            }
        }
    }

    private fun sendMessage(message: String) { // Check that we're actually connected before trying anything
        if (Constants.STATE_CONNECTED != bluetoothService?.getState()  ) {
            Snackbar.make(coordinator_layout_bluetooth, "You are not connected", Snackbar.LENGTH_LONG)
                .setAction("Connect") { reconnect() }.show()
            return
        } else {
            val send = (message + "\n").toByteArray()
            bluetoothService?.write(send)
        }
    }

    private class MyHandler(activity: BluetoothActivity) : Handler() {
        private val mActivity: WeakReference<BluetoothActivity> = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    Constants.STATE_CONNECTED -> {
                        activity!!.setStatus("Connected")
                        activity.reconnectButton!!.isVisible = false
                        activity.toolbar_progress_bar!!.visibility = View.GONE
                    }
                    Constants.STATE_CONNECTING -> {
                        activity!!.setStatus("Connecting")
                        activity.toolbar_progress_bar!!.visibility = View.VISIBLE
                    }
                    Constants.STATE_NONE -> {
                        activity!!.setStatus("Not Connected")
                        activity.toolbar_progress_bar!!.visibility = View.GONE
                    }
                    Constants.STATE_ERROR -> {
                        activity!!.setStatus("Error")
                        activity.reconnectButton!!.isVisible = true
                        activity.toolbar_progress_bar!!.visibility = View.GONE
                    }
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = String(writeBuf)
                    val messageWrite = ChatMessage("Me", writeMessage)
                    activity!!.addMessageToAdapter(messageWrite)
                }
                Constants.MESSAGE_READ -> {
                    val readMessage = msg.obj as String
                    if (readMessage != null && activity!!.showMessagesIsChecked) {
                        val messageRead = ChatMessage(activity.device!!.name, readMessage.trim { it <= ' ' })
                        activity.addMessageToAdapter(messageRead)
                    }
                }
                Constants.MESSAGE_SNACKBAR -> Snackbar.make(
                    activity!!.coordinator_layout_bluetooth,
                    msg.data.getString(Constants.SNACKBAR),
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Connect"
                    ) { activity.reconnect() }.show()
            }
        }

    }

    private fun addMessageToAdapter(chatMessage: ChatMessage) {
        chatAdapter?.add(chatMessage)
        scrollChatListViewToBottom()
    }

    private fun scrollChatListViewToBottom() {
        chat_list_view!!.post {
            // Select the last row so it will scroll into view...
            chatAdapter?.count?.minus(1)?.let { chat_list_view!!.smoothScrollToPosition(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.bluetooth_menu, menu)
        reconnectButton = menu.findItem(R.id.action_reconnect)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                bluetoothService?.stop()
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.action_reconnect -> {
                reconnect()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> snackTurnOn?.show()
                    BluetoothAdapter.STATE_TURNING_ON -> if (snackTurnOn?.isShownOrQueued == true) snackTurnOn?.dismiss()
                    BluetoothAdapter.STATE_ON -> reconnect()
                }
            }
        }
    }

    private fun setStatus(status: String) {
        toolbar!!.subtitle = status
    }

    private fun enableBluetooth() {
        setStatus("Enabling Bluetooth")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT)
    }

    private fun reconnect() {
        reconnectButton!!.isVisible = false
        bluetoothService?.stop()
        bluetoothService?.connect()
    }

}