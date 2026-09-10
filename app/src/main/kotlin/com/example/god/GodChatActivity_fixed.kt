package com.example.god

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.example.god.voice.AIState
import com.example.god.voice.VoiceManager

/**
 * Compact WhatsApp-style local chat workspace.
 * History is stored locally and survives app restarts.
 */
class GodChatActivity : ComponentActivity() {
    private lateinit var ai: AIManager
    private lateinit var history: ChatHistoryManager
    private lateinit var voice: VoiceManager
    private lateinit var roleManager: RoleManager

    private var currentId: String = ""
    private lateinit var messagesBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var titleView: TextView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        ai = AIManager(this)
        history = ChatHistoryManager(this)
        roleManager = ai.roleManager()

        voice = VoiceManager(this, object : VoiceManager.Listener {
            override fun onStateChanged(state: AIState) {}
            override fun onVoiceLevel(level: Float) {}
            override fun onTextRecognized(text: String) {
                runOnUiThread {
                    input.setText(text)
                    input.setSelection(input.length())
                    send(text)
                }
            }
            override fun onError(message: String) {
                runOnUiThread { Toast.makeText(this@GodChatActivity, message, Toast.LENGTH_SHORT).show() }
            }
        })

        val existing = history.all().firstOrNull()
        currentId = existing?.id ?: history.create().id
        buildUi()
        loadConversation()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(3, 5, 8))
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }

        val back = smallButton("‹") { finish() }
        top.addView(back, LinearLayout.LayoutParams(dp(42), dp(42)))

        titleView = TextView(this).apply {
            text = "GOD CHAT"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(4), 0)
        }
        top.addView(titleView, LinearLayout.LayoutParams(0, dp(48), 1f))

        top.addView(smallButton("ROLE") { showRoleDialog() }, LinearLayout.LayoutParams(dp(50), dp(42)))
        top.addView(smallButton("＋") { newConversation() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        top.addView(smallButton("☰") { showHistory() }, LinearLayout.LayoutParams(dp(42), dp(42)))

        root.addView(top)
        root.addView(View(this).apply { setBackgroundColor(Color.rgb(255, 145, 0)) },
            LinearLayout.LayoutParams(-1, dp(1)))

        val roleBar = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setPadding(dp(12), dp(4), dp(12), dp(4))
            text = roleText()
        }
        root.addView(roleBar, LinearLayout.LayoutParams(-1, dp(28)))

        messagesBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        scroll = ScrollView(this).apply {
            addView(messagesBox)
            isFillViewport = true
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(6), dp(7), dp(7))
        }

        input = EditText(this).apply {
            hint = "Message GOD..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            minLines = 1
            maxLines = 5
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(Color.rgb(12, 15, 18))
        }
        bottom.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        bottom.addView(smallButton("🎙") { voice.startListening() },
            LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(5) })
        bottom.addView(smallButton("SEND") {
            val q = input.text.toString().trim()
            if (q.isNotBlank()) send(q)
        }, LinearLayout.LayoutParams(dp(58), dp(48)).apply { leftMargin = dp(5) })

        root.addView(bottom)
        setContentView(root)
    }

    private fun loadConversation() {
        val c = history.find(currentId) ?: return
        titleView.text = c.title
        messagesBox.removeAllViews()
        if (c.messages.isEmpty()) {
            addBubble("GOD", "GOD is ready. This conversation will be saved locally.", false, null)
        } else {
            c.messages.forEach { addBubble(it.role, it.text, it.role == "USER", it.id) }
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun send(q: String) {
        val clean = q.trim()
        if (clean.isBlank()) return
        input.text.clear()

        history.addMessage(currentId, "USER", clean)
        addBubble("USER", clean, true, history.find(currentId)?.messages?.lastOrNull()?.id)

        val placeholder = addBubble("GOD", "Thinking…", false, null)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        ai.ask(clean) { answer ->
            runOnUiThread {
                placeholder.text = answer.text
                history.addMessage(currentId, "GOD", answer.text)
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun addBubble(role: String, text: String, user: Boolean, messageId: String?): TextView {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setBackgroundColor(if (user) Color.rgb(18, 24, 28) else Color.rgb(10, 14, 18))
            setOnLongClickListener {
                showMessageActions(messageId, text, role)
                true
            }
        }
        val row = LinearLayout(this).apply {
            gravity = if (user) Gravity.END else Gravity.START
            setPadding(dp(4), dp(3), dp(4), dp(3))
            addView(tv, LinearLayout.LayoutParams(dp(310), -2))
        }
        messagesBox.addView(row)
        return tv
    }

    private fun showMessageActions(id: String?, text: String, role: String) {
        val actions = if (role == "USER") {
            arrayOf("EDIT", "COPY", "DELETE")
        } else {
            arrayOf("COPY", "REGENERATE")
        }
        AlertDialogBuilderCompat.show(this, "MESSAGE", actions) { action ->
            when (action) {
                "COPY" -> copy(text)
                "EDIT" -> if (id != null) editMessage(id, text)
                "DELETE" -> if (id != null) {
                    history.deleteMessage(currentId, id)
                    loadConversation()
                }
                "REGENERATE" -> regenerateLast()
            }
        }
    }

    private fun editMessage(id: String, old: String) {
        val e = EditText(this).apply {
            setText(old)
            setTextColor(Color.WHITE)
        }
        AlertDialog.Builder(this).setTitle("EDIT MESSAGE").setView(e)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val newText = e.text.toString().trim()
                if (newText.isNotBlank()) {
                    history.updateMessage(currentId, id, newText)
                    loadConversation()
                }
            }.show()
    }

    private fun regenerateLast() {
        val c = history.find(currentId) ?: return
        val lastUser = c.messages.lastOrNull { it.role == "USER" } ?: return
        ai.ask(lastUser.text) { answer ->
            runOnUiThread {
                history.addMessage(currentId, "GOD", answer.text)
                loadConversation()
            }
        }
    }

    private fun showHistory() {
        val list = history.all()
        val labels = if (list.isEmpty()) arrayOf("No conversations") else list.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("GOD CHAT HISTORY")
            .setItems(labels) { _, which ->
                if (list.isNotEmpty()) {
                    currentId = list[which].id
                    loadConversation()
                }
            }
            .setNeutralButton("NEW CHAT") { _, _ -> newConversation() }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun newConversation() {
        currentId = history.create().id
        // New conversation starts with a clean short-term context.
        ai.conversationContext().clear()
        loadConversation()
    }

    private fun copy(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("GOD", text))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun showRoleDialog() {
        val roles = roleManager.all()
        val names = mutableListOf("NORMAL GOD")
        names.addAll(roles.map { it.name })
        names.add("＋ CREATE CUSTOM ROLE")

        AlertDialog.Builder(this)
            .setTitle("GOD ROLE")
            .setItems(names.toTypedArray()) { _, which ->
                when {
                    which == 0 -> {
                        roleManager.clearActive()
                        Toast.makeText(this, "Normal GOD mode", Toast.LENGTH_SHORT).show()
                        loadConversation()
                    }
                    which == names.lastIndex -> createRole()
                    else -> {
                        roleManager.setActive(roles[which - 1].id)
                        Toast.makeText(this, "Role: ${roles[which - 1].name}", Toast.LENGTH_SHORT).show()
                        loadConversation()
                    }
                }
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun createRole() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), 0)
        }
        val name = EditText(this).apply {
            hint = "Role name, e.g. My Friend"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val instructions = EditText(this).apply {
            hint = "Paste the role/personality instructions here..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            minLines = 5
            gravity = Gravity.TOP
        }
        box.addView(name, LinearLayout.LayoutParams(-1, dp(55)))
        box.addView(instructions, LinearLayout.LayoutParams(-1, dp(150)))

        AlertDialog.Builder(this)
            .setTitle("CREATE CUSTOM ROLE")
            .setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE + USE") { _, _ ->
                val n = name.text.toString().trim()
                val i = instructions.text.toString().trim()
                if (n.isNotBlank() && i.isNotBlank()) {
                    roleManager.save(n, i, true)
                    Toast.makeText(this, "Custom role enabled", Toast.LENGTH_SHORT).show()
                    loadConversation()
                }
            }
            .show()
    }

    private fun roleText(): String {
        val role = roleManager.activeRole()
        return if (role == null) "ROLE: GOD" else "ROLE: ${role.name}"
    }

    private fun smallButton(text: String, click: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 11f
        typeface = Typeface.MONOSPACE
        setBackgroundColor(Color.rgb(10, 13, 16))
        setOnClickListener { click() }
    }

    override fun onDestroy() {
        voice.release()
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

private object AlertDialogBuilderCompat {
    fun show(
        context: Context,
        title: String,
        items: Array<String>,
        action: (String) -> Unit
    ) {
        android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { _, which -> action(items[which]) }
            .setNegativeButton("CLOSE", null)
            .show()
    }
}
