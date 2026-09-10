package com.example.god

import java.util.Locale

data class UnderstoodMessage(
    val original: String,
    val normalized: String,
    val confidence: Float
)

/**
 * Conservative local speech/text cleanup.
 * It deliberately avoids aggressive autocorrection that could change a user's meaning.
 */
class SpeechUnderstandingManager {
    fun understand(text: String): UnderstoodMessage {
        val original = text.trim()
        if (original.isBlank()) return UnderstoodMessage("", "", 0f)

        var n = original
            .replace(Regex("\\s+"), " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" ?", "?")
            .replace(" !", "!")
            .trim()

        // Common speech-recognition artifacts. These are intentionally limited.
        n = n.replace(Regex("^hey\\s+god[, ]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^god[, ]*", RegexOption.IGNORE_CASE), "")
            .trim()

        val confidence = if (n == original) 1f else 0.94f
        return UnderstoodMessage(original, n, confidence)
    }

    fun lower(text: String): String = text.lowercase(Locale.getDefault()).trim()
}
