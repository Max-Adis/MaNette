package com.manette.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.manette.app.hid.DPadDirection
import com.manette.app.util.HapticManager
import kotlin.math.hypot
import kotlin.math.min

/**
 * Invisible-at-rest D-Pad overlay view aligned over the enlarged photorealistic D-Pad
 * on the controller image. Draws a pixel-exact 3D sunken ("enfoncé") depression effect
 * directly on the pixels of the directional arm(s) currently pressed.
 */
class DPadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface DPadListener {
        fun onDirection(direction: Byte)
    }
    var listener: DPadListener? = null

    private var currentDirection: Byte = DPadDirection.CENTER

    private var upPressed = false
    private var downPressed = false
    private var leftPressed = false
    private var rightPressed = false

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dstRect = Rect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 100% invisible at rest so the authentic photorealistic D-Pad on manette.jpg is shown untouched
        if (!upPressed && !downPressed && !leftPressed && !rightPressed) return

        val bounds = ManettePixelEffect.parseBounds(tag as? String) ?: return
        dstRect.set(0, 0, width, height)

        if (upPressed) {
            val bmp = ManettePixelEffect.getDPadArmBitmap(context, "UP", bounds[0], bounds[1], bounds[2], bounds[3])
            canvas.drawBitmap(bmp, null, dstRect, bmpPaint)
        }
        if (downPressed) {
            val bmp = ManettePixelEffect.getDPadArmBitmap(context, "DOWN", bounds[0], bounds[1], bounds[2], bounds[3])
            canvas.drawBitmap(bmp, null, dstRect, bmpPaint)
        }
        if (leftPressed) {
            val bmp = ManettePixelEffect.getDPadArmBitmap(context, "LEFT", bounds[0], bounds[1], bounds[2], bounds[3])
            canvas.drawBitmap(bmp, null, dstRect, bmpPaint)
        }
        if (rightPressed) {
            val bmp = ManettePixelEffect.getDPadArmBitmap(context, "RIGHT", bounds[0], bounds[1], bounds[2], bounds[3])
            canvas.drawBitmap(bmp, null, dstRect, bmpPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val size = min(width, height).toFloat()
        val deadZone = size * 0.08f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - cx
                val dy = event.y - cy
                val dist = hypot(dx, dy)

                val prevDirection = currentDirection
                if (dist < deadZone) {
                    upPressed = false; downPressed = false
                    leftPressed = false; rightPressed = false
                    currentDirection = DPadDirection.CENTER
                } else {
                    val threshold = size * 0.13f
                    upPressed = dy < -threshold
                    downPressed = dy > threshold
                    leftPressed = dx < -threshold
                    rightPressed = dx > threshold

                    currentDirection = when {
                        upPressed && leftPressed -> DPadDirection.UP_LEFT
                        upPressed && rightPressed -> DPadDirection.UP_RIGHT
                        downPressed && leftPressed -> DPadDirection.DOWN_LEFT
                        downPressed && rightPressed -> DPadDirection.DOWN_RIGHT
                        upPressed -> DPadDirection.UP
                        downPressed -> DPadDirection.DOWN
                        leftPressed -> DPadDirection.LEFT
                        rightPressed -> DPadDirection.RIGHT
                        else -> DPadDirection.CENTER
                    }
                }

                if (currentDirection != prevDirection) {
                    if (currentDirection != DPadDirection.CENTER) {
                        if (HapticManager.isHapticEnabled(context)) {
                            HapticManager.vibrateDPadPress(context)
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                    listener?.onDirection(currentDirection)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasActive = currentDirection != DPadDirection.CENTER
                upPressed = false; downPressed = false
                leftPressed = false; rightPressed = false
                currentDirection = DPadDirection.CENTER
                if (wasActive && HapticManager.isHapticEnabled(context)) {
                    HapticManager.vibrateButtonRelease(context)
                }
                listener?.onDirection(currentDirection)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun getCurrentDirection(): Byte = currentDirection
}
