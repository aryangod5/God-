package com.example.god

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * Clean GOD home HUD.
 *
 * Home intentionally exposes no task-specific feature buttons.
 * The reactor is the main interaction surface and the three-dot control opens
 * only device-management screens. Voice/chat/media/code abilities are routed
 * automatically from natural language.
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

    private val bg = Color.rgb(1, 3, 5)
    private val bg2 = Color.rgb(5, 8, 12)
    private val panel = Color.argb(236, 7, 10, 14)
    private val orange = Color.rgb(255, 145, 0)
    private val bright = Color.rgb(255, 193, 72)
    private val white = Color.rgb(245, 245, 245)
    private val dim = Color.rgb(135, 145, 156)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    private var angle = 0f
    private var innerAngle = 0f
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
        noticeUntil = System.currentTimeMillis() + 2400L
        invalidate()
    }

    fun returnHome() {
        menuVisible = false
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        drawBackground(c)

        angle = (angle + 0.32f) % 360f
        innerAngle = (innerAngle - 0.18f) % 360f
        smoothVoice += (voiceLevel - smoothVoice) * 0.075f

        drawTopHud(c)
        drawReactor(c)
        drawTinyStatus(c)

        if (menuVisible) drawMenu(c)
        if (noticeUntil > System.currentTimeMillis()) {
            drawNotice(c)
            postInvalidateDelayed(60L)
        }

        postInvalidateDelayed(16L)
    }

    private fun drawBackground(c: Canvas) {
        val g = RadialGradient(
            width * .5f, height * .48f, max(width, height) * .72f,
            intArrayOf(bg2, bg, Color.BLACK),
            floatArrayOf(0f, .55f, 1f),
            Shader.TileMode.CLAMP
        )
        fill.shader = g
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        fill.shader = null

        stroke.color = Color.argb(24, 255, 145, 0)
        stroke.strokeWidth = dp(1f)
        for (i in 1..8) {
            val y = height * i / 9f
            c.drawLine(dp(14f), y, width - dp(14f), y, stroke)
        }
    }

    private fun drawTopHud(c: Canvas) {
        drawText(c, "GOD", dp(22f), dp(42f), 15f, white, true)
        drawText(c, if (voiceActive) "LISTENING" else if (processing) "THINKING" else if (speaking) "SPEAKING" else "READY",
            dp(22f), dp(63f), 9f, white, false)

        // Small, clean three-dot control in otherwise empty top-right space.
        val x = width - dp(30f)
        val y = dp(38f)
        fill.color = orange
        c.drawCircle(x, y - dp(7f), dp(2.2f), fill)
        c.drawCircle(x, y, dp(2.2f), fill)
        c.drawCircle(x, y + dp(7f), dp(2.2f), fill)
    }

    private fun drawReactor(c: Canvas) {
        val cx = width * .5f
        val cy = height * .53f
        val radius = min(width, height) * .285f

        val glow = (0.18f + smoothVoice * 0.50f +
            if (voiceActive) .10f else 0f +
            if (processing) .08f else 0f +
            if (speaking) .10f else 0f).coerceIn(.18f, .92f)

        fill.shader = RadialGradient(
            cx, cy, radius * 1.65f,
            intArrayOf(
                Color.argb((105 * glow).toInt(), 255, 145, 0),
                Color.argb((40 * glow).toInt(), 255, 145, 0),
                Color.TRANSPARENT
            ),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius * 1.55f, fill)
        fill.shader = null

        stroke.color = Color.argb(80, 255, 145, 0)
        stroke.strokeWidth = dp(1f)
        c.drawCircle(cx, cy, radius * 1.02f, stroke)

        c.save()
        c.rotate(angle, cx, cy)
        for (i in 0 until 24) {
            val a = Math.toRadians((i * 15.0))
            val r1 = radius * .90f
            val r2 = if (i % 3 == 0) radius * 1.10f else radius * 1.02f
            val x1 = cx + cos(a).toFloat() * r1
            val y1 = cy + sin(a).toFloat() * r1
            val x2 = cx + cos(a).toFloat() * r2
            val y2 = cy + sin(a).toFloat() * r2
            stroke.color = if (i % 3 == 0) bright else Color.argb(130, 255, 145, 0)
            stroke.strokeWidth = if (i % 3 == 0) dp(2f) else dp(1f)
            c.drawLine(x1, y1, x2, y2, stroke)
        }
        c.restore()

        c.save()
        c.rotate(innerAngle, cx, cy)
        stroke.color = Color.argb(170, 255, 145, 0)
        stroke.strokeWidth = dp(1.5f)
        val oval = RectF(cx - radius * .72f, cy - radius * .72f, cx + radius * .72f, cy + radius * .72f)
        c.drawArc(oval, 25f, 105f, false, stroke)
        c.drawArc(oval, 205f, 105f, false, stroke)
        c.restore()

        fill.shader = RadialGradient(
            cx, cy, radius * .64f,
            intArrayOf(
                Color.argb(235, 255, 220, 140),
                Color.argb(170, 255, 145, 0),
                Color.argb(90, 120, 55, 0),
                Color.rgb(8, 9, 10)
            ),
            floatArrayOf(0f, .18f, .56f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius * .66f, fill)
        fill.shader = null

        stroke.color = Color.argb(210, 255, 145, 0)
        stroke.strokeWidth = dp(2f)
        c.drawCircle(cx, cy, radius * .67f, stroke)

        // Invisible voice interaction zone; visual is just a tiny mic HUD glyph.
        val micR = radius * .14f
        stroke.color = Color.argb(230, 255, 193, 72)
        stroke.strokeWidth = dp(2f)
        c.drawRoundRect(
            RectF(cx - micR * .34f, cy - micR * .62f, cx + micR * .34f, cy + micR * .22f),
            micR * .34f, micR * .34f, stroke
        )
        c.drawArc(
            RectF(cx - micR * .62f, cy - micR * .20f, cx + micR * .62f, cy + micR * .70f),
            0f, 180f, false, stroke
        )
        c.drawLine(cx, cy + micR * .70f, cx, cy + micR * 1.05f, stroke)
        c.drawLine(cx - micR * .38f, cy + micR * 1.05f, cx + micR * .38f, cy + micR * 1.05f, stroke)
    }

    private fun drawTinyStatus(c: Canvas) {
        val y = height * .53f + min(width, height) * .36f
        drawText(c, "CORE", dp(24f), y, 9f, dim, false)
        drawText(c, "VOICE", dp(24f), y + dp(18f), 9f, if (voiceActive) orange else dim, false)

        val right = width - dp(24f)
        drawTextRight(c, "LOCAL", right, y, 9f, dim)
        drawTextRight(c, if (processing) "PROCESSING" else "ONLINE", right, y + dp(18f), 9f, white)
    }

    private fun drawMenu(c: Canvas) {
        val left = width - dp(255f)
        val top = dp(86f)
        val right = width - dp(18f)
        val bottom = min(height - dp(18f), top + dp(470f))

        fill.color = panel
        c.drawRoundRect(RectF(left, top, right, bottom), dp(10f), dp(10f), fill)
        stroke.color = orange
        stroke.strokeWidth = dp(1.2f)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(10f), dp(10f), stroke)

        drawText(c, "SYSTEM", left + dp(18f), top + dp(28f), 11f, bright, true)

        // Only management/configuration screens live here.
        val items = arrayOf(
            "Apps", "Files", "Documents", "Memory",
            "Security", "Permissions", "Authorized Folder", "Settings"
        )

        items.forEachIndexed { i, label ->
            val yy = top + dp(65f + i * 47f)
            stroke.color = Color.argb(55, 255, 145, 0)
            stroke.strokeWidth = dp(1f)
            c.drawLine(left + dp(15f), yy + dp(14f), right - dp(15f), yy + dp(14f), stroke)
            drawText(c, label, left + dp(18f), yy, 12.5f, white, false)
        }
    }

    private fun drawNotice(c: Canvas) {
        val left = dp(24f)
        val right = width - dp(24f)
        val top = height - dp(110f)
        val bottom = height - dp(42f)
        fill.color = Color.argb(225, 7, 10, 14)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(8f), dp(8f), fill)
        stroke.color = orange
        stroke.strokeWidth = dp(1f)
        c.drawRoundRect(RectF(left, top, right, bottom), dp(8f), dp(8f), stroke)
        drawText(c, noticeTitle, left + dp(14f), top + dp(24f), 10f, bright, true)
        drawText(c, noticeBody, left + dp(14f), top + dp(46f), 10f, white, false)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y

        if (e.action != MotionEvent.ACTION_UP) return true

        // Three dots.
        if (x > width - dp(72f) && y < dp(80f)) {
            menuVisible = !menuVisible
            invalidate()
            return true
        }

        if (menuVisible) {
            val left = width - dp(255f)
            val top = dp(86f)
            val right = width - dp(18f)
            val rowTop = top + dp(43f)

            if (x in left..right && y >= rowTop && y <= top + dp(65f + 8 * 47f)) {
                val index = floor((y - rowTop) / dp(47f)).toInt()
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

        // Tap anywhere on the reactor = voice. This keeps voice/chat out of the menu.
        val cx = width * .5f
        val cy = height * .53f
        val r = min(width, height) * .36f
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
