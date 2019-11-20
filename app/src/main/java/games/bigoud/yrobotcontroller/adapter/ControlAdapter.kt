package games.bigoud.yrobotcontroller.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import games.bigoud.yrobotcontroller.R
import games.bigoud.yrobotcontroller.RobotControl

class ControlAdapter(private val robotControls: ArrayList<RobotControl>, private val listener: ClickOnRecycler) : RecyclerView.Adapter<ControlAdapter.ControlViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ControlViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ControlViewHolder(inflater, parent, listener)
    }

    override fun getItemCount(): Int = robotControls.size

    override fun onBindViewHolder(holder: ControlViewHolder, position: Int) {
        val robotControl: RobotControl = robotControls[position]
        holder.bind(robotControl)
    }

    class ControlViewHolder(inflater: LayoutInflater, parent: ViewGroup, private val listener: ClickOnRecycler) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.control_adapter, parent, false)) {

        private var nameTextView: TextView? = null

        init {
            nameTextView = itemView.findViewById(R.id.controlName)
        }

        fun bind(robotControl: RobotControl) {
            nameTextView?.text = robotControl.name
            itemView.setOnClickListener {
                listener.controlClicked()
            }
        }

    }

    interface ClickOnRecycler {
        fun controlClicked()
    }

}