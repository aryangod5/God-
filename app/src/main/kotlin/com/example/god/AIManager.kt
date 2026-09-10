package com.example.god

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

data class AIAnswer(val text: String, val sources: List<WebSource> = emptyList())

/** Local-first GOD intelligence. No API key is required for basic tasks. */
class AIManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("god_ai", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val research = WebResearchManager()

    fun endpoint() = prefs.getString("endpoint", "") ?: ""
    fun model() = prefs.getString("model", "") ?: ""
    fun webResearchEnabled() = prefs.getBoolean("web_research", true)

    fun configure(endpoint: String, key: String, model: String = "", webResearch: Boolean = true) {
        prefs.edit().putString("endpoint", endpoint).putString("key", key)
            .putString("model", model).putBoolean("web_research", webResearch).apply()
    }

    fun ask(message: String, callback: (AIAnswer) -> Unit) {
        Thread {
            val result = try {
                val q = message.trim()
                val local = localAnswer(q)
                when {
                    local != null -> AIAnswer(local)
                    webResearchEnabled() && shouldResearch(q) -> researchAnswer(q)
                    endpoint().isNotBlank() -> AIAnswer("An external AI provider is configured, but this local-first build does not send requests automatically. Use the provider screen to enable that integration.")
                    else -> AIAnswer("I can handle basic tasks locally. For current or public information I can research the web. For open-ended generative reasoning, an optional AI provider is required.")
                }
            } catch (e: Exception) { AIAnswer("GOD error: ${e.message ?: "unknown error"}") }
            main.post { callback(result) }
        }.start()
    }

    private fun localAnswer(q: String): String? {
        val l = q.lowercase(Locale.getDefault()).trim()
        if (l.matches(Regex("^(hi|hello|hey|hey god)[!. ]*$"))) return "Hello, Master. GOD is ready."
        if (l.contains("who are you")) return "I am GOD, a local-first Android assistant. I calculate and handle basic tasks on-device, and use public web research when current information is needed."
        if (l.contains("what can you do")) return "I can calculate, convert units, answer simple commands locally, research public web information, use memory, launch apps, work with approved files, and speak responses."
        if (l == "thanks" || l == "thank you") return "You're welcome."
        if (l == "time" || l.contains("what time is it")) return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        if (l == "date" || l.contains("what is today's date") || l.contains("what date is it")) return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        val math = l.replace("what is", "").replace("calculate", "").replace("compute", "")
            .replace("solve", "").replace("please", "").replace("?", "")
            .replace("×", "*").replace("÷", "/").replace("plus", "+")
            .replace("minus", "-").replace("multiplied by", "*").replace("times", "*")
            .replace("divided by", "/").replace("to the power of", "^").trim()
        if (math.any { it.isDigit() } && math.matches(Regex("[0-9+\\-*/().^%\\s]+"))) {
            return try { "The answer is ${format(ExpressionParser(math).parse())}." } catch (_: Exception) { null }
        }
        Regex("^([0-9.]+)\\s*%\\s*of\\s*([0-9.]+)$").find(l)?.let {
            return "The answer is ${format(it.groupValues[1].toDouble() * it.groupValues[2].toDouble() / 100.0)}."
        }
        return null
    }

    private fun shouldResearch(q: String): Boolean {
        val l = q.lowercase(Locale.getDefault())
        if (l.any { it.isDigit() } && l.matches(Regex("[0-9+\\-*/().^%\\s]+"))) return false
        val current = listOf("today","now","current","latest","recent","news","weather","price","score","schedule","release","version","update","who is","where is","when is","how much","best")
        if (current.any { l.contains(it) }) return true
        return l.startsWith("who ") || l.startsWith("what is ") || l.startsWith("what's ") ||
            l.startsWith("where ") || l.startsWith("when ") || l.startsWith("how ") ||
            l.startsWith("tell me about ") || l.startsWith("search ") || l.startsWith("find ")
    }

    private fun researchAnswer(q: String): AIAnswer {
        val web = research.research(q)
        if (web.sources.isEmpty()) return AIAnswer("I couldn't retrieve useful public-web results. Check your Internet connection or make the question more specific.")
        val terms = q.lowercase().split(Regex("\\W+")).filter { it.length >= 3 }.toSet()
        val chosen = web.sources.sortedByDescending { s -> terms.count { (s.title + " " + s.snippet).lowercase().contains(it) } }.take(4)
        val text = buildString {
            append("I researched the public web for:\n$q\n\n")
            chosen.forEachIndexed { i, s ->
                append("${i + 1}. ${s.title.ifBlank { "Web result" }}\n")
                if (s.snippet.isNotBlank()) append(s.snippet.trim()).append('\n')
                append(s.url).append("\n\n")
            }
            append("These are public-web results. No API key or external AI model was used for this answer.")
        }.trim()
        return AIAnswer(text, chosen)
    }

    private fun format(v: Double): String {
        if (!v.isFinite()) return "undefined"
        if (abs(v - v.toLong()) < 1e-10) return v.toLong().toString()
        return "%.8f".format(Locale.US, v).trimEnd('0').trimEnd('.')
    }
}

private class ExpressionParser(private val s: String) {
    private var p = 0
    fun parse(): Double { val v = expr(); skip(); if (p != s.length) error("bad expression"); return v }
    private fun expr(): Double { var v = term(); while (true) { skip(); v = when { take('+') -> v + term(); take('-') -> v - term(); else -> return v } } }
    private fun term(): Double { var v = power(); while (true) { skip(); v = when { take('*') -> v * power(); take('/') -> { val d=power(); if(abs(d)<1e-15) error("division by zero"); v/d }; else -> return v } } }
    private fun power(): Double { var v = unary(); skip(); if (take('^')) v = v.pow(power()); return v }
    private fun unary(): Double { skip(); if(take('+')) return unary(); if(take('-')) return -unary(); if(take('(')){val v=expr(); skip(); if(!take(')')) error("missing )"); return v}; return number() }
    private fun number(): Double { skip(); val start=p; var dots=0; while(p<s.length){val c=s[p]; if(c.isDigit()) p++ else if(c=='.'&&dots++==0)p++ else break}; if(start==p)error("number expected"); return s.substring(start,p).toDouble() }
    private fun skip(){while(p<s.length&&s[p].isWhitespace())p++}
    private fun take(c:Char):Boolean{skip(); if(p<s.length&&s[p]==c){p++;return true};return false}
}
