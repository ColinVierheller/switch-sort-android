package de.dhbw.switchsort.game

/**
 * Hält den Spielzustand einer laufenden Runde. Die Klasse ist zeitlos
 * modelliert: alle Zeitangaben kommen als Parameter herein (die App liefert
 * monotone Zeit via SystemClock.elapsedRealtime), damit die Logik auf der JVM
 * vollständig testbar ist.
 */
class GameSession(private val generator: BoardGenerator = BoardGenerator()) {

    companion object {
        const val MAX_MISSES = 3
    }

    /** Unveränderlicher Zustands-Schnappschuss einer Runde. */
    data class GameSnapshot(
        val board: List<Int>,
        val target: Int,
        val score: Int,
        val misses: Int,
        val elapsedMillis: Long,
        val finished: Boolean
    )

    /** Feldgröße n der aktuellen Runde (3, 4 oder 5); 0 vor dem ersten Start. */
    var boardSize: Int = 0
        private set

    private var board: List<Int> = emptyList()
    private var target: Int = 0
    private var score: Int = 0
    private var misses: Int = 0
    private var startMillis: Long = 0L
    private var started: Boolean = false

    /** Eingefrorene Endzeit; nicht null genau dann, wenn die Runde beendet ist. */
    private var finalElapsedMillis: Long? = null

    val isStarted: Boolean get() = started
    val isFinished: Boolean get() = finalElapsedMillis != null

    /**
     * Startet eine neue Runde mit [boardSize] (3, 4 oder 5).
     *
     * @throws IllegalArgumentException bei ungültiger Feldgröße.
     */
    fun start(boardSize: Int, startMillis: Long = 0L) {
        val generated = generator.generate(boardSize) // validiert boardSize
        this.boardSize = boardSize
        this.board = generated.numbers
        this.target = generated.target
        this.score = 0
        this.misses = 0
        this.startMillis = startMillis
        this.started = true
        this.finalElapsedMillis = null
    }

    /**
     * Stellt eine Runde aus einem gespeicherten Schnappschuss wieder her
     * (z. B. nach Rotation der Activity). [startMillis] muss so gewählt werden,
     * dass die verstrichene Zeit nahtlos weiterläuft, also
     * `jetzt - snapshot.elapsedMillis`. Bei einer beendeten Runde bleibt die
     * eingefrorene Zeit aus dem Schnappschuss bestehen.
     */
    fun restore(snapshot: GameSnapshot, startMillis: Long) {
        val size = boardSizeOf(snapshot.board)
        this.boardSize = size
        this.board = snapshot.board.toList()
        this.target = snapshot.target
        this.score = snapshot.score
        this.misses = snapshot.misses
        this.startMillis = startMillis
        this.started = true
        this.finalElapsedMillis = if (snapshot.finished) snapshot.elapsedMillis else null
    }

    /**
     * Verarbeitet einen Tipp auf [value] zum Zeitpunkt [nowMillis].
     * Treffer erhöhen den Score und erzeugen sofort ein neues Feld mit neuem
     * Ziel; Fehlversuche behalten Feld und Ziel. Nach dem dritten Fehlversuch
     * ist die Runde beendet und die Zeit eingefroren; weitere Tipps sind
     * wirkungslos.
     */
    fun tap(value: Int, nowMillis: Long): GameSnapshot {
        check(started) { "Keine laufende Runde – erst start() aufrufen." }
        if (isFinished) return snapshot(nowMillis)

        if (value == target) {
            score++
            val generated = generator.generate(boardSize)
            board = generated.numbers
            target = generated.target
        } else {
            misses++
            if (misses >= MAX_MISSES) {
                finalElapsedMillis = elapsedAt(nowMillis)
            }
        }
        return snapshot(nowMillis)
    }

    /** Liefert den aktuellen Zustand zum Zeitpunkt [nowMillis]. */
    fun snapshot(nowMillis: Long): GameSnapshot {
        check(started) { "Keine laufende Runde – erst start() aufrufen." }
        return GameSnapshot(
            board = board.toList(),
            target = target,
            score = score,
            misses = misses,
            elapsedMillis = elapsedAt(nowMillis),
            finished = isFinished
        )
    }

    private fun elapsedAt(nowMillis: Long): Long =
        finalElapsedMillis ?: maxOf(0L, nowMillis - startMillis)

    private fun boardSizeOf(board: List<Int>): Int {
        val n = when (board.size) {
            9 -> 3
            16 -> 4
            25 -> 5
            else -> throw IllegalArgumentException(
                "Ungültiges Brett mit ${board.size} Feldern; erwartet 9, 16 oder 25."
            )
        }
        return n
    }
}
