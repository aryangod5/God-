package com.example.god

import android.content.Context
import org.json.JSONArray

class MemoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("god_memory", Context.MODE_PRIVATE)

    fun save(text: String) {
        val all = getAll()
        if (all.toList().none { it == text }) all.put(text)
        prefs.edit().putString("items", all.toString()).apply()
    }

    fun getAll(): JSONArray =
        try { JSONArray(prefs.getString("items", "[]") ?: "[]") }
        catch (_: Exception) { JSONArray() }

    fun search(query: String): List<String> {
        val q = query.lowercase()
        val result = ArrayList<String>()
        val all = getAll()
        for (i in 0 until all.length()) {
            val value = all.optString(i)
            if (value.lowercase().contains(q)) result.add(value)
        }
        return result
    }

    fun clear() {
        prefs.edit().remove("items").apply()
    }

    fun delete(text: String) {
        val old = getAll()
        val next = JSONArray()
        for (i in 0 until old.length()) {
            val v = old.optString(i)
            if (v != text) next.put(v)
        }
        prefs.edit().putString("items", next.toString()).apply()
    }

    private fun JSONArray.toList(): List<String> {
        val out = ArrayList<String>()
        for (i in 0 until length()) out.add(optString(i))
        return out
    }
}
