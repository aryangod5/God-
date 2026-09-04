package com.example.god

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat as AndroidContextCompat

class MainActivity : AppCompatActivity() {

    private val blue = Color.rgb(30, 150, 255)
    private val brightBlue = Color.rgb(80, 190, 255)
    private val orange = Color.rgb(255, 145, 0)
    private val white = Color.rgb(225, 240, 255)
    private val dark = Color.rgb(3, 7, 14)

    private lateinit var rootLayout: LinearLayout

    private val setupPreferences by lazy {
        getSharedPreferences("GOD_SETUP", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (setupPreferences.getBoolean("setup_complete", false)) {
            showGodHome()
        } else {
            showSetupScreen()
        }
    }

    // ============================================================
    // GOD HOME
    // ============================================================

    private fun showGodHome() {

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        /*
         * TOP BAR
         */

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 18, 18, 10)
        }

        val menuButton = TextView(this).apply {
            text = "⋮"
            textSize = 32f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(18, 0, 18, 0)

            setOnClickListener {
                showGodMenu()
            }
        }

        val title = TextView(this).apply {
            text = "GOD"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(orange)
            gravity = Gravity.CENTER
        }

        val titleParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        topBar.addView(menuButton)
        topBar.addView(title, titleParams)

        /*
         * CORE AREA
         */

        val coreContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val core = GodCoreView(this)

        val coreParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        coreContainer.addView(core, coreParams)

        /*
         * STATUS
         */

        val status = TextView(this).apply {
            text = "GOD • ONLINE"
            textSize = 15f
            setTextColor(brightBlue)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }

        /*
         * TEXT INPUT
         */

