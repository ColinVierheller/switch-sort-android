package de.dhbw.switchsort.game

import kotlin.random.Random

/**
 * Erzeugt zufällige Spielfelder. Die Zufallsquelle ist injizierbar, damit
 * Tests deterministisch laufen (gleicher Seed → gleiches Ergebnis).
 */
class BoardGenerator(internal val random: Random = Random.Default) {

    /** Ein komplettes Spielfeld: gemischte Zahlen 1..n² plus Zielzahl. */
    data class Board(
        val size: Int,
        val numbers: List<Int>,
        val target: Int
    )

    val validSizes: Set<Int> = setOf(3, 4, 5)

    /**
     * Erzeugt ein gemischtes Feld mit den eindeutigen Zahlen 1..n² und wählt
     * die Zielzahl zufällig aus dem sichtbaren Feld.
     *
     * @throws IllegalArgumentException falls n nicht in {3, 4, 5} liegt.
     */
    fun generate(n: Int): Board {
        require(n in validSizes) { "Feldgröße muss 3, 4 oder 5 sein, war: $n" }
        val numbers = (1..n * n).toMutableList().apply { shuffle(random) }
        val target = numbers[random.nextInt(numbers.size)]
        return Board(size = n, numbers = numbers.toList(), target = target)
    }
}
