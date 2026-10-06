package de.dhbw.switchsort.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import de.dhbw.switchsort.R
import de.dhbw.switchsort.game.GameSession

/** Verbleibende Versuche als Punkte: gefüllt = verfügbar, Ring = verbraucht. */
class MissDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val dot = DOT_DP * density
    private val spacing = SPACING_DP * density
    private val stroke = STROKE_DP * density

    private val filled = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.textPrimary)
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.textSecondary)
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }

    private var misses = 0

    fun setMisses(value: Int) {
        if (value == misses && contentDescription != null) return
        misses = value
        val remaining = GameSession.MAX_MISSES - misses
        contentDescription = context.getString(
            R.string.game_misses_description, remaining, GameSession.MAX_MISSES
        )
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = GameSession.MAX_MISSES
        val w = (count * dot + (count - 1) * spacing).toInt() + paddingLeft + paddingRight
        val h = dot.toInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val r = dot / 2f
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val remaining = GameSession.MAX_MISSES - misses
        for (i in 0 until GameSession.MAX_MISSES) {
            val cx = paddingLeft + r + i * (dot + spacing)
            if (i < remaining) canvas.drawCircle(cx, cy, r, filled)
            else canvas.drawCircle(cx, cy, r - stroke / 2f, ring)
        }
    }

    private companion object {
        const val DOT_DP = 10f
        const val SPACING_DP = 7f
        const val STROKE_DP = 1.5f
    }
}
