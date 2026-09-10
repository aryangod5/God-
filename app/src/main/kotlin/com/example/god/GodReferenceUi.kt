package com.example.god

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * GOD home HUD.
 *
 * Visual direction:
 * - very dark charcoal/black base
 * - extremely subtle orange technical grid
 * - soft smoky/cloudy orange atmosphere
 * - thin futuristic frame/corner geometry
 * - original arc-reactor-inspired central reactor
 * - reactor rotation/energy activity is driven by recognition state, not by RMS size
 * - all readable UI text is white; orange is reserved for HUD graphics/active indicators
 */
class GodReferenceUi(
    context: Context,
    private val onAction: ((String) -> Unit)? = null
) : View(context) {

    companion object {
        const val ACTION_VOICE = "VOICE"
        const val ACTION_CHAT = "CHAT"
        const val ACTION_APPS = "APPS"
        const val ACTION_FILES = "FILES"
        const val ACTION_DOCUMENTS = "DOCUMENTS"
        const val ACTION_MEMORY = "MEMORY"
        const val ACTION_SECURITY = "SECURITY"
        const val ACTION_PERMISSIONS = "PERMISSIONS"
        const val ACTION_AUTHORIZED = "AUTHORIZED_FOLDER"
        const val ACTION_SETTINGS = "SETTINGS"
        const val ACTION_BACK = "BACK"
    }

    private val black = Color.rgb(2, 3, 4)
    private val charcoal = Color.rgb(8, 8, 9)
    private val orange = Color.rgb(255, 128, 20)
    private val amber = Color.rgb(255, 177, 62)
    private val white = Color.rgb(242, 242, 242)
    private val dimWhite = Color.rgb(142, 145, 148)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    private var rotation = 0f
    private var secondaryRotation = 0f
    private var energyRotation = 0f
    private var voiceLevel = 0f
    private var smoothVoice = 0f
    private var voiceActive = false
    private var processing = false
    private var speaking = false
    private var menuVisible = false

    private var noticeTitle = ""
    private var noticeBody = ""
    private var noticeUntil = 0L

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isClickable = true
    }

    fun setVoiceLevel(level: Float) {
        voiceLevel = level.coerceIn(0f, 1f)
        invalidate()
    }

    fun setVoiceActive(active: Boolean) {
        voiceActive = active
        invalidate()
    }

    fun setProcessing(active: Boolean) {
        processing = active
        invalidate()
    }

    fun setSpeaking(active: Boolean) {
        speaking = active
        invalidate()
    }

    fun showNotice(title: String, body: String) {
        noticeTitle = title
        noticeBody = body
        noticeUntil = System.currentTimeMillis() + 2600L
        invalidate()
    }

    fun returnHome() {
        menuVisible = false
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        drawBackground(c)

        // The reactor has a calm idle drift. Recognition/processing makes the mechanics move.
        val targetSpeed = when {
            voiceActive -> 1.35f
            processing -> 0.85f
            speaking -> 0.48f
            else -> 0.08f
        }
        rotation = (rotation + targetSpeed) % 360f
        secondaryRotation = (secondaryRotation - targetSpeed * 0.55f) % 360f
        energyRotation = (energyRotation + targetSpeed * 1.7f) % 360f

        smoothVoice += (voiceLevel - smoothVoice) * 0.075f

        drawFrame(c)
        drawTopHud(c)
        drawReactor(c)
        drawTinyStatus(c)

        if (menuVisible) drawMenu(c)
        if (noticeUntil > System.currentTimeMillis()) {
            drawNotice(c)
            postInvalidateDelayed(70L)
        }

        postInvalidateDelayed(16L)
    }

    private fun drawBackground(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.48f

        // Deep charcoal base.
        fill.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(charcoal, black, Color.rgb(1, 2, 3)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, fill)
        fill.shader = null

        // Soft cloudy/smoky orange atmosphere. Kept low-alpha so the grid remains subtle.
        val cloud1 = RadialGradient(
            w * 0.18f, h * 0.34f, max(w, h) * 0.55f,
            intArrayOf(Color.argb(24, 255, 108, 0), Color.argb(9, 255, 90, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        fill.shader = cloud1
        c.drawRect(0f, 0f, w, h, fill)

        val cloud2 = RadialGradient(
            w * 0.82f, h * 0.63f, max(w, h) * 0.58f,
            intArrayOf(Color.argb(18, 255, 142, 30), Color.argb(7, 255, 94, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP
        )
        fill.shader = cloud2
        c.drawRect(0f, 0f, w, h, fill)
        fill.shader = null

        // Technical grid: deliberately dim and non-glossy.
        stroke.color = Color.argb(22, 255, 128, 20)
        stroke.strokeWidth = dp(0.7f)
        val step = dp(28f)
        var x = 0f
        while (x <= w) {
            c.drawLine(x, 0f, x, h, stroke)
            x += step
        }
        var y = 0f
        while (y <= h) {
            c.drawLine(0f, y, w, y, stroke)
            y += step
        }

        // A second, larger technical grid is even fainter.
        stroke.color = Color.argb(10, 255, 160, 60)
        stroke.strokeWidth = dp(1f)
        val large = dp(112f)
        x = 0f
        while (x <= w) {
            c.drawLine(x, 0f, x, h, stroke)
            x += large
        }
        y = 0f
        while (y <= h) {
            c.drawLine(0f, y, w, y, stroke)
            y += large
        }

        // Very faint center haze to integrate the reactor with the background.
        fill.shader = RadialGradient(
            cx, cy, min(w, h) * 0.58f,
            intArrayOf(Color.argb(17, 255, 122, 10), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, min(w, h) * 0.58f, fill)
        fill.shader = null
    }

    private fun drawFrame(c: Canvas) {
        val pad = dp(14f)
        val top = dp(74f)
        val bottom = height - dp(24f)
        val right = width - pad
        val left = pad
        val cut = dp(18f)

        stroke.color = Color.argb(65, 255, 128, 20)
        stroke.strokeWidth = dp(1f)

        // Angular outer HUD frame.
        val path = Path().apply {
            moveTo(left + cut, top)
            lineTo(right - cut, top)
            lineTo(right, top + cut)
            lineTo(right, bottom - cut)
            lineTo(right - cut, bottom)
            lineTo(left + cut, bottom)
            lineTo(left, bottom - cut)
            lineTo(left, top + cut)
            close()
        }
        c.drawPath(path, stroke)

        // Corner brackets.
        stroke.color = Color.argb(90, 255, 145, 30)
        stroke.strokeWidth = dp(1.4f)
        val b = dp(34f)
        c.drawLine(left, top + b, left, top + dp(7f), stroke)
        c.drawLine(left, top + dp(7f), left + b, top + dp(7f), stroke)
        c.drawLine(right, top + b, right, top + dp(7f), stroke)
        c.drawLine(right, top + dp(7f), right - b, top + dp(7f), stroke)
        c.drawLine(left, bottom - b, left, bottom - dp(7f), stroke)
        c.drawLine(left, bottom - dp(7f), left + b, bottom - dp(7f), stroke)
        c.drawLine(right, bottom - b, right, bottom - dp(7f), stroke)
        c.drawLine(right, bottom - dp(7f), right - b, bottom - dp(7f), stroke)

        // Small side ticks, like a diagnostic frame rather than a button.
        stroke.color = Color.argb(45, 255, 128, 20)
        stroke.strokeWidth = dp(1f)
        for (i in 0 until 8) {
            val yy = top + dp(80f) + i * dp(28f)
            c.drawLine(left + dp(3f), yy, left + dp(12f), yy, stroke)
            c.drawLine(right - dp(12f), yy, right - dp(3f), yy, stroke)
        }
    }

    private fun drawTopHud(c: Canvas) {
        drawText(c, "GOD", dp(22f), dp(39f), 15f, white, true)
        val state = when {
            voiceActive -> "LISTENING"
            processing -> "THINKING"
            speaking -> "SPEAKING"
            else -> "READY"
        }
        drawText(c, state, dp(22f), dp(58f), 8.5f, white, false)

        // Empty top-right area contains only the three-dot menu.
        val x = width - dp(29f)
        val y = dp(36f)
        fill.color = orange
        c.drawCircle(x, y - dp(7f), dp(2f), fill)
        c.drawCircle(x, y, dp(2f), fill)
        c.drawCircle(x, y + dp(7f), dp(2f), fill)
    }

    private fun drawReactor(c: Canvas) {
        val cx = width * 0.5f
        val cy = height * 0.49f
        val radius = min(width, height) * 0.255f

        // RMS changes light/energy only. Geometry stays stable.
        val activity = (0.10f + smoothVoice * 0.48f +
            if (voiceActive) 0.16f else 0f +
            if (processing) 0.12f else 0f +
            if (speaking) 0.08f else 0f).coerceIn(0.10f, 0.88f)

        // Low-intensity atmospheric glow.
        fill.shader = RadialGradient(
            cx, cy, radius * 1.75f,
            intArrayOf(
                Color.argb((70 * activity).toInt(), 255, 125, 20),
                Color.argb((24 * activity).toInt(), 255, 125, 20),
                Color.TRANSPARENT
            ), null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius * 1.65f, fill)
        fill.shader = null

        // Outer housing.
        stroke.color = Color.argb(110, 255, 128, 20)
        stroke.strokeWidth = dp(1.2f)
        c.drawCircle(cx, cy, radius * 1.18f, stroke)
        stroke.color = Color.argb(48, 255, 128, 20)
        stroke.strokeWidth = dp(1f)
        c.drawCircle(cx, cy, radius * 1.28f, stroke)

        // Outer segmented ring.
        c.save()
        c.rotate(rotation, cx, cy)
        for (i in 0 until 48) {
            val a = Math.toRadians(i * 7.5)
            val longTick = i % 4 == 0
            val r1 = radius * if (longTick) 1.05f else 1.10f
            val r2 = radius * 1.17f
            val x1 = cx + cos(a).toFloat() * r1
            val y1 = cy + sin(a).toFloat() * r1
            val x2 = cx + cos(a).toFloat() * r2
            val y2 = cy + sin(a).toFloat() * r2
            stroke.color = if (longTick) Color.argb(125, 255, 160, 45) else Color.argb(65, 255, 128, 20)
            stroke.strokeWidth = if (longTick) dp(1.6f) else dp(0.8f)
            c.drawLine(x1, y1, x2, y2, stroke)
        }
        c.restore()

        // Mechanical triangular/slot geometry.
        c.save()
        c.rotate(secondaryRotation, cx, cy)
        stroke.color = Color.argb(125, 255, 142, 30)
        stroke.strokeWidth = dp(1.4f)
        for (i in 0 until 8) {
            c.save()
            c.rotate(i * 45f, cx, cy)
            val r = radius * 0.80f
            val p = Path().apply {
                moveTo(cx, cy - r * 0.36f)
                lineTo(cx + r * 0.11f, cy - r * 0.50f)
                lineTo(cx + r * 0.18f, cy - r * 0.30f)
                lineTo(cx + r * 0.07f, cy - r * 0.18f)
                close()
            }
            c.drawPath(p, stroke)
            c.restore()
        }
        c.restore()

        // Inner energy ring.
        c.save()
        c.rotate(energyRotation, cx, cy)
        stroke.color = Color.argb((115 + activity * 80).toInt(), 255, 171, 54)
        stroke.strokeWidth = dp(2f)
        val inner = RectF(cx - radius * .72f, cy - radius * .72f, cx + radius * .72f, cy + radius * .72f)
        c.drawArc(inner, 15f, 105f, false, stroke)
        c.drawArc(inner, 195f, 105f, false, stroke)
        c.restore()

        // Reactor face: dark metal + warm core, kept visually close to an arc reactor.
        fill.shader = RadialGradient(
            cx, cy, radius * 0.70f,
            intArrayOf(
                Color.argb(250, 255, 207, 116),
                Color.argb(220, 255, 145, 28),
                Color.argb(150, 72, 37, 11),
                Color.rgb(12, 10, 8)
            ),
            floatArrayOf(0f, 0.16f, 0.48f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius * .69f, fill)
        fill.shader = null

        stroke.color = Color.argb(190, 255, 153, 35)
        stroke.strokeWidth = dp(2f)
        c.drawCircle(cx, cy, radius * .70f, stroke)

        // Fixed central emitter: no size bouncing with microphone RMS.
        fill.shader = RadialGradient(
            cx, cy, radius * .26f,
            intArrayOf(
                Color.argb((255).coerceAtMost((180 + activity * 85).toInt()), 255, 236, 184),
                Color.argb(220, 255, 162, 42),
                Color.argb(30, 255, 120, 10)
            ), null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius * .25f, fill)
        fill.shader = null
        stroke.color = Color.argb(235, 255, 182, 64)
        stroke.strokeWidth = dp(2f)
        c.drawCircle(cx, cy, radius * .25f, stroke)
        stroke.color = Color.argb(160, 255, 145, 30)
        stroke.strokeWidth = dp(1f)
        c.drawCircle(cx, cy, radius * .12f, stroke)

        // Small center microphone symbol. Touch zone is much larger than the glyph.
        val micR = radius * .16f
        stroke.color = Color.argb(230, 255, 193, 80)
        stroke.strokeWidth = dp(1.7f)
        c.drawRoundRect(
            RectF(cx - micR * .34f, cy - micR * .68f, cx + micR * .34f, cy + micR * .16f),
            micR * .34f, micR * .34f, stroke
        )
        c.drawArc(
            RectF(cx - micR * .62f, cy - micR * .22f, cx + micR * .62f, cy + micR * .66f),
            0f, 180f, false, stroke
        )
        c.drawLine(cx, cy + micR * .66f, cx, cy + micR * 1.02f, stroke)
        c.drawLine(cx - micR * .38f, cy + micR * 1.02f, cx + micR * .38f, cy + micR * 1.02f, stroke)

        // Recognition activity ticks: brightness changes, geometry remains fixed.
        if (voiceActive || processing) {
            c.save()
            c.rotate(-rotation * .35f, cx, cy)
            stroke.color = Color.argb((100 + activity * 120).toInt(), 255, 154, 42)
            stroke.strokeWidth = dp(1.3f)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                val r1 = radius * 1.34f
                val r2 = radius * (1.38f + if (i % 2 == 0) 0.035f else 0f)
                c.drawLine(
                    cx + cos(a).toFloat() * r1,
                    cy + sin(a).toFloat() * r1,
                    cx + cos(a).toFloat() * r2,
                    cy + sin(a).toFloat() * r2,
                    stroke
                )
            }
            c.restore()
        }
    }

    private fun drawTinyStatus(c: Canvas) {
        val y = height * .49f + min(width, height) * .34f
        drawText(c, "CORE", dp(23f), y, 8f, dimWhite, false)
        drawText(c, if (voiceActive) "VOICE ACTIVE" else "VOICE STANDBY", dp(23f), y + dp(16f), 8f, white, false)
        drawTextRight(c, if (processing) "PROCESSING" else "LOCAL CORE", width - dp(23f), y, 8f, dimWhite)
        drawTextRight(c, if (speaking) "OUTPUT" else "ONLINE", width - dp(23f), y + dp(16f), 8f, white)
    }

    private fun drawMenu(c: Canvas) {
        val left = width - dp(258f)
        val top = dp(76f)
        val right = width - dp(16f)
        val bottom = min(height - dp(18f), top + dp(455f))

        fill.color = Color.argb(240, 7, 8, 9)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(9f), dp(9f), fill)
        stroke.color = Color.argb(170, 255, 128, 20)
        stroke.strokeWidth = dp(1.2f)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(9f), dp(9f), stroke)

        drawText(c, "SYSTEM", left + dp(17f), top + dp(27f), 10f, white, true)

        val items = arrayOf(
            "Apps", "Files", "Documents", "Memory",
            "Security", "Permissions", "Authorized Folder", "Settings"
        )

        items.forEachIndexed { i, label ->
            val yy = top + dp(61f + i * 47f)
            stroke.color = Color.argb(42, 255, 128, 20)
            stroke.strokeWidth = dp(1f)
            c.drawLine(left + dp(14f), yy + dp(13f), right - dp(14f), yy + dp(13f), stroke)
            drawText(c, label, left + dp(17f), yy, 12f, white, false)
        }
    }

    private fun drawNotice(c: Canvas) {
        val left = dp(22f)
        val right = width - dp(22f)
        val top = height - dp(108f)
        val bottom = height - dp(36f)
        fill.color = Color.argb(235, 6, 7, 8)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(8f), dp(8f), fill)
        stroke.color = Color.argb(180, 255, 128, 20)
        stroke.strokeWidth = dp(1f)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(8f), dp(8f), stroke)
        drawText(c, noticeTitle, left + dp(13f), top + dp(23f), 9f, white, true)
        drawText(c, noticeBody, left + dp(13f), top + dp(46f), 9f, white, false)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val x = e.x
        val y = e.y

        if (x > width - dp(72f) && y < dp(72f)) {
            menuVisible = !menuVisible
            invalidate()
            return true
        }

        if (menuVisible) {
            val left = width - dp(258f)
            val top = dp(76f)
            val right = width - dp(16f)
            val rowStart = top + dp(34f)
            if (x in left..right && y >= rowStart && y <= top + dp(61f + 8 * 47f)) {
                val index = floor((y - rowStart) / dp(47f)).toInt()
                when (index) {
                    0 -> onAction?.invoke(ACTION_APPS)
                    1 -> onAction?.invoke(ACTION_FILES)
                    2 -> onAction?.invoke(ACTION_DOCUMENTS)
                    3 -> onAction?.invoke(ACTION_MEMORY)
                    4 -> onAction?.invoke(ACTION_SECURITY)
                    5 -> onAction?.invoke(ACTION_PERMISSIONS)
                    6 -> onAction?.invoke(ACTION_AUTHORIZED)
                    7 -> onAction?.invoke(ACTION_SETTINGS)
                }
                menuVisible = false
                invalidate()
                return true
            }
            menuVisible = false
            invalidate()
            return true
        }

        // Large invisible reactor hit area; only the small center glyph is visible.
        val cx = width * .5f
        val cy = height * .49f
        val r = min(width, height) * .35f
        if (hypot(x - cx, y - cy) <= r) {
            onAction?.invoke(ACTION_VOICE)
            return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawText(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean) {
        text.textSize = dp(size)
        text.color = color
        text.typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s, x, y, text)
    }

    private fun drawTextRight(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        text.textSize = dp(size)
        text.color = color
        text.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        c.drawText(s, x - text.measureText(s), y, text)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
