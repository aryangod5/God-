package com.example.god

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animationTime = 0f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 3f
        linePaint.strokeCap = Paint.Cap.ROUND

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        val radius = minOf(width, height) * 0.28f

        canvas.drawColor(Color.rgb(5, 7, 12))

        /*
         * Animated surrounding lines
         */
        for (i in 0 until 80) {

            val angle =
                (i / 80f) * Math.PI * 2.0

            val wave =
                sin(animationTime * 2.0f + i * 0.45f) * 18f

            val innerRadius =
                radius + wave

            val outerRadius =
                innerRadius + 35f +
                        sin(animationTime * 1.5f + i) * 25f

            val startX =
                centerX + cos(angle).toFloat() * innerRadius

            val startY =
                centerY + sin(angle).toFloat() * innerRadius

            val endX =
                centerX + cos(angle).toFloat() * outerRadius

            val endY =
                centerY + sin(angle).toFloat() * outerRadius

            linePaint.alpha =
                (90 + 80 *
                        (0.5f +
                                0.5f *
                                sin(animationTime * 2f + i)
                        )
                        ).toInt()

            linePaint.strokeWidth =
                2f + (
                        1.5f *
                                (0.5f +
                                        0.5f *
                                        sin(animationTime * 2f + i)
                                )
                        ).toFloat()

            linePaint.shader = RadialGradient(
                centerX,
                centerY,
                radius * 2f,
                Color.rgb(255, 180, 40),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                linePaint
            )

            linePaint.shader = null
        }

        /*
         * Outer circular glow
         */
        val glowRadius =
            radius * (
                    1f +
                            0.08f *
                            sin(animationTime * 3f)
                    )

        corePaint.shader = RadialGradient(
            centerX,
            centerY,
            glowRadius * 1.7f,
            intArrayOf(
                Color.rgb(255, 190, 60),
                Color.rgb(255, 120, 0),
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
            glowRadius * 1.7f,
            corePaint
        )

        corePaint.shader = null

        /*
         * Small orange GOD core
         */
        corePaint.color = Color.rgb(255, 145, 0)

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 0.16f,
            corePaint
        )

        /*
         * Bright center
         */
        corePaint.color = Color.rgb(255, 220, 130)

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 0.07f,
            corePaint
        )

        animationTime += 0.035f

        postInvalidateOnAnimation()
    }
}
