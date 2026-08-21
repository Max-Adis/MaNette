package com.manette.app.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/**
 * Animated water wave view for the home screen bottom.
 * Draws layered sine waves to create a fluid water effect.
 */
class WaterWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val wavePath1 = Path()
    private val wavePath2 = Path()
    private val wavePath3 = Path()

    private val wavePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val wavePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val wavePaint3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var offset1 = 0f
    private var offset2 = 0f
    private var offset3 = 0f

    private val animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 4000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            val v = anim.animatedValue as Float
            offset1 = v
            offset2 = v * 0.8f
            offset3 = v * 1.2f
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradients(w, h)
    }

    private fun updateGradients(w: Int, h: Int) {
        wavePaint1.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x2B2563EB.toInt(), 0x661D4ED8.toInt()),
            null, Shader.TileMode.CLAMP
        )
        wavePaint2.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x203B82F6.toInt(), 0x4B1D4ED8.toInt()),
            null, Shader.TileMode.CLAMP
        )
        wavePaint3.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x1593C5FD.toInt(), 0x331D4ED8.toInt()),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val amplitude = h * 0.18f
        val baseY = h * 0.45f

        // Wave 3 (back layer)
        wavePath3.reset()
        wavePath3.moveTo(0f, h)
        for (x in 0..width step 4) {
            val y = baseY + amplitude * 0.8f * sin(offset3 + x * 0.022f)
            wavePath3.lineTo(x.toFloat(), y)
        }
        wavePath3.lineTo(w, h)
        wavePath3.close()
        canvas.drawPath(wavePath3, wavePaint3)

        // Wave 2 (mid layer)
        wavePath2.reset()
        wavePath2.moveTo(0f, h)
        for (x in 0..width step 4) {
            val y = baseY * 1.05f + amplitude * sin(offset2 + x * 0.018f + 1.2f)
            wavePath2.lineTo(x.toFloat(), y)
        }
        wavePath2.lineTo(w, h)
        wavePath2.close()
        canvas.drawPath(wavePath2, wavePaint2)

        // Wave 1 (front layer)
        wavePath1.reset()
        wavePath1.moveTo(0f, h)
        for (x in 0..width step 4) {
            val y = baseY * 1.1f + amplitude * 1.1f * sin(offset1 + x * 0.015f + 2.4f)
            wavePath1.lineTo(x.toFloat(), y)
        }
        wavePath1.lineTo(w, h)
        wavePath1.close()
        canvas.drawPath(wavePath1, wavePaint1)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
