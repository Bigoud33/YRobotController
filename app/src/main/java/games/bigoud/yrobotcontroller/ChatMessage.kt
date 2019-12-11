package games.bigoud.yrobotcontroller

import java.text.SimpleDateFormat
import java.util.*

class ChatMessage(var device: String, var message: String) {
    var time: Date = Date()
    var sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    fun getTime(): String {
        return sdf.format(time)
    }

}