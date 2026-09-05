package com.example.god

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
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

    private val microphonePermissionCode = 1001

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
                    }
                }

                override fun onTextRecognized(text: String) {

                    runOnUiThread {

                        voiceRecognizedText?.text =
                            if (text.isBlank()) {
                                "VOICE INPUT\n\nWaiting..."
                            } else {
                                "YOU SAID\n\n$text"
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
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.rgb(3, 7, 14))

        setContentView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        statusText = TextView(this)

        statusText?.text = "GOD SYSTEM • ONLINE"
        statusText?.setTextColor(Color.WHITE)
        statusText?.textSize = 14f
        statusText?.gravity = Gravity.CENTER
        statusText?.setPadding(10, 20, 10, 20)

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        godCoreView = GodCoreView(this)

        root.addView(
            godCoreView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        voiceRecognizedText = TextView(this)

        voiceRecognizedText?.text =
            "VOICE INPUT\n\nWaiting..."

        voiceRecognizedText?.setTextColor(Color.WHITE)
        voiceRecognizedText?.textSize = 15f
        voiceRecognizedText?.gravity = Gravity.CENTER
        voiceRecognizedText?.setPadding(20, 10, 20, 10)

        root.addView(
            voiceRecognizedText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val controls = LinearLayout(this)

        controls.orientation = LinearLayout.HORIZONTAL
        controls.gravity = Gravity.CENTER

        root.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val voiceButton = Button(this)

        voiceButton.text = "VOICE"
        voiceButton.setTextColor(Color.WHITE)

        voiceButton.setOnClickListener {
            startVoiceInput()
        }

        controls.addView(
            voiceButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val stopButton = Button(this)

        stopButton.text = "STOP"
        stopButton.setTextColor(Color.WHITE)

        stopButton.setOnClickListener {
            voiceManager?.cancelListening()
            voiceManager?.stopSpeaking()
        }

        controls.addView(
            stopButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val menuButton = Button(this)

        menuButton.text = "MENU"
        menuButton.setTextColor(Color.WHITE)

        menuButton.setOnClickListener {
            showSideMenu()
        }

        controls.addView(
            menuButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
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
                "GOD SYSTEM • OFFLINE"

            AIState.STARTING ->
                "GOD SYSTEM • STARTING"

            AIState.IDLE ->
                "GOD SYSTEM • ONLINE"

            AIState.LISTENING ->
                "GOD SYSTEM • LISTENING"

            AIState.PROCESSING ->
                "GOD SYSTEM • PROCESSING"

            AIState.SPEAKING ->
                "GOD SYSTEM • SPEAKING"

            AIState.ERROR ->
                "GOD SYSTEM • ERROR"
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

        val menu = LinearLayout(this)

        menu.orientation = LinearLayout.VERTICAL
        menu.setPadding(40, 40, 40, 40)
        menu.setBackgroundColor(Color.rgb(5, 8, 14))

        scrollView.addView(
            menu,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val title = TextView(this)

        title.text = "GOD SYSTEM MENU"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(10, 20, 10, 30)

        menu.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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

        val backButton = Button(this)

        backButton.text = "BACK TO GOD"
        backButton.setTextColor(Color.WHITE)

        backButton.setOnClickListener {
            showGodHome()
        }

        menu.addView(
            backButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(scrollView)
    }

    private fun addMenuButton(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = Button(this)

        button.text = text
        button.setTextColor(Color.WHITE)

        button.setOnClickListener {
            action()
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun showChatScreen() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(30, 30, 30, 30)
        root.setBackgroundColor(Color.rgb(3, 7, 14))

        val title = TextView(this)

        title.text = "GOD CHAT"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(10, 20, 10, 30)

        root.addView(title)

        val message = EditText(this)

        message.hint = "Ask GOD..."
        message.setHintTextColor(Color.GRAY)
        message.setTextColor(Color.WHITE)

        root.addView(
            message,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val sendButton = Button(this)

        sendButton.text = "SEND"
        sendButton.setTextColor(Color.WHITE)

        sendButton.setOnClickListener {

            val text = message.text.toString().trim()

            if (text.isNotEmpty()) {
                showMessage("Chat input received.")
            }
        }

        root.addView(sendButton)

        val backButton = Button(this)

        backButton.text = "BACK TO GOD"
        backButton.setTextColor(Color.WHITE)

        backButton.setOnClickListener {
            showGodHome()
        }

        root.addView(backButton)

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

    override fun onDestroy() {

        voiceManager?.release()
        voiceManager = null
        godCoreView = null

        super.onDestroy()
    }
}
