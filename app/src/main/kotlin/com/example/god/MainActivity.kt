package com.example.god

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
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

    private lateinit var root: FrameLayout
    private var godCoreView: GodCoreView? = null
    private var voiceManager: VoiceManager? = null
    private var notificationView: SystemNotificationView? = null
    private var voiceStateLabel: TextView? = null
    private var voiceInputLabel: TextView? = null

    private val micPermissionCode = 1001

    private val black = Color.rgb(2, 3, 5)
    private val panel = Color.argb(235, 8, 11, 15)
    private val orange = Color.rgb(255, 145, 0)
    private val brightOrange = Color.rgb(255, 195, 75)
    private val white = Color.WHITE
    private val dimWhite = Color.rgb(190, 190, 190)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        setupVoiceManager()
        showGodHome()
    }

    private fun setupVoiceManager() {
        voiceManager = VoiceManager(
            this,
            object : VoiceManager.Listener {
                override fun onStateChanged(state: AIState) {
                    runOnUiThread {
                        godCoreView?.setAIState(state)
                        updateVoiceState(state)
                        when (state) {
                            AIState.LISTENING -> showSystemNotice("VOICE", "LISTENING")
                            AIState.PROCESSING -> showSystemNotice("GOD AI", "PROCESSING")
                            AIState.SPEAKING -> showSystemNotice("GOD AI", "SPEAKING")
                            AIState.ERROR -> showSystemNotice("SYSTEM", "VOICE ERROR")
                            else -> Unit
                        }
                    }
                }

                override fun onVoiceLevel(level: Float) {
                    runOnUiThread { godCoreView?.setVoiceLevel(level) }
                }

                override fun onTextRecognized(text: String) {
                    runOnUiThread {
                        voiceInputLabel?.text =
                            if (text.isBlank()) "VOICE INPUT  //  WAITING"
                            else "YOU SAID  //  $text"
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        godCoreView?.setVoiceLevel(0f)
                        showSystemNotice("VOICE ERROR", message)
                    }
                }
            }
        )
    }

    private fun showGodHome() {
        root = FrameLayout(this)
        root.setBackgroundColor(black)
        setContentView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addTopHud()
        addCore()
        addGestureHud()
        addVoiceMonitor()
        addBottomHud()

        notificationView = SystemNotificationView(this)
        root.addView(
            notificationView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = dp(88)
                leftMargin = dp(20)
                rightMargin = dp(20)
            }
        )
        notificationView?.visibility = View.GONE
    }

    private fun addTopHud() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "G O D"
            textSize = 30f
            setTextColor(brightOrange)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }

        header.addView(title, LinearLayout.LayoutParams(0, dp(52), 1f))

        val menu = TextView(this).apply {
            text = "⋮"
            textSize = 34f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(2), 0)
            setOnClickListener {
                clickSound()
                showSideMenu()
            }
        }
        header.addView(menu, LinearLayout.LayoutParams(dp(42), dp(52)))

        container.addView(header)

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = hudFrame(orange, 0.88f)
            setPadding(dp(8), 0, dp(8), 0)
        }

        val items = listOf("● CORE: ONLINE", "● VOICE: READY", "● AI: CONNECTED", "● SYSTEM: STANDBY")
        items.forEachIndexed { index, item ->
            val tv = TextView(this).apply {
                text = item
                textSize = 9.5f
                setTextColor(if (index == 0) brightOrange else white)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                letterSpacing = 0.02f
            }
            status.addView(tv, LinearLayout.LayoutParams(0, dp(42), 1f))
        }

        container.addView(
            status,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                setMargins(0, dp(4), 0, 0)
            }
        )

        root.addView(
            container,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112))
        )
    }

    private fun addCore() {
        godCoreView = GodCoreView(this).apply {
            setVoiceClickListener {
                clickSound()
                startVoiceInput()
            }
        }

        root.addView(
            godCoreView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                topMargin = dp(108)
                bottomMargin = dp(198)
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        )
    }

    private fun addGestureHud() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = hudFrame(orange, 0.92f)
            setOnClickListener {
                clickSound()
                showSystemNotice("GESTURE", "CONTROL READY")
            }
        }

        val icon = TextView(this).apply {
            text = "☝"
            textSize = 27f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }
        val label = TextView(this).apply {
            text = "GESTURE\nCONTROL"
            textSize = 9f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
        }

        panel.addView(icon, LinearLayout.LayoutParams.MATCH_PARENT, dp(34))
        panel.addView(label, LinearLayout.LayoutParams.MATCH_PARENT, dp(34))

        root.addView(
            panel,
            FrameLayout.LayoutParams(dp(112), dp(88)).apply {
                leftMargin = dp(20)
                topMargin = dp(142)
            }
        )
    }

    private fun addVoiceMonitor() {
        val monitor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(6), dp(18), dp(6))
            background = hudFrame(orange, 0.95f)
        }

        val header = TextView(this).apply {
            text = "VOICE MONITOR  //  AUDIO CHANNEL"
            textSize = 8.5f
            setTextColor(white)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }

        voiceStateLabel = TextView(this).apply {
            text = "IDLE  //  MIC READY"
            textSize = 9.5f
            setTextColor(orange)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }

        voiceInputLabel = TextView(this).apply {
            text = "VOICE INPUT  //  WAITING"
            textSize = 8.5f
            setTextColor(dimWhite)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            maxLines = 1
        }

        monitor.addView(header, LinearLayout.LayoutParams.MATCH_PARENT, dp(20))
        monitor.addView(voiceStateLabel, LinearLayout.LayoutParams.MATCH_PARENT, dp(22))
        monitor.addView(voiceInputLabel, LinearLayout.LayoutParams.MATCH_PARENT, dp(22))

        root.addView(
            monitor,
            FrameLayout.LayoutParams(dp(360), dp(76), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(190)
            }
        )
    }

    private fun addBottomHud() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(6), dp(10), dp(8))
        }

        row.addView(createHealthPanel(), LinearLayout.LayoutParams(0, dp(128), 1f).apply {
            rightMargin = dp(5)
        })
        row.addView(createQuickAccessPanel(), LinearLayout.LayoutParams(0, dp(128), 1f).apply {
            leftMargin = dp(5)
        })

        root.addView(
            row,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(146), Gravity.BOTTOM).apply {
                bottomMargin = dp(28)
            }
        )

        val footer = TextView(this).apply {
            text = "SYS  ● ONLINE     CORE  ●     VOICE  ●     AI  ●"
            textSize = 8f
            setTextColor(dimWhite)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        root.addView(
            footer,
            FrameLayout.LayoutParams.MATCH_PARENT, dp(28), Gravity.BOTTOM
        )
    }

    private fun createHealthPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = hudFrame(orange, 0.92f)
        }

        panel.addView(TextView(this).apply {
            text = "SYSTEM HEALTH"
            textSize = 9f
            setTextColor(white)
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(22))

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val core = TextView(this).apply {
            text = "98%\nHEALTH"
            textSize = 13f
            setTextColor(orange)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = hudFrame(orange, 0.45f)
        }
        body.addView(core, LinearLayout.LayoutParams(dp(82), dp(78)).apply { rightMargin = dp(10) })

        val bars = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        listOf("CPU", "RAM", "STORAGE").forEachIndexed { index, name ->
            val line = TextView(this).apply {
                text = "$name   ${"█".repeat(5 + index)}${"░".repeat(7 - index)}"
                textSize = 8f
                setTextColor(if (index == 0) white else dimWhite)
                typeface = Typeface.MONOSPACE
            }
            bars.addView(line, LinearLayout.LayoutParams.MATCH_PARENT, dp(22))
        }

        body.addView(bars, LinearLayout.LayoutParams(0, dp(78), 1f))
        panel.addView(body, LinearLayout.LayoutParams.MATCH_PARENT, dp(86))
        return panel
    }

    private fun createQuickAccessPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = hudFrame(orange, 0.92f)
        }

        panel.addView(TextView(this).apply {
            text = "QUICK ACCESS"
            textSize = 9f
            setTextColor(white)
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(22))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addQuick(row, "CHAT", "▢") { showChatScreen() }
        addQuick(row, "APPS", "▦") { showSystemNotice("APPS", "MANAGER READY") }
        addQuick(row, "FILES", "□") { openFiles() }

        panel.addView(row, LinearLayout.LayoutParams.MATCH_PARENT, dp(86))
        return panel
    }

    private fun addQuick(parent: LinearLayout, label: String, icon: String, action: () -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = hudFrame(orange, 0.55f)
            setOnClickListener {
                clickSound()
                action()
            }
        }
        box.addView(TextView(this).apply {
            text = icon
            textSize = 24f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(45))
        box.addView(TextView(this).apply {
            text = label
            textSize = 8f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(24))
        parent.addView(box, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
            setMargins(dp(3), 0, dp(3), 0)
        })
    }

    private fun showSideMenu() {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(110, 0, 0, 0))
            setOnClickListener {
                clickSound()
                root.removeView(this)
            }
        }

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(16))
            background = hudFrame(orange, 0.96f)
        }

        menu.setOnClickListener { /* consume */ }

        menu.addView(TextView(this).apply {
            text = "G O D  //  SYSTEM"
            textSize = 18f
            setTextColor(white)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(42))

        menu.addView(TextView(this).apply {
            text = "CONTROL INTERFACE"
            textSize = 8f
            setTextColor(orange)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(26))

        val entries = listOf(
            "◉  VOICE" to { startVoiceInput() },
            "▢  CHAT" to { showChatScreen() },
            "▦  APPS" to { showSystemNotice("APPS", "MANAGER READY") },
            "□  FILES" to { openFiles() },
            "▤  DOCUMENTS" to { showSystemNotice("DOCUMENTS", "MODULE READY") },
            "◈  MEMORY" to { showSystemNotice("MEMORY", "MODULE READY") },
            "⌁  AI PROVIDER / API" to { openAIProvider() },
            "◇  SECURITY" to { openSecuritySettings() },
            "▣  PERMISSIONS" to { openAppSettings() },
            "□  AUTHORIZED FOLDER" to { openAuthorizedFolder() },
            "⚙  SETTINGS" to { openSettings() }
        )

        entries.forEach { (label, action) ->
            val item = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(white)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(4), 0)
                background = hudFrame(orange, 0.35f)
                setOnClickListener {
                    clickSound()
                    root.removeView(overlay)
                    action()
                }
            }
            menu.addView(item, LinearLayout.LayoutParams.MATCH_PARENT, dp(42).apply {
                // no-op; height is applied below
            })
            (item.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(3), 0, dp(3))
        }

        val scroll = ScrollView(this).apply {
            addView(menu)
            setBackgroundColor(Color.TRANSPARENT)
        }

        overlay.addView(
            scroll,
            FrameLayout.LayoutParams(dp(330), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END).apply {
                topMargin = dp(18)
                bottomMargin = dp(18)
                rightMargin = dp(10)
            }
        )

        root.addView(overlay)
    }

    private fun showChatScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(black)
        }

        layout.addView(TextView(this).apply {
            text = "G O D  //  CHAT"
            textSize = 22f
            setTextColor(white)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams.MATCH_PARENT, dp(55))

        val conversation = TextView(this).apply {
            text = "GOD\n\nHello, Master. GOD is ready."
            textSize = 14f
            setTextColor(white)
            typeface = Typeface.MONOSPACE
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = hudFrame(orange, 0.75f)
        }
        layout.addView(conversation, LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)

        val input = EditText(this).apply {
            hint = "ASK GOD..."
            setHintTextColor(Color.rgb(90, 90, 90))
            setTextColor(white)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            background = hudFrame(orange, 0.75f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        layout.addView(input, LinearLayout.LayoutParams.MATCH_PARENT, dp(90))

        val send = TextView(this).apply {
            text = "SEND TO GOD"
            textSize = 11f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            background = hudFrame(orange, 0.9f)
            setOnClickListener {
                clickSound()
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    conversation.append("\n\nYOU\n\n$text\n\nGOD\n\nI received your message.")
                    input.text.clear()
                    showSystemNotice("GOD CHAT", "MESSAGE RECEIVED")
                }
            }
        }
        layout.addView(send, LinearLayout.LayoutParams.MATCH_PARENT, dp(48))

        val back = TextView(this).apply {
            text = "BACK TO CORE"
            textSize = 10f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setOnClickListener { clickSound(); showGodHome() }
        }
        layout.addView(back, LinearLayout.LayoutParams.MATCH_PARENT, dp(42))

        setContentView(layout)
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                micPermissionCode
            )
            showSystemNotice("VOICE", "MICROPHONE PERMISSION REQUIRED")
            return
        }

        showSystemNotice("VOICE", "ACTIVATED")
        voiceManager?.startListening()
    }

    private fun updateVoiceState(state: AIState) {
        voiceStateLabel?.text = when (state) {
            AIState.OFFLINE -> "OFFLINE  //  MIC UNAVAILABLE"
            AIState.STARTING -> "STARTING  //  INITIALIZING"
            AIState.IDLE -> "IDLE  //  MIC READY"
            AIState.LISTENING -> "LISTENING  //  SPEAK NOW"
            AIState.PROCESSING -> "PROCESSING  //  GOD THINKING"
            AIState.SPEAKING -> "SPEAKING  //  GOD RESPONDING"
            AIState.ERROR -> "ERROR  //  CHECK VOICE SYSTEM"
        }
    }

    private fun showSystemNotice(title: String, message: String) {
        notificationView?.showNotice(title, message)
    }

    private fun openFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, 2001)
        } catch (_: Exception) {
            showSystemNotice("FILES", "FILE PICKER UNAVAILABLE")
        }
    }

    private fun openAuthorizedFolder() {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 2002)
        } catch (_: Exception) {
            showSystemNotice("FOLDER", "FOLDER PICKER UNAVAILABLE")
        }
    }

    private fun openAIProvider() {
        try {
            startActivity(Intent(this, AIProviderActivity::class.java))
        } catch (_: Exception) {
            showSystemNotice("AI PROVIDER", "MODULE NOT AVAILABLE")
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            showSystemNotice("PERMISSIONS", "SETTINGS UNAVAILABLE")
        }
    }

    private fun openSecuritySettings() {
        try {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: Exception) {
            showSystemNotice("SECURITY", "SETTINGS UNAVAILABLE")
        }
    }

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            showSystemNotice("SETTINGS", "SETTINGS UNAVAILABLE")
        }
    }

    private fun hudFrame(stroke: Int, alpha: Float): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.argb(225, 7, 10, 14))
            setStroke(dp(1), Color.argb((255 * alpha).toInt().coerceIn(1, 255), Color.red(stroke), Color.green(stroke), Color.blue(stroke)))
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micPermissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            2001 -> showSystemNotice("FILES", "FILE SELECTED")
            2002 -> showSystemNotice("AUTHORIZED FOLDER", "FOLDER SAVED")
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        voiceManager?.release()
        voiceManager = null
        godCoreView = null
        notificationView = null
        super.onDestroy()
    }
}
