package games.bigoud.yrobotcontroller

import java.util.*

interface Constants {
    companion object {
        const val TAG = "Arduino - Android"
        const val REQUEST_ENABLE_BT = 1
        // message types sent from the BluetoothChatService Handler
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_SNACKBAR = 4
        // Constants that indicate the current connection state
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_ERROR = 1
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3 // now connected to a remote device
        val myUUID =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        // Key names received from the BluetoothChatService Handler
        const val EXTRA_DEVICE = "EXTRA_DEVICE"
        const val SNACKBAR = "toast"
    }
}