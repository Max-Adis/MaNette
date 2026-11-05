package com.manette.app.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.roundToInt

/**
 * Layout sur mesure qui positionne et dimensionne ses enfants selon des pourcentages
 * par rapport à une image de fond affichée en "fitCenter" (1024x571).
 *
 * Le tag de chaque vue enfant est au format : "top,left,width,height"
 */
class GamepadOverlayLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // Ratio exact de l'image de fond (1024 / 571)
    private val imgRatio = 1024f / 571f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val viewRatio = width.toFloat() / height.toFloat()
        val renderW: Float
        val renderH: Float
        val offsetX: Float
        val offsetY: Float

        if (viewRatio > imgRatio) {
            renderH = height.toFloat()
            renderW = height * imgRatio
            offsetX = (width - renderW) / 2f
            offsetY = 0f
        } else {
            renderW = width.toFloat()
            renderH = width / imgRatio
            offsetX = 0f
            offsetY = (height - renderH) / 2f
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val tag = child.tag as? String
                if (tag != null) {
                    val parts = tag.split(",")
                    if (parts.size == 4) {
                        val topPct = parts[0].toFloatOrNull() ?: 0f
                        val leftPct = parts[1].toFloatOrNull() ?: 0f
                        val wPct = parts[2].toFloatOrNull() ?: 0f
                        val hPct = parts[3].toFloatOrNull() ?: 0f

                        val childL = (offsetX + renderW * leftPct).roundToInt()
                        val childT = (offsetY + renderH * topPct).roundToInt()
                        val childR = (offsetX + renderW * (leftPct + wPct)).roundToInt()
                        val childB = (offsetY + renderH * (topPct + hPct)).roundToInt()

                        child.measure(
                            MeasureSpec.makeMeasureSpec((childR - childL).coerceAtLeast(1), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec((childB - childT).coerceAtLeast(1), MeasureSpec.EXACTLY)
                        )
                        continue
                    }
                }
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val viewRatio = width.toFloat() / height.toFloat()

        val renderW: Float
        val renderH: Float
        val offsetX: Float
        val offsetY: Float

        if (viewRatio > imgRatio) {
            renderH = height.toFloat()
            renderW = height * imgRatio
            offsetX = (width - renderW) / 2f
            offsetY = 0f
        } else {
            renderW = width.toFloat()
            renderH = width / imgRatio
            offsetX = 0f
            offsetY = (height - renderH) / 2f
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val tag = child.tag as? String
                if (tag != null) {
                    val parts = tag.split(",")
                    if (parts.size == 4) {
                        val topPct = parts[0].toFloatOrNull() ?: 0f
                        val leftPct = parts[1].toFloatOrNull() ?: 0f
                        val wPct = parts[2].toFloatOrNull() ?: 0f
                        val hPct = parts[3].toFloatOrNull() ?: 0f

                        val childL = (offsetX + renderW * leftPct).roundToInt()
                        val childT = (offsetY + renderH * topPct).roundToInt()
                        val childR = (offsetX + renderW * (leftPct + wPct)).roundToInt()
                        val childB = (offsetY + renderH * (topPct + hPct)).roundToInt()

                        child.layout(childL, childT, childR, childB)
                        continue
                    }
                }
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }
}
