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

        fun onStateChanged(
            state: AIState
        )

        fun onTextRecognized(
            text: String
        )

        fun onError(
            message: String
        )
    }

    private var speechRecognizer:
            SpeechRecognizer? = null

    private var textToSpeech:
            TextToSpeech? = null

    private var isListening = false

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    // =========================================================
    // TEXT TO SPEECH
    // =========================================================

    private fun initializeTextToSpeech() {

        listener.onStateChanged(
            AIState.STARTING
        )

        textToSpeech =
            TextToSpeech(
                context
            ) { status ->

                if (status ==
                    TextToSpeech.SUCCESS
                ) {

                    val result =
                        textToSpeech?.setLanguage(
                            Locale.getDefault()
                        )

                    if (
                        result ==
                        TextToSpeech.LANG_MISSING_DATA ||
                        result ==
                        TextToSpeech.LANG_NOT_SUPPORTED
                    ) {

                        listener.onError(
                            "Text-to-speech language is not supported."
                        )

                    } else {

                        listener.onStateChanged(
                            AIState.IDLE
                        )
                    }

                } else {

                    listener.onError(
                        "Text-to-speech initialization failed."
                    )
                }
            }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {

                    listener.onStateChanged(
                        AIState.SPEAKING
                    )
                }

                override fun onDone(
                    utteranceId: String?
                ) {

                    listener.onStateChanged(
                        AIState.IDLE
                    )
                }

                override fun onError(
                    utteranceId: String?
                ) {

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

    // =========================================================
    // SPEECH RECOGNITION
    // =========================================================

    private fun initializeSpeechRecognizer() {

        if (
            !SpeechRecognizer.isRecognitionAvailable(
                context
            )
        ) {

            listener.onStateChanged(
                AIState.OFFLINE
            )

            listener.onError(
                "Speech recognition is not available on this device."
            )

            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(
                context
            )

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    listener.onStateChanged(
                        AIState.LISTENING
                    )
                }

                override fun onBeginningOfSpeech() {

                    listener.onStateChanged(
                        AIState.LISTENING
                    )
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                    // Audio level is available here.
                    // The visual waveform will use this
                    // in a later step.
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {

                    listener.onStateChanged(
                        AIState.PROCESSING
                    )
                }

                override fun onError(
                    error: Int
                ) {

                    isListening = false

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

                    isListening = false

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
            }
        )
    }

    // =========================================================
    // START LISTENING
    // =========================================================

    fun startListening() {

        if (
            speechRecognizer == null
        ) {

            listener.onStateChanged(
                AIState.OFFLINE
            )

            listener.onError(
                "Speech recognition is unavailable."
            )

            return
        }

        if (isListening) {
            return
        }

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

            isListening = true

            listener.onStateChanged(
                AIState.LISTENING
            )

            speechRecognizer?.startListening(
                intent
            )

        } catch (exception: Exception) {

            isListening = false

            listener.onStateChanged(
                AIState.ERROR
            )

            listener.onError(
                "Unable to start microphone: ${exception.message}"
            )
        }
    }

    // =========================================================
    // STOP LISTENING
    // =========================================================

    fun stopListening() {

        if (!isListening) {
            return
        }

        try {

            speechRecognizer?.stopListening()

        } catch (_: Exception) {
        }

        isListening = false
    }

    // =========================================================
    // CANCEL LISTENING
    // =========================================================

    fun cancelListening() {

        try {

            speechRecognizer?.cancel()

        } catch (_: Exception) {
        }

        isListening = false

        listener.onStateChanged(
            AIState.IDLE
        )
    }

    // =========================================================
    // COMMAND PROCESSING
    // =========================================================

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
                .lowercase(Locale.getDefault())

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
                    command.contains("cancel") -> {

                "Command cancelled."
            }

            command.contains("settings") ->

                "Settings are available from the GOD system menu."

            command.contains("what can you do") ||
                    command.contains("capabilities") ->

                "I can listen to your voice, understand commands, respond with speech, and control the GOD interface. More capabilities will be added to the system."

            else ->

                "I heard you say: $text. The AI provider can be connected later for full AI responses."
        }
    }

    // =========================================================
    // SPEAK
    // =========================================================

    private fun speak(
        text: String
    ) {

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

    // =========================================================
    // STOP SPEAKING
    // =========================================================

    fun stopSpeaking() {

        try {

            textToSpeech?.stop()

        } catch (_: Exception) {
        }

        listener.onStateChanged(
            AIState.IDLE
        )
    }

    // =========================================================
    // RELEASE
    // =========================================================

    fun release() {

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

        isListening = false
    }
}
