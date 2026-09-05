package com.example.god.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class AIState {
    OFFLINE,
    STARTING,
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onStateChanged(state: AIState)
        fun onPartialText(text: String)
        fun onFinalText(text: String)
        fun onResponse(text: String)
        fun onError(message: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var released = false

    init {
        initialize()
    }

    private fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onStateChanged(AIState.OFFLINE)
            listener.onError("Speech recognition is not available on this device.")
            return
        }

        listener.onStateChanged(AIState.STARTING)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                listener.onStateChanged(AIState.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                listener.onStateChanged(AIState.LISTENING)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Reserved for the live audio waveform in the next step.
                // The value is already coming from the real microphone.
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onEndOfSpeech() {
                listener.onStateChanged(AIState.PROCESSING)
            }

            override fun onError(error: Int) {
                if (released) return

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO ->
                        "Microphone audio error."

                    SpeechRecognizer.ERROR_CLIENT ->
                        "Speech recognition client error."

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Microphone permission is required."

                    SpeechRecognizer.ERROR_NETWORK ->
                        "Speech recognition network error."

                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Speech recognition timed out."

                    SpeechRecognizer.ERROR_NO_MATCH ->
                        "I didn't catch that."

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        "Speech recognizer is busy."

                    SpeechRecognizer.ERROR_SERVER ->
                        "Speech recognition server error."

                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "I didn't hear anything."

                    else ->
                        "Speech recognition error."
                }

                listener.onStateChanged(AIState.ERROR)
                listener.onError(message)

                if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                    error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                ) {
                    return
                }

                listener.onStateChanged(AIState.IDLE)
            }

            override fun onResults(results: Bundle?) {
                if (released) return

                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val text = matches
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (text.isEmpty()) {
                    listener.onStateChanged(AIState.IDLE)
                    listener.onError("I didn't catch that.")
                    return
                }

                listener.onFinalText(text)
                listener.onStateChanged(AIState.PROCESSING)

                processCommand(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                val text = matches
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (text.isNotEmpty()) {
                    listener.onPartialText(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        })

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())

                ttsReady =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

                if (!ttsReady) {
                    listener.onError("Text-to-speech language is not available.")
                }

                if (!released) {
                    listener.onStateChanged(AIState.IDLE)
                }
            } else {
                ttsReady = false
                listener.onStateChanged(AIState.ERROR)
                listener.onError("Text-to-speech could not be initialized.")
            }
        }
    }

    fun startListening() {
        if (released) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onStateChanged(AIState.OFFLINE)
            listener.onError("Speech recognition is not available.")
            return
        }

        listener.onStateChanged(AIState.STARTING)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            )
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            listener.onStateChanged(AIState.ERROR)
            listener.onError(
                e.message ?: "Could not start microphone."
            )
        }
    }

    fun stopListening() {
        if (released) return

        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun cancelListening() {
        if (released) return

        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {
        }

        listener.onStateChanged(AIState.IDLE)
    }

    private fun processCommand(text: String) {
        val response = generateLocalResponse(text)

        listener.onResponse(response)
        speak(response)
    }

    /**
     * Local GOD assistant.
     *
     * This deliberately does not require an API key.
     * A real AI provider can be connected later without changing
     * the microphone/TTS architecture.
     */
    private fun generateLocalResponse(input: String): String {
        val text = input.lowercase(Locale.getDefault()).trim()

        return when {
            text == "hello" ||
            text == "hi" ||
            text.contains("hello god") ||
            text.contains("hi god") -> {
                "Hello. GOD is online and ready."
            }

            text.contains("who are you") -> {
                "I am GOD, your Android assistant."
            }

            text.contains("are you there") -> {
                "Yes. I am here."
            }

            text.contains("how are you") -> {
                "All systems are operational."
            }

            text.contains("thank you") ||
            text.contains("thanks") -> {
                "You're welcome."
            }

            text.contains("stop") ||
            text.contains("cancel") -> {
                "Command cancelled."
            }

            text.contains("time") -> {
                val time = java.text.SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(java.util.Date())

                "The current time is $time."
            }

            text.contains("date") ||
            text.contains("today") -> {
                val date = java.text.SimpleDateFormat(
                    "EEEE, d MMMM yyyy",
                    Locale.getDefault()
                ).format(java.util.Date())

                "Today is $date."
            }

            text.contains("open settings") -> {
                "Settings command received."
            }

            text.contains("what can you do") ||
            text.contains("what do you do") -> {
                "I can listen to your voice, understand commands, speak responses, and control the GOD interface."
            }

            else -> {
                "I heard you say: $input. The local GOD assistant is working. Full AI intelligence can be connected later."
            }
        }
    }

    private fun speak(text: String) {
        if (released) return

        if (!ttsReady || textToSpeech == null) {
            listener.onStateChanged(AIState.IDLE)
            return
        }

        listener.onStateChanged(AIState.SPEAKING)

        val utteranceId = "GOD_RESPONSE"

        textToSpeech?.setSpeechRate(1.0f)
        textToSpeech?.setPitch(1.0f)

        textToSpeech?.setOnUtteranceProgressListener(
            object : android.speech.tts.UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    listener.onStateChanged(AIState.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    if (!released) {
                        listener.onStateChanged(AIState.IDLE)
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (!released) {
                        listener.onStateChanged(AIState.ERROR)
                        listener.onError("Text-to-speech failed.")
                    }
                }
            }
        )

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    fun stopSpeaking() {
        textToSpeech?.stop()

        if (!released) {
            listener.onStateChanged(AIState.IDLE)
        }
    }

    fun release() {
        if (released) return

        released = true

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }

        textToSpeech = null
    }
}
