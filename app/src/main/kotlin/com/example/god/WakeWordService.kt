package com.example.god.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** No-API-key, best-effort wake listener using Android speech recognition. */
class WakeWordService : Service() {
    companion object {
        const val ACTION_START = "com.example.god.START_WAKE_WORD"
        const val ACTION_STOP = "com.example.god.STOP_WAKE_WORD"
        const val ACTION_DETECTED = "com.example.god.WAKE_WORD_DETECTED"
        const val PREFS = "god_settings"
        const val KEY_ENABLED = "wake_enabled"
        private const val CHANNEL = "god_wake_word"
        private const val NOTIFICATION_ID = 7001
    }

    private var recognizer: SpeechRecognizer? = null
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopEngine(); stopSelf() }
            else -> startEngine()
        }
        return START_NOT_STICKY
    }

    private fun startEngine() {
        if (running || !SpeechRecognizer.isRecognitionAvailable(this)) return
        running = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: android.os.Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    val n = text.lowercase(Locale.getDefault())
                    if (n.contains("hey god") || n.contains("hey guard")) {
                        sendBroadcast(Intent(ACTION_DETECTED).setPackage(packageName))
                    }
                    restartListening()
                }
                override fun onError(error: Int) { restartListening() }
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
        restartListening()
    }

    private fun restartListening() {
        if (!running) return
        try {
            recognizer?.cancel()
            recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        } catch (_: Exception) { stopEngine() }
    }

    private fun stopEngine() {
        running = false
        try { recognizer?.cancel() } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    override fun onDestroy() { stopEngine(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "GOD Wake Word", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return b.setContentTitle("GOD wake listener active")
            .setContentText("Listening locally through Android speech recognition")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }
}
