package com.manette.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.manette.app.hid.DPadDirection

/**
 * D-Pad custom view – Style manette physique blanche & bleue.
 * Dessine une croix directionnelle en bleu avec effet 3D.
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
    private val armRect = RectF()
    private val centerRect = RectF()

    // ── Palette bleue ──────────────────────────────────────────────────────────
    // Bras normaux : bleu dégradé (haut = clair, bas = foncé)
    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3D6A9E.toInt()
        style = Paint.Style.FILL
    }
    // Bras pressé : bleu très foncé
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A3558.toInt()
        style = Paint.Style.FILL
    }
    // Bordure : bleu lumineux
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6A9FD4.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    // Flèches blanches
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE0FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    // Ombre portée
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x333D6A9E.toInt()
        style = Paint.Style.FILL
    }
    // Reflet supérieur
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x408AB4D8.toInt()
        style = Paint.Style.FILL
    }

    // Pressed states
    private var upPressed = false
    private var downPressed = false
    private var leftPressed = false
    private var rightPressed = false

    private var size = 0f
    private val armWidth get() = size / 3.2f
    private val armLength get() = size / 3.2f
    private val cornerRadius = 10f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        size = minOf(w, h).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        // Dessin désactivé : l'image de fond contient déjà le visuel du D-Pad.
        // Cette vue gère uniquement la détection tactile.
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, rotation: Float) {
        canvas.save()
        canvas.rotate(rotation, x, y)
        val s = armWidth * 0.28f
        val path = Path().apply {
            moveTo(x, y - s)
            lineTo(x + s * 0.75f, y + s * 0.55f)
            lineTo(x - s * 0.75f, y + s * 0.55f)
            close()
        }
        canvas.drawPath(path, arrowPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val arm = armWidth
        val len = armLength

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val tx = event.x
                val ty = event.y

                upPressed    = ty < cy - arm / 2 && ty > cy - arm / 2 - len && tx in (cx - arm / 2)..(cx + arm / 2)
                downPressed  = ty > cy + arm / 2 && ty < cy + arm / 2 + len && tx in (cx - arm / 2)..(cx + arm / 2)
                leftPressed  = tx < cx - arm / 2 && tx > cx - arm / 2 - len && ty in (cy - arm / 2)..(cy + arm / 2)
                rightPressed = tx > cx + arm / 2 && tx < cx + arm / 2 + len && ty in (cy - arm / 2)..(cy + arm / 2)

                currentDirection = when {
                    upPressed && leftPressed   -> DPadDirection.UP_LEFT
                    upPressed && rightPressed  -> DPadDirection.UP_RIGHT
                    downPressed && leftPressed -> DPadDirection.DOWN_LEFT
                    downPressed && rightPressed-> DPadDirection.DOWN_RIGHT
                    upPressed                  -> DPadDirection.UP
                    downPressed                -> DPadDirection.DOWN
                    leftPressed                -> DPadDirection.LEFT
                    rightPressed               -> DPadDirection.RIGHT
                    else                       -> DPadDirection.CENTER
                }
                listener?.onDirection(currentDirection)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                upPressed = false; downPressed = false
                leftPressed = false; rightPressed = false
                currentDirection = DPadDirection.CENTER
                listener?.onDirection(currentDirection)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun getCurrentDirection(): Byte = currentDirection
}
