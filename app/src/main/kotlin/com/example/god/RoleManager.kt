package com.example.god

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GodRole(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var instructions: String,
    var enabled: Boolean = true
)

class RoleManager(context: Context) {
    private val prefs = context.getSharedPreferences("god_roles", Context.MODE_PRIVATE)

    fun activeRole(): GodRole? {
        val id = prefs.getString("active_id", "").orEmpty()
        if (id.isBlank()) return null
        return all().firstOrNull { it.id == id && it.enabled }
    }

    fun all(): List<GodRole> {
        val out = mutableListOf<GodRole>()
        try {
            val a = JSONArray(prefs.getString("roles", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                out.add(GodRole(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = o.optString("name", "Custom role"),
                    instructions = o.optString("instructions"),
                    enabled = o.optBoolean("enabled", true)
                ))
            }
        } catch (_: Exception) {}
        return out
    }

    fun save(name: String, instructions: String, enabled: Boolean = true): GodRole {
        val roles = all().toMutableList()
        val role = GodRole(name = name.trim().ifBlank { "Custom role" }, instructions = instructions.trim(), enabled = enabled)
        roles.add(role)
        persist(roles)
        if (enabled) prefs.edit().putString("active_id", role.id).apply()
        return role
    }

    fun setActive(id: String?) {
        prefs.edit().putString("active_id", id.orEmpty()).apply()
    }

    fun delete(id: String) {
        persist(all().filterNot { it.id == id })
        if (prefs.getString("active_id", "") == id) prefs.edit().remove("active_id").apply()
    }

    fun clearActive() {
        prefs.edit().remove("active_id").apply()
    }

    private fun persist(roles: List<GodRole>) {
        val a = JSONArray()
        roles.forEach { r ->
            a.put(JSONObject().put("id", r.id).put("name", r.name).put("instructions", r.instructions).put("enabled", r.enabled))
        }
        prefs.edit().putString("roles", a.toString()).apply()
    }
}
