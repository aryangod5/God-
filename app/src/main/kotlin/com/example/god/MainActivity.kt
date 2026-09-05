package com.example.god

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

    private val blue = Color.rgb(30, 150, 255)
    private val orange = Color.rgb(255, 145, 0)
    private val dark = Color.rgb(3, 7, 14)
    private val panelDark = Color.rgb(5, 12, 22)
    private val cardDark = Color.rgb(8, 20, 35)
    private val white = Color.rgb(225, 240, 255)

    private lateinit var root: LinearLayout

    private var voiceManager: VoiceManager? = null
    private var voiceDialog: AlertDialog? = null
    private var voiceStateText: TextView? = null
    private var voiceRecognizedText: TextView? = null
    private var pendingVoiceStart = false

    // Central GOD core.
    private var godCoreView: GodCoreView? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            Toast.makeText(
                this,
                "Permission setup updated.",
                Toast.LENGTH_SHORT
            ).show()

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
            }
        }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupVoiceManager()

        if (isSetupFinished()) {
            showGodHome()
        } else {
            showSetupScreen()
        }
    }

    private fun setupVoiceManager() {

        voiceManager = VoiceManager(
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
                                "YOU SAID\n\n$text"
                            }
                    }
                }

                override fun onError(
                    message: String
                ) {
                    runOnUiThread {

                        godCoreView?.setVoiceLevel(0f)

                        voiceStateText?.text =
                            "ERROR\n\n$message"

                        Toast.makeText(
                            this@MainActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun isSetupFinished(): Boolean {
        return getSharedPreferences(
            "GOD_SETUP",
            MODE_PRIVATE
        )
            .getBoolean(
                "setup_complete",
                false
            )
    }

    private fun markSetupFinished() {

        getSharedPreferences(
            "GOD_SETUP",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "setup_complete",
                true
            )
            .apply()
    }

    private fun baseLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                28,
                28,
                28,
                28
            )

            setBackgroundColor(dark)
        }
    }

    private fun makeScroll(
        content: View
    ): ScrollView {

        return ScrollView(this).apply {

            setBackgroundColor(dark)

            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun title(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text = text
            textSize = 28f
            setTextColor(orange)
            gravity = Gravity.CENTER

            setPadding(
                0,
                20,
                0,
                25
            )
        }
    }

    private fun subtitle(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text = text
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER

            setPadding(
                10,
                0,
                10,
                20
            )
        }
    }

    private fun roundedBackground(
        color: Int,
        strokeColor: Int = blue,
        strokeWidth: Int = 1,
        radius: Float = 24f
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            setStroke(
                strokeWidth,
                strokeColor
            )

            cornerRadius = radius
        }
    }

    private fun addSpace(
        layout: LinearLayout,
        height: Int
    ) {

        val space = View(this)

        layout.addView(
            space,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            )
        )
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): TextView {

        return TextView(this).apply {

            this.text = "◆  $text"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER

            setPadding(
                20,
                20,
                20,
                20
            )

            background =
                roundedBackground(
                    cardDark,
                    orange,
                    2,
                    22f
                )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                action()
            }

            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

            params.setMargins(
                0,
                8,
                0,
                8
            )

            layoutParams = params
        }
    }

    // =========================================================
    // INITIAL SETUP
    // =========================================================

    private fun showSetupScreen() {

        root = baseLayout()

        root.addView(
            title("GOD INITIAL SETUP")
        )

        root.addView(
            subtitle(
                "Configure GOD before first use."
            )
        )

        val status =
            TextView(this).apply {

                text =
                    "SYSTEM STATUS\n\n" +
                            "● Core: READY\n" +
                            "● Security: NOT CONFIGURED\n" +
                            "● Permissions: CHECK REQUIRED\n" +
                            "● Folder: NOT AUTHORIZED"

                textSize = 16f
                setTextColor(white)

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                background =
                    roundedBackground(
                        panelDark,
                        blue,
                        2,
                        25f
                    )
            }

        root.addView(status)

        addSpace(root, 20)

        root.addView(
            makeButton(
                "SET UP PERMISSIONS"
            ) {
                requestRequiredPermissions()
            }
        )

        root.addView(
            makeButton(
                "SET UP FINGERPRINT / FACE SECURITY"
            ) {
                setupBiometric()
            }
        )

        root.addView(
            makeButton(
                "AUTHORIZE FILE FOLDER"
            ) {
                authorizeFolder()
            }
        )

        root.addView(
            makeButton(
                "OPTIONAL PERMISSIONS"
            ) {
                requestOptionalPermissions()
            }
        )

        addSpace(root, 20)

        root.addView(
            makeButton(
                "FINISH GOD SETUP"
            ) {

                markSetupFinished()

                Toast.makeText(
                    this,
                    "GOD setup complete.",
                    Toast.LENGTH_SHORT
                ).show()

                showGodHome()
            }
        )

        setContentView(
            makeScroll(root)
        )
    }

    private fun requestRequiredPermissions() {

        val permissions =
            mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= 23) {

            permissions.add(
                Manifest.permission.RECORD_AUDIO
            )

            permissions.add(
                Manifest.permission.CAMERA
            )
        }

        if (Build.VERSION.SDK_INT >= 33) {

            permissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        if (permissions.isNotEmpty()) {

            permissionLauncher.launch(
                permissions.toTypedArray()
            )
        }
    }

    private fun requestOptionalPermissions() {

        val permissions =
            mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= 32) {

            permissions.add(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        if (permissions.isNotEmpty()) {

            permissionLauncher.launch(
                permissions.toTypedArray()
            )

        } else {

            Toast.makeText(
                this,
                "No additional optional permissions are required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun authorizeFolder() {
        folderLauncher.launch(null)
    }

    // =========================================================
    // BIOMETRIC
    // =========================================================

    private fun setupBiometric() {

        val biometricManager =
            BiometricManager.from(this)

        val result =
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )

        if (
            result ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {

            Toast.makeText(
                this,
                "Biometric security is available on this device.",
                Toast.LENGTH_LONG
            ).show()

        } else {

            Toast.makeText(
                this,
                "Fingerprint / face biometric authentication is not available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun authenticateSecurity(
        onSuccess: () -> Unit
    ) {

        val executor =
            ContextCompat.getMainExecutor(this)

        val prompt =
            BiometricPrompt(
                this,
                executor,
                object :
                    BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result:
                        BiometricPrompt.AuthenticationResult
                    ) {

                        super.onAuthenticationSucceeded(
                            result
                        )

                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {

                        super.onAuthenticationError(
                            errorCode,
                            errString
                        )

                        Toast.makeText(
                            this@MainActivity,
                            "Authentication cancelled.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("GOD Security")
                .setSubtitle(
                    "Authenticate to continue"
                )
                .setNegativeButtonText(
                    "Cancel"
                )
                .build()

        prompt.authenticate(info)
    }

    // =========================================================
    // GOD HOME
    // =========================================================

    private fun showGodHome() {

        root = baseLayout()

        val topBar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val godText =
            TextView(this).apply {

                text = "GOD"
                textSize = 28f
                setTextColor(orange)
            }

        topBar.addView(
            godText,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val menuButton =
            TextView(this).apply {

                text = "⋮"
                textSize = 32f
                gravity = Gravity.CENTER
                setTextColor(white)

                setPadding(
                    20,
                    0,
                    10,
                    0
                )

                setOnClickListener {
                    showGodMenu()
                }
            }

        topBar.addView(menuButton)

        root.addView(topBar)

        addSpace(root, 5)

        godCoreView =
            GodCoreView(this)

        root.addView(
            godCoreView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val greeting =
            TextView(this).apply {

                text =
                    "HELLO, MASTER.\nGOD IS READY."

                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(white)

                setPadding(
                    0,
                    10,
                    0,
                    10
                )
            }

        root.addView(greeting)

        setContentView(root)
    }

    // =========================================================
    // GOD MENU
    // =========================================================

    private fun showGodMenu() {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "GOD SYSTEM MENU"
                )
                .setItems(
                    arrayOf(
                        "VOICE",
                        "CHAT",
                        "APPS",
                        "FILES",
                        "DOCUMENTS",
                        "MEMORY",
                        "AI PROVIDER / API",
                        "SECURITY",
                        "PERMISSIONS",
                        "AUTHORIZED FOLDER",
                        "SETTINGS"
                    )
                ) { _, which ->

                    when (which) {

                        0 -> openVoiceAssistant()

                        1 -> showChatScreen()

                        2 -> showMessage(
                            "APPS",
                            "App management module is ready."
                        )

                        3 -> showMessage(
                            "FILES",
                            "File module is ready. Use the Authorized Folder option first."
                        )

                        4 -> showMessage(
                            "DOCUMENTS",
                            "Document module is ready."
                        )

                        5 -> showMessage(
                            "MEMORY",
                            "GOD memory module is ready."
                        )

                        6 -> openAIProvider()

                        7 -> securitySettings()

                        8 -> permissionSettings()

                        9 -> authorizeFolder()

                        10 -> showSettings()
                    }
                }
                .create()

        dialog.show()
    }

    // =========================================================
    // VOICE ASSISTANT
    // =========================================================

    private fun openVoiceAssistant() {

        if (
            ContextCompat.checkSelfPermission(
          
