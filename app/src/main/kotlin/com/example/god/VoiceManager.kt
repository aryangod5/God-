package com.example.god.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class AIState { OFFLINE, STARTING, IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }

class VoiceManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onStateChanged(state: AIState)
        fun onVoiceLevel(level: Float)
        fun onTextRecognized(text: String)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryNoMatch = false
    private var listening = false
    private var lastRms = 0f

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            createRecognizer()
        } else {
            listener.onStateChanged(AIState.OFFLINE)
            listener.onError("Speech recognition is not available on this device.")
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    private fun createRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                listener.onStateChanged(AIState.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                listening = true
                listener.onStateChanged(AIState.LISTENING)
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                lastRms += (normalized - lastRms) * 0.18f
                listener.onVoiceLevel(lastRms)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                listening = false
                listener.onStateChanged(AIState.PROCESSING)
                listener.onVoiceLevel(0f)
            }

            override fun onError(error: Int) {
                listening = false
                listener.onVoiceLevel(0f)
                if (error == SpeechRecognizer.ERROR_NO_MATCH && !retryNoMatch) {
                    retryNoMatch = true
                    listener.onStateChanged(AIState.LISTENING)
                    handler.postDelayed({
                        if (!listening) startListeningInternal()
                    }, 350L)
                    return
                }
                retryNoMatch = false
                listener.onStateChanged(AIState.ERROR)
                listener.onError(errorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                listening = false
                retryNoMatch = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (text.isBlank()) {
                    listener.onStateChanged(AIState.ERROR)
                    listener.onError("I didn't catch that. Please speak again.")
                } else {
                    listener.onTextRecognized(text)
                    listener.onStateChanged(AIState.IDLE)
                }
                listener.onVoiceLevel(0f)
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    fun startListening() {
        // Barge-in: the user's new request always wins over current TTS.
        stopSpeaking(false)
        retryNoMatch = false
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onStateChanged(AIState.ERROR)
            listener.onError("Speech recognition is not available.")
            return
        }
        if (recognizer == null) createRecognizer()

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().ifBlank { "en-IN" }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        listener.onStateChanged(AIState.STARTING)
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            listener.onStateChanged(AIState.ERROR)
            listener.onError("Unable to start speech recognition.")
        }
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Please speak again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error. Check your internet connection."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Check microphone permission."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "The selected speech language is not supported."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "The selected speech language is unavailable."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech recognition service disconnected."
        else -> "Speech recognition error: $error"
    }

    fun stopListening() {
        retryNoMatch = false
        listening = false
        try { recognizer?.stopListening() } catch (_: Exception) {}
        listener.onVoiceLevel(0f)
        listener.onStateChanged(AIState.IDLE)
    }

    fun cancelListening() {
        retryNoMatch = false
        listening = false
        try { recognizer?.cancel() } catch (_: Exception) {}
        listener.onVoiceLevel(0f)
        listener.onStateChanged(AIState.IDLE)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        listener.onStateChanged(AIState.SPEAKING)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GOD_RESPONSE")
    }

    private fun stopSpeaking(updateState: Boolean) {
        try { tts?.stop() } catch (_: Exception) {}
        if (updateState) listener.onStateChanged(AIState.IDLE)
    }

    fun stopSpeaking() = stopSpeaking(true)

    fun release() {
        try { recognizer?.cancel() } catch (_: Exception) {}
        recognizer?.destroy()
        recognizer = null
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        handler.removeCallbacksAndMessages(null)
    }
}
