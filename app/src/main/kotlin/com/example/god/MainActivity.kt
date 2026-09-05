package com.example.god

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.god.voice.AIState
import com.example.god.voice.VoiceManager

class MainActivity : AppCompatActivity() {

    // ============================================================
    // GOD COLORS
    // ============================================================

    private val blue = Color.rgb(30, 150, 255)
    private val orange = Color.rgb(255, 145, 0)
    private val dark = Color.rgb(3, 7, 14)
    private val panelDark = Color.rgb(5, 12, 22)
    private val cardDark = Color.rgb(8, 20, 35)
    private val white = Color.rgb(225, 240, 255)

    // ============================================================
    // VIEWS / MANAGERS
    // ============================================================

    private lateinit var root: LinearLayout

    private var voiceManager: VoiceManager? = null

    private var voiceDialog: AlertDialog? = null

    private var voiceStateText: TextView? = null

    private var voiceRecognizedText: TextView? = null

    private var pendingVoiceStart = false

    private var godCoreView: GodCoreView? = null

    // ============================================================
    // PERMISSION HANDLER
    // ============================================================

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            if (pendingVoiceStart) {

                pendingVoiceStart = false

                val microphoneGranted =
                    result[Manifest.permission.RECORD_AUDIO] == true ||
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                if (microphoneGranted) {

                    showVoicePanel()

                    voiceManager?.startListening()

                } else {

                    showMessage(
                        "VOICE",
                        "Microphone permission is required for voice control."
                    )
                }

            } else {

                Toast.makeText(
                    this,
                    "Permission setup updated.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ============================================================
    // AUTHORIZED FOLDER HANDLER
    // ============================================================

    private val folderLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->

            if (uri != null) {

                try {

                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                } catch (_: Exception) {
                }

                getSharedPreferences(
                    "GOD_SETUP",
                    MODE_PRIVATE
                )
                    .edit()
                    .putString(
                        "authorized_folder",
                        uri.toString()
                    )
                    .apply()

                Toast.makeText(
                    this,
                    "Authorized folder saved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ============================================================
    // ACTIVITY START
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setupVoiceManager()

        if (isSetupFinished()) {

            showGodHome()

        } else {

            showSetupScreen()
        }
    }

    // ============================================================
    // VOICE MANAGER
    // ============================================================

    private fun setupVoiceManager() {

        voiceManager =
            VoiceManager(
                this,
                object : VoiceManager.Listener {

                    override fun onStateChanged(
                        state: AIState
                    ) {

                        runOnUiThread {

                            godCoreView?.setAIState(state)

                            updateVoiceState(state)
                        }
                    }

                    override fun onVoiceLevel(
                        level: Float
                    ) {

                        runOnUiThread {

                            godCoreView?.setVoiceLevel(level)
                        }
                    }

                    override fun onTextRecognized(
                        text: String
                    ) {

                        runOnUiThread {

                            voiceRecognizedText?.text =
                                if (text.isBlank()) {

                                    "VOICE INPUT\n\nWaiting..."

                                } else {

                                    "YOU SAID\n
