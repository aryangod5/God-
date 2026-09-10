package com.example.god

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/** Result returned to chat/voice. Sources are retained for future UI use but the answer is human-readable. */
data class AIAnswer(val text: String, val sources: List<WebSource> = emptyList())

/**
 * GOD's local-first intelligence layer.
 *
 * Order of work:
 * 1. Normalize messy speech/text locally.
 * 2. Check conversation facts and deterministic reasoning.
 * 3. Try local calculations and other simple answers.
 * 4. If needed, research the public web without an API key.
 * 5. Extract useful knowledge locally and rewrite it into a short, normal-user explanation.
 *
 * This file intentionally contains no active cloud provider/API-key path.
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
        prefs.edit()
            .remove("endpoint")
            .remove("key")
            .remove("model")
            .putBoolean("web_research", true)
            .apply()
    }

    // Compatibility methods for older project files. GOD does not use an API key.
    fun endpoint() = ""
    fun model() = ""
    fun webResearchEnabled() = true
    fun conversationContext() = conversation
    fun roleManager() = roles

    fun configure(endpoint: String, key: String, model: String = "", webResearch: Boolean = true) {
        // Deliberately ignore cloud credentials. Public research remains enabled.
        prefs.edit()
            .remove("endpoint")
            .remove("key")
            .remove("model")
            .putBoolean("web_research", true)
            .apply()
    }

    fun ask(message: String, callback: (AIAnswer) -> Unit) {
        Thread {
            val result = answerInternal(message)
            main.post { callback(result) }
        }.start()
    }

    fun answerInternal(message: String): AIAnswer {
        val understood = understanding.understand(message)
        val q = understood.normalized.trim()
        if (q.isBlank()) return AIAnswer("I didn't catch that. Please say it again.")

        conversation.addTurn("USER", q)

        val local = localAnswer(q)
        val result = if (local != null) {
            AIAnswer(applyRole(local))
        } else {
            val researched = researchAnswer(q)
            AIAnswer(applyRole(researched.text), researched.sources)
        }

        conversation.addTurn("GOD", result.text)
        return result
    }

    private fun applyRole(answer: String): String {
        val role = roles.activeRole() ?: return answer
        val style = role.instructions.trim()
        if (style.isBlank()) return answer

        val lower = style.lowercase(Locale.getDefault())
        return when {
            lower.contains("concise") || lower.contains("short") ->
                answer.lines().take(6).joinToString("\n").trim()
            else -> answer
        }
    }

    private fun localAnswer(q: String): String? {
        val l = q.lowercase(Locale.getDefault()).trim()

        Regex("""(?:my\s+)?name\s+is\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)?.let {
                val name = it.groupValues[1]
                conversation.setFact("user_name", name)
                return "Got it. I'll remember your name in this conversation: $name."
            }

        if (l.matches(Regex("^(hi|hello|hey|hey god)[!. ]*$")))
            return "Hello, Master. GOD is ready."

        if (l.contains("who are you"))
            return "I am GOD, a local-first Android assistant. I can use conversation context, local reasoning, calculations, approved memory, phone features, and public web research without requiring an API key."

        if (l.contains("what can you do"))
            return "I can calculate, reason about simple relationships, remember conversation facts, use approved memory, research public web information, work with files, launch apps, and speak responses."

        if (l == "thanks" || l == "thank you") return "You're welcome."

        if (l == "time" || l.contains("what time is it"))
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        if (l == "date" || l.contains("what is today's date") || l.contains("what date is it"))
            return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        // Relationship/context reasoning is intentionally attempted before web research.
        reasoning.reason(q)?.let { return it }

        val math = l
            .replace("what is", "")
            .replace("calculate", "")
            .replace("compute", "")
            .replace("solve", "")
            .replace("please", "")
            .replace("?", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("multiplied by", "*")
            .replace("times", "*")
            .replace("divided by", "/")
            .replace("to the power of", "^")
            .trim()

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

    private fun researchAnswer(q: String): AIAnswer {
        val result = research.research(q, 6)
        if (result.sources.isEmpty()) {
            return AIAnswer(
                "I couldn't find enough reliable public information to answer that right now. " +
                    "I can still try again when a connection is available."
            )
        }

        val summary = summarizeResearch(q, result)
        return AIAnswer(summary, result.sources)
    }

    /**
     * Local extractive/abstractive-style rewriting without a cloud model.
     * It does not dump raw search syntax or page HTML into the conversation.
     */
    private fun summarizeResearch(query: String, result: WebResearchResult): String {
        val rawBlocks = result.extractedText
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val candidates = ArrayList<String>()
        for (block in rawBlocks) {
            val page = block
                .replace(Regex("(?i)SOURCE\\s+\\d+"), "")
                .replace(Regex("(?i)TITLE:\\s*[^\\n]+"), "")
                .replace(Regex("(?i)URL:\\s*https?://[^\\s]+"), "")
                .replace(Regex("(?i)SNIPPET:"), "")
                .replace(Regex("(?i)PAGE TEXT:"), "")
                .trim()

            val sentences = splitSentences(page)
            candidates += sentences
        }

        val queryTerms = query
            .lowercase(Locale.getDefault())
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .distinct()

        val selected = candidates
            .map { sentence -> sentence to scoreSentence(sentence, queryTerms) }
            .filter { it.first.length >= 35 }
            .sortedByDescending { it.second }
            .distinctBy { normalizeForDuplicateCheck(it.first) }
            .take(7)
            .map { cleanForUser(it.first) }
            .filter { it.length >= 30 }

        val sourceNames = result.sources.take(3).map { cleanForUser(it.title) }.filter { it.isNotBlank() }

        if (selected.isEmpty()) {
            val snippets = result.sources
                .map { cleanForUser(it.snippet) }
                .filter { it.isNotBlank() }
                .take(5)
            if (snippets.isEmpty()) return "I found sources, but they did not provide enough readable information to summarize."
            return buildString {
                append("Here’s the clearest summary I could make from the public information I found:\n\n")
                snippets.forEachIndexed { i, s ->
                    append("• ").append(s).append(if (i == snippets.lastIndex) "" else "\n")
                }
            }.trim()
        }

        return buildString {
            append("Here’s the answer in simple terms:\n\n")
            selected.forEach { sentence ->
                append("• ").append(sentence).append('\n')
            }
            if (sourceNames.isNotEmpty()) {
                append("\nI checked public information from: ")
                append(sourceNames.joinToString(", "))
                append('.')
            }
        }.trim()
    }

    private fun splitSentences(text: String): List<String> {
        return text
            .replace(Regex("\\s+"), " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length >= 20 }
    }

    private fun scoreSentence(sentence: String, terms: List<String>): Int {
        val lower = sentence.lowercase(Locale.getDefault())
        var score = 0
        for (term in terms) if (lower.contains(term)) score += 3
        if (sentence.length in 55..260) score += 2
        if (lower.contains("according to") || lower.contains("research") || lower.contains("study")) score += 1
        if (sentence.count { it == ':' || it == '{' || it == '}' || it == '<' || it == '>' } > 2) score -= 3
        return score
    }

    private fun cleanForUser(value: String): String {
        return value
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^[\\-–—•*]+\\s*"), "")
            .trim()
    }

    private fun normalizeForDuplicateCheck(value: String): String {
        return value.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "").take(220)
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
        return when {
            take('+') -> unary()
            take('-') -> -unary()
            else -> numberOrParen()
        }
    }

    private fun numberOrParen(): Double {
        skip()
        if (take('(')) {
            val v = expr()
            skip()
            if (!take(')')) error("missing )")
            return v
        }
        val start = p
        while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++
        if (start == p) error("number expected")
        return s.substring(start, p).toDouble()
    }

    private fun take(ch: Char): Boolean {
        if (p < s.length && s[p] == ch) {
            p++
            return true
        }
        return false
    }

    private fun skip() {
        while (p < s.length && s[p].isWhitespace()) p++
    }
}
