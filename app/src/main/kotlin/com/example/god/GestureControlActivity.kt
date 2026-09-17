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
import com.example.god.gesture.GestureInterpreter
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Visible diagnostic/preview screen. The real Android-wide gesture session is GestureControlService.
 */
class GestureControlActivity : ComponentActivity() {
    companion object { private const val CAMERA_REQUEST = 8801 }

    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private lateinit var executor: ExecutorService
    private var recognizer: GestureRecognizer? = null
    private val interpreter = GestureInterpreter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        status = TextView(this).apply {
            text = "GOD // GESTURE DIAGNOSTIC\nOPEN PALM = WAKE • SWIPE = NAVIGATE\nPINCH = SELECT • FIST = CANCEL"
            setTextColor(Color.WHITE); textSize = 14f; setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.argb(190, 0, 0, 0))
        }
        root.addView(status, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.TOP })
        setContentView(root)
        executor = Executors.newSingleThreadExecutor()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
    }

    private fun setupRecognizer() {
        val base = BaseOptions.builder().setModelAssetPath("gesture_recognizer.task").build()
        recognizer = GestureRecognizer.createFromOptions(
            this,
            GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(base)
                .setMinHandDetectionConfidence(0.60f)
                .setMinHandPresenceConfidence(0.60f)
                .setMinTrackingConfidence(0.60f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> handleResult(result) }
                .setErrorListener { error -> runOnUiThread { status.text = "GESTURE ERROR\n${error.message}" } }
                .build()
        )
    }

    private fun startCamera() {
        setupRecognizer()
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val cameraPreview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(320, 240))
                .build()
            analysis.setAnalyzer(executor) { image ->
                try {
                    val bitmap = image.toBitmap()
                    recognizer?.recognizeAsync(BitmapImageBuilder(bitmap).build(), android.os.SystemClock.uptimeMillis())
                } catch (e: Exception) {
                    runOnUiThread { status.text = "GESTURE FRAME ERROR\n${e.message ?: "unknown"}" }
                } finally { image.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, cameraPreview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleResult(result: GestureRecognizerResult) {
        val action = interpreter.interpret(result) ?: return
        status.post { status.text = "GOD // GESTURE\n$action\n\nEngine stable • debounced" }
        sendBroadcast(android.content.Intent("com.example.god.GESTURE_ACTION").setPackage(packageName).putExtra("action", action))
        GestureEvents.emit(action)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startCamera() else finish()
    }

    override fun onDestroy() {
        recognizer?.close()
        executor.shutdownNow()
        super.onDestroy()
    }

    object GestureEvents {
        private var listener: ((String) -> Unit)? = null
        fun listen(block: (String) -> Unit) { listener = block }
        fun clear() { listener = null }
        fun emit(action: String) { listener?.invoke(action) }
    }
}
