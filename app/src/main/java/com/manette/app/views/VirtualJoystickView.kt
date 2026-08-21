package com.manette.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Analog virtual joystick view drawn on Canvas.
 * Style : blanc & bleu – correspond au design de la manette physique de l'image.
 * Supports multi-touch and provides normalized X/Y values in [-127, 127].
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
    private var baseRadius = 0f
    private var thumbRadius = 0f
    private var activePointerId = -1
    private var isPressed = false

    // ── Palette bleue de la manette physique ──────────────────────────────────
    // Cercle de base : blanc avec bordure bleue douce
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Fond blanc légèrement bleuté
        color = 0xFFECF3FB.toInt()
        style = Paint.Style.FILL
    }
    private val baseBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Bordure bleu doux
        color = 0xFF8AB4D8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val baseInnerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Anneau intérieur subtil
        color = 0xFFD0E4F7.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A4F7C.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x333D6A9E.toInt()
        style = Paint.Style.FILL
    }
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Croix de guidage bleu très pâle
        color = 0x80AACDE8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // ── Ripple animation ──────────────────────────────────────────────────────
    private var rippleAlpha = 0f
    private var rippleRadius = 0f
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFF3D6A9E.toInt()
    }

    // ── Highlight sur le thumb ─────────────────────────────────────────────
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x70FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f * 0.90f
        thumbRadius = baseRadius * 0.40f
        thumbX = centerX
        thumbY = centerY
        updateThumbGradient()
    }

    private fun updateThumbGradient() {
        // Dégradé radial bleu : clair au centre → foncé sur les bords (comme la photo)
        thumbPaint.shader = RadialGradient(
            thumbX - thumbRadius * 0.25f, thumbY - thumbRadius * 0.25f, thumbRadius,
            intArrayOf(
                0xFF7AADD8.toInt(),   // bleu clair (reflet haut-gauche)
                0xFF4A7AB0.toInt(),   // bleu moyen
                0xFF2A4F7C.toInt()    // bleu foncé (bords)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        // Dessin désactivé : on utilise le visuel de l'image de fond (manette.jpg)
        // La vue sert uniquement de zone tactile invisible.
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                activePointerId = event.getPointerId(idx)
                isPressed = true
                rippleRadius = thumbRadius
                rippleAlpha = 0.5f
                updatePosition(event.getX(idx), event.getY(idx))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) updatePosition(event.getX(idx), event.getY(idx))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                if (event.getPointerId(idx) == activePointerId) {
                    returnToCenter()
                    activePointerId = -1
                    isPressed = false
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
        val maxDist = baseRadius - thumbRadius
        if (dist > maxDist) {
            val angle = atan2(dy, dx)
            thumbX = centerX + cos(angle) * maxDist
            thumbY = centerY + sin(angle) * maxDist
        } else {
            thumbX = x
            thumbY = y
        }
        dispatchValues()
        invalidate()
    }

    private fun returnToCenter() {
        thumbX = centerX
        thumbY = centerY
        listener?.onMove(0, 0)
        invalidate()
    }

    private fun dispatchValues() {
        val maxDist = baseRadius - thumbRadius
        val nx = ((thumbX - centerX) / maxDist * 127f).toInt().coerceIn(-127, 127)
        val ny = ((thumbY - centerY) / maxDist * 127f).toInt().coerceIn(-127, 127)
        listener?.onMove(nx, ny)
    }

    /** Get current X value [-127, 127] */
    fun getValueX(): Int {
        val maxDist = baseRadius - thumbRadius
        return ((thumbX - centerX) / maxDist * 127f).toInt().coerceIn(-127, 127)
    }

    /** Get current Y value [-127, 127] */
    fun getValueY(): Int {
        val maxDist = baseRadius - thumbRadius
        return ((thumbY - centerY) / maxDist * 127f).toInt().coerceIn(-127, 127)
    }
}
