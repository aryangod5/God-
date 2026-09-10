package com.example.god

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.god.voice.AIState
import com.example.god.voice.VoiceManager
import com.example.god.voice.WakeWordService

class MainActivity : ComponentActivity() {
    private var godUi: GodReferenceUi? = null
    private var voice: VoiceManager? = null
    private lateinit var ai: AIManager
    private lateinit var memory: MemoryManager
    private lateinit var security: SecurityManager

    private val micCode = 101
    private val folderCode = 102

    private val gestureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            val action = intent?.getStringExtra("action") ?: return
            runOnUiThread {
                when (action) {
                    "WAKE" -> startVoice()
                    "CANCEL" -> voice?.cancelListening()
                    "SELECT" -> godUi?.showNotice("GESTURE", "SELECT")
                    "SWIPE_LEFT" -> godUi?.showNotice("GESTURE", "PREVIOUS PANEL")
                    "SWIPE_RIGHT" -> godUi?.showNotice("GESTURE", "NEXT PANEL")
                }
            }
        }
    }

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == WakeWordService.ACTION_DETECTED) {
                runOnUiThread {
                    godUi?.showNotice("HEY GOD", "WAKE WORD DETECTED")
                    startVoice()
                }
            }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        ai = AIManager(this)
        memory = MemoryManager(this)
        security = SecurityManager(this)
        createVoice()
        showHome()

        ContextCompat.registerReceiver(
            this, wakeReceiver,
            android.content.IntentFilter(WakeWordService.ACTION_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, gestureReceiver,
            android.content.IntentFilter("com.example.god.GESTURE_ACTION"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        startWakeWordIfConfigured()
    }

    private fun startWakeWordIfConfigured() {
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val key = getSharedPreferences(WakeWordService.PREFS, MODE_PRIVATE)
            .getString(WakeWordService.KEY_ACCESS, "").orEmpty()
        if (key.isBlank()) return
        try {
            val intent = Intent(this, WakeWordService::class.java).setAction(WakeWordService.ACTION_START)
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } catch (_: Exception) {}
    }

    private fun createVoice() {
        voice = VoiceManager(this, object : VoiceManager.Listener {
            override fun onStateChanged(s: AIState) {
                runOnUiThread {
                    godUi?.setVoiceActive(s == AIState.LISTENING)
                    godUi?.setProcessing(s == AIState.PROCESSING)
                    godUi?.setSpeaking(s == AIState.SPEAKING)
                }
            }

            override fun onVoiceLevel(level: Float) {
                runOnUiThread { godUi?.setVoiceLevel(level) }
            }

            override fun onTextRecognized(text: String) {
                if (text.isBlank()) return
                runOnUiThread { godUi?.showNotice("VOICE INPUT", "RECOGNIZED") }
                handleUserText(text)
            }

            override fun onError(message: String) {
                runOnUiThread { godUi?.showNotice("VOICE ERROR", message.take(50)) }
            }
        })
    }

    private fun showHome() {
        godUi = GodReferenceUi(this) { action -> handleAction(action) }
        setContentView(godUi)
    }

    private fun handleAction(action: String) {
        when (action) {
            GodReferenceUi.ACTION_VOICE -> startVoice()
            GodReferenceUi.ACTION_CHAT -> showChat()
            GodReferenceUi.ACTION_APPS -> showApps()
            GodReferenceUi.ACTION_FILES -> showFiles()
            GodReferenceUi.ACTION_DOCUMENTS -> showDocuments()
            GodReferenceUi.ACTION_MEMORY -> showMemory()
            GodReferenceUi.ACTION_SECURITY -> showSecurity()
            GodReferenceUi.ACTION_PERMISSIONS -> showPermissions()
            GodReferenceUi.ACTION_AUTHORIZED -> openFolder()
            GodReferenceUi.ACTION_SETTINGS -> showSettings()
            GodReferenceUi.ACTION_BACK -> showHome()
        }
    }

    private fun handleUserText(text: String) {
        val lower = text.lowercase().trim()

        if (lower.startsWith("remember ")) {
            val value = text.substringAfter("remember ").trim()
            memory.save(value)
            godUi?.showNotice("MEMORY", "SAVED")
            voice?.speak("I saved that to memory.")
            return
        }

        if (lower == "clear memory" || lower == "forget everything") {
            memory.clear()
            godUi?.showNotice("MEMORY", "CLEARED")
            voice?.speak("Memory cleared.")
            return
        }

        ai.ask(text) { answer ->
            runOnUiThread {
                godUi?.showNotice("GOD AI", "RESPONSE READY")
                voice?.speak(answer.text)
            }
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micCode)
            return
        }
        // VoiceManager performs barge-in: starting a new listen stops current TTS.
        voice?.startListening()
    }

    private fun showChat() {
        startActivity(Intent(this, GodChatActivity::class.java))
    }

    private fun base(title: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.rgb(2, 3, 5))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams(0, dp(55), 1f))
        top.addView(btn("BACK") { showHome() }, LinearLayout.LayoutParams(dp(100), dp(48)))
        root.addView(top)
        root.addView(View(this).apply { setBackgroundColor(Color.rgb(255, 145, 0)) },
            LinearLayout.LayoutParams(-1, dp(1)))
        return root
    }

    private fun btn(text: String, click: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 11f
        typeface = Typeface.MONOSPACE
        setBackgroundColor(Color.rgb(10, 12, 15))
        setOnClickListener { click() }
    }

    private fun showApps() {
        val root = base("GOD // APPS")
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        AppManager(this).launchableApps().forEach { app ->
            list.addView(btn(app.label) {
                try { startActivity(app.intent) }
                catch (_: Exception) { godUi?.showNotice("APPS", "CANNOT OPEN") }
            }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(6) })
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun showFiles() {
        val root = base("GOD // FILES")
        root.addView(btn("OPEN FILE") { FileManager(this).openFilePicker() },
            LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("AUTHORIZED FOLDER") { openFolder() },
            LinearLayout.LayoutParams(-1, dp(55)))
        setContentView(root)
    }

    private fun showDocuments() {
        val root = base("GOD // DOCUMENTS")
        root.addView(TextView(this).apply {
            text = "Text documents can be read and sent to GOD. PDF files can be opened with the Android document system."
            setTextColor(Color.WHITE)
            textSize = 13f
        }, LinearLayout.LayoutParams(-1, dp(90)))
        root.addView(btn("OPEN DOCUMENT") { FileManager(this).openFilePicker() },
            LinearLayout.LayoutParams(-1, dp(55)))
        setContentView(root)
    }

    private fun showMemory() {
        val root = base("GOD // MEMORY")
        val input = EditText(this).apply {
            hint = "Approved memory"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        root.addView(input, LinearLayout.LayoutParams(-1, dp(80)))
        root.addView(btn("SAVE MEMORY") {
            input.text.toString().trim().takeIf { it.isNotBlank() }?.let {
                memory.save(it); input.text.clear()
            }
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("CLEAR ALL MEMORY") {
            AlertDialog.Builder(this).setTitle("CLEAR MEMORY?")
                .setMessage("This deletes locally stored GOD memories.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("DELETE") { _, _ -> memory.clear() }
                .show()
        }, LinearLayout.LayoutParams(-1, dp(55)))
        setContentView(root)
    }

    private fun showSecurity() {
        val root = base("GOD // SECURITY")
        root.addView(TextView(this).apply {
            text = if (security.hasPin()) "APP LOCK: PIN ENABLED" else "APP LOCK: NOT ENABLED"
            setTextColor(Color.WHITE); textSize = 14f
        })
        root.addView(btn("SET / CHANGE PIN") {
            val input = EditText(this).apply { inputType = 2; setTextColor(Color.WHITE) }
            AlertDialog.Builder(this).setTitle("GOD PIN").setView(input)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE") { _, _ -> security.setPin(input.text.toString()) }
                .show()
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("REMOVE PIN") { security.removePin() },
            LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("ANDROID APP SECURITY") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }, LinearLayout.LayoutParams(-1, dp(55)))
        setContentView(root)
    }

    private fun showPermissions() {
        val root = base("GOD // PERMISSIONS")
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        root.addView(TextView(this).apply {
            text = "MICROPHONE: ${if (mic) "GRANTED" else "NOT GRANTED"}"
            setTextColor(Color.WHITE); textSize = 14f
        })
        root.addView(btn("REQUEST MICROPHONE") { startVoice() },
            LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("OPEN ANDROID PERMISSIONS") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }, LinearLayout.LayoutParams(-1, dp(55)))
        setContentView(root)
    }

    // Gesture control is an internal control channel, not a user-facing feature button.
    private fun showGestureInternal() {
        startActivity(Intent(this, GestureControlActivity::class.java))
    }

    private fun showSettings() {
        val root = base("GOD // SETTINGS")
        root.addView(TextView(this).apply {
            text = "WAKE WORD\nThe current wake-word engine requires its own local engine credential. This is not an AI/API key and is used only for the wake-word engine."
            setTextColor(Color.WHITE); textSize = 13f
        }, LinearLayout.LayoutParams(-1, dp(90)))

        val wakeKey = EditText(this).apply {
            hint = "Wake-word engine credential"
            setText(getSharedPreferences(WakeWordService.PREFS, MODE_PRIVATE)
                .getString(WakeWordService.KEY_ACCESS, "").orEmpty())
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(wakeKey, LinearLayout.LayoutParams(-1, dp(60)))
        root.addView(btn("SAVE + ENABLE HEY GOD") {
            getSharedPreferences(WakeWordService.PREFS, MODE_PRIVATE).edit()
                .putString(WakeWordService.KEY_ACCESS, wakeKey.text.toString().trim()).apply()
            startWakeWordIfConfigured()
            godUi?.showNotice("WAKE WORD", "HEY GOD ENGINE ENABLED")
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("STOP HEY GOD") {
            stopService(Intent(this, WakeWordService::class.java))
            godUi?.showNotice("WAKE WORD", "ENGINE STOPPED")
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("CORE ANIMATION: ON") {
            godUi?.showNotice("SETTINGS", "CORE ACTIVE")
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("VOICE / TTS") {
            godUi?.showNotice("SETTINGS", "VOICE READY")
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("WEB RESEARCH: ON") {
            godUi?.showNotice("SETTINGS", "WEB RESEARCH ACTIVE")
        }, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(btn("RETURN TO GOD HOME") { showHome() },
            LinearLayout.LayoutParams(-1, dp(55)))
        setContentView(root)
    }

    private fun openFolder() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, folderCode
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == folderCode && resultCode == RESULT_OK) {
            data?.data?.let { FileManager(this).saveAuthorizedFolder(it) }
            showHome()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == micCode && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            voice?.startListening()
            startWakeWordIfConfigured()
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(wakeReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(gestureReceiver) } catch (_: Exception) {}
        voice?.release()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
