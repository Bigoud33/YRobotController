package games.bigoud.yrobotcontroller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BluetoothService(handler: Handler, device: BluetoothDevice) {
    private val myHandler: Handler
    private var state: Int
    var myDevice: BluetoothDevice
    private var connectThread: ConnectThread? = null
    var connectedThread: ConnectedThread? = null
    @Synchronized
    fun connect() {
        Log.d(
            Constants.TAG,
            "Connecting to: " + myDevice.name + " - " + myDevice.address
        )
        // Start the thread to connect with the given device
        setState(Constants.STATE_CONNECTING)
        connectThread = ConnectThread(myDevice)
        connectThread!!.start()
    }

    @Synchronized
    fun stop() {
        cancelConnectThread()
        cancelConnectedThread()
        setState(Constants.STATE_NONE)
    }

    @Synchronized
    private fun setState(state: Int) {
        Log.d(Constants.TAG, "setState() " + this.state + " -> " + state)
        this.state = state
        // Give the new state to the Handler so the UI Activity can update
        myHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
    }

    @Synchronized
    fun getState(): Int {
        return state
    }

    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice) {
        Log.d(Constants.TAG, "connected to: " + device.name)
        cancelConnectThread()
        // Start the thread to manage the connection and perform transmissions
        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()
        setState(Constants.STATE_CONNECTED)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        Log.e(Constants.TAG, "Connection Failed")
        // Send a failure item_message back to the Activity
        val msg = myHandler.obtainMessage(Constants.MESSAGE_SNACKBAR)
        val bundle = Bundle()
        bundle.putString(Constants.SNACKBAR, "Unable to connect")
        msg.data = bundle
        myHandler.sendMessage(msg)
        setState(Constants.STATE_ERROR)
        cancelConnectThread()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        Log.e(Constants.TAG, "Connection Lost")
        // Send a failure item_message back to the Activity
        val msg = myHandler.obtainMessage(Constants.MESSAGE_SNACKBAR)
        val bundle = Bundle()
        bundle.putString(Constants.SNACKBAR, "Cconnection was lost")
        msg.data = bundle
        myHandler.sendMessage(msg)
        setState(Constants.STATE_ERROR)
        cancelConnectedThread()
    }

    private fun cancelConnectThread() { // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
    }

    private fun cancelConnectedThread() { // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
    }

    fun write(out: ByteArray?) { // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state != Constants.STATE_CONNECTED) {
                Log.e(Constants.TAG, "Trying to send but not connected")
                return
            }
            r = connectedThread
        }
        // Perform the write unsynchronized
        r!!.write(out)
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmDevice: BluetoothDevice
        override fun run() {
            try { // Connect the device through the socket. This will block
// until it succeeds or throws an exception
                mmSocket!!.connect()
            } catch (connectException: IOException) { // Unable to connect; close the socket and get out
                Log.e(Constants.TAG, "Unable to connect", connectException)
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.e(
                        Constants.TAG,
                        "Unable to close() socket during connection failure",
                        closeException
                    )
                }
                connectionFailed()
                return
            }
            synchronized(this@BluetoothService) { connectThread = null }
            // Do work to manage the connection (in a separate thread)
            connected(mmSocket, mmDevice)
        }

        /** Will cancel an in-progress connection, and close the socket  */
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(Constants.TAG, "Close() socket failed", e)
            }
        }

        init { // Use a temporary object that is later assigned to mmSocket,
// because mmSocket is final
            var tmp: BluetoothSocket? = null
            mmDevice = device
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try { // MY_UUID is the app's UUID string, also used by the server code
                val uuid = Constants.myUUID
                tmp = device.createRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e(Constants.TAG, "Create RFcomm socket failed", e)
            }
            mmSocket = tmp
        }
    }

    inner class ConnectedThread(private val mmSocket: BluetoothSocket?) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.i(Constants.TAG, "Begin connectedThread")
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes: Int // bytes returned from read()
            val readMessage = StringBuilder()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    val read = String(buffer, 0, bytes)
                    readMessage.append(read)
                    if (read.contains("\n")) {
                        myHandler.obtainMessage(
                            Constants.MESSAGE_READ,
                            bytes,
                            -1,
                            readMessage.toString()
                        ).sendToTarget()
                        readMessage.setLength(0)
                    }
                } catch (e: IOException) {
                    Log.e(Constants.TAG, "Connection Lost", e)
                    connectionLost()
                    break
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(bytes: ByteArray?) {
            try {
                mmOutStream!!.write(bytes)
                myHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, bytes).sendToTarget()
            } catch (e: IOException) {
                Log.e(Constants.TAG, "Exception during write", e)
            }
        }
        /* Call this from the main activity to shutdown the connection */
        /** Will cancel an in-progress connection, and close the socket  */
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(Constants.TAG, "close() of connect socket failed", e)
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            // Get the input and output streams, using temp objects because
// member streams are final
            try {
                tmpIn = mmSocket!!.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(Constants.TAG, "Temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    init {
        state = Constants.STATE_NONE
        myHandler = handler
        myDevice = device
    }
}