package de.dhbw.switchsort

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import de.dhbw.switchsort.game.BoardGenerator
import de.dhbw.switchsort.game.GameSession
import de.dhbw.switchsort.score.AppSettingsRepository
import de.dhbw.switchsort.score.HighScoreEntry
import de.dhbw.switchsort.score.HighScoreRepository
import de.dhbw.switchsort.score.SettingsNormalizer
import java.util.Locale

/**
 * Einzige Activity der App mit vier Panels (Menü, Optionen, Highscore, Spiel).
 * Klassische Android Views ohne AndroidX/Compose.
 *
 * Zeitmessung: monotone SystemClock.elapsedRealtime()-Werte werden in die
 * JVM-testbare [GameSession] gegeben. Rotation: der komplette Rundenstand
 * wird in onSaveInstanceState gesichert und per [GameSession.restore]
 * wiederhergestellt.
 */
class MainActivity : Activity() {

    private enum class Panel { MENU, OPTIONS, GAME, SCORES }

    private lateinit var settings: AppSettingsRepository
    private lateinit var highScores: HighScoreRepository
    private var session: GameSession = GameSession(BoardGenerator())

    private var currentPanel = Panel.MENU

    /** true, solange eine Runde läuft (für Tippen und Timer). */
    private var roundActive = false

