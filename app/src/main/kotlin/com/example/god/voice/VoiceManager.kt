package com.example.god.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
        fun onVoiceLevel(level: Float)
        fun onTextRecognized(text: String)
        fun onError(message: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var isListening = false
    private var isReleased = false

    private var smoothedVoiceLevel = 0f

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    private fun initializeTextToSpeech() {

        listener.onStateChanged(AIState.STARTING)

        textToSpeech = TextToSpeech(context) { status ->

            if (isReleased) return@TextToSpeech

            if (status == TextToSpeech.SUCCESS) {

                val result =
                    textToSpeech?.setLanguage(Locale.getDefault())

                if (
                    result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {

                    listener.onError(
                        "Text-to-speech language is not supported."
                    )

                } else {

                    listener.onStateChanged(AIState.IDLE)
                }

            } else {

                listener.onError(
                    "Text-to-speech initialization failed."
                )
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {

                    if (isReleased) return

                    listener.onStateChanged(
                        AIState.SPEAKING
                    )
                }

                override fun onDone(utteranceId: String?) {

                    if (isReleased) return

                    listener.onVoiceLevel(0f)

                    listener.onStateChanged(
                        AIState.IDLE
                    )
                }

                override fun onError(utteranceId: String?) {

                    if (isReleased) return

                    listener.onVoiceLevel(0f)

                    listener.onStateChanged(
                        AIState.ERROR
                    )

                    listener.onError(
                        "Voice output failed."
                    )
                }
            }
        )
    }

    private fun initializeSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {

            listener.onStateChanged(
                AIState.OFFLINE
            )

            listener.onError(
                "Speech recognition is not available on this device."
            )

            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(context)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    if (isReleased) return

                    resetVoiceLevel()

                    listener.onStateChanged(
                        AIState.LISTENING
                    )
                }

                override fun onBeginningOfSpeech() {

                    if (isReleased) return

                    listener.onStateChanged(
                        AIState.LISTENING
                    )
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {

                    if (
                        isReleased ||
                        !isListening
                    ) {
                        return
                    }

                    val normalized =
                        normalizeRms(rmsdB)

                    val smoothing =
                        if (
                            normalized >
                            smoothedVoiceLevel
                        ) {
                            0.35f
                        } else {
                            0.12f
                        }

                    smoothedVoiceLevel +=
                        (
                            normalized -
                            smoothedVoiceLevel
                        ) * smoothing

                    smoothedVoiceLevel =
                        clamp(
                            smoothedVoiceLevel,
                            0f,
                            1f
                        )

                    listener.onVoiceLevel(
                        smoothedVoiceLevel
                    )
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {

                    if (isReleased) return

                    resetVoiceLevel()

                    listener.onStateChanged(
                        AIState.PROCESSING
                    )
                }

                override fun onError(
                    error: Int
                ) {

                    if (isReleased) return

                    isListening = false

                    resetVoiceLevel()

                    listener.onStateChanged(
                        AIState.IDLE
                    )

                    val message =
                        when (error) {

                            SpeechRecognizer.ERROR_AUDIO ->
                                "Microphone audio error."

                            SpeechRecognizer.ERROR_CLIENT ->
                                "Speech recognition client error."

                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission is required."

                            SpeechRecognizer.ERROR_NETWORK ->
                                "Speech recognition network error."

                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Speech recognition network timeout."

                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "I couldn't understand that."

                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Speech recognizer is busy."

                            SpeechRecognizer.ERROR_SERVER ->
                                "Speech recognition server error."

                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "No speech detected."

                            else ->
                                "Speech recognition error."
                        }

                    listener.onError(
                        message
                    )
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    if (isReleased) return

                    isListening = false

                    resetVoiceLevel()

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    if (text.isBlank()) {

                        listener.onStateChanged(
                            AIState.IDLE
                        )

                        return
                    }

                    listener.onTextRecognized(
                        text
                    )

                    listener.onStateChanged(
                        AIState.PROCESSING
                    )

                    processCommand(
                        text
                    )
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )
    }

    private fun normalizeRms(
        rmsdB: Float
    ): Float {

        val minimumDb = -55f
        val maximumDb = 0f

        val bounded =
            max(
                minimumDb,
                min(
                    maximumDb,
                    rmsdB
                )
            )

        val normalized =
            (bounded - minimumDb) /
                    (maximumDb - minimumDb)

        val adjusted =
            when {

                normalized < 0.08f ->
                    0f

                normalized < 0.20f ->
                    normalized * 0.35f

                else ->
                    normalized
            }

        return clamp(
            adjusted,
            0f,
            1f
        )
    }

    private fun clamp(
        value: Float,
        minimum: Float,
        maximum: Float
    ): Float {

        return max(
            minimum,
            min(
                maximum,
                value
            )
        )
    }

    private fun resetVoiceLevel() {

        smoothedVoiceLevel = 0f

        if (!isReleased) {

            listener.onVoiceLevel(
                0f
            )
        }
    }

    fun startListening() {

        if (isReleased) return

        if (speechRecognizer == null) {

            listener.onStateChanged(
                AIState.OFFLINE
            )

            listener.onError(
                "Speech recognition is unavailable."
            )

            return
        }

        if (isListening) return

        stopSpeaking()

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

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
                    3
                )
            }

        try {

            resetVoiceLevel()

            isListening = true

            listener.onStateChanged(
                AIState.LISTENING
            )

            speechRecognizer?.startListening(
                intent
            )

        } catch (exception: Exception) {

            isListening = false

            resetVoiceLevel()

            listener.onStateChanged(
                AIState.ERROR
            )

            listener.onError(
                "Unable to start microphone: ${exception.message}"
            )
        }
    }

    fun stopListening() {

        if (!isListening) return

        try {

            speechRecognizer?.stopListening()

        } catch (_: Exception) {
        }

        isListening = false

        resetVoiceLevel()
    }

    fun cancelListening() {

        try {

            speechRecognizer?.cancel()

        } catch (_: Exception) {
        }

        isListening = false

        resetVoiceLevel()

        if (!isReleased) {

            listener.onStateChanged(
                AIState.IDLE
            )
        }
    }

    private fun processCommand(
        text: String
    ) {

        val response =
            generateLocalResponse(
                text
            )

        speak(
            response
        )
    }

    private fun generateLocalResponse(
        text: String
    ): String {

        val command =
            text
                .trim()
                .lowercase(
                    Locale.getDefault()
                )

        return when {

            command.contains("hello") ||
            command.contains("hi") ||
            command.contains("hey") ->

                "Hello, Master. GOD is online and ready."

            command.contains("who are you") ||
            command.contains("what are you") ->

                "I am GOD, your personal AI assistant."

            command.contains("are you there") ||
            command.contains("can you hear me") ->

                "Yes, Master. I can hear you."

            command.contains("what time") ->

                "The current time is ${
                    java.text.SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                    ).format(
                        java.util.Date()
                    )
                }."

            command.contains("what date") ||
            command.contains("today's date") ||
            command.contains("what day") ->

                "Today is ${
                    java.text.SimpleDateFormat(
                        "EEEE, d MMMM yyyy",
                        Locale.getDefault()
                    ).format(
                        java.util.Date()
                    )
                }."

            command.contains("thank") ->

                "You're welcome, Master."

            command.contains("stop") ||
            command.contains("cancel") ->

                "Command cancelled."

            command.contains("settings") ->

                "Settings are available from the GOD system menu."

            command.contains("what can you do") ||
            command.contains("capabilities") ->

                "I can listen to your voice, understand commands, respond with speech, and control the GOD interface. More capabilities will be added to the system."

            else ->

                "I heard you say: $text. The AI provider can be connected later for full AI responses."
        }
    }

    private fun speak(
        text: String
    ) {

        if (isReleased) return

        val tts =
            textToSpeech

        if (tts == null) {

            listener.onStateChanged(
                AIState.ERROR
            )

            listener.onError(
                "Voice output is not ready."
            )

            return
        }

        try {

            listener.onStateChanged(
                AIState.SPEAKING
            )

            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "GOD_RESPONSE"
            )

        } catch (exception: Exception) {

            listener.onStateChanged(
                AIState.ERROR
            )

            listener.onError(
                "Unable to speak response: ${exception.message}"
            )
        }
    }

    fun stopSpeaking() {

        try {

            textToSpeech?.stop()

        } catch (_: Exception) {
        }

        if (!isReleased) {

            listener.onVoiceLevel(
                0f
            )

            listener.onStateChanged(
                AIState.IDLE
            )
        }
    }

    fun release() {

        if (isReleased) return

        isReleased = true

        isListening = false

        smoothedVoiceLevel = 0f

        try {

            speechRecognizer?.cancel()

        } catch (_: Exception) {
        }

        try {

            speechRecognizer?.destroy()

        } catch (_: Exception) {
        }

        speechRecognizer = null

        try {

            textToSpeech?.stop()

        } catch (_: Exception) {
        }

        try {

            textToSpeech?.shutdown()

        } catch (_: Exception) {
        }

        textToSpeech = null
    }
}
