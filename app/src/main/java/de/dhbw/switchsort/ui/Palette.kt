package de.dhbw.switchsort.ui

import android.graphics.Color

/**
 * Farbton-basierte Palette für den Hintergrundverlauf und die davon
 * abgeleitete Akzentfarbe. Sättigung und Helligkeit sind je Modus fest,
 * damit jeder Farbton pastellig (hell) bzw. gedeckt (dunkel) bleibt.
 */
object Palette {

    const val START_HUE = 200f
    const val HUE_STEP = 18f
    const val HUE_ANIM_MS = 600L

    /** Versatz des unteren Verlaufstons, damit der Verlauf lebendig wirkt. */
    private const val GRADIENT_SPREAD = 34f

    const val IDLE_HUE_RANGE = 40f
    const val IDLE_CYCLE_MS = 24_000L

    private val LIGHT_TOP = Tone(saturation = 0.22f, value = 1.0f)
    private val LIGHT_BOTTOM = Tone(saturation = 0.34f, value = 0.95f)
    private val DARK_TOP = Tone(saturation = 0.38f, value = 0.28f)
    private val DARK_BOTTOM = Tone(saturation = 0.42f, value = 0.17f)

    private val LIGHT_ACCENT = Tone(saturation = 0.50f, value = 0.62f)
    private val DARK_ACCENT = Tone(saturation = 0.45f, value = 0.42f)
    private val DARK_HIGHLIGHT = Tone(saturation = 0.30f, value = 0.95f)

    private data class Tone(val saturation: Float, val value: Float) {
        fun color(hue: Float): Int =
            Color.HSVToColor(floatArrayOf(normalizeHue(hue), saturation, value))
    }

    fun gradientColors(hue: Float, night: Boolean): IntArray {
        val top = if (night) DARK_TOP else LIGHT_TOP
        val bottom = if (night) DARK_BOTTOM else LIGHT_BOTTOM
        return intArrayOf(top.color(hue), bottom.color(hue + GRADIENT_SPREAD))
    }

    /** Text-/Akzentfarbe auf den hellen Pill-Buttons, passend zum Verlauf. */
    fun accent(hue: Float, night: Boolean): Int =
        (if (night) DARK_ACCENT else LIGHT_ACCENT).color(hue + GRADIENT_SPREAD / 2)

    /** Hervorhebung direkt auf dem Verlauf (z. B. „Neuer Rekord“). */
    fun highlight(hue: Float, night: Boolean): Int =
        (if (night) DARK_HIGHLIGHT else LIGHT_ACCENT).color(hue + GRADIENT_SPREAD / 2)

    fun normalizeHue(hue: Float): Float = ((hue % 360f) + 360f) % 360f
}
