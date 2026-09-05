package com.example.god

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * GOD Reference UI
 *
 * A code-drawn replacement UI based on the supplied GOD reference:
 * - black/dark metallic background
 * - amber/orange HUD geometry and glow
 * - white information text
 * - large concentric reactor/core
 * - left gesture-control HUD
 * - right vertical system menu
 * - voice monitor
 * - system-health and quick-access panels
 *
 * This file contains NO generated images and NO bitmap assets.
 *
 * Integration:
 *   setContentView(GodReferenceUi(this) { action -> ... })
 *
 * The callback gives the host Activity the action selected by the user.
 * "VOICE" is also fired by the invisible touch zone in the center of the core.
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
        const val ACTION_PROVIDER = "AI_PROVIDER"
        const val ACTION_SECURITY = "SECURITY"
        const val ACTION_PERMISSIONS = "PERMISSIONS"
        const val ACTION_AUTHORIZED = "AUTHORIZED_FOLDER"
        const val ACTION_SETTINGS = "SETTINGS"
        const val ACTION_GESTURE = "GESTURE"
        const val ACTION_BACK = "BACK"
    }

    private val black = Color.rgb(1, 3, 5)
    private val black2 = Color.rgb(5, 8, 12)
    private val panel = Color.argb(226, 7, 11, 16)
    private val panelStrong = Color.argb(242, 5, 8, 12)

    private val orange = Color.rgb(255, 145, 0)
    private val brightOrange = Color.rgb(255, 177, 28)
    private val amber = Color.rgb(255, 203, 92)
    private val white = Color.rgb(245, 245, 245)
    private val softWhite = Color.rgb(218, 222, 228)
    private val dim = Color.rgb(145, 153, 164)

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    private enum class Page {
        HOME, CHAT, APPS, FILES, DOCUMENTS, MEMORY,
        PROVIDER, SECURITY, PERMISSIONS, AUTHORIZED, SETTINGS
    }

    private var page = Page.HOME
    private var menuVisible = true

    // Core motion is fixed-speed. Audio level changes glow only.
    private var coreAngle = 0f
    private var counterAngle = 0f
    private var voiceLevel = 0f
    private var smoothVoice = 0f
    private var voiceActive = false
    private var processing = false
    private var speaking = false

    private var noticeTitle = ""
    private var noticeBody = ""
    private var noticeUntil = 0L

    init {
        isFocusable = true
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        p.color = black
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
        page = Page.HOME
        menuVisible = true
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        smoothVoice += (voiceLevel - smoothVoice) * 0.075f

        if (page == Page.HOME) {
            drawHome(c)
        } else {
            drawSecondaryPage(c)
        }

        if (noticeUntil > System.currentTimeMillis()) {
            drawNotice(c)
            postInvalidateDelayed(40L)
        }

        // Slow planetary/reaction motion. Never driven by RMS level.
        coreAngle = (coreAngle + 0.20f) % 360f
        counterAngle = (counterAngle - 0.13f) % 360f
        postInvalidateDelayed(16L)
    }

    private fun drawHome(c: Canvas) {
        drawBackground(c)
        drawTopIdentity(c)
        drawStatusStrip(c)
        drawMechanicalSideStructures(c)

        drawReactor(c)

        drawGestureCard(c)
        drawVoiceMonitor(c)
        drawHealthCard(c)
        drawQuickAccess(c)

        if (menuVisible) drawRightMenu(c)
    }

    private fun drawBackground(c: Canvas) {
        c.drawColor(black)

        // Deep vertical atmosphere.
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            black,
            black2,
            Shader.TileMode.CLAMP
        )
        p.shader = gradient
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        p.shader = null

        // Very subtle technical grid, deliberately low contrast.
        stroke.strokeWidth = dp(0.55f)
        stroke.color = Color.argb(23, 255, 145, 0)
        val step = dp(44f)
        var x = 0f
        while (x <= width) {
            c.drawLine(x, dp(104f), x, height.toFloat(), stroke)
            x += step
        }
        var y = dp(104f)
        while (y <= height) {
            c.drawLine(0f, y, width.toFloat(), y, stroke)
            y += step
        }

        // Horizontal floor perspective.
        stroke.color = Color.argb(45, 255, 145, 0)
        for (i in 1..7) {
            val yy = height - dp(70f) - i * dp(26f)
            c.drawLine(dp(18f), yy, width - dp(18f), yy, stroke)
        }
    }

    private fun drawTopIdentity(c: Canvas) {
        drawCenteredText(c, "GOD", width / 2f, dp(71f), 42f, brightOrange, true)

        // Thin angular header rail.
        stroke.strokeWidth = dp(1.25f)
        stroke.color = orange
        val y = dp(103f)
        val path = Path()
        path.moveTo(dp(27f), y)
        path.lineTo(width * 0.34f, y)
        path.lineTo(width * 0.375f, y + dp(17f))
        path.lineTo(width * 0.625f, y + dp(17f))
        path.lineTo(width * 0.66f, y)
        path.lineTo(width - dp(27f), y)
        c.drawPath(path, stroke)

        // Vertical menu glyph.
        drawText(c, "⋮", width - dp(37f), dp(77f), 34f, white, false)
    }

    private fun drawStatusStrip(c: Canvas) {
        val l = dp(29f)
        val t = dp(129f)
        val r = width - dp(29f)
        val b = dp(191f)

        drawAngularPanel(c, l, t, r, b, orange, 1.15f, panelStrong)

        val labels = arrayOf(
            "CORE: ONLINE",
            "VOICE: READY",
            "AI: CONNECTED",
            "SYSTEM: STANDBY"
        )

        val usable = r - l - dp(20f)
        val segment = usable / labels.size
        labels.forEachIndexed { i, label ->
            val x = l + dp(15f) + segment * i
            drawStatusDot(c, x, t + dp(31f), orange)
            drawText(c, label, x + dp(14f), t + dp(37f), 12.5f, white, false)
        }
    }

    private fun drawMechanicalSideStructures(c: Canvas) {
        // Industrial side brackets visible behind the reactor.
        stroke.strokeWidth = dp(2f)
        stroke.color = Color.argb(92, brightOrange.red(), brightOrange.green(), brightOrange.blue())

        val left = Path()
        left.moveTo(dp(19f), dp(247f))
        left.lineTo(dp(45f), dp(295f))
        left.lineTo(dp(45f), height - dp(305f))
        left.lineTo(dp(22f), height - dp(265f))
        c.drawPath(left, stroke)

        val right = Path()
        right.moveTo(width - dp(19f), dp(247f))
        right.lineTo(width - dp(45f), dp(295f))
        right.lineTo(width - dp(45f), height - dp(305f))
        right.lineTo(width - dp(22f), height - dp(265f))
        c.drawPath(right, stroke)

        // Small illuminated machinery ticks.
        for (i in 0 until 8) {
            val yy = dp(310f) + i * dp(56f)
            stroke.strokeWidth = dp(3f)
            stroke.color = Color.argb(
                (40 + 55 * (i % 3)).coerceIn(0, 255),
                orange.red(), orange.green(), orange.blue()
            )
            c.drawLine(dp(31f), yy, dp(47f), yy, stroke)
            c.drawLine(width - dp(47f), yy, width - dp(31f), yy, stroke)
        }
    }

    private fun drawReactor(c: Canvas) {
        val cx = width * 0.50f
        val cy = height * 0.485f
        val base = min(width, height) * 0.235f

        val stateEnergy = when {
            speaking -> 0.98f
            processing -> 0.84f
            voiceActive -> 0.38f + smoothVoice * 0.62f
            else -> 0.28f
        }

        // Soft orange atmospheric halo.
        val halo = RadialGradient(
            cx, cy, base * 1.55f,
            intArrayOf(
                Color.argb((40 + stateEnergy * 50f).toInt(), 255, 130, 0),
                Color.argb(18, 255, 105, 0),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        p.shader = halo
        c.drawCircle(cx, cy, base * 1.55f, p)
        p.shader = null

        // Outer precision rings.
        for (i in 0 until 7) {
            val radius = base * (1.22f + i * 0.105f)
            stroke.strokeWidth = if (i == 2 || i == 5) dp(2f) else dp(0.8f)
            stroke.color = Color.argb(
                (55 + stateEnergy * 110f).toInt().coerceIn(0, 255),
                orange.red(), orange.green(), orange.blue()
            )
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val start = coreAngle * (if (i % 2 == 0) 1f else -0.70f) + i * 17f
            c.drawArc(rect, start, if (i % 3 == 0) 245f else 315f, false, stroke)
        }

        // Segmented ring like the reference.
        drawSegmentRing(c, cx, cy, base * 1.34f, 36, 5f, coreAngle)
        drawSegmentRing(c, cx, cy, base * 1.03f, 28, 7f, -counterAngle)

        // Radial engineering marks.
        for (i in 0 until 24) {
            val a = Math.toRadians(i * 15.0 + counterAngle * 0.5)
            val r1 = base * 0.80f
            val r2 = if (i % 3 == 0) base * 1.02f else base * 0.91f
            stroke.strokeWidth = dp(if (i % 3 == 0) 2f else 0.8f)
            stroke.color = Color.argb(115, orange.red(), orange.green(), orange.blue())
            c.drawLine(
                cx + cos(a).toFloat() * r1,
                cy + sin(a).toFloat() * r1,
                cx + cos(a).toFloat() * r2,
                cy + sin(a).toFloat() * r2,
                stroke
            )
        }

        // Inner lattice.
        for (i in 0 until 5) {
            val radius = base * (0.35f + i * 0.12f)
            stroke.strokeWidth = dp(if (i == 2) 2.3f else 1f)
            stroke.color = Color.argb(
                (90 + stateEnergy * 115f).toInt().coerceIn(0, 255),
                brightOrange.red(), brightOrange.green(), brightOrange.blue()
            )
            c.drawCircle(cx, cy, radius, stroke)
        }

        // Four energy emitters.
        for (i in 0 until 4) {
            val a = Math.toRadians(i * 90.0 + coreAngle * 0.42)
            val r = base * 0.58f
            val ex = cx + cos(a).toFloat() * r
            val ey = cy + sin(a).toFloat() * r
            fill.color = Color.argb(
                (150 + stateEnergy * 105f).toInt().coerceIn(0, 255),
                255, 153, 0
            )
            c.drawCircle(ex, ey, base * 0.045f, fill)
            stroke.strokeWidth = dp(1.2f)
            stroke.color = amber
            c.drawLine(
                cx + cos(a).toFloat() * base * 0.31f,
                cy + sin(a).toFloat() * base * 0.31f,
                ex,
                ey,
                stroke
            )
        }

        // Core glow. Size remains fixed; only intensity changes.
        val coreGlow = RadialGradient(
            cx, cy, base * 0.56f,
            intArrayOf(
                Color.WHITE,
                Color.argb((245 * stateEnergy).toInt(), 255, 190, 55),
                Color.argb((185 * stateEnergy).toInt(), 255, 90, 0),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.14f, 0.46f, 1f),
            Shader.TileMode.CLAMP
        )
        p.shader = coreGlow
        c.drawCircle(cx, cy, base * 0.56f, p)
        p.shader = null

        stroke.strokeWidth = dp(2.4f)
        stroke.color = Color.argb(230, brightOrange.red(), brightOrange.green(), brightOrange.blue())
        c.drawCircle(cx, cy, base * 0.29f, stroke)

        fill.color = Color.WHITE
        c.drawCircle(cx, cy, base * 0.075f, fill)

        // Axis crosshair.
        stroke.strokeWidth = dp(1f)
        stroke.color = Color.argb(170, amber.red(), amber.green(), amber.blue())
        c.drawLine(cx - base * 1.44f, cy, cx + base * 1.44f, cy, stroke)
        c.drawLine(cx, cy - base * 1.44f, cx, cy + base * 1.44f, stroke)

        // Tiny mechanical labels around the reactor.
        drawText(c, "CORE // 01", cx - dp(42f), cy - base * 1.52f, 8f, dim, false)
        drawText(c, "REACTOR", cx - dp(30f), cy + base * 1.52f, 8f, dim, false)
    }

    private fun drawSegmentRing(
        c: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        count: Int,
        gap: Float,
        angle: Float
    ) {
        val sweep = (360f / count) - gap
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        stroke.strokeWidth = dp(4f)
        for (i in 0 until count) {
            val alpha = if (i % 4 == 0) 225 else 125
            stroke.color = Color.argb(alpha, orange.red(), orange.green(), orange.blue())
            c.drawArc(rect, angle + i * 360f / count, sweep, false, stroke)
        }
    }

    private fun drawGestureCard(c: Canvas) {
        val l = dp(27f)
        val t = dp(222f)
        val r = dp(195f)
        val b = dp(360f)

        drawAngularPanel(c, l, t, r, b, orange, 1.15f, panel)

        drawText(c, "GESTURE", l + dp(18f), t + dp(31f), 11.5f, white, false)
        drawText(c, "CONTROL", l + dp(18f), t + dp(51f), 11.5f, white, false)

        // Code-drawn hand sign, not an image.
        stroke.strokeWidth = dp(2.2f)
        stroke.color = orange
        val hx = l + dp(85f)
        val hy = t + dp(108f)

        // Palm.
        val palm = Path()
        palm.moveTo(hx - dp(23f), hy + dp(10f))
        palm.lineTo(hx - dp(27f), hy - dp(16f))
        palm.quadTo(hx - dp(28f), hy - dp(25f), hx - dp(21f), hy - dp(27f))
        palm.quadTo(hx - dp(15f), hy - dp(28f), hx - dp(13f), hy - dp(20f))
        palm.lineTo(hx - dp(10f), hy - dp(45f))
        palm.quadTo(hx - dp(9f), hy - dp(52f), hx - dp(3f), hy - dp(52f))
        palm.quadTo(hx + dp(4f), hy - dp(52f), hx + dp(4f), hy - dp(44f))
        palm.lineTo(hx + dp(5f), hy - dp(22f))
        palm.lineTo(hx + dp(7f), hy - dp(60f))
        palm.quadTo(hx + dp(8f), hy - dp(67f), hx + dp(14f), hy - dp(66f))
        palm.quadTo(hx + dp(21f), hy - dp(65f), hx + dp(21f), hy - dp(57f))
        palm.lineTo(hx + dp(21f), hy - dp(23f))
        palm.lineTo(hx + dp(24f), hy - dp(51f))
        palm.quadTo(hx + dp(25f), hy - dp(58f), hx + dp(31f), hy - dp(56f))
        palm.quadTo(hx + dp(37f), hy - dp(54f), hx + dp(35f), hy - dp(46f))
        palm.lineTo(hx + dp(32f), hy - dp(12f))
        palm.quadTo(hx + dp(30f), hy + dp(10f), hx + dp(12f), hy + dp(16f))
        palm.lineTo(hx - dp(8f), hy + dp(17f))
        palm.close()
        c.drawPath(palm, stroke)
    }

    private fun drawVoiceMonitor(c: Canvas) {
        val l = width * 0.285f
        val r = width * 0.715f
        val t = height - dp(375f)
        val b = height - dp(272f)

        drawAngularPanel(c, l, t, r, b, orange, 1.25f, Color.argb(210, 4, 7, 10))

        val bars = 29
        val left = l + dp(37f)
        val usable = r - l - dp(74f)
        val centerY = t + dp(60f)

        for (i in 0 until bars) {
            val distance = abs(i - bars / 2)
            val shape = 0.20f + 0.80f * (1f - distance / (bars / 2f))
            val h = dp(6f) + dp(24f) * shape * (0.25f + smoothVoice * 0.95f)

            stroke.strokeWidth = dp(2f)
            stroke.color = Color.argb(
                (70 + smoothVoice * 185f).toInt().coerceIn(0, 255),
                orange.red(), orange.green(), orange.blue()
            )

            val x = left + usable * i / (bars - 1f)
            c.drawLine(x, centerY - h, x, centerY + h, stroke)
        }

        // Center marker.
        stroke.strokeWidth = dp(1f)
        stroke.color = Color.argb(170, amber.red(), amber.green(), amber.blue())
        c.drawLine(l + dp(17f), centerY, l + dp(28f), centerY, stroke)
        c.drawLine(r - dp(28f), centerY, r - dp(17f), centerY, stroke)

        val state = when {
            speaking -> "SPEAKING"
            processing -> "PROCESSING"
            voiceActive -> "LISTENING"
            else -> "VOICE READY"
        }
        drawCenteredText(c, state, width / 2f, b - dp(12f), 9f, white, false)
    }

    private fun drawHealthCard(c: Canvas) {
        val l = dp(27f)
        val r = width * 0.49f
        val t = height - dp(245f)
        val b = height - dp(43f)

        drawAngularPanel(c, l, t, r, b, orange, 1.15f, panel)

        drawText(c, "SYSTEM HEALTH", l + dp(20f), t + dp(34f), 13f, softWhite, false)

        val cx = l + dp(83f)
        val cy = t + dp(112f)

        stroke.strokeWidth = dp(2.2f)
        stroke.color = orange
        c.drawCircle(cx, cy, dp(49f), stroke)
        stroke.strokeWidth = dp(0.8f)
        stroke.color = amber
        c.drawCircle(cx, cy, dp(57f), stroke)

        drawCenteredText(c, "98%", cx, cy + dp(8f), 21f, orange, false)

        val labels = arrayOf("CPU", "RAM", "STORAGE")
        val values = floatArrayOf(0.78f, 0.68f, 0.51f)

        labels.forEachIndexed { i, label ->
            val yy = t + dp(76f + i * 39f)
            drawText(c, label, l + dp(154f), yy, 10f, softWhite, false)

            stroke.strokeWidth = dp(7f)
            stroke.color = Color.argb(70, orange.red(), orange.green(), orange.blue())
            val x1 = l + dp(204f)
            val x2 = r - dp(20f)
            c.drawLine(x1, yy - dp(4f), x2, yy - dp(4f), stroke)

            stroke.color = orange
            c.drawLine(x1, yy - dp(4f), x1 + (x2 - x1) * values[i], yy - dp(4f), stroke)
        }
    }

    private fun drawQuickAccess(c: Canvas) {
        val l = width * 0.51f
        val r = width - dp(27f)
        val t = height - dp(245f)
        val b = height - dp(43f)

        drawAngularPanel(c, l, t, r, b, orange, 1.15f, panel)

        drawText(c, "QUICK ACCESS", l + dp(20f), t + dp(34f), 13f, softWhite, false)

        val labels = arrayOf("CHAT", "APPS", "FILES")
        val cellW = (r - l - dp(52f)) / 3f

        labels.forEachIndexed { i, label ->
            val cl = l + dp(16f) + i * (cellW + dp(10f))
            val cr = cl + cellW
            val ct = t + dp(54f)
            val cb = b - dp(18f)

            drawAngularPanel(c, cl, ct, cr, cb, orange, 0.85f, Color.argb(120, 4, 7, 10))

            val cx = (cl + cr) / 2f
            drawReferenceIcon(c, i, cx, ct + dp(49f), dp(24f), orange)
            drawCenteredText(c, label, cx, cb - dp(15f), 9.5f, white, false)
        }
    }

    private fun drawRightMenu(c: Canvas) {
        val l = width - dp(248f)
        val t = dp(226f)
        val r = width - dp(28f)
        val b = dp(846f)

        drawAngularPanel(c, l, t, r, b, Color.argb(230, 255, 145, 0), 1.0f, panelStrong)

        val items = arrayOf(
            "Voice", "Chat", "Apps", "Files", "Documents", "Memory",
            "AI Provider/API", "Security", "Permissions", "Authorized Folder", "Settings"
        )

        items.forEachIndexed { i, label ->
            val yy = t + dp(39f + i * 53.5f)
            drawReferenceIcon(c, i, l + dp(31f), yy - dp(5f), dp(21f), orange)
            drawText(c, label, l + dp(58f), yy, 13.5f, white, false)
        }
    }

    private fun drawSecondaryPage(c: Canvas) {
        drawBackground(c)
        drawTopIdentity(c)

        drawText(c, "‹", dp(27f), dp(78f), 38f, white, false)

        val title = when (page) {
            Page.CHAT -> "CHAT"
            Page.APPS -> "APPS"
            Page.FILES -> "FILES"
            Page.DOCUMENTS -> "DOCUMENTS"
            Page.MEMORY -> "MEMORY"
            Page.PROVIDER -> "AI PROVIDER / API"
            Page.SECURITY -> "SECURITY"
            Page.PERMISSIONS -> "PERMISSIONS"
            Page.AUTHORIZED -> "AUTHORIZED FOLDER"
            Page.SETTINGS -> "SETTINGS"
            else -> "GOD"
        }

        drawAngularPanel(
            c, dp(28f), dp(120f), width - dp(28f),
            height - dp(40f), orange, 1.25f, panel
        )
        drawText(c, title, dp(53f), dp(164f), 17f, white, true)

        when (page) {
            Page.CHAT -> drawChat(c)
            Page.APPS -> drawApps(c)
            Page.FILES -> drawFiles(c)
            Page.DOCUMENTS -> drawDocuments(c)
            Page.MEMORY -> drawMemory(c)
            Page.PROVIDER -> drawProvider(c)
            Page.SECURITY -> drawSecurity(c)
            Page.PERMISSIONS -> drawPermissions(c)
            Page.AUTHORIZED -> drawAuthorized(c)
            Page.SETTINGS -> drawSettings(c)
            else -> Unit
        }
    }

    private fun drawChat(c: Canvas) {
        drawText(c, "GOD // COMMUNICATION CHANNEL", dp(53f), dp(215f), 10f, amber, false)
        drawAngularPanel(c, dp(48f), dp(238f), width - dp(48f), height - dp(135f),
            Color.argb(160, 3, 6, 9), 0.9f)
        drawText(c, "READY", dp(68f), dp(275f), 11f, orange, false)
        drawText(c, "VOICE OR TEXT INPUT", dp(68f), height - dp(175f), 12f, white, false)
        drawText(c, "GOD IS WAITING.", dp(68f), height - dp(135f), 13f, softWhite, false)
    }

    private fun drawApps(c: Canvas) {
        drawText(c, "APPLICATION GRID", dp(53f), dp(215f), 10f, amber, false)
        val names = arrayOf("SYSTEM", "PHONE", "MESSAGES", "BROWSER", "CAMERA", "MEDIA")
        names.forEachIndexed { i, name ->
            val col = i % 2
            val row = i / 2
            val l = dp(50f) + col * (width / 2f - dp(34f))
            val t = dp(245f) + row * dp(74f)
            drawAngularPanel(c, l, t, l + width / 2f - dp(48f), t + dp(54f),
                orange, 0.85f, Color.argb(120, 5, 8, 12))
            drawReferenceIcon(c, i + 2, l + dp(30f), t + dp(27f), dp(20f), orange)
            drawText(c, name, l + dp(55f), t + dp(32f), 11f, white, false)
        }
    }

    private fun drawFiles(c: Canvas) {
        drawText(c, "STORAGE CONTROL", dp(53f), dp(215f), 10f, amber, false)
        drawText(c, "AUTHORIZED FOLDER", dp(53f), dp(260f), 14f, white, false)
        drawText(c, "BROWSE FILES", dp(53f), dp(313f), 13f, softWhite, false)
        drawText(c, "DOCUMENTS", dp(53f), dp(366f), 13f, softWhite, false)
    }

    private fun drawDocuments(c: Canvas) {
        drawText(c, "DOCUMENT INDEX", dp(53f), dp(215f), 10f, amber, false)
        drawText(c, "PDF", dp(53f), dp(260f), 13f, white, false)
        drawText(c, "TEXT", dp(53f), dp(310f), 13f, white, false)
        drawText(c, "SUMMARIZE / SEARCH / ASK", dp(53f), dp(360f), 12f, softWhite, false)
    }

    private fun drawMemory(c: Canvas) {
        drawText(c, "MEMORY CORE", dp(53f), dp(215f), 10f, amber, false)
        drawText(c, "USER-APPROVED DATA", dp(53f), dp(260f), 14f, white, false)
        drawText(c, "STORE", dp(53f), dp(315f), 12f, softWhite, false)
        drawText(c, "RETRIEVE", dp(53f), dp(365f), 12f, softWhite, false)
        drawText(c, "DELETE", dp(53f), dp(415f), 12f, softWhite, false)
    }

    private fun drawProvider(c: Canvas) {
        drawText(c, "AI CONNECTION", dp(53f), dp(215f), 10f, amber, false)
        drawText(c, "PROVIDER", dp(53f), dp(265f), 12f, dim, false)
        drawText(c, "MODEL", dp(53f), dp(320f), 12f, dim, false)
        drawText(c, "CREDENTIALS STORED OUTSIDE UI", dp(53f), dp(375f), 12f, white, false)
        drawText(c, "WEB RESEARCH / RELEVANCE / SUMMARY", dp(53f), dp(430f), 11f, softWhite, false)
    }

    private fun drawSecurity(c: Canvas) {
        drawText(c, "SECURITY MATRIX", dp(53f), dp(215f), 10f, amber, false)
        drawText(c, "APP LOCK", dp(53f), dp(265f), 14f, white, false)
        drawText(c, "SESSION PROTECTION", dp(53f), dp(315f), 13f, softWhite, false)
        drawText(c, "SECURE", width - dp(130f), dp(265f), 11f, orange, false)
    }

    private fun drawPermissions(c: Canvas) {
        drawText(c, "PERMISSION MATRIX", dp(53f), dp(215f), 10f, amber, false)
        arrayOf("MICROPHONE", "CAMERA", "NOTIFICATIONS", "STORAGE").forEachIndexed { i, s ->
            val yy = dp(270f + i * 55f)
            drawText(c, s, dp(53f), yy, 13f, white, false)
            drawText(c, "CHECK", width - dp(125f), yy, 10f, orange, false)
        }
    }

    private fun drawAuthorized(c: Canvas) {
        drawText(c, "STORAGE AUTHORIZATION", dp(53f), dp(215f), 10f, amber, false)
        drawText(c, "AUTHORIZED FOLDER", dp(53f), dp(270f), 14f, white, false)
        drawText(c, "ANDROID SYSTEM ACCESS", dp(53f), dp(320f), 12f, softWhite, false)
        drawText(c, "PERSISTED ACCESS", dp(53f), dp(370f), 12f, orange, false)
    }

    private fun drawSettings(c: Canvas) {
        drawText(c, "SYSTEM CONFIGURATION", dp(53f), dp(215f), 10f, amber, false)
        arrayOf(
            "CORE ANIMATION", "VOICE / TTS", "AI PROVIDER",
            "MEMORY", "SECURITY", "APPEARANCE"
        ).forEachIndexed { i, s ->
            drawText(c, s, dp(53f), dp(270f + i * 50f), 13f, white, false)
        }
    }

    private fun drawNotice(c: Canvas) {
        val l = dp(55f)
        val r = width - dp(55f)
        val t = dp(205f)
        val b = t + dp(87f)
        drawAngularPanel(c, l, t, r, b, orange, 1.2f, panelStrong)
        drawText(c, noticeTitle, l + dp(18f), t + dp(30f), 10f, amber, true)
        drawText(c, noticeBody, l + dp(18f), t + dp(58f), 12f, white, false)
    }

    private fun drawAngularPanel(
        c: Canvas,
        l: Float,
        t: Float,
        r: Float,
        b: Float,
        borderColor: Int,
        thickness: Float,
        fillColor: Int
    ) {
        fill.color = fillColor
        c.drawRect(l, t, r, b, fill)

        stroke.strokeWidth = dp(thickness)
        stroke.color = borderColor

        val cut = dp(13f)
        val path = Path()
        path.moveTo(l + cut, t)
        path.lineTo(r - cut, t)
        path.lineTo(r, t + cut)
        path.lineTo(r, b - cut)
        path.lineTo(r - cut, b)
        path.lineTo(l + cut, b)
        path.lineTo(l, b - cut)
        path.lineTo(l, t + cut)
        path.close()
        c.drawPath(path, stroke)

        // Reference-like inner highlight line.
        stroke.strokeWidth = dp(0.55f)
        stroke.color = Color.argb(
            70, borderColor.red(), borderColor.green(), borderColor.blue()
        )
        c.drawLine(l + cut, t + dp(5f), r - cut, t + dp(5f), stroke)
    }

    private fun drawStatusDot(c: Canvas, x: Float, y: Float, color: Int) {
        p.color = color
        p.setShadowLayer(dp(7f), 0f, 0f, color)
        c.drawCircle(x, y, dp(5f), p)
        p.clearShadowLayer()
    }

    /**
     * Small line-art icons deliberately use the same thin amber geometry
     * instead of Android/Material icon assets.
     */
    private fun drawReferenceIcon(
        c: Canvas,
        index: Int,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int
    ) {
        stroke.strokeWidth = dp(1.8f)
        stroke.color = color

        when (index) {
            0 -> { // Chat
                val rect = RectF(cx - size * .55f, cy - size * .40f, cx + size * .55f, cy + size * .35f)
                c.drawRoundRect(rect, size * .16f, size * .16f, stroke)
                val tail = Path()
                tail.moveTo(cx - size * .15f, cy + size * .35f)
                tail.lineTo(cx - size * .32f, cy + size * .62f)
                tail.lineTo(cx + size * .05f, cy + size * .35f)
                c.drawPath(tail, stroke)
            }
            1 -> { // Apps
                for (dx in -1..1) for (dy in -1..1) {
                    val rr = RectF(
                        cx + dx * size * .48f - size * .16f,
                        cy + dy * size * .48f - size * .16f,
                        cx + dx * size * .48f + size * .16f,
                        cy + dy * size * .48f + size * .16f
                    )
                    c.drawRoundRect(rr, dp(2f), dp(2f), stroke)
                }
            }
            2 -> { // Folder
                val path = Path()
                path.moveTo(cx - size * .60f, cy - size * .35f)
                path.lineTo(cx - size * .15f, cy - size * .35f)
                path.lineTo(cx + size * .02f, cy - size * .18f)
                path.lineTo(cx + size * .60f, cy - size * .18f)
                path.lineTo(cx + size * .55f, cy + size * .43f)
                path.lineTo(cx - size * .60f, cy + size * .43f)
                path.close()
                c.drawPath(path, stroke)
            }
            3 -> { // Documents
                val rect = RectF(cx - size * .42f, cy - size * .58f, cx + size * .42f, cy + size * .58f)
                c.drawRect(rect, stroke)
                c.drawLine(cx - size * .23f, cy - size * .18f, cx + size * .25f, cy - size * .18f, stroke)
                c.drawLine(cx - size * .23f, cy + size * .08f, cx + size * .25f, cy + size * .08f, stroke)
            }
            4 -> { // Memory / neural
                c.drawCircle(cx, cy, size * .42f, stroke)
                c.drawCircle(cx - size * .36f, cy - size * .18f, size * .12f, stroke)
                c.drawCircle(cx + size * .36f, cy - size * .18f, size * .12f, stroke)
                c.drawLine(cx - size * .25f, cy - size * .05f, cx - size * .12f, cy - size * .15f, stroke)
                c.drawLine(cx + size * .25f, cy - size * .05f, cx + size * .12f, cy - size * .15f, stroke)
            }
            5 -> { // Cloud
                val path = Path()
                path.moveTo(cx - size * .55f, cy + size * .20f)
                path.quadTo(cx - size * .60f, cy - size * .22f, cx - size * .22f, cy - size * .22f)
                path.quadTo(cx - size * .08f, cy - size * .60f, cx + size * .28f, cy - size * .36f)
                path.quadTo(cx + size * .63f, cy - size * .35f, cx + size * .58f, cy + size * .20f)
                path.close()
                c.drawPath(path, stroke)
            }
            6 -> { // Shield
                val path = Path()
                path.moveTo(cx, cy - size * .60f)
                path.lineTo(cx + size * .48f, cy - size * .38f)
                path.lineTo(cx + size * .38f, cy + size * .35f)
                path.lineTo(cx, cy + size * .60f)
                path.lineTo(cx - size * .38f, cy + size * .35f)
                path.lineTo(cx - size * .48f, cy - size * .38f)
                path.close()
                c.drawPath(path, stroke)
            }
            7 -> { // Lock
                val rr = RectF(cx - size * .43f, cy - size * .10f, cx + size * .43f, cy + size * .48f)
                c.drawRect(rr, stroke)
                val arc = RectF(cx - size * .30f, cy - size * .55f, cx + size * .30f, cy + size * .20f)
                c.drawArc(arc, 180f, 180f, false, stroke)
            }
            8 -> { // Permissions / person
                c.drawCircle(cx, cy - size * .30f, size * .18f, stroke)
                val path = Path()
                path.moveTo(cx - size * .46f, cy + size * .45f)
                path.quadTo(cx, cy + size * .02f, cx + size * .46f, cy + size * .45f)
                c.drawPath(path, stroke)
            }
            9 -> { // Authorized folder
                val path = Path()
                path.moveTo(cx - size * .60f, cy - size * .35f)
                path.lineTo(cx - size * .15f, cy - size * .35f)
                path.lineTo(cx + size * .02f, cy - size * .18f)
                path.lineTo(cx + size * .60f, cy - size * .18f)
                path.lineTo(cx + size * .55f, cy + size * .43f)
                path.lineTo(cx - size * .60f, cy + size * .43f)
                path.close()
                c.drawPath(path, stroke)
            }
            else -> { // Settings / gear
                c.drawCircle(cx, cy, size * .35f, stroke)
                c.drawCircle(cx, cy, size * .12f, stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    c.drawLine(
                        cx + cos(a).toFloat() * size * .40f,
                        cy + sin(a).toFloat() * size * .40f,
                        cx + cos(a).toFloat() * size * .60f,
                        cy + sin(a).toFloat() * size * .60f,
                        stroke
                    )
                }
            }
        }
    }

    private fun drawText(
        c: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean
    ) {
        text.textSize = dp(size)
        text.color = color
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
        c.drawText(value, x, y, text)
    }

    private fun drawCenteredText(
        c: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean
    ) {
        text.textSize = dp(size)
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
        drawText(c, value, x - text.measureText(value) / 2f, y, size, color, bold)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true

        val x = event.x
        val y = event.y

        if (page != Page.HOME) {
            if (x < dp(90f) && y < dp(105f)) {
                page = Page.HOME
                menuVisible = true
                onAction?.invoke(ACTION_BACK)
                invalidate()
            }
            return true
        }

        // Entire center reactor is touchable. There is no visible mic button.
        val cx = width * 0.50f
        val cy = height * 0.485f
        val radius = min(width, height) * 0.27f
        if ((x - cx).pow(2) + (y - cy).pow(2) <= radius.pow(2)) {
            performClick()
            onAction?.invoke(ACTION_VOICE)
            showNotice("GOD", "VOICE CHANNEL ACTIVE")
            return true
        }

        // Right menu.
        if (menuVisible && x >= width - dp(258f) && y >= dp(215f) && y <= dp(865f)) {
            val index = ((y - dp(226f)) / dp(53.5f)).toInt()
            selectMenu(index)
            return true
        }

        // Gesture HUD.
        if (x <= dp(210f) && y >= dp(215f) && y <= dp(370f)) {
            onAction?.invoke(ACTION_GESTURE)
            showNotice("GESTURE CONTROL", "CONTROL READY")
            return true
        }

        // Quick access.
        if (y >= height - dp(250f)) {
            val center = width * 0.51f
            if (x >= center) {
                val third = (width - center - dp(27f)) / 3f
                val index = ((x - center) / third).toInt()
                when (index) {
                    0 -> selectPage(Page.CHAT, ACTION_CHAT)
                    1 -> selectPage(Page.APPS, ACTION_APPS)
                    else -> selectPage(Page.FILES, ACTION_FILES)
                }
            } else {
                selectPage(Page.CHAT, ACTION_CHAT)
            }
            return true
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun selectMenu(index: Int) {
        when (index) {
            0 -> {
                onAction?.invoke(ACTION_VOICE)
                showNotice("GOD", "VOICE CHANNEL ACTIVE")
            }
            1 -> selectPage(Page.CHAT, ACTION_CHAT)
            2 -> selectPage(Page.APPS, ACTION_APPS)
            3 -> selectPage(Page.FILES, ACTION_FILES)
            4 -> selectPage(Page.DOCUMENTS, ACTION_DOCUMENTS)
            5 -> selectPage(Page.MEMORY, ACTION_MEMORY)
            6 -> selectPage(Page.PROVIDER, ACTION_PROVIDER)
            7 -> selectPage(Page.SECURITY, ACTION_SECURITY)
            8 -> selectPage(Page.PERMISSIONS, ACTION_PERMISSIONS)
            9 -> selectPage(Page.AUTHORIZED, ACTION_AUTHORIZED)
            10 -> selectPage(Page.SETTINGS, ACTION_SETTINGS)
        }
    }

    private fun selectPage(target: Page, action: String) {
        page = target
        menuVisible = false
        onAction?.invoke(action)
        invalidate()
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
