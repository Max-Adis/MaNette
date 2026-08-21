package com.manette.app.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/**
 * Animated water ripple view for the splash screen background.
 * Draws expanding concentric rings that simulate water ripples.
 */
class WaterRippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val ringCount = 5
    private val rings = Array(ringCount) { i -> Ring(i.toFloat() / ringCount) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    data class Ring(val offset: Float, var alpha: Float = 0f, var radius: Float = 0f)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = maxOf(width, height) * 0.8f
        val t = (animator.animatedValue as Float)

        for (i in 0 until ringCount) {
            val progress = (t + i.toFloat() / ringCount) % 1f
            val radius = progress * maxRadius
            val alpha = (1f - progress) * 0.35f

            paint.color = ((alpha * 255).toInt() shl 24) or 0x3B82F6
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }
}
