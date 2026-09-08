package com.example.god

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.hypot

/** Real-time camera gesture control using MediaPipe Gesture Recognizer + hand landmarks. */
class GestureControlActivity : ComponentActivity() {
    companion object { private const val CAMERA_REQUEST = 8801 }

    private lateinit var preview: PreviewView
    private lateinit var status: TextView
    private lateinit var executor: ExecutorService
    private var recognizer: GestureRecognizer? = null
    private var lastX = Float.NaN
    private var lastEventAt = 0L
    private var pinchLatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = PreviewView(this)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(preview, FrameLayout.LayoutParams(-1, -1))
        status = TextView(this).apply {
            text = "GOD // CAMERA GESTURE CONTROL\nOPEN PALM = WAKE   •   SWIPE = NAVIGATE\nPINCH = SELECT   •   FIST = CANCEL\n\nTap BACK to exit"
            setTextColor(Color.WHITE); textSize = 14f; setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.argb(190, 0, 0, 0))
        }
        val lp = FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.TOP }
        root.addView(status, lp)
        setContentView(root)
        executor = Executors.newSingleThreadExecutor()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
    }

    private fun setupRecognizer() {
        val base = com.google.mediapipe.tasks.core.BaseOptions.builder()
            .setModelAssetPath("gesture_recognizer.task").build()
        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(base)
            .setMinHandDetectionConfidence(0.55f)
            .setMinHandPresenceConfidence(0.55f)
            .setMinTrackingConfidence(0.55f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> handleResult(result) }
            .setErrorListener { error -> runOnUiThread { status.text = "GESTURE ENGINE ERROR\n${error.message}" } }
            .build()
        recognizer = GestureRecognizer.createFromOptions(this, options)
    }

    private fun startCamera() {
        setupRecognizer()
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val previewUse = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { image ->
                try {
                    val bitmap = image.toBitmap()
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    recognizer?.recognizeAsync(mpImage, android.os.SystemClock.uptimeMillis())
                } catch (_: Exception) {} finally { image.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUse, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleResult(result: com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult, image: com.google.mediapipe.framework.image.MPImage? = null) {
        val gesture = result.gestures().firstOrNull()?.firstOrNull()?.categoryName().orEmpty()
        val hand = result.landmarks().firstOrNull()
        val now = System.currentTimeMillis()
        var action: String? = null

        if (gesture == "Open_Palm") action = "WAKE"
        if (gesture == "Closed_Fist") action = "CANCEL"

        if (hand != null && hand.size >= 21) {
            val thumb = hand[4]; val index = hand[8]
            val pinchDistance = hypot(thumb.x() - index.x(), thumb.y() - index.y())
            if (pinchDistance < 0.075f) {
                if (!pinchLatched) { action = "SELECT"; pinchLatched = true }
            } else if (pinchDistance > 0.11f) pinchLatched = false

            val wristX = hand[0].x()
            if (!lastX.isNaN() && now - lastEventAt > 500) {
                val dx = wristX - lastX
                if (kotlin.math.abs(dx) > 0.22f) action = if (dx > 0) "SWIPE_RIGHT" else "SWIPE_LEFT"
            }
            lastX = wristX
        }

        if (action != null && now - lastEventAt > 650) {
            lastEventAt = now
            status.post { status.text = "GOD // GESTURE\n$action\n\nOPEN PALM = WAKE • SWIPE = NAVIGATE\nPINCH = SELECT • FIST = CANCEL" }
            sendBroadcast(android.content.Intent("com.example.god.GESTURE_ACTION").setPackage(packageName).putExtra("action", action))
            GestureEvents.emit(action!!)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startCamera()
        else finish()
    }

    override fun onDestroy() {
        recognizer?.close(); executor.shutdown(); super.onDestroy()
    }

    object GestureEvents {
        private var listener: ((String) -> Unit)? = null
        fun listen(block: (String) -> Unit) { listener = block }
        fun clear() { listener = null }
        fun emit(action: String) { listener?.invoke(action) }
    }

}