    /** Schutz gegen doppeltes Speichern eines Ergebnisses (auch über Rotation). */
    private var savedThisRound = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (roundActive && session.isStarted && !session.isFinished) {
                updateTime(session.snapshot(SystemClock.elapsedRealtime()).elapsedMillis)
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    // Panels
    private lateinit var panelMenu: View
    private lateinit var panelOptions: View
    private lateinit var panelScores: View
    private lateinit var panelGame: View

    // Spiel-Views
    private lateinit var textTarget: TextView
    private lateinit var textScore: TextView
    private lateinit var textMisses: TextView
    private lateinit var textTime: TextView
    private lateinit var textFinished: TextView
    private lateinit var gridBoard: GridLayout
    private lateinit var btnRestart: Button

    // Options-Views
    private lateinit var editPlayerName: EditText
    private lateinit var radioBoardSize: RadioGroup
    private lateinit var textOptionsError: TextView

    private lateinit var textScores: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = AppSettingsRepository(this)
        highScores = HighScoreRepository(this)

        bindViews()
        wireMenu()
        wireOptions()
        wireScores()
        wireGame()

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_BOARD)) {
            restoreRound(savedInstanceState)
        } else {
            showPanel(Panel.MENU)
        }
    }

    private fun bindViews() {
        panelMenu = findViewById(R.id.panel_menu)
        panelOptions = findViewById(R.id.panel_options)
        panelScores = findViewById(R.id.panel_scores)
        panelGame = findViewById(R.id.panel_game)

        textTarget = findViewById(R.id.text_target)
        textScore = findViewById(R.id.text_score)
        textMisses = findViewById(R.id.text_misses)
        textTime = findViewById(R.id.text_time)
        textFinished = findViewById(R.id.text_finished)
        gridBoard = findViewById(R.id.grid_board)
        btnRestart = findViewById(R.id.btn_restart)

        editPlayerName = findViewById(R.id.edit_player_name)
        radioBoardSize = findViewById(R.id.radio_board_size)
        textOptionsError = findViewById(R.id.text_options_error)

        textScores = findViewById(R.id.text_scores)
    }

    private fun wireMenu() {
        findViewById<Button>(R.id.btn_start).setOnClickListener { startGame() }
        findViewById<Button>(R.id.btn_scores).setOnClickListener { showScores() }
        findViewById<Button>(R.id.btn_options).setOnClickListener { showOptions() }
        findViewById<Button>(R.id.btn_exit).setOnClickListener { finish() }
    }

    private fun wireOptions() {
        findViewById<Button>(R.id.btn_options_save).setOnClickListener { saveOptions() }
        findViewById<Button>(R.id.btn_options_back).setOnClickListener { showPanel(Panel.MENU) }
    }

    private fun wireScores() {
        findViewById<Button>(R.id.btn_scores_back).setOnClickListener { showPanel(Panel.MENU) }
    }

    private fun wireGame() {
        btnRestart.setOnClickListener { startGame() }
        findViewById<Button>(R.id.btn_game_back).setOnClickListener {
            // Abbruch: Runde verworfen, nichts wird gespeichert.
            roundActive = false
            stopTicker()
            showPanel(Panel.MENU)
        }
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    private fun showPanel(panel: Panel) {
        currentPanel = panel
        panelMenu.visibility = if (panel == Panel.MENU) View.VISIBLE else View.GONE
        panelOptions.visibility = if (panel == Panel.OPTIONS) View.VISIBLE else View.GONE
        panelScores.visibility = if (panel == Panel.SCORES) View.VISIBLE else View.GONE
        panelGame.visibility = if (panel == Panel.GAME) View.VISIBLE else View.GONE
        if (panel != Panel.GAME) stopTicker()
        if (panel == Panel.GAME && roundActive && session.isStarted && !session.isFinished) {
            startTicker()
        }
    }

    override fun onBackPressed() {
        if (currentPanel != Panel.MENU) {
            // Zurück aus laufendem Spiel: Runde verworfen (nicht gespeichert).
            if (currentPanel == Panel.GAME) {
                roundActive = false
                stopTicker()
            }
            showPanel(Panel.MENU)
        } else {
            super.onBackPressed()
        }
    }

    // ------------------------------------------------------------------
    // Optionen
    // ------------------------------------------------------------------

    private fun showOptions() {
        editPlayerName.setText(settings.loadPlayerName())
        editPlayerName.setSelection(editPlayerName.text.length)
        textOptionsError.visibility = View.GONE
        when (settings.loadBoardSize()) {
            4 -> radioBoardSize.check(R.id.radio_board_4)
            5 -> radioBoardSize.check(R.id.radio_board_5)
            else -> radioBoardSize.check(R.id.radio_board_3)
        }
        showPanel(Panel.OPTIONS)
    }

    private fun saveOptions() {
        val nameResult = settings.savePlayerName(editPlayerName.text.toString())
        if (nameResult is SettingsNormalizer.NameValidation.TooLong) {
            textOptionsError.text = getString(R.string.options_error_name_too_long)
            textOptionsError.visibility = View.VISIBLE
            return
        }
        val size = when (radioBoardSize.checkedRadioButtonId) {
            R.id.radio_board_4 -> 4
            R.id.radio_board_5 -> 5
            else -> 3
        }
        settings.saveBoardSize(size)
        Toast.makeText(this, R.string.options_saved, Toast.LENGTH_SHORT).show()
        showPanel(Panel.MENU)
    }

    // ------------------------------------------------------------------
    // Highscore
    // ------------------------------------------------------------------

    private fun showScores() {
        val entries = highScores.load()
        textScores.text = if (entries.isEmpty()) {
            getString(R.string.scores_empty)
        } else {
            entries.mapIndexed { index, entry ->
                getString(
                    R.string.scores_entry,
                    index + 1,
                    entry.playerName,
                    entry.boardSize,
                    entry.score,
                    formatTime(entry.durationMillis)
                )
            }.joinToString("\n")
        }
        showPanel(Panel.SCORES)
    }

    // ------------------------------------------------------------------
    // Spiel
    // ------------------------------------------------------------------

    private fun startGame() {
        session = GameSession(BoardGenerator())
        session.start(settings.loadBoardSize(), SystemClock.elapsedRealtime())
        roundActive = true
        savedThisRound = false
        textFinished.visibility = View.GONE
        btnRestart.visibility = View.GONE
        renderGame(session.snapshot(SystemClock.elapsedRealtime()))
        showPanel(Panel.GAME)
        startTicker()
    }

    private fun onCellTap(value: Int) {
        if (!roundActive || session.isFinished) return
        val snapshot = session.tap(value, SystemClock.elapsedRealtime())
        renderGame(snapshot)
        if (snapshot.finished) {
            onRoundFinished(snapshot)
        }
    }

    private fun onRoundFinished(snapshot: GameSession.GameSnapshot) {
        roundActive = false
        stopTicker()
        textFinished.text = getString(
            R.string.game_finished, snapshot.score, formatTime(snapshot.elapsedMillis)
        )
        textFinished.visibility = View.VISIBLE
        btnRestart.visibility = View.VISIBLE
        setBoardEnabled(false)
        if (!savedThisRound) {
            highScores.add(
                HighScoreEntry(
                    playerName = settings.loadPlayerName(),
                    boardSize = session.boardSize,
                    score = snapshot.score,
                    durationMillis = snapshot.elapsedMillis,
                    finishedAtMillis = System.currentTimeMillis()
                )
            )
            savedThisRound = true
        }
    }

    private fun renderGame(snapshot: GameSession.GameSnapshot) {
        textTarget.text = getString(R.string.game_target, snapshot.target)
        textScore.text = getString(R.string.game_score, snapshot.score)
        textMisses.text = getString(R.string.game_misses, snapshot.misses, GameSession.MAX_MISSES)
        updateTime(snapshot.elapsedMillis)
        renderBoard(snapshot)
    }

    private fun renderBoard(snapshot: GameSession.GameSnapshot) {
        val n = session.boardSize
        gridBoard.removeAllViews()
        gridBoard.columnCount = n
        snapshot.board.forEach { value ->
            val button = Button(this).apply {
                text = value.toString()
                textSize = 18f
                isEnabled = roundActive
                setOnClickListener { onCellTap(value) }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                val margin = (2 * resources.displayMetrics.density).toInt()
                setMargins(margin, margin, margin, margin)
            }
            gridBoard.addView(button, params)
        }
    }

    private fun setBoardEnabled(enabled: Boolean) {
        for (i in 0 until gridBoard.childCount) {
            gridBoard.getChildAt(i).isEnabled = enabled
        }
    }

    private fun updateTime(elapsedMillis: Long) {
        textTime.text = getString(R.string.game_time, formatTime(elapsedMillis))
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, TICK_MS)
    }

    private fun stopTicker() {
        handler.removeCallbacks(ticker)
    }

    // ------------------------------------------------------------------
    // Rotation / Prozesswechsel
    // ------------------------------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (currentPanel == Panel.GAME && session.isStarted) {
            val snapshot = session.snapshot(SystemClock.elapsedRealtime())
            outState.putIntegerArrayList(KEY_BOARD, ArrayList(snapshot.board))
            outState.putInt(KEY_TARGET, snapshot.target)
            outState.putInt(KEY_SCORE, snapshot.score)
            outState.putInt(KEY_MISSES, snapshot.misses)
            outState.putInt(KEY_BOARD_SIZE, session.boardSize)
            outState.putLong(KEY_ELAPSED, snapshot.elapsedMillis)
            outState.putBoolean(KEY_FINISHED, snapshot.finished)
            outState.putBoolean(KEY_SAVED, savedThisRound)
        }
    }

    private fun restoreRound(state: Bundle) {
        val board = state.getIntegerArrayList(KEY_BOARD)?.map { it } ?: return showPanel(Panel.MENU)
        val elapsed = state.getLong(KEY_ELAPSED, 0L)
        val snapshot = GameSession.GameSnapshot(
            board = board,
            target = state.getInt(KEY_TARGET),
            score = state.getInt(KEY_SCORE),
            misses = state.getInt(KEY_MISSES),
            elapsedMillis = elapsed,
            finished = state.getBoolean(KEY_FINISHED)
        )
        val boardSize = state.getInt(KEY_BOARD_SIZE, SettingsNormalizer.DEFAULT_BOARD_SIZE)
        require(snapshot.board.size == boardSize * boardSize) { "Inkonsistenter Spielstand" }

        val now = SystemClock.elapsedRealtime()
        session = GameSession(BoardGenerator())
        session.restore(snapshot, startMillis = now - elapsed)
        savedThisRound = state.getBoolean(KEY_SAVED, false)
        roundActive = !snapshot.finished

        renderGame(session.snapshot(now))
        if (snapshot.finished) {
            // Wiederherstellung nach Ende: Anzeige ohne erneutes Speichern.
            textFinished.text = getString(
                R.string.game_finished, snapshot.score, formatTime(snapshot.elapsedMillis)
            )
            textFinished.visibility = View.VISIBLE
            btnRestart.visibility = View.VISIBLE
            setBoardEnabled(false)
        }
        showPanel(Panel.GAME)
    }

    override fun onStop() {
        super.onStop()
        stopTicker()
    }

    override fun onResume() {
        super.onResume()
        if (currentPanel == Panel.GAME && roundActive && session.isStarted && !session.isFinished) {
            startTicker()
        }
    }

    companion object {
        private const val TICK_MS = 1_000L

        private const val KEY_BOARD = "switchsort.board"
        private const val KEY_TARGET = "switchsort.target"
        private const val KEY_SCORE = "switchsort.score"
        private const val KEY_MISSES = "switchsort.misses"
        private const val KEY_BOARD_SIZE = "switchsort.board_size"
        private const val KEY_ELAPSED = "switchsort.elapsed"
        private const val KEY_FINISHED = "switchsort.finished"
        private const val KEY_SAVED = "switchsort.saved"
    }
}
