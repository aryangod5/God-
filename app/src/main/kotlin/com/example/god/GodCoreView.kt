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
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animationTime = 0f

    private val orange = Color.rgb(255, 145, 0)
    private val brightOrange = Color.rgb(255, 205, 100)
    private val blue = Color.rgb(40, 130, 255)
    private val background = Color.rgb(2, 5, 10)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND

        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeCap = Paint.Cap.ROUND

        detailPaint.style = Paint.Style.STROKE
        detailPaint.strokeCap = Paint.Cap.ROUND

        isClickable = true

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        backgroundPaint.color = background

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            backgroundPaint
        )

        drawTechEnvironment(canvas)
        drawHudElements(canvas, centerX, centerY)
        drawGodCore(canvas, centerX, centerY)

        animationTime += 0.025f
        postInvalidateOnAnimation()
    }

    private fun drawTechEnvironment(canvas: Canvas) {

        linePaint.color = orange
        linePaint.strokeWidth = 1.5f
        linePaint.alpha = 45

        val horizontalSpacing = 95f

        var y = 20f

        while (y < height) {

            val movement =
                sin(animationTime * 0.5f + y * 0.01f) * 5f

            canvas.drawLine(
                0f,
                y + movement,
                width.toFloat(),
                y + movement,
                linePaint
            )

            y += horizontalSpacing
        }

        for (i in -6..12) {

            val startX =
                i * 115f -
                        (animationTime * 12f % 115f)

            val path = Path()

            path.moveTo(startX, 0f)

            path.lineTo(
                startX + 65f,
                height * 0.25f
            )

            path.lineTo(
                startX - 25f,
                height * 0.5f
            )

            path.lineTo(
                startX + 80f,
                height * 0.75f
            )

            path.lineTo(
                startX + 15f,
                height.toFloat()
            )

            canvas.drawPath(path, linePaint)
        }

        detailPaint.color = orange

        for (i in 0 until 14) {

            val x =
                (i * 151f +
                        animationTime * 15f) %
                        width.coerceAtLeast(1)

            val yPosition =
                50f +
                        ((i * 83f) %
                                height.coerceAtLeast(100))

            detailPaint.alpha = 75

            canvas.drawLine(
                x,
                yPosition,
                x + 32f,
                yPosition,
                detailPaint
            )

            canvas.drawLine(
                x + 32f,
                yPosition,
                x + 44f,
                yPosition - 10f,
                detailPaint
            )
        }
    }

    private fun drawHudElements(
        canvas: Canvas,
        centerX: Float,
        centerY: Float
    ) {

        val baseRadius =
            minOf(width, height) * 0.19f

        for (i in 0 until 7) {

            val pulse =
                sin(
                    animationTime * 1.8f +
                            i * 0.65f
                )

            val radius =
                baseRadius +
                        i * 23f +
                        pulse * 7f

            ringPaint.color = orange

            ringPaint.alpha =
                (30 + i * 8)
                    .coerceAtMost(90)

            ringPaint.strokeWidth =
                if (i == 0) 3f else 1.5f

            canvas.drawCircle(
                centerX,
                centerY,
                radius,
                ringPaint
            )
        }

        // Small rotating HUD segments

        detailPaint.color = orange

        for (i in 0 until 24) {

            val angle =
                animationTime * 0.25f +
                        i * Math.PI * 2.0 / 24.0

            val inner =
                baseRadius + 155f

            val outer =
                inner + 12f

            val x1 =
                centerX +
                        cos(angle).toFloat() * inner

            val y1 =
                centerY +
                        sin(angle).toFloat() * inner

            val x2 =
                centerX +
                        cos(angle).toFloat() * outer

            val y2 =
                centerY +
                        sin(angle).toFloat() * outer

            detailPaint.alpha =
                if (i % 3 == 0) 130 else 55

            detailPaint.strokeWidth =
                if (i % 3 == 0) 3f else 1.5f

            canvas.drawLine(
                x1,
                y1,
                x2,
                y2,
                detailPaint
            )
        }

        // Minimal blue accent ring

        ringPaint.color = blue
        ringPaint.alpha = 80
        ringPaint.strokeWidth = 1.5f

        canvas.drawCircle(
            centerX,
            centerY,
            baseRadius + 105f,
            ringPaint
        )
    }

    private fun drawGodCore(
        canvas: Canvas,
        centerX: Float,
        centerY: Float
    ) {

        val baseRadius =
            minOf(width, height) * 0.19f

        // Animated energy rays

        linePaint.color = orange

        for (i in 0 until 72) {

            val angle =
                i * Math.PI * 2.0 / 72.0

            val wave =
                sin(
                    animationTime * 3f +
                            i * 0.62f
                )

            val innerRadius =
                baseRadius +
                        wave * 10f

            val outerRadius =
                innerRadius +
                        22f +
                        wave * 17f

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
                (70 + wave * 80)
                    .toInt()
                    .coerceIn(20, 155)

            linePaint.strokeWidth =
                1.5f +
                        (wave + 1f) * 0.7f

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                linePaint
            )
        }

        // Orange atmospheric glow

        val glowRadius =
            baseRadius *
                    (0.9f +
                            sin(animationTime * 3f) *
                            0.08f)

        corePaint.shader =
            RadialGradient(
                centerX,
                centerY,
                glowRadius * 2.2f,
                intArrayOf(
                    brightOrange,
                    orange,
                    Color.argb(80, 255, 100, 0),
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.25f,
                    0.55f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

        canvas.drawCircle(
            centerX,
            centerY,
            glowRadius * 2.2f,
            corePaint
        )

        corePaint.shader = null

        // Main GOD energy sphere

        corePaint.shader =
            RadialGradient(
                centerX - baseRadius * 0.25f,
                centerY - baseRadius * 0.25f,
                baseRadius,
                intArrayOf(
                    Color.rgb(255, 245, 200),
                    orange,
                    Color.rgb(190, 65, 0)
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
            baseRadius * 0.18f,
            corePaint
        )

        corePaint.shader = null

        // Bright center

        corePaint.color = brightOrange

        canvas.drawCircle(
            centerX,
            centerY,
            baseRadius * 0.065f,
            corePaint
        )
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
