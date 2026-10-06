package de.dhbw.switchsort.score

import java.io.Serializable

/**
 * Ein Highscore-Eintrag einer beendeten Runde.
 *
 * [finishedAtMillis] ist eine Wall-Clock-Zeit (System.currentTimeMillis) und
 * dient nur der Anzeige bzw. der stabilen Sortierung bei voller Gleichheit;
 * Spieldauern werden ausschließlich über monotone Zeit gemessen.
 */
data class HighScoreEntry(
    val playerName: String,
    val boardSize: Int,
    val score: Int,
    val durationMillis: Long,
    val finishedAtMillis: Long
) : Serializable {

    companion object {
        const val serialVersionUID: Long = 1L

        /**
         * Rangfolge: Treffer absteigend, bei Gleichstand kürzere Dauer zuerst,
         * bei voller Gleichheit früher beendet zuerst.
         */
        val RANK_COMPARATOR: Comparator<HighScoreEntry> =
            compareByDescending<HighScoreEntry> { it.score }
                .thenBy { it.durationMillis }
                .thenBy { it.finishedAtMillis }
    }
}
