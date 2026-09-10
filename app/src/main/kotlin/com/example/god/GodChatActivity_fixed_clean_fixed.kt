package com.example.god

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GodChatActivity : AppCompatActivity() {
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var ai: AIManager

    private val orange = Color.rgb(255, 145, 0)
    private val white = Color.WHITE
    private val dark = Color.rgb(4, 8, 14)
    private val card = Color.rgb(10, 16, 24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ai = AIManager(this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 14, 12)
        }

        val title = TextView(this).apply {
            text = "GOD CHAT"
            setTextColor(white)
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val back = TextView(this).apply {
            text = "BACK"
            setTextColor(orange)
            textSize = 13f
            setPadding(16, 10, 10, 10)
            setOnClickListener { finish() }
        }

        header.addView(title)
        header.addView(back)
        root.addView(header)

        val divider = View(this).apply {
            setBackgroundColor(orange)
            alpha = 0.45f
        }
        root.addView(divider, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1
        ))

        scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 18, 14, 18)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 10, 10)
        }

        input = EditText(this).apply {
            hint = "Message GOD..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 16f
            maxLines = 4
            setPadding(14, 12, 14, 12)
            setBackgroundColor(card)
        }

        val send = TextView(this).apply {
            text = "SEND"
            setTextColor(orange)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(16, 12, 10, 12)
            setOnClickListener { sendMessage() }
        }

        bottom.addView(input, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ))
        bottom.addView(send)
        root.addView(bottom)

        setContentView(root)
        addGodMessage("GOD is ready. How can I help?")
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        addUserMessage(text)
        ai.ask(text) { answer -> addGodMessage(answer.text) }
    }

    private fun addUserMessage(text: String) = addMessage("YOU", text, true)
    private fun addGodMessage(text: String) = addMessage("GOD", text, false)

    private fun addMessage(label: String, text: String, user: Boolean) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 11, 14, 11)
            setBackgroundColor(card)
        }

        val title = TextView(this).apply {
            this.text = label
            textColor = if (user) white else orange
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val body = TextView(this).apply {
            this.text = text
            setTextColor(white)
            textSize = 15f
            setPadding(0, 5, 0, 0)
            setTextIsSelectable(true)
            setOnLongClickListener {
                showMessageActions(text)
                true
            }
        }

        box.addView(title)
        box.addView(body)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 10)
        messages.addView(box, params)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun showMessageActions(text: String) {
        AlertDialog.Builder(this)
            .setItems(arrayOf("COPY", "DELETE")) { dialog, which ->
                if (which == 0) {
                    val clipboard = getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("GOD message", text)
                    )
                } else {
                    for (i in messages.childCount - 1 downTo 0) {
                        val child = messages.getChildAt(i) as? ViewGroup ?: continue
                        val body = child.getChildAt(1) as? TextView ?: continue
                        if (body.text.toString() == text) {
                            messages.removeViewAt(i)
                            break
                        }
                    }
                }
                dialog.dismiss()
            }
            .show()
    }
}
