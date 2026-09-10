package com.example.god

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Short-term conversation context.
 * This is separate from permanent user-approved MemoryManager.
 */
class ConversationContextManager(context: Context) {
    private val prefs = context.getSharedPreferences("god_conversation_context", Context.MODE_PRIVATE)
    private val maxTurns = 24

    data class Turn(val role: String, val text: String)

    private val facts = linkedMapOf<String, String>()
    private val turns = ArrayDeque<Turn>()

    init {
        load()
    }

    @Synchronized
    fun addTurn(role: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        turns.addLast(Turn(role, clean))
        while (turns.size > maxTurns) turns.removeFirst()
        persist()
    }

    @Synchronized
    fun recentTurns(): List<Turn> = turns.toList()

    @Synchronized
    fun setFact(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        facts[key.trim().lowercase()] = value.trim()
        persist()
    }

    @Synchronized
    fun getFact(key: String): String? = facts[key.trim().lowercase()]

    @Synchronized
    fun factsSnapshot(): Map<String, String> = facts.toMap()

    @Synchronized
    fun clear() {
        facts.clear()
        turns.clear()
        prefs.edit().clear().apply()
    }

    @Synchronized
    private fun persist() {
        val f = JSONObject()
        facts.forEach { (k, v) -> f.put(k, v) }
        val t = JSONArray()
        turns.forEach {
            t.put(JSONObject().put("role", it.role).put("text", it.text))
        }
        prefs.edit().putString("facts", f.toString()).putString("turns", t.toString()).apply()
    }

    private fun load() {
        try {
            val f = JSONObject(prefs.getString("facts", "{}") ?: "{}")
            f.keys().forEach { key -> facts[key] = f.optString(key) }
            val t = JSONArray(prefs.getString("turns", "[]") ?: "[]")
            for (i in 0 until t.length()) {
                val o = t.optJSONObject(i) ?: continue
                val role = o.optString("role")
                val text = o.optString("text")
                if (role.isNotBlank() && text.isNotBlank()) turns.addLast(Turn(role, text))
            }
            while (turns.size > maxTurns) turns.removeFirst()
        } catch (_: Exception) {
            facts.clear()
            turns.clear()
        }
    }
}
