package com.example.god

/** Maps gesture signals to semantic GOD commands. */
class GestureManager {
    enum class Command { WAKE, NEXT, PREVIOUS, SELECT, CANCEL }
    fun map(action: String): Command? = when (action) {
        "WAKE" -> Command.WAKE
        "SWIPE_RIGHT" -> Command.NEXT
        "SWIPE_LEFT" -> Command.PREVIOUS
        "SELECT" -> Command.SELECT
        "CANCEL" -> Command.CANCEL
        else -> null
    }
}
