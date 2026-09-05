package com.example.god

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import com.example.god.voice.AIState
import kotlin.math.cos
import kotlin.math.sin

class GodCoreView(context: Context) : View(context) {

    private val orange = Color.rgb(255, 145, 0)
    private val amber = Color.rgb(255, 195, 75)
    private val deepOrange = Color.rgb(185, 65, 0)
    private val dark = Color.rgb(2, 3, 5)

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var aiState = AIState.IDLE
    private var voiceLevel = 0f
    private var smoothVoice = 0f
    private var rotation = 0f
    private var reverseRotation = 0f
    private var voiceClickListener: (() -> Unit)? = null

    fun setAIState(state: AIState) {
        aiState = state
        invalidate()
    }

    fun setVoiceLevel(level: Float) {
        voiceLevel = level.coerceIn(0f, 1f)
        invalidate()
    }

    fun setVoiceClickListener(listener: () -> Unit) {
        voiceClickListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cx = width / 2f
            val cy = height * 0.50f
            val base = minOf(width, height) * 0.235f
            val micY = cy + base * 0.58f
            val dx = event.x - cx
            val dy = event.y - micY
            if (dx * dx + dy * dy <= (base * 0.20f) * (base * 0.20f)) {
                performClick()
                voiceClickListener?.invoke()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height * 0.50f
        val base = minOf(width, height) * 0.235f

        smoothVoice += (voiceLevel - smoothVoice) * 0.08f

        val stateEnergy = when (aiState) {
            AIState.LISTENING -> 0.35f + smoothVoice * 0.65f
            AIState.PROCESSING -> 0.85f
            AIState.SPEAKING -> 0.95f
            AIState.STARTING -> 0.65f
            AIState.ERROR -> 0.40f
            else -> 0.32f
        }

        rotation += 0.22f
        reverseRotation -= 0.14f

        drawAtmosphere(canvas, cx, cy, base, stateEnergy)
        drawMachinery(canvas, cx, cy, base)
        drawOuterRings(canvas, cx, cy, base, stateEnergy)
        drawEnergyCore(canvas, cx, cy, base, stateEnergy)
        drawVoiceControl(canvas, cx, cy, base, stateEnergy)

        postInvalidateDelayed(16L)
    }

    private fun drawAtmosphere(canvas: Canvas, cx: Float, cy: Float, base: Float, energy: Float) {
        fill.shader = RadialGradient(
            cx, cy, base * 3.2f,
            intArrayOf(
                Color.argb((85 + energy * 70).toInt(), 255, 145, 0),
                Color.argb((25 + energy * 25).toInt(), 255, 110, 0),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, base * 3.2f, fill)
        fill.shader = null
    }

    private fun drawMachinery(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        // Large angular brackets surrounding the reactor.
        stroke.color = Color.argb(105, 255, 145, 0)
        stroke.strokeWidth = 2f

        val r = base * 2.15f
        val gap = base * 0.28f
        val left = cx - r
        val right = cx + r
        val top = cy - r
        val bottom = cy + r

        val path = Path()
        path.moveTo(left + gap, top)
        path.lineTo(left, top + gap)
        path.lineTo(left, cy - base * 0.55f)
        path.lineTo(left - base * 0.10f, cy)
        path.lineTo(left, cy + base * 0.55f)
        path.lineTo(left, bottom - gap)
        path.lineTo(left + gap, bottom)
        canvas.drawPath(path, stroke)

        val path2 = Path()
        path2.moveTo(right - gap, top)
        path2.lineTo(right, top + gap)
        path2.lineTo(right, cy - base * 0.55f)
        path2.lineTo(right + base * 0.10f, cy)
        path2.lineTo(right, cy + base * 0.55f)
        path2.lineTo(right, bottom - gap)
        path2.lineTo(right - gap, bottom)
        canvas.drawPath(path2, stroke)

        // Horizontal mechanical rails.
        for (side in -1..1 step 2) {
            val y = cy + side * base * 1.48f
            stroke.alpha = 85
            canvas.drawLine(cx - base * 1.95f, y, cx - base * 1.20f, y, stroke)
            canvas.drawLine(cx + base * 1.20f, y, cx + base * 1.95f, y, stroke)
        }

        // Small fixed indicator blocks.
        fill.color = Color.rgb(255, 145, 0)
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30.0))
            val rr = base * 2.00f
            val x = cx + cos(angle).toFloat() * rr
            val y = cy + sin(angle).toFloat() * rr
            canvas.drawRect(x - 4f, y - 2f, x + 4f, y + 2f, fill)
        }
    }

    private fun drawOuterRings(canvas: Canvas, cx: Float, cy: Float, base: Float, energy: Float) {
        val rings = floatArrayOf(1.05f, 1.22f, 1.39f, 1.58f, 1.80f, 2.02f)
        rings.forEachIndexed { index, scale ->
            val r = base * scale
            stroke.color = Color.argb(
                (42 + index * 10 + energy * 35).toInt().coerceAtMost(180),
                255, 145, 0
            )
            stroke.strokeWidth = if (index < 2) 2.0f else 1.2f

            canvas.save()
            canvas.rotate(
                if (index % 2 == 0) rotation * (1f + index * 0.08f)
                else reverseRotation * (1f + index * 0.07f),
                cx, cy
            )

            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(rect, 0f, 72f + index * 9f, false, stroke)
            canvas.drawArc(rect, 145f, 52f + index * 8f, false, stroke)
            canvas.drawArc(rect, 250f, 42f + index * 7f, false, stroke)

            if (index >= 2) {
                for (tick in 0 until 18) {
                    if ((tick + index) % 3 == 0) {
                        val a = Math.toRadians(tick * 20.0)
                        val inner = r - 7f
                        val outer = r + 7f
                        canvas.drawLine(
                            cx + cos(a).toFloat() * inner,
                            cy + sin(a).toFloat() * inner,
                            cx + cos(a).toFloat() * outer,
                            cy + sin(a).toFloat() * outer,
                            stroke
                        )
                    }
                }
            }
            canvas.restore()
        }

        // Bright segmented power arc.
        canvas.save()
        canvas.rotate(-rotation * 0.55f, cx, cy)
        stroke.color = Color.argb((130 + energy * 100).toInt().coerceAtMost(255), 255, 145, 0)
        stroke.strokeWidth = base * 0.055f
        val rr = base * 1.66f
        canvas.drawArc(RectF(cx - rr, cy - rr, cx + rr, cy + rr), 206f, 42f, false, stroke)
        canvas.drawArc(RectF(cx - rr, cy - rr, cx + rr, cy + rr), 326f, 32f, false, stroke)
        canvas.restore()
    }

    private fun drawEnergyCore(canvas: Canvas, cx: Float, cy: Float, base: Float, energy: Float) {
        // Rotating internal lattice.
        canvas.save()
        canvas.rotate(rotation * 1.7f, cx, cy)
        stroke.color = Color.argb((110 + energy * 100).toInt().coerceAtMost(255), 255, 145, 0)
        stroke.strokeWidth = 1.5f

        for (i in 0 until 5) {
            val r = base * (0.34f + i * 0.12f)
            canvas.drawCircle(cx, cy, r, stroke)
        }

        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val inner = base * 0.25f
            val outer = base * 0.98f
            canvas.drawLine(
                cx + cos(a).toFloat() * inner,
                cy + sin(a).toFloat() * inner,
                cx + cos(a).toFloat() * outer,
                cy + sin(a).toFloat() * outer,
                stroke
            )
        }
        canvas.restore()

        // Core glow.
        fill.shader = RadialGradient(
            cx - base * 0.18f,
            cy - base * 0.18f,
            base * 0.92f,
            intArrayOf(
                Color.argb((235 + energy * 20).toInt().coerceAtMost(255), 255, 245, 195),
                Color.argb((245).toInt(), 255, 175, 25),
                Color.argb((145 + energy * 80).toInt().coerceAtMost(225), 210, 70, 0),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.20f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, base * 1.00f, fill)
        fill.shader = null

        // Stable physical center. No voice-based scaling.
        fill.color = Color.rgb(255, 220, 115)
        canvas.drawCircle(cx, cy, base * 0.22f, fill)

        fill.color = Color.WHITE
        canvas.drawCircle(cx, cy, base * 0.075f, fill)

        // Four cardinal energy emitters.
        stroke.color = Color.argb((140 + energy * 100).toInt().coerceAtMost(255), 255, 145, 0)
        stroke.strokeWidth = 3f
        val emitterInner = base * 0.42f
        val emitterOuter = base * 0.82f
        for (i in 0 until 4) {
            val a = Math.toRadians(i * 90.0)
            canvas.drawLine(
                cx + cos(a).toFloat() * emitterInner,
                cy + sin(a).toFloat() * emitterInner,
                cx + cos(a).toFloat() * emitterOuter,
                cy + sin(a).toFloat() * emitterOuter,
                stroke
            )
        }
    }

    private fun drawVoiceControl(canvas: Canvas, cx: Float, cy: Float, base: Float, energy: Float) {
        val micY = cy + base * 0.58f
        val active = aiState == AIState.LISTENING

        // Dedicated microphone HUD circle integrated with the core.
        stroke.color = Color.argb(
            if (active) (190 + energy * 65).toInt().coerceAtMost(255) else 165,
            255, 145, 0
        )
        stroke.strokeWidth = if (active) 3.2f else 2.0f
        canvas.drawCircle(cx, micY, base * 0.20f, stroke)

        stroke.strokeWidth = 2.4f
        val micTop = micY - base * 0.09f
        val micBottom = micY + base * 0.055f
        canvas.drawRoundRect(
            RectF(cx - base * 0.055f, micTop, cx + base * 0.055f, micBottom),
            base * 0.055f, base * 0.055f, stroke
        )
        canvas.drawArc(
            RectF(cx - base * 0.105f, micY - base * 0.01f, cx + base * 0.105f, micY + base * 0.115f),
            0f, 180f, false, stroke
        )
        canvas.drawLine(cx, micY + base * 0.11f, cx, micY + base * 0.15f, stroke)
        canvas.drawLine(cx - base * 0.06f, micY + base * 0.15f, cx + base * 0.06f, micY + base * 0.15f, stroke)

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (active) amber else Color.WHITE
            textSize = base * 0.075f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            if (active) "LISTENING" else "VOICE",
            cx,
            micY + base * 0.30f,
            label
        )

        if (active) {
            stroke.color = Color.argb((100 + energy * 130).toInt().coerceAtMost(230), 255, 145, 0)
            stroke.strokeWidth = 1.5f
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                val inner = base * 0.25f
                val outer = inner + base * (0.04f + smoothVoice * 0.08f)
                canvas.drawLine(
                    cx + cos(a).toFloat() * inner,
                    micY + sin(a).toFloat() * inner,
                    cx + cos(a).toFloat() * outer,
                    micY + sin(a).toFloat() * outer,
                    stroke
                )
            }
        }
    }
}
