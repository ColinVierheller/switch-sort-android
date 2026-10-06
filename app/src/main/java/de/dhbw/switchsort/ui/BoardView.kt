package de.dhbw.switchsort.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import de.dhbw.switchsort.R

/**
 * Quadratisches n×n-Spielfeld. Die Kantenlänge ergibt sich aus dem
 * verfügbaren Platz (Breite und Höhe), begrenzt auf [R.dimen.board_max_size],
 * damit die Blöcke auf großen Displays nicht überdimensioniert wirken.
 */
class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    var onCellTap: ((BlockCellView) -> Unit)? = null

    private var columns = 3
    private val gap = resources.getDimensionPixelSize(R.dimen.board_gap)
    private val maxSide = resources.getDimensionPixelSize(R.dimen.board_max_size)
    private var cellSize = 0
    private var baseTextSize = 0f

    init {
        clipChildren = false
        clipToPadding = false
    }

    fun setBoard(values: List<Int>, n: Int, animate: Boolean) {
        removeAllViews()
        columns = n
        baseTextSize = resources.getDimension(
            when (n) {
                3 -> R.dimen.cell_text_3
                4 -> R.dimen.cell_text_4
                else -> R.dimen.cell_text_5
            }
        )
        val stagger = if (values.size > 1) {
            minOf(STAGGER_MS, STAGGER_BUDGET_MS / (values.size - 1))
        } else 0L
        values.forEachIndexed { index, v ->
            val cell = BlockCellView(context).apply {
                value = v
                text = context.getString(R.string.number, v)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextSize)
                setOnClickListener { onCellTap?.invoke(this) }
            }
            if (animate) {
                cell.alpha = 0f
                cell.scaleX = ENTER_SCALE
                cell.scaleY = ENTER_SCALE
                cell.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(index * stagger)
                    .setDuration(ENTER_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            addView(cell)
        }
    }

    fun setCellsEnabled(enabled: Boolean) {
        for (i in 0 until childCount) getChildAt(i).isEnabled = enabled
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val availW = w - paddingLeft - paddingRight
        val availH = if (hMode == MeasureSpec.UNSPECIFIED) availW
        else MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        val side = minOf(availW, availH, maxSide).coerceAtLeast(0)
        cellSize = ((side - gap * (columns - 1)) / columns).coerceAtLeast(0)

        // Bei wenig Platz (z. B. Querformat) schrumpft die Schrift mit der Zelle.
        val textPx = minOf(baseTextSize, cellSize * MAX_TEXT_RATIO)
        val spec = MeasureSpec.makeMeasureSpec(cellSize, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            val cell = getChildAt(i) as BlockCellView
            if (cell.textSize != textPx) cell.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx)
            cell.measure(spec, spec)
        }

        val h = if (hMode == MeasureSpec.EXACTLY) MeasureSpec.getSize(heightMeasureSpec)
        else side + paddingTop + paddingBottom
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val grid = cellSize * columns + gap * (columns - 1)
        val left = paddingLeft + (width - paddingLeft - paddingRight - grid) / 2
        val top = paddingTop + (height - paddingTop - paddingBottom - grid) / 2
        for (i in 0 until childCount) {
            val x = left + (i % columns) * (cellSize + gap)
            val y = top + (i / columns) * (cellSize + gap)
            getChildAt(i).layout(x, y, x + cellSize, y + cellSize)
        }
    }

    private companion object {
        const val STAGGER_MS = 15L
        const val STAGGER_BUDGET_MS = 120L
        const val ENTER_MS = 160L
        const val ENTER_SCALE = 0.85f
        const val MAX_TEXT_RATIO = 0.4f
    }
}
