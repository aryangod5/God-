package com.example.god

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class SystemNotificationView(context: Context) : LinearLayout(context) {

    private val orange = Color.rgb(255, 145, 0)
    private val white = Color.WHITE
    private val panel = Color.argb(242, 7, 10, 14)

    private val titleView = TextView(context)
    private val messageView = TextView(context)
    private var hideAnimator: ObjectAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply {
            setColor(panel)
            setStroke(dp(1), orange)
            cornerRadius = dp(4).toFloat()
        }
        elevation = dp(8).toFloat()

        val marker = TextView(context).apply {
            text = "◆"
            textSize = 11f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }
        addView(marker, LayoutParams(dp(28), dp(42)))

        val textColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        titleView.textSize = 10f
        titleView.setTextColor(orange)
        titleView.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        titleView.letterSpacing = 0.08f

        messageView.textSize = 11f
        messageView.setTextColor(white)
        messageView.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        textColumn.addView(titleView, LayoutParams.MATCH_PARENT, dp(21))
        textColumn.addView(messageView, LayoutParams.MATCH_PARENT, dp(21))
        addView(textColumn, LayoutParams(0, dp(44), 1f))

        alpha = 0f
        translationY = -dp(18).toFloat()
    }

    fun showNotice(title: String, message: String) {
        titleView.text = title.uppercase()
        messageView.text = message.uppercase()

        hideAnimator?.cancel()

        visibility = View.VISIBLE

        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180L)
            .setListener(null)
            .start()

        hideAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f).apply {
            startDelay = 1450L
            duration = 280L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    translationY = -dp(18).toFloat()
                }
            })
            start()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
