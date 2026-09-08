package com.example.god.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.content.Intent
import java.util.Locale
import kotlin.math.abs

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
    private var recorder: AudioRecord? = null
    private var monitorThread: Thread? = null
    private var monitoring = false
    private val handler = Handler(Looper.getMainLooper())

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    listener.onStateChanged(AIState.LISTENING)
                    startMonitor()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    listener.onVoiceLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    stopMonitor()
                    listener.onStateChanged(AIState.PROCESSING)
                }
                override fun onError(error: Int) {
                    stopMonitor()
                    listener.onStateChanged(AIState.ERROR)
                    listener.onError("Speech recognition error: $error")
                }
                override fun onResults(results: android.os.Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    listener.onTextRecognized(text)
                    listener.onStateChanged(AIState.IDLE)
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        listener.onStateChanged(AIState.STARTING)
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        stopMonitor()
        listener.onStateChanged(AIState.IDLE)
    }

    fun cancelListening() {
        recognizer?.cancel()
        stopMonitor()
        listener.onStateChanged(AIState.IDLE)
    }

    fun speak(text: String) {
        listener.onStateChanged(AIState.SPEAKING)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GOD_RESPONSE")
    }

    fun stopSpeaking() {
        tts?.stop()
        listener.onStateChanged(AIState.IDLE)
    }

    private fun startMonitor() {
        if (monitoring) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        try {
            val min = AudioRecord.getMinBufferSize(
                16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                min.coerceAtLeast(2048)
            )
            recorder?.startRecording()
            monitoring = true
            monitorThread = Thread {
                val buffer = ShortArray(1024)
                while (monitoring) {
                    val n = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (n > 0) {
                        var sum = 0.0
                        for (i in 0 until n) sum += buffer[i].toDouble() * buffer[i].toDouble()
                        val rms = kotlin.math.sqrt(sum / n) / 32768.0
                        val level = rms.toFloat().coerceIn(0f, 1f)
                        handler.post { listener.onVoiceLevel(level) }
                    }
                }
            }.also { it.start() }
        } catch (_: Exception) {
            stopMonitor()
        }
    }

    private fun stopMonitor() {
        monitoring = false
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        monitorThread = null
        handler.post { listener.onVoiceLevel(0f) }
    }

    fun release() {
        stopMonitor()
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
        tts = null
    }
}
