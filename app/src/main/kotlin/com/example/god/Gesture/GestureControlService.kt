package com.example.god.gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.LifecycleService
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.god.R
import com.example.god.automation.GodAccessibilityService
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Background gesture session. It is intentionally user-started through semantic intent;
 * Android may still stop/restrict camera foreground services according to OS rules.
 */
class GestureControlService : LifecycleService() {
    companion object {
        const val ACTION_START = "com.example.god.START_GESTURE_CONTROL"
        const val ACTION_STOP = "com.example.god.STOP_GESTURE_CONTROL"
        const val ACTION_GESTURE = "com.example.god.GESTURE_ACTION"
        const val PREFS = "god_settings"
        const val KEY_ENABLED = "gesture_enabled"
        private const val CHANNEL = "god_gesture"
        private const val NOTIFICATION_ID = 7010
    }

    private lateinit var executor: ExecutorService
    private var recognizer: GestureRecognizer? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val interpreter = GestureInterpreter()
    private var lastFrameAt = 0L

    override fun onCreate() {
        super.onCreate()
        executor = Executors.newSingleThreadExecutor()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEngine()
                stopSelf()
            }
            else -> startEngine()
        }
        return START_NOT_STICKY
    }

    private fun startEngine() {
        if (recognizer != null) return
        try {
            val base = BaseOptions.builder().setModelAssetPath("gesture_recognizer.task").build()
            recognizer = GestureRecognizer.createFromOptions(
                this,
                GestureRecognizer.GestureRecognizerOptions.builder()
                    .setBaseOptions(base)
                    .setMinHandDetectionConfidence(0.60f)
                    .setMinHandPresenceConfidence(0.60f)
                    .setMinTrackingConfidence(0.60f)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, _ ->
                        val action = interpreter.interpret(result) ?: return@setResultListener
                        GodAccessibilityService.current()?.performGestureAction(action)
                        sendBroadcast(Intent(ACTION_GESTURE).setPackage(packageName).putExtra("action", action))
                    }
                    .setErrorListener { error ->
                        sendBroadcast(Intent(ACTION_GESTURE).setPackage(packageName).putExtra("action", "ERROR").putExtra("message", error.message ?: "Gesture engine error"))
                    }
                    .build()
            )
            bindCamera()
        } catch (_: Exception) {
            stopEngine()
            stopSelf()
        }
    }

    private fun bindCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cameraProvider = future.get()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(320, 240))
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    try {
                        val bitmap = image.toBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        recognizer?.recognizeAsync(mpImage, android.os.SystemClock.uptimeMillis())
                    } catch (_: Exception) {
                        // A single bad frame must not kill the gesture session.
                    } finally {
                        image.close()
                    }
                }
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            } catch (_: Exception) {
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopEngine() {
        try { cameraProvider?.unbindAll() } catch (_: Exception) {}
        cameraProvider = null
        try { recognizer?.close() } catch (_: Exception) {}
        recognizer = null
        lastFrameAt = 0L
        interpreter.reset()
    }

    override fun onDestroy() {
        stopEngine()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent) = super.onBind(intent)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "GOD Gesture Control", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return b.setContentTitle("GOD gesture control")
            .setContentText("Gesture control is ready")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }
}
