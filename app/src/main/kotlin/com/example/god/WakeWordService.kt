package com.example.god.voice

import ai.picovoice.porcupine.PorcupineManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.example.god.R

/**
 * Real foreground wake-word engine. It requires a Picovoice AccessKey and a custom
 * `hey_god.ppn` model in src/main/assets. No API key is embedded in the APK.
 */
class WakeWordService : Service() {
    companion object {
        const val ACTION_START = "com.example.god.START_WAKE_WORD"
        const val ACTION_STOP = "com.example.god.STOP_WAKE_WORD"
        const val ACTION_DETECTED = "com.example.god.WAKE_WORD_DETECTED"
        const val PREFS = "god_settings"
        const val KEY_ACCESS = "wake_access_key"
        private const val CHANNEL = "god_wake_word"
        private const val NOTIFICATION_ID = 7001
    }

    private var manager: PorcupineManager? = null

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
        return START_STICKY
    }

    private fun startEngine() {
        if (manager != null) return
        val key = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ACCESS, "").orEmpty()
        if (key.isBlank()) return
        try {
            manager = PorcupineManager.Builder()
                .setAccessKey(key)
                .setKeywordPath("hey_god.ppn")
                .setSensitivity(0.62f)
                .build(this) {
                    sendBroadcast(Intent(ACTION_DETECTED).setPackage(packageName))
                }
            manager?.start()
        } catch (_: Exception) {
            manager = null
        }
    }

    private fun stopEngine() {
        try { manager?.stop() } catch (_: Exception) {}
        manager?.delete()
        manager = null
    }

    override fun onDestroy() {
        stopEngine()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "GOD Wake Word", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return b.setContentTitle("GOD wake word active")
            .setContentText("Listening locally for the Hey GOD keyword")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }
}
