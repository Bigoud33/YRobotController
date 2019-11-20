package games.bigoud.yrobotcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import games.bigoud.yrobotcontroller.adapter.ControlAdapter
import kotlinx.android.synthetic.main.activity_control_list.*
import java.util.*

val myUUID = UUID.fromString("17238529-14392282-1145529-0885F965B47A25")

class ControlListActivity : AppCompatActivity(), ControlAdapter.ClickOnRecycler {

    var address =  ""
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var btSocket: BluetoothSocket
    var isBtConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        address = intent.getStringExtra(DeviceListActivity.EXTRA_ADRESS)
        setContentView(R.layout.activity_control_list)
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val robotControls = ArrayList<RobotControl>()
        robotControls.add(RobotControl("Avance"))
        robotControls.add(RobotControl("Fais un carré"))
        robotControls.add(RobotControl("Zigzag"))
        val adapter = ControlAdapter(robotControls, this)
        recyclerView.adapter = adapter
    }

    override fun controlClicked() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
