package com.manette.app.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.manette.app.util.HapticManager

/**
 * Invisible-at-rest overlay button view that renders a pixel-exact 3D sunken ("enfoncé")
 * depression effect directly over the button's exact pixels from manette.jpg ONLY when pressed.
 */
class GamepadButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    enum class Style {
        ACTION_CIRCLE,
        PILL_CENTER,
        SHOULDER_L,
        SHOULDER_R
    }

    var buttonLabel: String = "A"
    var buttonStyle: Style = Style.ACTION_CIRCLE
    var onButtonStateChanged: ((pressed: Boolean) -> Unit)? = null

    private var pressDepth: Float = 0f
    private var isButtonPressed: Boolean = false
    private var depthAnimator: ValueAnimator? = null

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dstRect = Rect()

    fun isCurrentlyPressed(): Boolean = isButtonPressed

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 100% invisible at rest so the authentic photorealistic button on manette.jpg is shown untouched
        if (pressDepth <= 0.02f) return

        val bounds = ManettePixelEffect.parseBounds(tag as? String) ?: return
        val effectBmp = ManettePixelEffect.getButtonEffectBitmap(
            context = context,
            key = buttonLabel,
            x0 = bounds[0],
            y0 = bounds[1],
            x1 = bounds[2],
            y1 = bounds[3],
            style = buttonStyle
        )

        dstRect.set(0, 0, width, height)
        bmpPaint.alpha = (pressDepth * 255f).toInt().coerceIn(0, 255)
        canvas.drawBitmap(effectBmp, null, dstRect, bmpPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isButtonPressed = true
                if (HapticManager.isHapticEnabled(context)) {
                    HapticManager.vibrateButtonPress(context)
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                animateDepthTo(1f, 40L)
                onButtonStateChanged?.invoke(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isButtonPressed) {
                    isButtonPressed = false
                    if (HapticManager.isHapticEnabled(context)) {
                        HapticManager.vibrateButtonRelease(context)
                    }
                    animateDepthTo(0f, 100L)
                    onButtonStateChanged?.invoke(false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateDepthTo(target: Float, durationMs: Long) {
        depthAnimator?.cancel()
        if (target == 1f || !isAttachedToWindow) {
            pressDepth = target
            invalidate()
            return
        }
        val start = pressDepth
        depthAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = durationMs
            interpolator = if (target == 0f) OvershootInterpolator(1.2f) else DecelerateInterpolator()
            addUpdateListener { anim ->
                pressDepth = (anim.animatedValue as Float).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        depthAnimator?.cancel()
    }
}
