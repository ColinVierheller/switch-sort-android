package de.dhbw.switchsort.score

/**
 * Reine, JVM-testbare Ranking-Logik: sortiert Einträge nach
 * [HighScoreEntry.RANK_COMPARATOR] und begrenzt auf die Top 10.
 */
object HighScoreRanking {

    const val MAX_ENTRIES = 10

    /**
     * Fügt [new] in [existing] ein und liefert eine neue, sortierte Liste mit
     * höchstens [MAX_ENTRIES] Einträgen. Die Eingabeliste wird nicht verändert.
     */
    fun insert(existing: List<HighScoreEntry>, new: HighScoreEntry): List<HighScoreEntry> =
        (existing + new)
            .sortedWith(HighScoreEntry.RANK_COMPARATOR)
            .take(MAX_ENTRIES)
}
