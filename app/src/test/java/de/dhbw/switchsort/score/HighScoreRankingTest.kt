package de.dhbw.switchsort.score

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HighScoreRankingTest {

    private fun entry(
        name: String = "Spieler",
        score: Int = 0,
        durationMillis: Long = 60_000L,
        finishedAtMillis: Long = 1_000L
    ) = HighScoreEntry(
        playerName = name,
        boardSize = 3,
        score = score,
        durationMillis = durationMillis,
        finishedAtMillis = finishedAtMillis
    )

    @Test
    fun `Sortierung nach Treffern absteigend`() {
        val low = entry(name = "low", score = 1)
        val high = entry(name = "high", score = 9)
        val mid = entry(name = "mid", score = 5)

        val ranked = HighScoreRanking.insert(listOf(low, high), mid)

        assertEquals(listOf("high", "mid", "low"), ranked.map { it.playerName })
    }

    @Test
    fun `Gleichstand bei Treffern kuerzere Dauer gewinnt`() {
        val slow = entry(name = "slow", score = 5, durationMillis = 90_000L)
        val fast = entry(name = "fast", score = 5, durationMillis = 30_000L)

        val ranked = HighScoreRanking.insert(listOf(slow), fast)

        assertEquals(listOf("fast", "slow"), ranked.map { it.playerName })
    }

    @Test
    fun `Gleichstand bei Treffern und Dauer frueherer Zeitstempel gewinnt`() {
        val later = entry(name = "later", score = 5, durationMillis = 30_000L, finishedAtMillis = 2_000L)
        val earlier = entry(name = "earlier", score = 5, durationMillis = 30_000L, finishedAtMillis = 1_000L)

        val ranked = HighScoreRanking.insert(listOf(later), earlier)

        assertEquals(listOf("earlier", "later"), ranked.map { it.playerName })
    }

    @Test
    fun `Liste wird auf Top 10 gekuerzt und der Schwaechste faellt heraus`() {
        val existing = (1..10).map { entry(name = "p$it", score = it) }
        val weak = entry(name = "weak", score = 0)

        val ranked = HighScoreRanking.insert(existing, weak)

        assertEquals(10, ranked.size)
        assertFalse(ranked.any { it.playerName == "weak" })
        assertTrue(ranked.any { it.playerName == "p10" })
    }

    @Test
    fun `neuer starker Eintrag verdrangt den bisherigen letzten Platz`() {
        val existing = (1..10).map { entry(name = "p$it", score = it) }
        val strong = entry(name = "strong", score = 100)

        val ranked = HighScoreRanking.insert(existing, strong)

        assertEquals(10, ranked.size)
        assertEquals("strong", ranked.first().playerName)
        assertFalse(ranked.any { it.playerName == "p1" })
    }

    @Test
    fun `Eingabeliste und Eintraege bleiben unveraendert`() {
        val a = entry(name = "a", score = 3)
        val b = entry(name = "b", score = 7)
        val existing = listOf(a, b)
        val new = entry(name = "c", score = 5)

        val ranked = HighScoreRanking.insert(existing, new)

        assertNotSame(existing, ranked)
        assertEquals(listOf(a, b), existing) // Originalreihenfolge unberuehrt
        assertEquals(listOf(b, new, a), ranked)
    }

    @Test
    fun `Einfuegen in leere Liste`() {
        val new = entry(name = "solo", score = 2)
        assertEquals(listOf(new), HighScoreRanking.insert(emptyList(), new))
    }
}
