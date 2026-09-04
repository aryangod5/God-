package com.example.god

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
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

    private val preferencesName = "GOD_SETTINGS"

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            Toast.makeText(
                this,
                "Permission setup checked.",
                Toast.LENGTH_SHORT
            ).show()
        }

    private val folderLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->

            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    getSharedPreferences(preferencesName, MODE_PRIVATE)
                        .edit()
                        .putString("authorized_folder", uri.toString())
                        .apply()

                    Toast.makeText(
                        this,
                        "Folder authorized for GOD.",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Could not authorize this folder.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences =
            getSharedPreferences(preferencesName, MODE_PRIVATE)

        if (!preferences.getBoolean("setup_complete", false)) {
            showInitialSetup()
        } else {
            showGodHome()
        }
    }

    private fun showInitialSetup() {

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(35, 50, 35, 50)
        }

        val title = TextView(this).apply {
            text = "GOD"
            textSize = 42f
            gravity = Gravity.CENTER
        }

        layout.addView(title)

        val description = TextView(this).apply {
            text =
                "\nWelcome, Master.\n\n" +
                "Let's complete the initial GOD setup.\n\n" +
                "First we will check the permissions and security options GOD can use on this device."
            textSize = 18f
            gravity = Gravity.CENTER
        }

        layout.addView(description)

        val permissionsButton = Button(this).apply {
            text = "1. Set Up Permissions"

            setOnClickListener {
                requestPermissionsForGod()
            }
        }

        layout.addView(permissionsButton)

        val biometricButton = Button(this).apply {
            text = "2. Set Up Fingerprint / Face Security"

            setOnClickListener {
                setupBiometric()
            }
        }

        layout.addView(biometricButton)

        val folderButton = Button(this).apply {
            text = "3. Authorize File Folder"

            setOnClickListener {
                openFolderPicker()
            }
        }

        layout.addView(folderButton)

        val finishButton = Button(this).apply {
            text = "Finish GOD Setup"

            setOnClickListener {

                getSharedPreferences(preferencesName, MODE_PRIVATE)
                    .edit()
                    .putBoolean("setup_complete", true)
                    .apply()

                showGodHome()
            }
        }

        layout.addView(finishButton)

        scrollView.addView(layout)

        setContentView(scrollView)
    }

    private fun requestPermissionsForGod() {

        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun setupBiometric() {

        val biometricManager =
            BiometricManager.from(this)

        when (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
        ) {

            BiometricManager.BIOMETRIC_SUCCESS -> {

                val executor =
                    ContextCompat.getMainExecutor(this)

                val biometricPrompt =
                    BiometricPrompt(
                        this,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult
                            ) {
                                super.onAuthenticationSucceeded(result)

                                getSharedPreferences(
                                    preferencesName,
                                    MODE_PRIVATE
                                )
                                    .edit()
                                    .putBoolean(
                                        "biometric_enabled",
                                        true
                                    )
                                    .apply()

                                Toast.makeText(
                                    this@MainActivity,
                                    "Biometric security is ready.",
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
                                    "Biometric setup cancelled.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                val promptInfo =
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("GOD Security")
                        .setSubtitle("Confirm your fingerprint or face")
                        .setNegativeButtonText("Cancel")
                        .build()

                biometricPrompt.authenticate(promptInfo)
            }

            else -> {

                Toast.makeText(
                    this,
                    "Strong biometric authentication is not available on this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openFolderPicker() {

        folderLauncher.launch(null)
    }

    private fun showGodHome() {

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(35, 50, 35, 50)
        }

        val title = TextView(this).apply {
            text = "GOD"
            textSize = 42f
            gravity = Gravity.CENTER
        }

        layout.addView(title)

        val greeting = TextView(this).apply {
            text = "\nHello, Master."
            textSize = 21f
            gravity = Gravity.CENTER
        }

        layout.addView(greeting)

        val status = TextView(this).apply {
            text = "\nGOD is ready."
            textSize = 17f
            gravity = Gravity.CENTER
        }

        layout.addView(status)

        addModuleButton(
            layout,
            "Voice Assistant"
        ) {
            Toast.makeText(
                this,
                "Voice Assistant will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "AI Chat"
        ) {
            Toast.makeText(
                this,
                "AI Chat will use your configured AI provider.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "Apps"
        ) {
            Toast.makeText(
                this,
                "App control will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "Files"
        ) {
            Toast.makeText(
                this,
                "File access will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "Documents"
        ) {
            Toast.makeText(
                this,
                "Document tools will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "3D"
        ) {
            Toast.makeText(
                this,
                "3D tools will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "Memory"
        ) {
            Toast.makeText(
                this,
                "Memory will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addModuleButton(
            layout,
            "Settings"
        ) {
            showSettings()
        }

        scrollView.addView(layout)

        setContentView(scrollView)
    }

    private fun addModuleButton(
        layout: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = Button(this).apply {
            this.text = text
            setOnClickListener {
                action()
            }
        }

        layout.addView(button)
    }

    private fun showSettings() {

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 50, 35, 50)
        }

        val title = TextView(this).apply {
            text = "GOD Settings"
            textSize = 30f
            gravity = Gravity.CENTER
        }

        layout.addView(title)

        val aiButton = Button(this).apply {
            text = "AI Provider / API"

            setOnClickListener {

                val intent =
                    Intent(
                        this@MainActivity,
                        AIProviderActivity::class.java
                    )

                startActivity(intent)
            }
        }

        layout.addView(aiButton)

        val securityButton = Button(this).apply {
            text = "Security & Biometrics"

            setOnClickListener {
                setupBiometric()
            }
        }

        layout.addView(securityButton)

        val folderButton = Button(this).apply {
            text = "Authorized Folder"

            setOnClickListener {
                openFolderPicker()
            }
        }

        layout.addView(folderButton)

        val permissionsButton = Button(this).apply {
            text = "Permission Settings"

            setOnClickListener {
                openAppSettings()
            }
        }

        layout.addView(permissionsButton)

        val resetButton = Button(this).apply {
            text = "Run Initial Setup Again"

            setOnClickListener {

                getSharedPreferences(
                    preferencesName,
                    MODE_PRIVATE
                )
                    .edit()
                    .putBoolean("setup_complete", false)
                    .apply()

                showInitialSetup()
            }
        }

        layout.addView(resetButton)

        val backButton = Button(this).apply {
            text = "Back to GOD"

            setOnClickListener {
                showGodHome()
            }
        }

        layout.addView(backButton)

        scrollView.addView(layout)

        setContentView(scrollView)
    }

    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        intent.data =
            Uri.parse("package:$packageName")

        startActivity(intent)
    }
}
