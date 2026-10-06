package de.dhbw.switchsort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardGeneratorTest {

    private fun assertValidBoard(board: BoardGenerator.Board, n: Int) {
        assertEquals(n, board.size)
        assertEquals(n * n, board.numbers.size)
        assertEquals((1..n * n).toList(), board.numbers.sorted())
        assertEquals(n * n, board.numbers.distinct().size)
        assertTrue("Ziel muss im Feld enthalten sein", board.target in board.numbers)
    }

    @Test
    fun `Feld 3x3 enthaelt genau die Zahlen 1 bis 9`() {
        assertValidBoard(BoardGenerator(Random(1L)).generate(3), 3)
    }

    @Test
    fun `Feld 4x4 enthaelt genau die Zahlen 1 bis 16`() {
        assertValidBoard(BoardGenerator(Random(2L)).generate(4), 4)
    }

    @Test
    fun `Feld 5x5 enthaelt genau die Zahlen 1 bis 25`() {
        assertValidBoard(BoardGenerator(Random(3L)).generate(5), 5)
    }

    @Test
    fun `gleicher Seed erzeugt gleiches Feld und gleiches Ziel`() {
        val first = BoardGenerator(Random(42L)).generate(4)
        val second = BoardGenerator(Random(42L)).generate(4)
        assertEquals(first, second)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Feldgroesse 2 wird abgelehnt`() {
        BoardGenerator(Random(0L)).generate(2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Feldgroesse 6 wird abgelehnt`() {
        BoardGenerator(Random(0L)).generate(6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Feldgroesse 0 wird abgelehnt`() {
        BoardGenerator(Random(0L)).generate(0)
    }
}
