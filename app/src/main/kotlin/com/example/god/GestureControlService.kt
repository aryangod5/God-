package com.example.god.Gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.god.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureControlService : LifecycleService() {

    companion object {
        const val ACTION_GESTURE = "com.example.god.GESTURE_ACTION"
        private const val CHANNEL_ID = "god_gesture"
        private const val NOTIFICATION_ID = 2001
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_GESTURE") {
            stopSelf()
            return START_NOT_STICKY
        }

        bindCamera()
        return START_NOT_STICKY
    }

    private fun bindCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()

                val preview = Preview.Builder().build()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    // Gesture analysis is supplied by the gesture interpreter.
                    // Keep this service lightweight and release each frame promptly.
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                sendBroadcast(
                    Intent(ACTION_GESTURE)
                        .setPackage(packageName)
                        .putExtra("action", "ERROR")
                        .putExtra("message", e.message ?: "Unable to start gesture camera")
                )
                stopSelf()
            }
        }, mainExecutor)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GOD gesture control")
            .setContentText("Gesture control is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "GOD gesture control",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        super.onDestroy()
    }
}
