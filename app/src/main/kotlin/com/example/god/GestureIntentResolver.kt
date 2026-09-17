package com.example.god

import java.util.Locale

/**
 * Meaning-based gesture-control command resolver.
 * It intentionally matches concepts and intent rather than one exact sentence.
 */
object GestureIntentResolver {
    enum class Intent { ENABLE, DISABLE, NONE }
    data class Result(val intent: Intent, val confidence: Float)

    fun resolve(text: String): Result {
        val n = normalize(text)
        if (n.isBlank()) return Result(Intent.NONE, 0f)

        val gestureScore = score(n, gestureTerms)
        if (gestureScore <= 0) return Result(Intent.NONE, 0f)

        val enable = score(n, enableTerms)
        val disable = score(n, disableTerms)
        val explicitDisable = disable >= 2
        val explicitEnable = enable >= 2

        return when {
            explicitDisable && disable >= enable -> Result(Intent.DISABLE, confidence(disable, gestureScore))
            explicitEnable && enable > disable -> Result(Intent.ENABLE, confidence(enable, gestureScore))
            else -> Result(Intent.NONE, 0f)
        }
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.getDefault())
        .replace(Regex("[’']"), "")
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun score(text: String, terms: List<Pair<String, Int>>): Int = terms.sumOf { (term, weight) ->
        if (Regex("(^|\\s)${Regex.escape(term)}(\\s|$)").containsMatchIn(text)) weight else 0
    }

    private fun confidence(intentScore: Int, gestureScore: Int): Float =
        (0.55f + intentScore * 0.08f + gestureScore * 0.04f).coerceAtMost(0.98f)

    private val gestureTerms = listOf(
        "gesture" to 3, "gestures" to 3, "hand" to 2, "hands" to 2,
        "movement" to 1, "movements" to 1, "motion" to 1,
        "wave" to 1, "waving" to 1, "hand control" to 3,
        "hand controls" to 3, "hand command" to 3, "hand commands" to 3,
        "air gesture" to 3, "air gestures" to 3
    )

    private val enableTerms = listOf(
        "start" to 2, "enable" to 3, "activate" to 3, "turn on" to 3,
        "switch on" to 3, "switch" to 1, "begin" to 2, "launch" to 2,
        "use" to 1, "ready" to 1, "allow" to 1, "let me" to 1,
        "make" to 1, "work" to 1, "watch" to 1, "recognize" to 2,
        "recognise" to 2, "on" to 2, "ready for" to 2
    )

    private val disableTerms = listOf(
        "stop" to 3, "disable" to 3, "deactivate" to 3, "turn off" to 3,
        "switch off" to 3, "shut down" to 3, "cancel" to 2,
        "dont" to 2, "do not" to 2, "no longer" to 3, "not anymore" to 3,
        "enough" to 2, "off" to 3, "done with" to 3, "finished with" to 3, "stop using" to 3
    )
}
