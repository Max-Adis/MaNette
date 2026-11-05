package com.manette.app.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Analog virtual joystick view drawn on Canvas with realistic 3D ball-joint & thumbstick movement.
 * Mimics a real physical controller analog stick:
 * - Deep recessed dark socket well covering the static background stick
 * - 3D metallic/polymer ball-joint dome & tilting cylindrical stem
 * - Dynamic perspective shadow cast onto the controller shell
 * - Realistic blue rubberized thumb cap with outer grip ridge and concave inner dish
 * - Spring-back physical animation (OvershootInterpolator) when released
 * - Edge-click haptic feedback when hitting the outer physical gate
 */
class VirtualJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // ── Callback ──────────────────────────────────────────────────────────────
    interface JoystickListener {
        fun onMove(x: Int, y: Int) // values in [-127, 127]
    }
    var listener: JoystickListener? = null

    // ── State ─────────────────────────────────────────────────────────────────
    private var thumbX = 0f
    private var thumbY = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var socketRadius = 0f
    private var capRadius = 0f
    private var maxDisplacement = 0f
    private var activePointerId = -1
    private var isPressed = false
    private var wasAtRim = false

    private var returnAnimator: ValueAnimator? = null

    // ── Reusable Paints & Geometry ────────────────────────────────────────────
    private val housingRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val housingBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val socketCavityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val socketInnerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val ballJointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val stemHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dropShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val capSideSkirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val capOuterRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val capRimHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val capGripTicksPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x380A192F
        strokeCap = Paint.Cap.ROUND
    }
    private val capRidgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val capConcaveBowlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val capGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val activeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val ovalRect = RectF()
    private val stemPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val minDim = min(w, h).toFloat()

        // Socket covers the static stick in the photo; cap matches the real stick head size
        socketRadius = minDim * 0.365f
        capRadius = minDim * 0.325f
        maxDisplacement = minDim * 0.155f

        thumbX = centerX
        thumbY = centerY

        setupStaticShaders()
    }

    private fun setupStaticShaders() {
        if (socketRadius <= 0f) return

        // Outer white/silver recessed housing bezel matching the controller shell well
        housingRingPaint.shader = LinearGradient(
            centerX - socketRadius, centerY - socketRadius,
            centerX + socketRadius, centerY + socketRadius,
            intArrayOf(
                0xFFD0DCEB.toInt(),
                0xFFE8EEF6.toInt(),
                0xFFFFFFFF.toInt()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        housingBevelPaint.strokeWidth = socketRadius * 0.06f
        housingBevelPaint.shader = LinearGradient(
            centerX, centerY - socketRadius,
            centerX, centerY + socketRadius,
            0x998DA4C4.toInt(),
            0xCCFFFFFF.toInt(),
            Shader.TileMode.CLAMP
        )

        // Deep dark socket cavity inside the controller shell
        socketCavityPaint.shader = RadialGradient(
            centerX, centerY,
            socketRadius * 0.92f,
            intArrayOf(
                0xFF0A182E.toInt(),
                0xFF071122.toInt(),
                0xFF030710.toInt()
            ),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        socketInnerShadowPaint.strokeWidth = socketRadius * 0.08f
        socketInnerShadowPaint.color = 0xAA00040A.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (socketRadius <= 0f) return

        val dx = thumbX - centerX
        val dy = thumbY - centerY
        val dist = hypot(dx, dy)
        val tiltFraction = (dist / maxDisplacement).coerceIn(0f, 1f)
        val angleRad = atan2(dy, dx)

        // 1. Recessed controller shell bezel & deep socket cavity (masks static photo stick)
        canvas.drawCircle(centerX, centerY, socketRadius * 1.04f, housingRingPaint)
        canvas.drawCircle(centerX, centerY, socketRadius * 1.02f, housingBevelPaint)
        canvas.drawCircle(centerX, centerY, socketRadius * 0.90f, socketCavityPaint)
        canvas.drawCircle(centerX, centerY, socketRadius * 0.87f, socketInnerShadowPaint)

        // 2. Internal 3D Ball-Joint Dome (shifts slightly with stick tilt)
        val ballCenterX = centerX + dx * 0.28f
        val ballCenterY = centerY + dy * 0.28f
        val ballRadius = socketRadius * 0.62f
        ballJointPaint.shader = RadialGradient(
            ballCenterX - ballRadius * 0.3f,
            ballCenterY - ballRadius * 0.3f,
            ballRadius,
            intArrayOf(
                0xFF3E6B9E.toInt(),
                0xFF1D3B63.toInt(),
                0xFF0B1930.toInt()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(ballCenterX, ballCenterY, ballRadius, ballJointPaint)

        // 3. Cast Shadow from the elevated stick cap onto the socket & shell
        val shadowOffsetX = dx * 0.35f + capRadius * 0.06f
        val shadowOffsetY = dy * 0.35f + capRadius * (0.14f + 0.12f * tiltFraction)
        val shadowCenterX = centerX + dx + shadowOffsetX
        val shadowCenterY = centerY + dy + shadowOffsetY
        val shadowRadius = capRadius * (1.06f + 0.06f * tiltFraction)

        dropShadowPaint.shader = RadialGradient(
            shadowCenterX, shadowCenterY, shadowRadius,
            intArrayOf(
                0x75040E21.toInt(),
                0x380A1E3C.toInt(),
                0x000A1E3C
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(shadowCenterX, shadowCenterY, shadowRadius, dropShadowPaint)

        // 4. Cylindrical Shaft / Stem connecting ball joint to the underside of the thumb cap
        if (dist > 2f) {
            stemPaint.strokeWidth = capRadius * 0.46f
            stemPaint.shader = LinearGradient(
                centerX, centerY,
                thumbX, thumbY,
                0xFF0D203B.toInt(),
                0xFF285082.toInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawLine(centerX + dx * 0.18f, centerY + dy * 0.18f, thumbX - dx * 0.12f, thumbY - dy * 0.12f, stemPaint)

            // Specular highlight along the shaft
            stemHighlightPaint.strokeWidth = capRadius * 0.14f
            stemHighlightPaint.color = 0x448AB8E6
            val perpX = -sin(angleRad) * (capRadius * 0.10f)
            val perpY = cos(angleRad) * (capRadius * 0.10f)
            canvas.drawLine(
                centerX + dx * 0.20f + perpX,
                centerY + dy * 0.20f + perpY,
                thumbX - dx * 0.15f + perpX,
                thumbY - dy * 0.15f + perpY,
                stemHighlightPaint
            )
        }

        // 5. 3D Perspective Thumb Cap (foreshortens slightly along tilt axis like a real tilted disc)
        canvas.save()
        val pressScale = if (isPressed) 1.03f else 1.0f
        canvas.translate(thumbX, thumbY)
        canvas.scale(pressScale, pressScale)

        // Slight 3D perspective compression along the tilt direction
        val deg = Math.toDegrees(angleRad.toDouble()).toFloat()
        val squeeze = 1f - (0.085f * tiltFraction)
        canvas.rotate(deg)
        canvas.scale(squeeze, 1f)
        canvas.rotate(-deg)

        // 5a. 3D Under-skirt / Side thickness of the thumbstick cap (visible on the side opposite to tilt)
        val skirtShiftX = -dx * 0.14f
        val skirtShiftY = -dy * 0.14f + capRadius * 0.06f
        capSideSkirtPaint.shader = RadialGradient(
            skirtShiftX, skirtShiftY, capRadius * 1.02f,
            intArrayOf(
                0xFF173357.toInt(),
                0xFF0F2442.toInt(),
                0xFF071326.toInt()
            ),
            floatArrayOf(0f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(skirtShiftX, skirtShiftY, capRadius * 1.01f, capSideSkirtPaint)

        // 5b. Main Outer Rubberized Thumbstick Cap (matches photo's matte blue)
        val lightOffX = -capRadius * 0.28f - dx * 0.12f
        val lightOffY = -capRadius * 0.28f - dy * 0.12f
        capOuterRimPaint.shader = RadialGradient(
            lightOffX, lightOffY, capRadius * 1.25f,
            intArrayOf(
                0xFF7FA2D4.toInt(), // Top-left directional light reflection
                0xFF4772A8.toInt(), // Mid matte blue
                0xFF1F4576.toInt(), // Deep royal blue
                0xFF0F294D.toInt()  // Dark outer edge
            ),
            floatArrayOf(0f, 0.35f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, capRadius, capOuterRimPaint)

        // Subtle outer rim edge highlight
        capRimHighlightPaint.strokeWidth = capRadius * 0.035f
        capRimHighlightPaint.shader = LinearGradient(
            -capRadius, -capRadius,
            capRadius, capRadius,
            0x99B8D4F2.toInt(),
            0x44091930.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, capRadius * 0.98f, capRimHighlightPaint)

        // 5c. Tactile Knurled Grip Micro-Texture on the outer ring
        val tickInner = capRadius * 0.76f
        val tickOuter = capRadius * 0.91f
        capGripTicksPaint.strokeWidth = capRadius * 0.025f
        for (i in 0 until 24) {
            val a = (i * 15.0) * Math.PI / 180.0
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            canvas.drawLine(
                ca * tickInner, sa * tickInner,
                ca * tickOuter, sa * tickOuter,
                capGripTicksPaint
            )
        }

        // 5d. Inner Concave Thumb Dish (the sunken bowl where the thumb rests)
        // In a concave bowl, the shadow is on the light side (top-left) and highlight on the bottom-right!
        val bowlRadius = capRadius * 0.68f
        val bowlHighlightX = bowlRadius * 0.25f + dx * 0.10f
        val bowlHighlightY = bowlRadius * 0.28f + dy * 0.10f
        capConcaveBowlPaint.shader = RadialGradient(
            bowlHighlightX, bowlHighlightY, bowlRadius * 1.15f,
            intArrayOf(
                0xFF3E6EA6.toInt(), // Lower-right inner bowl reflection
                0xFF173D73.toInt(), // Deep blue center
                0xFF0D254C.toInt(), // Upper-left inner bowl shadow
                0xFF071630.toInt()  // Rim crease shadow
            ),
            floatArrayOf(0f, 0.45f, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, bowlRadius, capConcaveBowlPaint)

        // Ridge border separating the outer ring and inner concave dish
        capRidgeBorderPaint.strokeWidth = capRadius * 0.045f
        capRidgeBorderPaint.shader = LinearGradient(
            -bowlRadius, -bowlRadius,
            bowlRadius, bowlRadius,
            0xAA081A38.toInt(),
            0x9978A4D8.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, bowlRadius, capRidgeBorderPaint)

        // Soft top-left specular crescent on the raised outer ring
        ovalRect.set(-capRadius * 0.82f, -capRadius * 0.82f, capRadius * 0.82f, capRadius * 0.82f)
        capGlossPaint.shader = LinearGradient(
            0f, -capRadius * 0.85f,
            0f, 0f,
            0x38FFFFFF,
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )
        canvas.drawArc(ovalRect, 200f, 120f, false, capRimHighlightPaint)

        // Active glow ring when touched
        if (isPressed) {
            activeGlowPaint.strokeWidth = capRadius * 0.04f
            activeGlowPaint.color = 0x6668B5FF
            canvas.drawCircle(0f, 0f, capRadius * 1.02f, activeGlowPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                returnAnimator?.cancel()
                val idx = event.actionIndex
                activePointerId = event.getPointerId(idx)
                isPressed = true
                wasAtRim = false
                if (com.manette.app.util.HapticManager.isHapticEnabled(context)) {
                    com.manette.app.util.HapticManager.vibrateStickGrab(context)
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                updatePosition(event.getX(idx), event.getY(idx))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) {
                    updatePosition(event.getX(idx), event.getY(idx))
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                if (event.getPointerId(idx) == activePointerId) {
                    activePointerId = -1
                    isPressed = false
                    wasAtRim = false
                    if (com.manette.app.util.HapticManager.isHapticEnabled(context)) {
                        com.manette.app.util.HapticManager.vibrateButtonRelease(context)
                    }
                    animateReturnToCenter()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updatePosition(x: Float, y: Float) {
        val dx = x - centerX
        val dy = y - centerY
        val dist = hypot(dx, dy)
        if (dist > maxDisplacement) {
            val angle = atan2(dy, dx)
            thumbX = centerX + cos(angle) * maxDisplacement
            thumbY = centerY + sin(angle) * maxDisplacement
            if (!wasAtRim) {
                wasAtRim = true
                if (com.manette.app.util.HapticManager.isHapticEnabled(context)) {
                    com.manette.app.util.HapticManager.vibrateStickRimHit(context)
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        } else {
            thumbX = x
            thumbY = y
            if (dist < maxDisplacement * 0.88f) {
                wasAtRim = false
            }
        }
        dispatchValues()
        invalidate()
    }

    private fun animateReturnToCenter() {
        val startX = thumbX
        val startY = thumbY
        returnAnimator?.cancel()

        if (hypot(startX - centerX, startY - centerY) < 1f) {
            thumbX = centerX
            thumbY = centerY
            listener?.onMove(0, 0)
            invalidate()
            return
        }

        returnAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 190L
            // Physical spring-back overshoot like a real mechanical thumbstick
            interpolator = OvershootInterpolator(1.6f)
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                thumbX = startX + (centerX - startX) * fraction
                thumbY = startY + (centerY - startY) * fraction
                dispatchValues()
                invalidate()
            }
            start()
        }
    }

    private fun dispatchValues() {
        if (maxDisplacement <= 0f) return
        val nx = (((thumbX - centerX) / maxDisplacement) * 127f).toInt().coerceIn(-127, 127)
        val ny = (((thumbY - centerY) / maxDisplacement) * 127f).toInt().coerceIn(-127, 127)
        listener?.onMove(nx, ny)
    }

    /** Get current X value [-127, 127] */
    fun getValueX(): Int {
        if (maxDisplacement <= 0f) return 0
        return (((thumbX - centerX) / maxDisplacement) * 127f).toInt().coerceIn(-127, 127)
    }

    /** Get current Y value [-127, 127] */
    fun getValueY(): Int {
        if (maxDisplacement <= 0f) return 0
        return (((thumbY - centerY) / maxDisplacement) * 127f).toInt().coerceIn(-127, 127)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        returnAnimator?.cancel()
    }
}

