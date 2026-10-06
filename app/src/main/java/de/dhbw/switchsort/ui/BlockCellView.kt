package de.dhbw.switchsort.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import de.dhbw.switchsort.R

/**
 * Spielfeld-Zelle als flacher Block mit Tiefenkante: die Oberfläche liegt
 * um [depth] über der dunkleren Basis. Beim Drücken sinkt sie auf die Basis,
 * das ersetzt einen Schatten und wirkt wie eine physische Taste.
 *
 * Die Kante wird als Differenz Basis − Oberfläche gezeichnet, weil sich
 * halbtransparente Flächen sonst überlagern und die Oberfläche abdunkeln.
 */
class BlockCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    var value: Int = 0

    private val density = resources.displayMetrics.density
    private val depth = DEPTH_DP * density
    private val radius = RADIUS_DP * density

    private val surfaceColor = context.getColor(R.color.cellSurface)
    private val errorColor = context.getColor(R.color.cellError)

    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.cellEdge)
    }

    private val baseRect = RectF()
    private val surfaceRect = RectF()
    private val basePath = Path()
    private val surfacePath = Path()
    private val edgePath = Path()

    private var surfaceOffset = 0f
    private var flashAnimator: Animator? = null
    private var shakeAnimator: Animator? = null

    init {
        gravity = Gravity.CENTER
        maxLines = 1
        includeFontPadding = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(context.getColor(R.color.textPrimary))
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = minOf(radius, w * MAX_RADIUS_RATIO)
        baseRect.set(0f, depth, w, h)
        surfaceRect.set(0f, surfaceOffset, w, h - depth + surfaceOffset)

        basePath.rewind()
        basePath.addRoundRect(baseRect, r, r, Path.Direction.CW)
        surfacePath.rewind()
        surfacePath.addRoundRect(surfaceRect, r, r, Path.Direction.CW)
        edgePath.rewind()
        edgePath.op(basePath, surfacePath, Path.Op.DIFFERENCE)

        canvas.drawPath(edgePath, edgePaint)
        canvas.drawPath(surfacePath, surfacePaint)

        canvas.save()
        canvas.translate(0f, surfaceOffset - depth / 2f)
        super.onDraw(canvas)
        canvas.restore()
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        val target = if (pressed) depth else 0f
        if (target != surfaceOffset) {
            surfaceOffset = target
            invalidate()
        }
    }

    /** Kurzer Federeffekt beim Antippen. */
    fun playTap() {
        animate().cancel()
        scaleX = TAP_SCALE
        scaleY = TAP_SCALE
        animate().scaleX(1f).scaleY(1f)
            .setDuration(TAP_MS)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    /** Fehlversuch: Oberfläche kurz pastellrot, dazu horizontales Schütteln. */
    fun playError() {
        flashAnimator?.cancel()
        flashAnimator = ValueAnimator.ofArgb(surfaceColor, errorColor, surfaceColor).apply {
            duration = FLASH_MS
            addUpdateListener {
                surfacePaint.color = it.animatedValue as Int
                invalidate()
            }
            start()
        }
        shakeAnimator?.cancel()
        val d = SHAKE_DP * density
        shakeAnimator = ObjectAnimator.ofFloat(
            this, View.TRANSLATION_X, 0f, -d, d, -d * 0.6f, d * 0.6f, -d * 0.3f, 0f
        ).apply {
            duration = SHAKE_MS
            start()
        }
    }

    override fun onDetachedFromWindow() {
        flashAnimator?.cancel()
        shakeAnimator?.cancel()
        animate().cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val DEPTH_DP = 4f
        const val RADIUS_DP = 14f
        const val MAX_RADIUS_RATIO = 0.22f
        const val SHAKE_DP = 8f
        const val TAP_SCALE = 0.92f
        const val TAP_MS = 160L
        const val FLASH_MS = 450L
        const val SHAKE_MS = 250L
    }
}
