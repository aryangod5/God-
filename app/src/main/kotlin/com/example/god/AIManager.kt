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

    init {
        // Remove legacy cloud-provider credentials from the active GOD configuration.
        prefs.edit().remove("endpoint").remove("key").remove("model").apply()
    }

    // Cloud/API configuration is intentionally not used by GOD.
    // These compatibility accessors return empty values so older project files still compile.
    fun endpoint() = ""
    fun model() = ""
    fun webResearchEnabled() = true
    fun conversationContext() = conversation
    fun roleManager() = roles

    // Kept only for source compatibility with older project files.
    // GOD does not store or use an AI provider endpoint, model, or API key.
    fun configure(endpoint: String, key: String, model: String = "", webResearch: Boolean = true) {
        prefs.edit().remove("endpoint").remove("key").remove("model")
            .putBoolean("web_research", true).apply()
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
            webResearchEnabled() -> applyRole(researchAnswer(q))
            else -> AIAnswer(applyRole(
                "I could not complete that request with the capabilities available on this device."
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
        // Local answers are attempted first. Anything not solved locally may be researched.
        return q.isNotBlank()
    }

    private fun researchAnswer(q: String): AIAnswer {
        val l = q.lowercase(Locale.getDefault())

        val generationIntent =
            l.contains("generate an image") || l.contains("create an image") ||
            l.contains("make an image") || l.contains("generate a picture") ||
            l.contains("create a video") || l.contains("generate a video") ||
            l.contains("make a video") || l.contains("3d character") ||
            l.contains("3d model") || l.contains("text to video") ||
            l.contains("text to image")

        val searchQuery = if (generationIntent) {
            when {
                l.contains("video") && l.contains("3d") ->
                    "$q free 3D character video generator no API"
                l.contains("video") ->
                    "$q free AI video generator no API"
                l.contains("3d") ->
                    "$q free 3D character generator no API"
                else ->
                    "$q free AI image generator no API"
            }
        } else q

        val web = research.research(searchQuery)
        if (web.sources.isEmpty()) {
            return AIAnswer(
                "I couldn't complete that request with the information currently available."
            )
        }

        val terms = searchQuery.lowercase().split(Regex("\\W+"))
            .filter { it.length >= 3 }.toSet()

        val chosen = web.sources
            .sortedByDescending { s ->
                terms.count { (s.title + " " + s.snippet).lowercase().contains(it) }
            }
            .take(5)

        val text = buildString {
            // Do not expose the internal research process.
            // For generation requests, automatically turn the results into recommendations.
            if (generationIntent) {
                append("I can't render that kind of high-quality media entirely on this phone without a generation model. ")
                append("I found these options that can handle the request:

")
            } else {
                append("Here is the most useful information I found:

")
            }

            chosen.forEachIndexed { i, s ->
                append("${i + 1}. ${s.title.ifBlank { "Result ${i + 1}" }}
")
                if (s.snippet.isNotBlank()) append(s.snippet.trim()).append('
')
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
