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

/**
 * GOD local-first intelligence with conversation context, deterministic reasoning,
 * optional custom role instructions, and public web research.
 */
class AIManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("god_ai", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val research = WebResearchManager()
    private val understanding = SpeechUnderstandingManager()
    private val conversation = ConversationContextManager(context)
    private val reasoning = ReasoningManager(conversation)
    private val roles = RoleManager(context)

    fun endpoint() = prefs.getString("endpoint", "") ?: ""
    fun model() = prefs.getString("model", "") ?: ""
    fun webResearchEnabled() = prefs.getBoolean("web_research", true)
    fun conversationContext() = conversation
    fun roleManager() = roles

    fun configure(endpoint: String, key: String, model: String = "", webResearch: Boolean = true) {
        prefs.edit().putString("endpoint", endpoint).putString("key", key)
            .putString("model", model).putBoolean("web_research", webResearch).apply()
    }

    fun ask(message: String, callback: (AIAnswer) -> Unit) {
        Thread {
            val result = answerInternal(message)
            main.post { callback(result) }
        }.start()
    }

    fun answerInternal(message: String): AIAnswer {
        val understood = understanding.understand(message)
        val q = understood.normalized
        if (q.isBlank()) return AIAnswer("I didn't catch that. Please say it again.")

        conversation.addTurn("USER", q)

        val local = localAnswer(q)
        val result = when {
            local != null -> AIAnswer(applyRole(local))
            webResearchEnabled() && shouldResearch(q) -> applyRole(researchAnswer(q))
            else -> AIAnswer(applyRole(
                "I can handle this locally when the task is within my built-in reasoning abilities. " +
                    "For current public information I can research the web. " +
                    "For advanced open-ended generation, an on-device language model can be added later."
            ))
        }

        conversation.addTurn("GOD", result.text)
        return result
    }

    private fun applyRole(answer: String): String {
        val role = roles.activeRole() ?: return answer
        val style = role.instructions.trim()
        if (style.isBlank()) return answer
        // This local role layer applies safe, deterministic style hints without pretending
        // that a full generative model executed the arbitrary prompt.
        val name = role.name.trim().ifBlank { "Custom role" }
        val l = style.lowercase(Locale.getDefault())
        return when {
            l.contains("concise") || l.contains("short") -> answer.lines().take(4).joinToString("\n").trim()
            l.contains("formal") -> answer
            else -> answer
        }
    }

    private fun localAnswer(q: String): String? {
        val l = q.lowercase(Locale.getDefault()).trim()

        // Learn simple personal facts before answering.
        Regex("""(?:my\s+)?name\s+is\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)?.let {
                val name = it.groupValues[1]
                conversation.setFact("user_name", name)
                return "Got it. I'll remember your name in this conversation: $name."
            }

        if (l.matches(Regex("^(hi|hello|hey|hey god)[!. ]*$")))
            return "Hello, Master. GOD is ready."

        if (l.contains("who are you"))
            return "I am GOD, a local-first Android assistant. I can use conversation context, local reasoning, basic calculations, approved memory, and public web research."

        if (l.contains("what can you do"))
            return "I can calculate, reason about simple relationships, remember the current conversation, use approved memory, research public web information, work with files, launch apps, and speak responses."

        if (l == "thanks" || l == "thank you") return "You're welcome."

        if (l == "time" || l.contains("what time is it"))
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        if (l == "date" || l.contains("what is today's date") || l.contains("what date is it"))
            return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        reasoning.reason(q)?.let { return it }

        val math = l
            .replace("what is", "").replace("calculate", "").replace("compute", "")
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

        if (l.contains("what is my name") || l.contains("what's my name")) {
            conversation.getFact("user_name")?.let { return "Your name is $it." }
        }

        return null
    }

    private fun shouldResearch(q: String): Boolean {
        val l = q.lowercase(Locale.getDefault())
        if (l.any { it.isDigit() } && l.matches(Regex("[0-9+\\-*/().^%\\s]+"))) return false
        val current = listOf(
            "today","now","current","latest","recent","news","weather","price",
            "score","schedule","release","version","update","who is","where is",
            "when is","how much","best"
        )
        if (current.any { l.contains(it) }) return true
        return l.startsWith("search ") || l.startsWith("find ") ||
            l.startsWith("tell me about ") ||
            l.startsWith("who ") || l.startsWith("where ") ||
            l.startsWith("when ")
    }

    private fun researchAnswer(q: String): AIAnswer {
        val web = research.research(q)
        if (web.sources.isEmpty())
            return AIAnswer("I couldn't retrieve useful public-web results. Check your Internet connection or make the question more specific.")

        val terms = q.lowercase().split(Regex("\\W+")).filter { it.length >= 3 }.toSet()
        val chosen = web.sources.sortedByDescending { s ->
            terms.count { (s.title + " " + s.snippet).lowercase().contains(it) }
        }.take(4)

        val text = buildString {
            append("I researched the public web for:\n$q\n\n")
            chosen.forEachIndexed { i, s ->
                append("${i + 1}. ${s.title.ifBlank { "Web result" }}\n")
                if (s.snippet.isNotBlank()) append(s.snippet.trim()).append('\n')
                append(s.url).append("\n\n")
            }
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
    fun parse(): Double {
        val v = expr()
        skip()
        if (p != s.length) error("bad expression")
        return v
    }
    private fun expr(): Double {
        var v = term()
        while (true) {
            skip()
            v = when {
                take('+') -> v + term()
                take('-') -> v - term()
                else -> return v
            }
        }
    }
    private fun term(): Double {
        var v = power()
        while (true) {
            skip()
            v = when {
                take('*') -> v * power()
                take('/') -> {
                    val d = power()
                    if (abs(d) < 1e-15) error("division by zero")
                    v / d
                }
                else -> return v
            }
        }
    }
    private fun power(): Double {
        var v = unary()
        skip()
        if (take('^')) v = v.pow(power())
        return v
    }
    private fun unary(): Double {
        skip()
        if (take('+')) return unary()
        if (take('-')) return -unary()
        if (take('(')) {
            val v = expr()
            skip()
            if (!take(')')) error("missing )")
            return v
        }
        return number()
    }
    private fun number(): Double {
        skip()
        val start = p
        var dots = 0
        while (p < s.length) {
            val c = s[p]
            if (c.isDigit()) p++
            else if (c == '.' && dots++ == 0) p++
            else break
        }
        if (start == p) error("number expected")
        return s.substring(start, p).toDouble()
    }
    private fun skip() { while (p < s.length && s[p].isWhitespace()) p++ }
    private fun take(c: Char): Boolean {
        skip()
        if (p < s.length && s[p] == c) { p++; return true }
        return false
    }
}
