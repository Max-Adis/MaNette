package com.manette.app.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.manette.app.R
import kotlin.math.min

/**
 * Renders smooth, anti-aliased 3D sunken ("enfoncé") effects in the exact 1024x571
 * pixel coordinate space of R.drawable.manette so that every button and D-Pad arm
 * aligns with 0.0px offset on any screen size or aspect ratio.
 */
object ManettePixelEffect {

    const val REF_W = 1024
    const val REF_H = 571

    private var refBitmap: Bitmap? = null
    private val buttonBitmapCache = HashMap<String, Bitmap>()

    @Synchronized
    fun getRefBitmap(context: Context): Bitmap {
        refBitmap?.let { return it }
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.manette, opts)
        val scaled = if (raw.width == REF_W && raw.height == REF_H) {
            raw
        } else {
            Bitmap.createScaledBitmap(raw, REF_W, REF_H, true)
        }
        refBitmap = scaled
        return scaled
    }

    /**
     * Parses a view's tag "topPct,leftPct,wPct,hPct" into pixel bounds [x0, y0, x1, y1] in 1024x571 space.
     */
    fun parseBounds(tagStr: String?): IntArray? {
        if (tagStr == null) return null
        val parts = tagStr.split(",")
        if (parts.size != 4) return null
        val topPct = parts[0].toFloatOrNull() ?: return null
        val leftPct = parts[1].toFloatOrNull() ?: return null
        val wPct = parts[2].toFloatOrNull() ?: return null
        val hPct = parts[3].toFloatOrNull() ?: return null

        val x0 = (leftPct * REF_W).toInt().coerceIn(0, REF_W - 1)
        val y0 = (topPct * REF_H).toInt().coerceIn(0, REF_H - 1)
        val x1 = ((leftPct + wPct) * REF_W).toInt().coerceIn(x0 + 1, REF_W)
        val y1 = ((topPct + hPct) * REF_H).toInt().coerceIn(y0 + 1, REF_H)
        return intArrayOf(x0, y0, x1, y1)
    }

    @Synchronized
    fun getButtonEffectBitmap(
        context: Context,
        key: String,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        style: GamepadButtonView.Style
    ): Bitmap {
        val cacheKey = "$key:$x0,$y0,$x1,$y1:$style"
        buttonBitmapCache[cacheKey]?.let { return it }

        val bw = x1 - x0
        val bh = y1 - y0
        val outBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBmp)

        when (style) {
            GamepadButtonView.Style.SHOULDER_L -> drawCurvedShoulder(context, canvas, x0, y0, bw, bh, isLeft = true)
            GamepadButtonView.Style.SHOULDER_R -> drawCurvedShoulder(context, canvas, x0, y0, bw, bh, isLeft = false)
            GamepadButtonView.Style.ACTION_CIRCLE -> drawExactActionCircle(context, canvas, key, x0, y0, bw, bh)
            GamepadButtonView.Style.PILL_CENTER -> drawExactCenterPill(context, canvas, key, x0, y0, bw, bh)
        }

        buttonBitmapCache[cacheKey] = outBmp
        return outBmp
    }

    @Synchronized
    fun getDPadArmBitmap(
        context: Context,
        arm: String,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int
    ): Bitmap {
        val cacheKey = "DPAD_$arm:$x0,$y0,$x1,$y1"
        buttonBitmapCache[cacheKey]?.let { return it }

        val bw = x1 - x0
        val bh = y1 - y0
        val outBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBmp)

        // Exact pixel coordinates of the 4 D-Pad arms on the 1024x571 manette.jpg canvas:
        // Vertical bar top face: x in [187..238] (cx = 212.5, width = 51px), y in [181..342]
        // Horizontal bar top face: x in [132..289], y in [235..286] (cy = 260.5, height = 51px)
        val rectG = when (arm) {
            "UP" -> RectF(187f, 181f, 238f, 244f)
            "DOWN" -> RectF(187f, 277f, 238f, 342f)
            "LEFT" -> RectF(132f, 235f, 195f, 286f)
            "RIGHT" -> RectF(229f, 235f, 289f, 286f)
            else -> RectF()
        }
        val localRect = RectF(
            rectG.left - x0,
            rectG.top - y0,
            rectG.right - x0,
            rectG.bottom - y0
        )
        val corner = 10f

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                localRect.left, localRect.top, localRect.left, localRect.bottom,
                0xB4030A18.toInt(),
                0x680D274C,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(localRect, corner, corner, fillPaint)

        val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = 0xDD020710.toInt()
        }
        canvas.drawRoundRect(localRect, corner, corner, creasePaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
            color = 0xD838BDF8.toInt()
        }
        canvas.drawRoundRect(localRect, corner, corner, rimPaint)

        buttonBitmapCache[cacheKey] = outBmp
        return outBmp
    }

    /**
     * Renders a smooth, symmetrical cubic-Bezier 3D sunken ("enfoncé") effect that hugs
     * the exact ergonomic arch of the L and R shoulder panels on manette.jpg (1024x571).
     */
    private fun drawCurvedShoulder(
        context: Context,
        canvas: Canvas,
        x0: Int,
        y0: Int,
        bw: Int,
        bh: Int,
        isLeft: Boolean
    ) {
        fun tx(gx: Float): Float = (if (isLeft) gx else (1024f - gx)) - x0
        fun ty(gy: Float): Float = (gy + if (isLeft) 0f else 2f) - y0

        val path = Path().apply {
            moveTo(tx(136f), ty(112f))
            cubicTo(tx(165f), ty(62f), tx(225f), ty(56f), tx(332f), ty(56f))
            quadTo(tx(350f), ty(57f), tx(366f), ty(76f))
            lineTo(tx(346f), ty(111f))
            cubicTo(tx(295f), ty(103f), tx(215f), ty(121f), tx(136f), ty(149f))
            close()
        }

        // 1. Deep concave darkening across the curved shoulder arch
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, ty(55f), 0f, ty(145f),
                intArrayOf(0xC8020814.toInt(), 0x8C071B3A.toInt(), 0x5512386E),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(path, fillPaint)

        // 2. Top recessed crease along the upper arch edge (simulates the bumper pushed downward into the shell)
        val topArcPath = Path().apply {
            moveTo(tx(138f), ty(110f))
            cubicTo(tx(166f), ty(64f), tx(225f), ty(58f), tx(332f), ty(58f))
            quadTo(tx(349f), ty(59f), tx(364f), ty(76f))
        }
        val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0xE001050C.toInt()
        }
        canvas.drawPath(topArcPath, creasePaint)

        // 3. Sleek active cyan-blue contour following the exact curved shoulder silhouette
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            color = 0xCC38BDF8.toInt()
        }
        canvas.drawPath(path, rimPaint)
    }

    private fun drawExactActionCircle(
        context: Context,
        canvas: Canvas,
        key: String,
        x0: Int,
        y0: Int,
        bw: Int,
        bh: Int
    ) {
        // Exact measured pixel centers & radius of the circular button caps in 1024x571 manette.jpg:
        // Y: (812.0, 181.5), X: (731.5, 254.5), B: (889.5, 255.0), A: (811.5, 325.5), r = 37.5px
        val (gxCenter, gyCenter) = when (key) {
            "Y" -> 812.0f to 181.5f
            "X" -> 731.5f to 254.5f
            "B" -> 889.5f to 255.0f
            "A" -> 811.5f to 325.5f
            else -> (x0 + bw / 2f) to (y0 + bh / 2f)
        }
        val cx = gxCenter - x0
        val cy = gyCenter - y0
        val r = 37.5f

        val sunkenShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                cx, cy - r * 0.25f, r * 1.1f,
                intArrayOf(0x98040D1C.toInt(), 0x64081A36, 0x281E4E8C),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, r, sunkenShadePaint)

        val topCreasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            shader = LinearGradient(
                cx, cy - r, cx, cy + r,
                0xEE02060E.toInt(),
                0x0002060E,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, r - 1.5f, topCreasePaint)

        val activeRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = 0xD838BDF8.toInt()
        }
        canvas.drawCircle(cx, cy, r, activeRimPaint)
    }

    private fun drawExactCenterPill(
        context: Context,
        canvas: Canvas,
        key: String,
        x0: Int,
        y0: Int,
        bw: Int,
        bh: Int
    ) {
        // Exact measured pixel bounds of SELECT and START pills on 1024x571 manette.jpg:
        // SELECT: x in [425..479] (cx = 452.0, w = 54), y in [231..256] (cy = 243.5, h = 25)
        // START : x in [530..584] (cx = 557.0, w = 54), y in [231..256] (cy = 243.5, h = 25)
        val rectG = if (key == "SELECT") {
            RectF(425f, 231f, 479f, 256f)
        } else {
            RectF(530f, 231f, 584f, 256f)
        }
        val localRect = RectF(
            rectG.left - x0,
            rectG.top - y0,
            rectG.right - x0,
            rectG.bottom - y0
        )
        val r = localRect.height() / 2f

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                localRect.centerX(), localRect.top, localRect.centerX(), localRect.bottom,
                0xBB030914.toInt(),
                0x660F294D,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(localRect, r, r, fillPaint)

        val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.0f
            color = 0xDD02060D.toInt()
        }
        canvas.drawRoundRect(localRect, r, r, creasePaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
            color = 0xDD38BDF8.toInt()
        }
        canvas.drawRoundRect(localRect, r, r, rimPaint)
    }
}
