package com.example.god

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("GOD_SETTINGS", Context.MODE_PRIVATE)
    }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            showSetupScreen()
        }

    private val folderPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                prefs.edit()
                    .putString("authorized_folder", uri.toString())
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

        if (prefs.getBoolean("setup_complete", false)) {
            showGodHome()
        } else {
            showSetupScreen()
        }
    }

    // ============================================================
    // GOD SETUP
    // ============================================================

    private fun showSetupScreen() {

        val scroll = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 60, 40, 60)
        }

        val title = makeText(
            "GOD",
            42f,
            Gravity.CENTER
        )

        val subtitle = makeText(
            "Initial Setup",
            24f,
            Gravity.CENTER
        )

        val explanation = makeText(
            "\nWelcome to GOD.\n\n" +
                    "Before using GOD, Android permissions and " +
                    "security features need to be configured.\n\n" +
                    "Permissions granted to GOD are remembered by Android " +
                    "and this app will not repeatedly request them unless " +
                    "Android requires permission again.",
            17f,
            Gravity.CENTER
        )

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(explanation)

        layout.addView(
            makeButton("1. Required Permissions") {
                requestRequiredPermissions()
            }
        )

        layout.addView(
            makeButton("2. Fingerprint / Face Security") {
                setupBiometric()
            }
        )

        layout.addView(
            makeButton("3. Authorize File Folder") {
                folderPicker.launch(null)
            }
        )

        layout.addView(
            makeButton("4. Optional Permissions") {
                requestOptionalPermissions()
            }
        )

        layout.addView(
            makeButton("Finish GOD Setup") {

                prefs.edit()
                    .putBoolean("setup_complete", true)
                    .apply()

                showGodHome()
            }
        )

        val aiInfo = makeText(
            "\nAI PROVIDER\n\n" +
                    "GOD will contain an AI Provider section where " +
                    "you can configure an API endpoint, API key and " +
                    "model. This will be connected to the GOD AI system.",
            16f,
            Gravity.CENTER
        )

        layout.addView(aiInfo)

        scroll.addView(layout)

        setContentView(scroll)
    }

    // ============================================================
    // PERMISSIONS
    // ============================================================

    private fun requestRequiredPermissions() {

        val permissions = mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isEmpty()) {

            Toast.makeText(
                this,
                "Required permissions are already granted.",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            permissionLauncher.launch(
                permissions.toTypedArray()
            )
        }
    }

    private fun requestOptionalPermissions() {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {

            if (
                checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )

                return
            }
        }

        Toast.makeText(
            this,
            "No additional runtime permission is required here.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ============================================================
    // BIOMETRIC SECURITY
    // ============================================================

    private fun setupBiometric() {

        val biometricManager =
            BiometricManager.from(this)

        val result =
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )

        when (result) {

            BiometricManager.BIOMETRIC_SUCCESS -> {

                val executor =
                    ContextCompat.getMainExecutor(this)

                val prompt =
                    BiometricPrompt(
                        this,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult
                            ) {
                                super.onAuthenticationSucceeded(result)

                                prefs.edit()
                                    .putBoolean(
                                        "biometric_enabled",
                                        true
                                    )
                                    .apply()

                                Toast.makeText(
                                    this@MainActivity,
                                    "GOD biometric security enabled.",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                                    errString,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                val info =
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("GOD Security")
                        .setSubtitle(
                            "Confirm your fingerprint or face"
                        )
                        .setNegativeButtonText("Cancel")
                        .build()

                prompt.authenticate(info)
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {

                Toast.makeText(
                    this,
                    "No fingerprint or face is enrolled. " +
                            "Set one up in Android Settings.",
                    Toast.LENGTH_LONG
                ).show()

                openSecuritySettings()
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {

                Toast.makeText(
                    this,
                    "This device does not support biometric hardware.",
                    Toast.LENGTH_LONG
                ).show()
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {

                Toast.makeText(
                    this,
                    "Biometric hardware is currently unavailable.",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {

                Toast.makeText(
                    this,
                    "Biometric authentication is unavailable.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openSecuritySettings() {

        try {
            startActivity(
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            )
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_SETTINGS)
            )
        }
    }

    // ============================================================
    // GOD HOME
    // ============================================================

    private fun showGodHome() {

        val scroll = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(35, 45, 35, 45)
        }

        val title = makeText(
            "GOD",
            44f,
            Gravity.CENTER
        )

        val greeting = makeText(
            "\nHello, Master.\n\n" +
                    "GOD is ready.",
            21f,
            Gravity.CENTER
        )

        layout.addView(title)
        layout.addView(greeting)

        layout.addView(
            makeButton("🎤 Voice Assistant") {
                showModuleMessage(
                    "Voice Assistant",
                    "Voice recognition and speech control will be connected here."
                )
            }
        )

        layout.addView(
            makeButton("💬 AI Chat") {
                showModuleMessage(
                    "AI Chat",
                    "The configured AI provider will power GOD Chat."
                )
            }
        )

        layout.addView(
            makeButton("📱 Apps") {
                showModuleMessage(
                    "App Control",
                    "GOD will detect compatible installed apps and launch them using Android APIs."
                )
            }
        )

        layout.addView(
            makeButton("📁 Files") {
                showModuleMessage(
                    "Files",
                    "GOD will search files inside folders that you authorize."
                )
            }
        )

        layout.addView(
            makeButton("📄 Documents") {
                showModuleMessage(
                    "Documents",
                    "Document and PDF creation will be connected here."
                )
            }
        )

        layout.addView(
            makeButton("🧊 3D") {
                showModuleMessage(
                    "3D",
                    "The 3D model and interactive-document system will be connected here."
                )
            }
        )

        layout.addView(
            makeButton("🧠 Memory") {
                showModuleMessage(
                    "Memory",
                    "GOD memory will only save information when you explicitly request it."
                )
            }
        )

        layout.addView(
            makeButton("⚙️ Settings") {
                showSettings()
            }
        )

        scroll.addView(layout)

        setContentView(scroll)
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    private fun showSettings() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 45, 35, 45)
        }

        layout.addView(
            makeText(
                "GOD Settings",
                32f,
                Gravity.CENTER
            )
        )

        layout.addView(
            makeButton("🤖 AI Provider / API") {
                showModuleMessage(
                    "AI Provider",
                    "This section will let you enter an API endpoint, API key and model name."
                )
            }
        )

        layout.addView(
            makeButton("🔐 Security & Biometrics") {
                setupBiometric()
            }
        )

        layout.addView(
            makeButton("📁 Authorized Folder") {
                folderPicker.launch(null)
            }
        )

        layout.addView(
            makeButton("🔔 Permission Settings") {
                openAppSettings()
            }
        )

        layout.addView(
            makeButton("Run Initial Setup Again") {

                prefs.edit()
                    .putBoolean("setup_complete", false)
                    .apply()

                showSetupScreen()
            }
        )

        layout.addView(
            makeButton("← Back to GOD") {
                showGodHome()
            }
        )

        setContentView(layout)
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun makeText(
        text: String,
        size: Float,
        gravity: Int
    ): TextView {

        return TextView(this).apply {
            this.text = text
            textSize = size
            this.gravity = gravity
            setPadding(10, 15, 10, 15)
            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
        }
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            this.text = text

            textSize = 16f

            setOnClickListener {
                action()
            }

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10)
                }
        }
    }

    private fun showModuleMessage(
        title: String,
        message: String
    ) {

        Toast.makeText(
            this,
            "$title\n$message",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun openAppSettings() {

        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = Uri.parse(
                    "package:$packageName"
                )
            }

        startActivity(intent)
    }
}
