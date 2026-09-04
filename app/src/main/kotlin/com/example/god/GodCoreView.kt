package com.example.god

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class GodCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animationTime = 0f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND

        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeCap = Paint.Cap.ROUND

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        /*
         * FUTURISTIC DARK BACKGROUND
         */
        backgroundPaint.color = Color.rgb(3, 7, 14)
        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            backgroundPaint
        )

        /*
         * BLUE FUTURISTIC ANGULAR LINES
         */
        drawBackgroundLines(
            canvas,
            centerX,
            centerY
        )

        /*
         * CIRCULAR GOD CORE SYSTEM
         */
        drawGodCore(
            canvas,
            centerX,
            centerY
        )

        animationTime += 0.025f

        postInvalidateOnAnimation()
    }

    private fun drawBackgroundLines(
        canvas: Canvas,
        centerX: Float,
        centerY: Float
    ) {

        linePaint.color = Color.rgb(25, 130, 255)
        linePaint.alpha = 75
        linePaint.strokeWidth = 2f

        val spacing = 85f

        /*
         * Horizontal technical lines
         */
        var y = 40f

        while (y < height) {

            val offset =
                sin(animationTime * 0.4f + y * 0.01f) * 8f

            canvas.drawLine(
                0f,
                y + offset,
                width.toFloat(),
                y + offset,
                linePaint
            )

            y += spacing
        }

        /*
         * Diagonal technical lines
         */
        for (i in -5..12) {

            val startX =
                i * 100f -
                        (animationTime * 15f % 100f)

            val path = Path()

            path.moveTo(
                startX,
                0f
            )

            path.lineTo(
                startX + 70f,
                height * 0.25f
            )

            path.lineTo(
                startX - 20f,
                height * 0.5f
            )

            path.lineTo(
                startX + 90f,
                height * 0.75f
            )

            path.lineTo(
                startX + 20f,
                height.toFloat()
            )

            canvas.drawPath(
                path,
                linePaint
            )
        }

        /*
         * Small HUD-style markers
         */
        for (i in 0 until 12) {

            val x =
                (i * 137f + animationTime * 10f) %
                        width

            val markerY =
                60f +
                        ((i * 97f) % maxOf(height - 120, 1).toFloat())

            linePaint.alpha = 110
            linePaint.strokeWidth = 2f

            canvas.drawLine(
                x,
                markerY,
                x + 35f,
                markerY,
                linePaint
            )

            canvas.drawLine(
                x + 35f,
                markerY,
                x + 48f,
                markerY - 12f,
                linePaint
            )
        }
    }

    private fun drawGodCore(
        canvas: Canvas,
        centerX: Float,
        centerY: Float
    ) {

        val baseRadius =
            minOf(width, height) * 0.20f

        /*
         * Animated circular energy rings
         */
        for (i in 0 until 6) {

            val pulse =
                sin(
                    animationTime * 2f +
                            i * 0.8f
                )

            val radius =
                baseRadius +
                        i * 22f +
                        pulse * 8f

            ringPaint.color =
                Color.rgb(30, 150, 255)

            ringPaint.alpha =
                (45 + i * 8).coerceAtMost(100)

            ringPaint.strokeWidth =
                if (i == 0) 3f else 2f

            canvas.drawCircle(
                centerX,
                centerY,
                radius,
                ringPaint
            )
        }

        /*
         * Orange energy lines around the core
         */
        linePaint.color =
            Color.rgb(255, 145, 0)

        for (i in 0 until 72) {

            val angle =
                i * Math.PI * 2.0 / 72.0

            val wave =
                sin(
                    animationTime * 3f +
                            i * 0.65f
                )

            val innerRadius =
                baseRadius +
                        wave * 12f

            val outerRadius =
                innerRadius +
                        25f +
                        wave * 18f

            val startX =
                centerX +
                        cos(angle).toFloat() *
                        innerRadius

            val startY =
                centerY +
                        sin(angle).toFloat() *
                        innerRadius

            val endX =
                centerX +
                        cos(angle).toFloat() *
                        outerRadius

            val endY =
                centerY +
                        sin(angle).toFloat() *
                        outerRadius

            linePaint.alpha =
                (90 +
                        wave * 70)
                    .toInt()
                    .coerceIn(25, 160)

            linePaint.strokeWidth =
                1.5f +
                        (wave + 1f) * 0.8f

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                linePaint
            )
        }

        /*
         * Orange glow
         */
        val glowRadius =
            baseRadius *
                    (0.9f +
                            sin(animationTime * 3f) *
                            0.08f)

        corePaint.shader =
            RadialGradient(
                centerX,
                centerY,
                glowRadius * 1.8f,
                intArrayOf(
                    Color.rgb(255, 220, 130),
                    Color.rgb(255, 140, 0),
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.35f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

        canvas.drawCircle(
            centerX,
            centerY,
            glowRadius * 1.8f,
            corePaint
        )

        corePaint.shader = null

        /*
         * Small orange GOD core
         */
        corePaint.color =
            Color.rgb(255, 145, 0)

        canvas.drawCircle(
            centerX,
            centerY,
            baseRadius * 0.16f,
            corePaint
        )

        /*
         * Bright center
         */
        corePaint.color =
            Color.rgb(255, 235, 180)

        canvas.drawCircle(
            centerX,
            centerY,
            baseRadius * 0.06f,
            corePaint
        )
    }
}
