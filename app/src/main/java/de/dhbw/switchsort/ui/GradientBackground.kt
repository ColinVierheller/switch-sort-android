package de.dhbw.switchsort.ui

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Vollflächiger vertikaler Verlauf, dessen Farbton sich animiert verschieben
 * lässt. Pro Treffer wandert der Ton weiter (Fortschrittsgefühl), im Menü
 * pendelt er langsam hin und her.
 */
class GradientBackground(private val night: Boolean, initialHue: Float) {

    var hue: Float = Palette.normalizeHue(initialHue)
        private set

    val drawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        Palette.gradientColors(hue, night)
    )

    var onHueChanged: ((Float) -> Unit)? = null

    private var animator: ValueAnimator? = null
    private var idling = false

    fun shiftHue(delta: Float) {
        idling = false
        animateHue(hue, hue + delta, Palette.HUE_ANIM_MS) {
            interpolator = DecelerateInterpolator()
        }
    }

    fun startIdle() {
        if (idling) return
        idling = true
        animateHue(hue, hue + Palette.IDLE_HUE_RANGE, Palette.IDLE_CYCLE_MS / 2) {
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
    }

    fun stopIdle() {
        if (!idling) return
        idling = false
        animator?.cancel()
        animator = null
    }

    fun cancel() {
        idling = false
        animator?.cancel()
        animator = null
    }

    private fun animateHue(from: Float, to: Float, duration: Long, setup: ValueAnimator.() -> Unit) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            setup()
            addUpdateListener { applyHue(it.animatedValue as Float) }
            start()
        }
    }

    fun applyHue(value: Float) {
        hue = Palette.normalizeHue(value)
        drawable.colors = Palette.gradientColors(hue, night)
        onHueChanged?.invoke(hue)
    }
}
