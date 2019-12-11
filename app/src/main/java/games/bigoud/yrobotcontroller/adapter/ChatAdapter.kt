package games.bigoud.yrobotcontroller.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import games.bigoud.yrobotcontroller.ChatMessage
import games.bigoud.yrobotcontroller.R

class ChatAdapter(context: Context) : ArrayAdapter<ChatMessage?>(context, 0) {
    // View lookup cache
    internal class ViewHolder(view: View?) {
        var time: TextView? = view?.findViewById(R.id.time_text_view)
        var device: TextView? = view?.findViewById(R.id.device_text_view)
        var message: TextView? = view?.findViewById(R.id.message_text_view)

    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View { // Get the data item for this position
        var view = convertView
        val chatMessage: ChatMessage? = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag
        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.item_message, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        // Populate the data into the template view using the data object
        viewHolder.time?.text = chatMessage?.getTime()
        viewHolder.device?.text = chatMessage?.device.plus(":")
        viewHolder.message?.text = chatMessage?.message
        // Return the completed to render on screen
        return view!!
    }
}