        val inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 8, 18, 18)
        }

        val messageBox = TextView(this).apply {
            text = "Tap the GOD Core to speak"
            textSize = 15f
            setTextColor(Color.rgb(150, 180, 210))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 20, 18)
            background = roundedBackground(
                Color.rgb(8, 20, 35),
                Color.rgb(25, 100, 180),
                24f
            )

            setOnClickListener {
                showTextInput()
            }
        }

        val messageParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        inputBar.addView(messageBox, messageParams)

        rootLayout.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        rootLayout.addView(
            coreContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        rootLayout.addView(status)

        rootLayout.addView(inputBar)

        setContentView(rootLayout)

        /*
         * TAP CORE = VOICE
         */

        core.setOnClickListener {
            startVoiceMode()
        }
    }

    // ============================================================
    // FUTURISTIC MENU
    // ============================================================

    private fun showGodMenu() {

        val scroll = ScrollView(this).apply {
            setBackgroundColor(dark)
        }

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 35, 28, 35)
        }

        val header = TextView(this).apply {
            text = "GOD SYSTEM"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(orange)
            setPadding(0, 0, 0, 8)
        }

        val subtitle = TextView(this).apply {
            text = "CONTROL CENTER"
            textSize = 12f
            setTextColor(blue)
            setPadding(0, 0, 0, 25)
        }

        menu.addView(header)
        menu.addView(subtitle)

        addMenuItem(menu, "◉", "VOICE ASSISTANT") {
            startVoiceMode()
        }

        addMenuItem(menu, "◇", "CHAT") {
            showTextInput()
        }

        addMenuItem(menu, "◆", "APPS") {
            showMessage("Apps module")
        }

        addMenuItem(menu, "◇", "FILES") {
            showMessage("Files module")
        }

        addMenuItem(menu, "◆", "DOCUMENTS") {
            showMessage("Documents module")
        }

        addMenuItem(menu, "◇", "MEMORY") {
            showMessage("Memory module")
        }

        addMenuDivider(menu)

        addMenuItem(menu, "◆", "AI PROVIDER / API") {
            val intent = Intent(
                this,
                AIProviderActivity::class.java
            )
            startActivity(intent)
        }

        addMenuItem(menu, "◆", "SECURITY") {
            showSecurityPanel()
        }

        addMenuItem(menu, "◆", "PERMISSIONS") {
            showPermissionPanel()
        }

        addMenuItem(menu, "◆", "AUTHORIZED FOLDER") {
            authorizeFolder()
        }

        addMenuItem(menu, "◆", "SETTINGS") {
            showSettingsPanel()
        }

        addMenuDivider(menu)

        val back = TextView(this).apply {
            text = "‹  BACK TO GOD"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(20, 25, 20, 25)

            setOnClickListener {
                showGodHome()
            }
        }

        menu.addView(back)

        scroll.addView(menu)
        setContentView(scroll)
    }

    private fun addMenuItem(
        menu: LinearLayout,
        icon: String,
        title: String,
        action: () -> Unit
    ) {

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 20, 20, 20)

            background = roundedBackground(
                Color.rgb(7, 18, 31),
                Color.rgb(25, 90, 160),
                22f
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                action()
            }
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 20f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }

        val textView = TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            setPadding(18, 0, 0, 0)
        }

        item.addView(
            iconView,
            LinearLayout.LayoutParams(
                45,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        item.addView(
            textView,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 7, 0, 7)

        menu.addView(item, params)
    }

    private fun addMenuDivider(menu: LinearLayout) {

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(20, 80, 130))
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        )

        params.setMargins(0, 18, 0, 18)

        menu.addView(divider, params)
    }

    // ============================================================
    // VOICE
    // ============================================================

    private fun startVoiceMode() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )

            return
        }

        Toast.makeText(
            this,
            "GOD is listening...",
            Toast.LENGTH_SHORT
        ).show()

        /*
         * Real speech recognition will be connected
         * in the next voice module step.
         */
    }

    // ============================================================
    // TEXT CHAT
    // ============================================================

    private fun showTextInput() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 35, 25, 25)
            setBackgroundColor(dark)
        }

        val title = TextView(this).apply {
            text = "GOD // CHAT"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(orange)
            setPadding(0, 0, 0, 25)
        }

        val input = android.widget.EditText(this).apply {
            hint = "Ask GOD anything..."
            textSize = 17f
            setTextColor(white)
            setHintTextColor(Color.rgb(100, 130, 160))
            setPadding(20, 20, 20, 20)
            background = roundedBackground(
                Color.rgb(8, 20, 35),
                blue,
                22f
            )
        }

        val send = TextView(this).apply {
            text = "SEND  ➤"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)

            background = roundedBackground(
                Color.rgb(10, 30, 50),
                orange,
                24f
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "AI connection will be connected next.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        layout.addView(title)
        layout.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val sendParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        sendParams.setMargins(0, 20, 0, 0)

        layout.addView(send, sendParams)

        val back = createBackButton {
            showGodHome()
        }

        layout.addView(back)

        setContentView(layout)
    }

    // ============================================================
    // PERMISSIONS
    // ============================================================

    private fun showPermissionPanel() {

        val panel = GodPanel.create(
            this,
            "GOD // Permissions"
        )

        GodPanel.addSection(
            this,
            panel,
            "MICROPHONE",
            if (hasPermission(Manifest.permission.RECORD_AUDIO))
                "✓ ENABLED"
            else
                "○ NOT GRANTED"
        )

        GodPanel.addSection(
            this,
            panel,
            "CAMERA",
            if (hasPermission(Manifest.permission.CAMERA))
                "✓ ENABLED"
            else
                "○ NOT GRANTED"
        )

        if (Build.VERSION.SDK_INT >= 33) {

            GodPanel.addSection(
                this,
                panel,
                "NOTIFICATIONS",
                if (hasPermission(Manifest.permission.POST_NOTIFICATIONS))
                    "✓ ENABLED"
                else
                    "○ NOT GRANTED"
            )
        }

        GodPanel.addButton(
            this,
            panel,
            "OPEN ANDROID PERMISSION SETTINGS"
        ) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        GodPanel.addButton(
            this,
            panel,
            "BACK TO GOD"
        ) {
            showGodHome()
        }

        setContentView(panel)
    }

    private fun hasPermission(permission: String): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ============================================================
    // SECURITY
    // ============================================================

    private fun showSecurityPanel() {

        val panel = GodPanel.create(
            this,
            "GOD // Security"
        )

        val biometricManager =
            BiometricManager.from(this)

        val biometricAvailable =
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS

        GodPanel.addSection(
            this,
            panel,
            "BIOMETRIC SECURITY",
            if (biometricAvailable)
                "✓ AVAILABLE"
            else
                "○ NOT AVAILABLE"
        )

        GodPanel.addButton(
            this,
            panel,
            "TEST BIOMETRIC"
        ) {
            authenticateWithBiometric()
        }

        GodPanel.addButton(
            this,
            panel,
            "BACK TO GOD"
        ) {
            showGodHome()
        }

        setContentView(panel)
    }

    private fun authenticateWithBiometric() {

        val executor =
            AndroidContextCompat.getMainExecutor(this)

        val prompt =
            BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)

                        Toast.makeText(
                            this@MainActivity,
                            "GOD security verified.",
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
                    }
                }
            )

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("GOD Security")
                .setSubtitle("Authenticate to continue")
                .setNegativeButtonText("Cancel")
                .build()

        prompt.authenticate(promptInfo)
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    private fun showSettingsPanel() {

        val panel = GodPanel.create(
            this,
            "GOD // Settings"
        )

        GodPanel.addSection(
            this,
            panel,
            "AI PROVIDER",
            "Configure your AI connection"
        ) {
            startActivity(
                Intent(
                    this,
                    AIProviderActivity::class.java
                )
            )
        }

        GodPanel.addSection(
            this,
            panel,
            "PERMISSIONS",
            "Manage Android permissions"
        ) {
            showPermissionPanel()
        }

        GodPanel.addSection(
            this,
            panel,
            "SECURITY",
            "Biometric protection"
        ) {
            showSecurityPanel()
        }

        GodPanel.addButton(
            this,
            panel,
            "RUN INITIAL SETUP AGAIN"
        ) {
            setupPreferences.edit()
                .putBoolean("setup_complete", false)
                .apply()

            showSetupScreen()
        }

        GodPanel.addButton(
            this,
            panel,
            "BACK TO GOD"
        ) {
            showGodHome()
        }

        setContentView(panel)
    }

    // ============================================================
    // SETUP
    // ============================================================

    private fun showSetupScreen() {

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 40)
            setBackgroundColor(dark)
        }

        val title = TextView(this).apply {
            text = "GOD INITIAL SETUP"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(orange)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        layout.addView(title)

        addSetupButton(
            layout,
            "SET UP MICROPHONE"
        ) {
            requestPermission(
                Manifest.permission.RECORD_AUDIO,
                101
            )
        }

        addSetupButton(
            layout,
            "SET UP CAMERA"
        ) {
            requestPermission(
                Manifest.permission.CAMERA,
                102
            )
        }

        if (Build.VERSION.SDK_INT >= 33) {

            addSetupButton(
                layout,
                "SET UP NOTIFICATIONS"
            ) {
                requestPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    103
                )
