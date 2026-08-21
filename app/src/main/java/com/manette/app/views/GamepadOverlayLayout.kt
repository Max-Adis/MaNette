package com.manette.app.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * Layout sur mesure qui positionne et dimensionne ses enfants selon des pourcentages
 * par rapport à une image de fond affichée en "fitCenter".
 * 
 * Le tag de chaque vue enfant doit être au format : "top,left,width,height"
 * Exemple : android:tag="0.2313,0.2520,0.0970,0.1717"
 */
class GamepadOverlayLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // Ratio de l'image de fond (1024 / 571 = 1.7933f)
    private val imgRatio = 1024f / 571f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val viewRatio = width.toFloat() / height.toFloat()
        var renderW = width.toFloat()
        var renderH = height.toFloat()

        // Calculer les dimensions effectives de l'image (fitCenter)
        if (viewRatio > imgRatio) { // Vue plus large que l'image
            renderH = height.toFloat()
            renderW = height * imgRatio
        } else { // Vue plus haute que l'image
            renderW = width.toFloat()
            renderH = width / imgRatio
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val tag = child.tag as? String
                if (tag != null) {
                    val parts = tag.split(",")
                    if (parts.size == 4) {
                        val wPct = parts[2].toFloatOrNull() ?: 0f
                        val hPct = parts[3].toFloatOrNull() ?: 0f

                        val childW = (renderW * wPct).toInt()
                        val childH = (renderH * hPct).toInt()
                        
                        child.measure(
                            MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY)
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
        
        var renderW = width.toFloat()
        var renderH = height.toFloat()
        var offsetX = 0f
        var offsetY = 0f

        // Calculer les offsets (centrage de l'image)
        if (viewRatio > imgRatio) {
            renderH = height.toFloat()
            renderW = height * imgRatio
            offsetX = (width - renderW) / 2f
        } else {
            renderW = width.toFloat()
            renderH = width / imgRatio
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
                        
                        val childW = child.measuredWidth
                        val childH = child.measuredHeight
                        
                        val childL = (offsetX + renderW * leftPct).toInt()
                        val childT = (offsetY + renderH * topPct).toInt()
                        
                        child.layout(childL, childT, childL + childW, childT + childH)
                        continue
                    }
                }
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }
}
