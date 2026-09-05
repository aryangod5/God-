package com.example.god

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.god.voice.AIState
import com.example.god.voice.VoiceManager

class MainActivity : ComponentActivity() {

    private var godCoreView: GodCoreView? = null
    private var voiceManager: VoiceManager? = null

    private var voiceRecognizedText: TextView? = null
    private var statusText: TextView? = null
    private var voiceMonitor: TextView? = null

    private val microphonePermissionCode = 1001

    private val black = Color.rgb(2, 4, 7)
    private val panel = Color.rgb(8, 10, 13)
    private val orange = Color.rgb(255, 145, 0)
    private val brightOrange = Color.rgb(255, 205, 100)
    private val white = Color.WHITE
    private val dimWhite = Color.rgb(175, 175, 175)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        initializeVoiceManager()
        showGodHome()
    }

    private fun initializeVoiceManager() {

        voiceManager = VoiceManager(
            this,
            object : VoiceManager.Listener {

                override fun onStateChanged(state: AIState) {

                    runOnUiThread {
                        godCoreView?.setAIState(state)
                        updateVoiceState(state)
                    }
                }

                override fun onVoiceLevel(level: Float) {

                    runOnUiThread {
                        godCoreView?.setVoiceLevel(level)
                        updateVoiceMonitor(level)
                    }
                }

                override fun onTextRecognized(text: String) {

                    runOnUiThread {

                        voiceRecognizedText?.text =
                            if (text.isBlank()) {
                                "VOICE INPUT  //  WAITING"
                            } else {
                                "YOU SAID  //  $text"
                            }
                    }
                }

                override fun onError(message: String) {

                    runOnUiThread {
                        showMessage(message)
                    }
                }
            }
        )
    }

    private fun showGodHome() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setBackgroundColor(black)
        root.setPadding(dp(14), dp(8), dp(14), dp(8))

        setContentView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ---------------------------------------------------------
        // TOP HUD
        // ---------------------------------------------------------

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val title = TextView(this)
        title.text = "G O D"
        title.setTextColor(white)
        title.textSize = 21f
        title.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        title.letterSpacing = 0.18f

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        statusText = TextView(this)
        statusText?.text = "●  ONLINE"
        statusText?.setTextColor(orange)
        statusText?.textSize = 12f
        statusText?.gravity = Gravity.CENTER
        statusText?.typeface = Typeface.MONOSPACE

        header.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // ---------------------------------------------------------
        // TOP TECHNICAL LINE
        // ---------------------------------------------------------

        val topLine = TextView(this)
        topLine.text = "────────────────  CORE CONTROL  ────────────────"
        topLine.setTextColor(orange)
        topLine.alpha = 0.55f
        topLine.textSize = 10f
        topLine.gravity = Gravity.CENTER
        topLine.typeface = Typeface.MONOSPACE

        root.addView(
            topLine,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(24)
            )
        )

        // ---------------------------------------------------------
        // CORE AREA
        // ---------------------------------------------------------

        val coreFrame = LinearLayout(this)
        coreFrame.orientation = LinearLayout.VERTICAL
        coreFrame.gravity = Gravity.CENTER

        root.addView(
            coreFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        godCoreView = GodCoreView(this)

        coreFrame.addView(
            godCoreView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(320)
            )
        )

        val coreLabel = TextView(this)
        coreLabel.text = "GOD CORE  //  ACTIVE"
        coreLabel.setTextColor(orange)
        coreLabel.textSize = 10f
        coreLabel.gravity = Gravity.CENTER
        coreLabel.typeface = Typeface.MONOSPACE
        coreLabel.letterSpacing = 0.12f

        coreFrame.addView(
            coreLabel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        // ---------------------------------------------------------
        // VOICE MONITOR
        // ---------------------------------------------------------

        val voicePanel = createPanel()

        val voiceHeader = TextView(this)
        voiceHeader.text = "VOICE MONITOR  //  AUDIO CHANNEL"
        voiceHeader.setTextColor(white)
        voiceHeader.textSize = 10f
        voiceHeader.typeface = Typeface.MONOSPACE

        voicePanel.addView(
            voiceHeader,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(24)
            )
        )

        voiceMonitor = TextView(this)
        voiceMonitor?.text = "IDLE   ▏▏▏▏▏▏▏▏▏▏▏   MIC READY"
        voiceMonitor?.setTextColor(orange)
        voiceMonitor?.textSize = 11f
        voiceMonitor?.typeface = Typeface.MONOSPACE
        voiceMonitor?.gravity = Gravity.CENTER

        voicePanel.addView(
            voiceMonitor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(32)
            )
        )

        voiceRecognizedText = TextView(this)
        voiceRecognizedText?.text = "VOICE INPUT  //  WAITING"
        voiceRecognizedText?.setTextColor(white)
        voiceRecognizedText?.textSize = 11f
        voiceRecognizedText?.gravity = Gravity.CENTER
        voiceRecognizedText?.typeface = Typeface.MONOSPACE
        voiceRecognizedText?.setPadding(
            dp(4),
            0,
            dp(4),
            0
        )

        voicePanel.addView(
            voiceRecognizedText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        root.addView(
            voicePanel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(92)
            )
        )

        // ---------------------------------------------------------
        // QUICK CONTROL BUTTONS
        // ---------------------------------------------------------

        val controls = LinearLayout(this)
        controls.orientation = LinearLayout.HORIZONTAL
        controls.gravity = Gravity.CENTER

        root.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
            )
        )

        val voiceButton = createHudButton("VOICE")

        voiceButton.setOnClickListener {
            startVoiceInput()
        }

        controls.addView(
            voiceButton,
            buttonWeight()
        )

        val chatButton = createHudButton("CHAT")

        chatButton.setOnClickListener {
            showChatScreen()
        }

        controls.addView(
            chatButton,
            buttonWeight()
        )

        val menuButton = createHudButton("MENU")

        menuButton.setOnClickListener {
            showSideMenu()
        }

        controls.addView(
            menuButton,
            buttonWeight()
        )

        // ---------------------------------------------------------
        // BOTTOM SYSTEM STATUS
        // ---------------------------------------------------------

        val bottom = TextView(this)
        bottom.text =
            "SYS  ●  ONLINE     CPU  --     RAM  --     BAT  --     NET  ●"
        bottom.setTextColor(dimWhite)
        bottom.textSize = 9f
        bottom.gravity = Gravity.CENTER
        bottom.typeface = Typeface.MONOSPACE

        root.addView(
            bottom,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )
    }

    private fun createPanel(): LinearLayout {

        val panelLayout = LinearLayout(this)

        panelLayout.orientation = LinearLayout.VERTICAL
        panelLayout.gravity = Gravity.CENTER_VERTICAL
        panelLayout.setPadding(
            dp(12),
            dp(6),
            dp(12),
            dp(6)
        )

        val drawable = GradientDrawable()
        drawable.setColor(panel)
        drawable.setStroke(dp(1), Color.rgb(120, 70, 10))
        panelLayout.background = drawable

        return panelLayout
    }

    private fun createHudButton(text: String): Button {

        val button = Button(this)

        button.text = text
        button.setTextColor(white)
        button.textSize = 11f
        button.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        button.gravity = Gravity.CENTER
        button.isAllCaps = false
        button.setPadding(0, 0, 0, 0)

        val drawable = GradientDrawable()
        drawable.setColor(Color.rgb(10, 11, 13))
        drawable.setStroke(dp(1), orange)
        drawable.cornerRadius = dp(3).toFloat()

        button.background = drawable

        return button
    }

    private fun buttonWeight(): LinearLayout.LayoutParams {

        val params = LinearLayout.LayoutParams(
            0,
            dp(46),
            1f
        )

        params.setMargins(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        return params
    }

    private fun updateVoiceMonitor(level: Float) {

        val clamped = level.coerceIn(0f, 1f)

        val bars = (clamped * 10f)
            .toInt()
            .coerceIn(0, 10)

        val waveform = buildString {

            for (i in 0 until 10) {

                append(
                    if (i < bars) {
                        when {
                            i % 3 == 0 -> "▂"
                            i % 3 == 1 -> "▅"
                            else -> "▃"
                        }
                    } else {
                        "▁"
                    }
                )
            }
        }

        voiceMonitor?.text =
            "AUDIO  $waveform   MIC ACTIVE"
    }

    private fun startVoiceInput() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                microphonePermissionCode
            )

            return
        }

        voiceManager?.startListening()
    }

    private fun updateVoiceState(state: AIState) {

        statusText?.text = when (state) {

            AIState.OFFLINE ->
                "●  OFFLINE"

            AIState.STARTING ->
                "●  STARTING"

            AIState.IDLE ->
                "●  ONLINE"

            AIState.LISTENING ->
                "●  LISTENING"

            AIState.PROCESSING ->
                "●  PROCESSING"

            AIState.SPEAKING ->
                "●  SPEAKING"

            AIState.ERROR ->
                "●  ERROR"
        }
    }

    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showSideMenu() {

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(black)

        val menu = LinearLayout(this)

        menu.orientation = LinearLayout.VERTICAL
        menu.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(20)
        )
        menu.setBackgroundColor(black)

        scrollView.addView(
            menu,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val title = TextView(this)

        title.text = "G O D  //  SYSTEM"
        title.textSize = 20f
        title.setTextColor(white)
        title.gravity = Gravity.CENTER
        title.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        title.letterSpacing = 0.12f
        title.setPadding(10, 20, 10, 8)

        menu.addView(title)

        val subtitle = TextView(this)

        subtitle.text = "CONTROL INTERFACE"
        subtitle.textSize = 9f
        subtitle.setTextColor(orange)
        subtitle.gravity = Gravity.CENTER
        subtitle.typeface = Typeface.MONOSPACE

        menu.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        addMenuButton(menu, "VOICE") {
            showGodHome()
            startVoiceInput()
        }

        addMenuButton(menu, "CHAT") {
            showChatScreen()
        }

        addMenuButton(menu, "APPS") {
            showMessage("Apps manager is being prepared.")
        }

        addMenuButton(menu, "FILES") {
            openFiles()
        }

        addMenuButton(menu, "DOCUMENTS") {
            showMessage("Documents manager is being prepared.")
        }

        addMenuButton(menu, "MEMORY") {
            showMessage("Memory manager is being prepared.")
        }

        addMenuButton(menu, "AI PROVIDER / API") {
            showMessage("AI provider settings are being prepared.")
        }

        addMenuButton(menu, "SECURITY") {
            openSecuritySettings()
        }

        addMenuButton(menu, "PERMISSIONS") {
            openAppSettings()
        }

        addMenuButton(menu, "AUTHORIZED FOLDER") {
            openAuthorizedFolder()
        }

        addMenuButton(menu, "SETTINGS") {
            openSettings()
        }

        val backButton = createHudButton("BACK TO GOD")

        backButton.setOnClickListener {
            showGodHome()
        }

        val backParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )

        backParams.setMargins(
            0,
            dp(18),
            0,
            0
        )

        menu.addView(
            backButton,
            backParams
        )

        setContentView(scrollView)
    }

    private fun addMenuButton(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = createHudButton(text)

        button.setOnClickListener {
            action()
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )

        params.setMargins(
            0,
            dp(5),
            0,
            dp(5)
        )

        parent.addView(
            button,
            params
        )
    }

    private fun showChatScreen() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )
        root.setBackgroundColor(black)

        val title = TextView(this)

        title.text = "G O D  //  CHAT"
        title.textSize = 20f
        title.setTextColor(white)
        title.gravity = Gravity.CENTER
        title.typeface = Typeface.MONOSPACE

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        val line = TextView(this)

        line.text = "────────────────────────────"
        line.setTextColor(orange)
        line.gravity = Gravity.CENTER

        root.addView(line)

        val message = EditText(this)

        message.hint = "ASK GOD..."
        message.setHintTextColor(Color.rgb(100, 100, 100))
        message.setTextColor(white)
        message.textSize = 14f
        message.typeface = Typeface.MONOSPACE
        message.setSingleLine(false)

        val messageBackground = GradientDrawable()
        messageBackground.setColor(panel)
        messageBackground.setStroke(dp(1), Color.rgb(120, 70, 10))
        messageBackground.cornerRadius = dp(3).toFloat()

        message.background = messageBackground

        val messageParams = LinearLayout.LayoutParams(


    private fun addMenuButton(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = createHudButton(text)

        button.setOnClickListener {
            action()
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )

        params.setMargins(
            0,
            dp(5),
            0,
            dp(5)
        )

        parent.addView(
            button,
            params
        )
    }

    private fun showChatScreen() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )
        root.setBackgroundColor(black)

        val title = TextView(this)

        title.text = "G O D  //  CHAT"
        title.textSize = 20f
        title.setTextColor(white)
        title.gravity = Gravity.CENTER
        title.typeface = Typeface.MONOSPACE

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        val line = TextView(this)

        line.text = "────────────────────────────"
        line.setTextColor(orange)
        line.gravity = Gravity.CENTER

        root.addView(line)

        val message = EditText(this)

        message.hint = "ASK GOD..."
        message.setHintTextColor(Color.rgb(100, 100, 100))
        message.setTextColor(white)
        message.textSize = 14f
        message.typeface = Typeface.MONOSPACE
        message.setSingleLine(false)

        val messageBackground = GradientDrawable()
        messageBackground.setColor(panel)
        messageBackground.setStroke(dp(1), Color.rgb(120, 70, 10))
        messageBackground.cornerRadius = dp(3).toFloat()

        message.background = messageBackground

        val messageParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(110)
        )

        messageParams.setMargins(
            0,
            dp(20),
            0,
            dp(10)
        )

        root.addView(
            message,
            messageParams
        )

        val sendButton = createHudButton("SEND TO GOD")

        sendButton.setOnClickListener {

            val text = message.text.toString().trim()

            if (text.isNotEmpty()) {
                showMessage("Chat input received.")
            }
        }

        root.addView(
            sendButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val backButton = createHudButton("BACK TO CORE")

        backButton.setOnClickListener {
            showGodHome()
        }

        val backParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )

        backParams.setMargins(
            0,
            dp(12),
            0,
            0
        )

        root.addView(
            backButton,
            backParams
        )

        setContentView(root)
    }

    private fun openFiles() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"

        startActivityForResult(intent, 2001)
    }

    private fun openAuthorizedFolder() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

        startActivityForResult(intent, 2002)
    }

    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )

        startActivity(intent)
    }

    private fun openSecuritySettings() {

        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)

        try {
            startActivity(intent)
        } catch (_: Exception) {
            showMessage("Security settings are not available.")
        }
    }

    private fun openSettings() {

        val intent = Intent(Settings.ACTION_SETTINGS)

        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == microphonePermissionCode) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                startVoiceInput()

            } else {

                showMessage(
                    "Microphone permission is required for voice control."
                )
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (resultCode != RESULT_OK) {
            return
        }

        when (requestCode) {

            2001 -> {

                val uri = data?.data

                if (uri != null) {

                    showMessage(
                        "File selected successfully."
                    )
                }
            }

            2002 -> {

                val uri = data?.data

                if (uri != null) {

                    try {

                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                        showMessage(
                            "Authorized folder saved."
                        )

                    } catch (_: Exception) {

                        showMessage(
                            "Folder selected."
                        )
                    }
                }
            }
        }
    }

    private fun dp(value: Int): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onDestroy() {

        voiceManager?.release()
        voiceManager = null
        godCoreView = null

        super.onDestroy()
    }
}
