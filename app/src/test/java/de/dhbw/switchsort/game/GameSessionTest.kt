package de.dhbw.switchsort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class GameSessionTest {

    private lateinit var session: GameSession

    @Before
    fun setUp() {
        // Deterministischer Generator, damit kein Zufallsverhalten ungeprüft bleibt.
        session = GameSession(BoardGenerator(Random(7L)))
    }

    private fun GameSession.GameSnapshot.isValidBoard(boardSize: Int): Boolean =
        board.size == boardSize * boardSize &&
            board.sorted() == (1..boardSize * boardSize).toList() &&
            target in board

    @Test
    fun `nach Start ist die Zeit 0 und Score sowie Fehler 0`() {
        session.start(3, startMillis = 5_000L)

        val snapshot = session.snapshot(nowMillis = 5_000L)

        assertEquals(0L, snapshot.elapsedMillis)
        assertEquals(0, snapshot.score)
        assertEquals(0, snapshot.misses)
        assertFalse(snapshot.finished)
        assertTrue(snapshot.isValidBoard(3))
        assertEquals(3, session.boardSize)
    }

    @Test
    fun `Zeit vor dem Start wird auf 0 begrenzt`() {
        session.start(3, startMillis = 5_000L)
        assertEquals(0L, session.snapshot(nowMillis = 4_000L).elapsedMillis)
    }

    @Test
    fun `Zeit waechst monoton mit der Uhr`() {
        session.start(4, startMillis = 0L)
        val early = session.snapshot(nowMillis = 1_500L).elapsedMillis
        val later = session.snapshot(nowMillis = 3_000L).elapsedMillis
        assertEquals(1_500L, early)
        assertEquals(3_000L, later)
        assertTrue(later >= early)
    }

    @Test
    fun `Treffer erhoeht den Score und liefert ein frisches gueltiges Feld`() {
        session.start(3, startMillis = 0L)
        val before = session.snapshot(0L)

        val after = session.tap(before.target, nowMillis = 800L)

        assertEquals(1, after.score)
        assertEquals(0, after.misses)
        assertTrue(after.isValidBoard(3))
        assertTrue(after.target in after.board)
        assertEquals(800L, after.elapsedMillis)
        assertFalse(after.finished)
    }

    @Test
    fun `Fehlversuch erhoeht den Zaehler und behaelt Feld und Ziel`() {
        session.start(3, startMillis = 0L)
        val before = session.snapshot(0L)
        val wrongValue = wrongValueFor(before)

        val after = session.tap(wrongValue, nowMillis = 500L)

        assertEquals(1, after.misses)
        assertEquals(0, after.score)
        assertEquals(before.board, after.board)
        assertEquals(before.target, after.target)
        assertFalse(after.finished)
    }

    @Test
    fun `zwei Fehlversuche beenden das Spiel noch nicht`() {
        session.start(3, startMillis = 0L)
        val first = session.tap(wrongValueFor(session.snapshot(0L)), 100L)
        val second = session.tap(wrongValueFor(first), 200L)

        assertEquals(2, second.misses)
        assertFalse(second.finished)
    }

    @Test
    fun `dritter Fehlversuch beendet das Spiel und friert die Zeit ein`() {
        session.start(3, startMillis = 0L)
        var snap = session.snapshot(0L)
        snap = session.tap(wrongValueFor(snap), 100L)
        snap = session.tap(wrongValueFor(snap), 200L)
        val atFinish = session.tap(wrongValueFor(snap), 1_234L)

        assertEquals(3, atFinish.misses)
        assertTrue(atFinish.finished)
        assertEquals(1_234L, atFinish.elapsedMillis)

        // Eingefroren: spätere Abfragen liefern denselben Stand.
        val muchLater = session.snapshot(nowMillis = 999_999L)
        assertEquals(1_234L, muchLater.elapsedMillis)
        assertTrue(muchLater.finished)
    }

    @Test
    fun `Taps nach Spielende veraendern nichts`() {
        session.start(3, startMillis = 0L)
        var snap = session.snapshot(0L)
        repeat(3) { snap = session.tap(wrongValueFor(snap), 100L * (it + 1)) }
        val finishedBoard = snap.board
        val finishedTarget = snap.target

        // Falscher Tap nach Ende
        val afterWrong = session.tap(wrongValueFor(snap), 50_000L)
        assertEquals(3, afterWrong.misses)
        assertEquals(0, afterWrong.score)
        assertEquals(finishedBoard, afterWrong.board)
        assertEquals(finishedTarget, afterWrong.target)
        assertEquals(snap.elapsedMillis, afterWrong.elapsedMillis)

        // Selbst ein "Treffer" nach Ende zählt nicht.
        val afterRight = session.tap(finishedTarget, 60_000L)
        assertEquals(0, afterRight.score)
        assertTrue(afterRight.finished)
    }

    @Test
    fun `restorte Session setzt exakt an gespeichertem Stand fort`() {
        session.start(4, startMillis = 0L)
        val s0 = session.snapshot(0L)
        session.tap(s0.target, 1_000L)          // +1 Treffer, neues Feld
        val s1 = session.snapshot(1_000L)
        session.tap(wrongValueFor(s1), 2_000L)  // 1 Fehlversuch
        val saved = session.snapshot(2_000L)

        val restored = GameSession(BoardGenerator(Random(11L)))
        restored.restore(saved, startMillis = 3_000L - saved.elapsedMillis)

        val r = restored.snapshot(3_000L)
        assertEquals(saved.board, r.board)
        assertEquals(saved.target, r.target)
        assertEquals(1, r.score)
        assertEquals(1, r.misses)
        assertEquals(2_000L, r.elapsedMillis)
        assertFalse(r.finished)
        assertEquals(4, restored.boardSize)

        // Läuft weiter: ein Treffer zählt danach wieder.
        val afterTap = restored.tap(r.target, 5_000L)
        assertEquals(2, afterTap.score)
        assertEquals(4_000L, afterTap.elapsedMillis)
    }

    @Test
    fun `restorte beendete Session bleibt beendet und eingefroren`() {
        session.start(3, startMillis = 0L)
        var snap = session.snapshot(0L)
        repeat(3) { snap = session.tap(wrongValueFor(snap), 10L * (it + 1)) }

        val restored = GameSession(BoardGenerator(Random(13L)))
        restored.restore(snap, startMillis = 0L)

        val r = restored.snapshot(500_000L)
        assertTrue(r.finished)
        assertEquals(snap.elapsedMillis, r.elapsedMillis)
        restored.tap(r.target, 600_000L)
        assertEquals(snap.score, restored.snapshot(700_000L).score)
    }

    /** Ein Wert, der garantiert ungleich dem aktuellen Ziel ist. */
    private fun wrongValueFor(snapshot: GameSession.GameSnapshot): Int =
        snapshot.board.first { it != snapshot.target }
}
