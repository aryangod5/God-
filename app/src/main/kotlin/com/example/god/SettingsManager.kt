package com.example.god

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("god_settings", Context.MODE_PRIVATE)

    var coreAnimation: Boolean
        get() = prefs.getBoolean("core_animation", true)
        set(v) = prefs.edit().putBoolean("core_animation", v).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", true)
        set(v) = prefs.edit().putBoolean("voice_enabled", v).apply()

    var gesturesEnabled: Boolean
        get() = prefs.getBoolean("gestures_enabled", false)
        set(v) = prefs.edit().putBoolean("gestures_enabled", v).apply()

    var webResearchEnabled: Boolean
        get() = prefs.getBoolean("web_research", true)
        set(v) = prefs.edit().putBoolean("web_research", v).apply()
}
