package com.example.god

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    var text: String,
    val time: Long = System.currentTimeMillis()
)

data class ChatConversation(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val created: Long = System.currentTimeMillis(),
    var updated: Long = System.currentTimeMillis(),
    val messages: MutableList<ChatMessage> = mutableListOf()
)

class ChatHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("god_chat_history", Context.MODE_PRIVATE)
    private val conversations = mutableListOf<ChatConversation>()

    init { load() }

    @Synchronized
    fun all(): List<ChatConversation> = conversations.sortedByDescending { it.updated }.map { it.copy(messages = it.messages.toMutableList()) }

    @Synchronized
    fun create(title: String = "New conversation"): ChatConversation {
        val c = ChatConversation(title = title)
        conversations.add(c)
        persist()
        return c
    }

    @Synchronized
    fun find(id: String): ChatConversation? = conversations.firstOrNull { it.id == id }

    @Synchronized
    fun addMessage(conversationId: String, role: String, text: String): ChatMessage? {
        val c = conversations.firstOrNull { it.id == conversationId } ?: return null
        val m = ChatMessage(role = role, text = text)
        c.messages.add(m)
        c.updated = System.currentTimeMillis()
        if (c.title == "New conversation" && role == "USER") c.title = makeTitle(text)
        persist()
        return m
    }

    @Synchronized
    fun updateMessage(conversationId: String, messageId: String, newText: String): Boolean {
        val c = conversations.firstOrNull { it.id == conversationId } ?: return false
        val m = c.messages.firstOrNull { it.id == messageId } ?: return false
        m.text = newText.trim()
        c.updated = System.currentTimeMillis()
        persist()
        return true
    }

    @Synchronized
    fun deleteMessage(conversationId: String, messageId: String): Boolean {
        val c = conversations.firstOrNull { it.id == conversationId } ?: return false
        val removed = c.messages.removeIf { it.id == messageId }
        if (removed) {
            c.updated = System.currentTimeMillis()
            persist()
        }
        return removed
    }

    @Synchronized
    fun deleteConversation(id: String): Boolean {
        val removed = conversations.removeIf { it.id == id }
        if (removed) persist()
        return removed
    }

    @Synchronized
    fun renameConversation(id: String, title: String) {
        conversations.firstOrNull { it.id == id }?.let {
            it.title = title.trim().ifBlank { "Conversation" }
            it.updated = System.currentTimeMillis()
            persist()
        }
    }

    @Synchronized
    fun clearAll() {
        conversations.clear()
        persist()
    }

    private fun makeTitle(text: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= 32) clean else clean.take(29) + "..."
    }

    private fun persist() {
        val array = JSONArray()
        conversations.forEach { c ->
            val o = JSONObject()
                .put("id", c.id).put("title", c.title)
                .put("created", c.created).put("updated", c.updated)
            val msgs = JSONArray()
            c.messages.forEach { m ->
                msgs.put(JSONObject().put("id", m.id).put("role", m.role).put("text", m.text).put("time", m.time))
            }
            o.put("messages", msgs)
            array.put(o)
        }
        prefs.edit().putString("conversations", array.toString()).apply()
    }

    private fun load() {
        try {
            val array = JSONArray(prefs.getString("conversations", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val c = ChatConversation(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    title = o.optString("title", "Conversation"),
                    created = o.optLong("created", System.currentTimeMillis()),
                    updated = o.optLong("updated", System.currentTimeMillis())
                )
                val msgs = o.optJSONArray("messages") ?: JSONArray()
                for (j in 0 until msgs.length()) {
                    val m = msgs.optJSONObject(j) ?: continue
                    c.messages.add(ChatMessage(
                        id = m.optString("id").ifBlank { UUID.randomUUID().toString() },
                        role = m.optString("role"),
                        text = m.optString("text"),
                        time = m.optLong("time", System.currentTimeMillis())
                    ))
                }
                conversations.add(c)
            }
        } catch (_: Exception) {
            conversations.clear()
        }
    }

    private fun ChatConversation.copy(messages: MutableList<ChatMessage>): ChatConversation =
        ChatConversation(id, title, created, updated, messages)
}
