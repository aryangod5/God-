package com.example.god

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object GodPanel {

    private val blueColor = Color.rgb(30, 150, 255)
    private val orangeColor = Color.rgb(255, 145, 0)
    private val panelBackgroundColor = Color.rgb(5, 12, 22)
    private val cardBackgroundColor = Color.rgb(8, 20, 35)
    private val textColor = Color.rgb(225, 240, 255)

    fun create(
        context: Context,
        title: String
    ): LinearLayout {

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 30, 28, 30)

            background = GradientDrawable().apply {
                setColor(panelBackgroundColor)
                setStroke(2, blueColor)
                cornerRadius = 28f
            }

            elevation = 12f
        }

        val titleView = TextView(context).apply {
            text = title.uppercase()
            textSize = 22f
            setTextColor(orangeColor)
            setPadding(0, 0, 0, 24)
        }

        panel.addView(titleView)

        return panel
    }

    fun addSection(
        context: Context,
        panel: LinearLayout,
        label: String,
        value: String,
        onClick: (() -> Unit)? = null
    ) {

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)

            background = GradientDrawable().apply {
                setColor(cardBackgroundColor)
                setStroke(1, Color.rgb(25, 100, 180))
                cornerRadius = 20f
            }

            if (onClick != null) {
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    onClick()
                }
            }
        }

        val labelView = TextView(context).apply {
            text = "◆  $label"
            textSize = 14f
            setTextColor(blueColor)
        }

        val valueView = TextView(context).apply {
            text = value
            textSize = 17f
            setTextColor(textColor)
            setPadding(0, 8, 0, 0)
        }

        card.addView(labelView)
        card.addView(valueView)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 8, 0, 8)

        panel.addView(card, params)
    }

    fun addButton(
        context: Context,
        panel: LinearLayout,
        title: String,
        onClick: () -> Unit
    ) {

        val button = TextView(context).apply {
            text = "◆  $title"
            textSize = 16f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)

            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 30, 50))
                setStroke(2, orangeColor)
                cornerRadius = 24f
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                onClick()
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 12, 0, 12)

        panel.addView(button, params)
    }
}
