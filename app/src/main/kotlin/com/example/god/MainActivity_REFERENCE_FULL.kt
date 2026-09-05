package com.example.god

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.god.voice.AIState
import com.example.god.voice.VoiceManager

/**
 * Main host for the code-drawn GOD reference interface.
 *
 * Important:
 * - GodReferenceUi owns the visual HUD and its touch zones.
 * - VoiceManager remains the existing voice/STT/TTS engine.
 * - No API keys or secrets are stored here.
 * - The center reactor remains touchable without drawing a visible microphone button.
 */
class MainActivity : ComponentActivity() {

    private var godUi: GodReferenceUi? = null
    private var voiceManager: VoiceManager? = null

    private val micPermissionCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        setupVoiceManager()
        showReferenceHome()
    }

    private fun showReferenceHome() {
        val ui = GodReferenceUi(this) { action ->
            handleUiAction(action)
        }

        godUi = ui
        setContentView(ui)
    }

    private fun setupVoiceManager() {
        voiceManager = VoiceManager(
            this,
            object : VoiceManager.Listener {
                override fun onStateChanged(state: AIState) {
                    runOnUiThread {
                        val ui = godUi ?: return@runOnUiThread

                        ui.setVoiceActive(state == AIState.LISTENING)
                        ui.setProcessing(state == AIState.PROCESSING)
                        ui.setSpeaking(state == AIState.SPEAKING)

                        when (state) {
                            AIState.LISTENING -> ui.showNotice("VOICE", "LISTENING")
                            AIState.PROCESSING -> ui.showNotice("GOD AI", "PROCESSING")
                            AIState.SPEAKING -> ui.showNotice("GOD AI", "SPEAKING")
                            AIState.ERROR -> ui.showNotice("SYSTEM", "VOICE ERROR")
                            else -> Unit
                        }
                    }
                }

                override fun onVoiceLevel(level: Float) {
                    runOnUiThread {
                        godUi?.setVoiceLevel(level)
                    }
                }

                override fun onTextRecognized(text: String) {
                    // The reference HUD deliberately keeps the main screen clean.
                    // VoiceManager still receives and processes the recognized text.
                    if (text.isNotBlank()) {
                        runOnUiThread {
                            godUi?.showNotice("VOICE INPUT", "RECOGNIZED")
                        }
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        godUi?.setVoiceLevel(0f)
                        godUi?.showNotice("VOICE ERROR", message)
                    }
                }
            }
        )
    }

    private fun handleUiAction(action: String) {
        when (action) {
            GodReferenceUi.ACTION_VOICE -> {
                clickSound()
                startVoiceInput()
            }

            GodReferenceUi.ACTION_CHAT -> {
                clickSound()
                showChatScreen()
            }

            GodReferenceUi.ACTION_APPS -> {
                clickSound()
                showAppsManager()
            }

            GodReferenceUi.ACTION_FILES -> {
                clickSound()
                openFiles()
            }

            GodReferenceUi.ACTION_DOCUMENTS -> {
                clickSound()
                showModuleNotice("DOCUMENTS", "MODULE READY")
            }

            GodReferenceUi.ACTION_MEMORY -> {
                clickSound()
                showModuleNotice("MEMORY", "MODULE READY")
            }

            GodReferenceUi.ACTION_PROVIDER -> {
                clickSound()
                openAIProvider()
            }

            GodReferenceUi.ACTION_SECURITY -> {
                clickSound()
                openSecuritySettings()
            }

            GodReferenceUi.ACTION_PERMISSIONS -> {
                clickSound()
                openAppSettings()
            }

            GodReferenceUi.ACTION_AUTHORIZED -> {
                clickSound()
                openAuthorizedFolder()
            }

            GodReferenceUi.ACTION_SETTINGS -> {
                clickSound()
                openSettings()
            }

            GodReferenceUi.ACTION_GESTURE -> {
                clickSound()
                showModuleNotice("GESTURE CONTROL", "CONTROL READY")
            }

            GodReferenceUi.ACTION_BACK -> {
                clickSound()
            }
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                micPermissionCode
            )
            godUi?.showNotice("VOICE", "MICROPHONE PERMISSION REQUIRED")
            return
        }

        godUi?.showNotice("VOICE", "ACTIVATED")
        voiceManager?.startListening()
    }

    private fun showModuleNotice(title: String, message: String) {
        godUi?.showNotice(title, message)
    }

    private fun showAppsManager() {
        // If a dedicated AppsActivity exists in the project, use it without
        // introducing a compile-time dependency on a class that may not exist.
        try {
            val intent = Intent(this, Class.forName("com.example.god.AppsActivity"))
            startActivity(intent)
        } catch (_: Exception) {
            godUi?.showNotice("APPS", "MANAGER READY")
        }
    }

    private fun showChatScreen() {
        // Keep the existing working chat entry point simple and dependency-free.
        val input = android.widget.EditText(this).apply {
            hint = "ASK GOD..."
            setHintTextColor(Color.rgb(100, 100, 100))
            setTextColor(Color.WHITE)
            textSize = 16f
            setSingleLine(false)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = hudFrame()
        }

        val conversation = android.widget.TextView(this).apply {
            text = "GOD\n\nHello, Master. GOD is ready."
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = hudFrame()
        }

        val send = android.widget.TextView(this).apply {
            text = "SEND TO GOD"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            background = hudFrame()
            setOnClickListener {
                clickSound()
                val value = input.text.toString().trim()
                if (value.isEmpty()) return@setOnClickListener

                conversation.append("\n\nYOU\n\n$value\n\nGOD\n\nI received your message.")
                input.text.clear()
            }
        }

        val back = android.widget.TextView(this).apply {
            text = "BACK TO CORE"
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            setOnClickListener {
                clickSound()
                showReferenceHome()
            }
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(Color.rgb(1, 3, 5))
        }

        layout.addView(
            android.widget.TextView(this).apply {
                text = "G O D  //  CHAT"
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                typeface = android.graphics.Typeface.MONOSPACE
            },
            android.widget.LinearLayout.LayoutParams(-1, dp(58))
        )
        layout.addView(
            conversation,
            android.widget.LinearLayout.LayoutParams(-1, 0, 1f)
        )
        layout.addView(
            input,
            android.widget.LinearLayout.LayoutParams(-1, dp(88))
        )
        layout.addView(
            send,
            android.widget.LinearLayout.LayoutParams(-1, dp(50))
        )
        layout.addView(
            back,
            android.widget.LinearLayout.LayoutParams(-1, dp(44))
        )

        setContentView(layout)
    }

    private fun openFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        try {
            startActivityForResult(intent, 2001)
        } catch (_: Exception) {
            godUi?.showNotice("FILES", "FILE PICKER UNAVAILABLE")
        }
    }

    private fun openAuthorizedFolder() {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 2002)
        } catch (_: Exception) {
            godUi?.showNotice("FOLDER", "FOLDER PICKER UNAVAILABLE")
        }
    }

    private fun openAIProvider() {
        try {
            startActivity(Intent(this, AIProviderActivity::class.java))
        } catch (_: Exception) {
            godUi?.showNotice("AI PROVIDER", "MODULE NOT AVAILABLE")
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) {
            godUi?.showNotice("PERMISSIONS", "SETTINGS UNAVAILABLE")
        }
    }

    private fun openSecuritySettings() {
        try {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: Exception) {
            godUi?.showNotice("SECURITY", "SETTINGS UNAVAILABLE")
        }
    }

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            godUi?.showNotice("SETTINGS", "SETTINGS UNAVAILABLE")
        }
    }

    private fun hudFrame(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.argb(238, 6, 9, 13))
            setStroke(dp(1), Color.rgb(255, 145, 0))
            cornerRadius = dp(5).toFloat()
        }
    }

    private fun clickSound() {
        try {
            val tone = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION,
                55
            )
            tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 65)
            android.os.Handler(mainLooper).postDelayed({ tone.release() }, 100)
        } catch (_: Exception) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == micPermissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else if (requestCode == micPermissionCode) {
            godUi?.showNotice("VOICE", "MICROPHONE ACCESS DENIED")
            Toast.makeText(this, "Microphone permission is required for voice control.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        when (requestCode) {
            2001 -> godUi?.showNotice("FILES", "FILE SELECTED")
            2002 -> godUi?.showNotice("AUTHORIZED FOLDER", "FOLDER SAVED")
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        voiceManager?.release()
        voiceManager = null
        godUi = null
        super.onDestroy()
    }
}
