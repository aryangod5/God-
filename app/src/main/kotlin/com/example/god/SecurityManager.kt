package com.example.god

import android.content.Context
import java.security.MessageDigest

class SecurityManager(context: Context) {
    private val prefs = context.getSharedPreferences("god_security", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.getString("pin_hash", null) != null

    fun setPin(pin: String) {
        if (pin.length < 4) return
        prefs.edit().putString("pin_hash", hash(pin)).apply()
    }

    fun verify(pin: String): Boolean =
        prefs.getString("pin_hash", null) == hash(pin)

    fun removePin() {
        prefs.edit().remove("pin_hash").apply()
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
