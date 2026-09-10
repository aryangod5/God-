package com.example.god

import java.util.Locale

/**
 * Lightweight deterministic reasoning for common multi-step questions.
 * No network or API key is required.
 */
class ReasoningManager(private val context: ConversationContextManager) {

    fun reason(question: String): String? {
        val q = question.trim()
        if (q.isBlank()) return null
        val l = q.lowercase(Locale.getDefault())

        // Direct personal-context questions.
        if (l.matches(Regex(".*\\b(my|what is my)\\s+name\\b.*")) ||
            l.contains("what's my name") || l.contains("what is my name")) {
            val name = context.getFact("user_name") ?: return null
            return "Your name is $name."
        }

        // "What did I say..." style recall.
        if (l.contains("what did i tell you") || l.contains("what did i say")) {
            val name = context.getFact("user_name")
            if (name != null && l.contains("name")) return "You told me your name is $name."
        }

        // Extract simple family facts from the current message.
        extractFacts(q)

        // Direct relation question using known names.
        val relation = familyQuestion(q)
        if (relation != null) return relation

        // Generic "who is X to Y?" using stored relations.
        val whoTo = Regex("""who\s+is\s+([A-Za-z][A-Za-z0-9_-]*)\s+(?:to|for)\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)
        if (whoTo != null) {
            val a = whoTo.groupValues[1]
            val b = whoTo.groupValues[2]
            return inferRelation(a, b)
        }

        return null
    }

    private fun extractFacts(q: String) {
        Regex("""(?:my\s+)?name\s+is\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)?.let { context.setFact("user_name", it.groupValues[1]) }

        Regex("""my\s+son(?:'s)?\s+name\s+is\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)?.let { context.setFact("user_son", it.groupValues[1]) }

        Regex("""my\s+daughter(?:'s)?\s+name\s+is\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)?.let { context.setFact("user_daughter", it.groupValues[1]) }

        Regex("""([A-Za-z][A-Za-z0-9_-]*)\s+is\s+my\s+(son|daughter|father|mother|brother|sister)""", RegexOption.IGNORE_CASE)
            .find(q)?.let {
                context.setFact("relation_${it.groupValues[2].lowercase()}", it.groupValues[1])
            }

        Regex("""my\s+(son|daughter|father|mother|brother|sister)\s+is\s+([A-Za-z][A-Za-z0-9_-]*)""", RegexOption.IGNORE_CASE)
            .find(q)?.let {
                context.setFact("relation_${it.groupValues[1].lowercase()}", it.groupValues[2])
            }
    }

    private fun familyQuestion(q: String): String? {
        val l = q.lowercase(Locale.getDefault())
        val son = context.getFact("user_son")
        val user = context.getFact("user_name")

        if (son != null && user != null) {
            val mentionsSon = l.contains(son.lowercase())
            if (mentionsSon && (l.contains("grandpa") || l.contains("grandfather"))) {
                val father = context.getFact("relation_father")
                return if (father != null) {
                    "$son's grandfather is $father."
                } else {
                    "$son's grandfather would be $user's father, but you haven't told me your father's name."
                }
            }
            if (mentionsSon && (l.contains("father") || l.contains("dad"))) {
                return "$son's father is $user."
            }
        }

        return null
    }

    private fun inferRelation(a: String, b: String): String? {
        val al = a.lowercase()
        val bl = b.lowercase()
        val user = context.getFact("user_name")?.lowercase()
        val son = context.getFact("user_son")?.lowercase()

        if (user != null && son != null) {
            if (al == user && bl == son) return "$a is $b's father."
            if (al == son && bl == user) return "$a is $b's son."
        }
        return null
    }
}
