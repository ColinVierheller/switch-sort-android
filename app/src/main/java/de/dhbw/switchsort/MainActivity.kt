package de.dhbw.switchsort

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import de.dhbw.switchsort.game.BoardGenerator
import de.dhbw.switchsort.game.GameSession
import de.dhbw.switchsort.score.AppSettingsRepository
import de.dhbw.switchsort.score.HighScoreEntry
import de.dhbw.switchsort.score.HighScoreRepository
import de.dhbw.switchsort.score.SettingsNormalizer
import de.dhbw.switchsort.ui.BlockCellView
import de.dhbw.switchsort.ui.BoardView
import de.dhbw.switchsort.ui.GradientBackground
import de.dhbw.switchsort.ui.MissDotsView
import de.dhbw.switchsort.ui.Palette
import de.dhbw.switchsort.ui.ScoreListRenderer
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

    /** Ob die beendete Runde Platz 1 erreicht hat (rotiert mit). */
    private var newRecord = false

    /** Aktuell gezeichnetes Feld; neu aufgebaut wird nur bei Änderung. */
    private var displayedBoard: List<Int> = emptyList()

    private lateinit var background: GradientBackground

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
    private lateinit var missDots: MissDotsView
    private lateinit var textTime: TextView
    private lateinit var boardView: BoardView
    private lateinit var gameContent: View
    private lateinit var overlayGameOver: View
    private lateinit var textFinalScore: TextView
    private lateinit var textFinalTime: TextView
    private lateinit var textNewRecord: TextView
    private lateinit var pillButtons: List<Button>

    // Options-Views
    private lateinit var editPlayerName: EditText
    private lateinit var radioBoardSize: RadioGroup
    private lateinit var radioTheme: RadioGroup
    private lateinit var textOptionsError: TextView

    /** Modus beim Öffnen der Optionen; Speichern -> recreate() nur bei Änderung. */
    private var themeModeAtOpen = SettingsNormalizer.DEFAULT_THEME_MODE

    private lateinit var textScoresEmpty: TextView
    private lateinit var scrollScores: View
    private lateinit var listScores: LinearLayout

    /**
     * Manueller Hell/Dunkel-Modus ohne AppCompat: bei Hell/Dunkel wird der
     * Basis-Kontext mit überschriebener uiMode-Konfiguration gewrappt, sodass
     * die Ressourcen aus values-night auflösen. Bei System bleibt der
     * Basis-Kontext unverändert (der System-Nachtmodus greift selbst).
     */
    override fun attachBaseContext(newBase: Context) {
        when (AppSettingsRepository(newBase).loadThemeMode()) {
            SettingsNormalizer.THEME_LIGHT ->
                super.attachBaseContext(wrapWithNightMode(newBase, Configuration.UI_MODE_NIGHT_NO))
            SettingsNormalizer.THEME_DARK ->
                super.attachBaseContext(wrapWithNightMode(newBase, Configuration.UI_MODE_NIGHT_YES))
            else -> super.attachBaseContext(newBase)
        }
    }

    private fun wrapWithNightMode(base: Context, nightMode: Int): Context {
        val config = Configuration(base.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        return base.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = AppSettingsRepository(this)
        highScores = HighScoreRepository(this)

        bindViews()
        setUpBackground(savedInstanceState?.getFloat(KEY_HUE, Palette.START_HUE) ?: Palette.START_HUE)
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
        missDots = findViewById(R.id.misses_dots)
        textTime = findViewById(R.id.text_time)
        boardView = findViewById(R.id.board_view)
        gameContent = findViewById(R.id.game_content)
        overlayGameOver = findViewById(R.id.overlay_game_over)
        textFinalScore = findViewById(R.id.text_final_score)
        textFinalTime = findViewById(R.id.text_final_time)
        textNewRecord = findViewById(R.id.text_new_record)
        pillButtons = listOf(
            findViewById(R.id.btn_start),
            findViewById(R.id.btn_options_save),
            findViewById(R.id.btn_restart)
        )

        editPlayerName = findViewById(R.id.edit_player_name)
        radioBoardSize = findViewById(R.id.radio_board_size)
        radioTheme = findViewById(R.id.radio_theme)
        textOptionsError = findViewById(R.id.text_options_error)

        textScoresEmpty = findViewById(R.id.text_scores)
        scrollScores = findViewById(R.id.scroll_scores)
        listScores = findViewById(R.id.list_scores)
    }

    private fun setUpBackground(hue: Float) {
        val night = resources.getBoolean(R.bool.is_night)
        background = GradientBackground(night, hue)
        background.onHueChanged = { h ->
            val accent = Palette.accent(h, night)
            pillButtons.forEach { it.setTextColor(accent) }
            textNewRecord.setTextColor(Palette.highlight(h, night))
        }
        background.applyHue(hue)
        window.setBackgroundDrawable(background.drawable)
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
        boardView.onCellTap = { cell -> onCellTap(cell) }
        findViewById<Button>(R.id.btn_restart).setOnClickListener { startGame() }
        val leave = View.OnClickListener {
            // Abbruch: Runde verworfen, nichts wird gespeichert.
            roundActive = false
            stopTicker()
            showPanel(Panel.MENU)
        }
        findViewById<Button>(R.id.btn_game_back).setOnClickListener(leave)
        findViewById<View>(R.id.btn_game_close).setOnClickListener(leave)
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
        if (panel == Panel.GAME) {
            background.stopIdle()
        } else {
            stopTicker()
            cancelGameAnimations()
            background.startIdle()
        }
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
        themeModeAtOpen = settings.loadThemeMode()
        when (themeModeAtOpen) {
            SettingsNormalizer.THEME_LIGHT -> radioTheme.check(R.id.radio_theme_light)
            SettingsNormalizer.THEME_DARK -> radioTheme.check(R.id.radio_theme_dark)
            else -> radioTheme.check(R.id.radio_theme_system)
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
        val themeMode = when (radioTheme.checkedRadioButtonId) {
            R.id.radio_theme_light -> SettingsNormalizer.THEME_LIGHT
            R.id.radio_theme_dark -> SettingsNormalizer.THEME_DARK
            else -> SettingsNormalizer.THEME_SYSTEM
        }
        settings.saveThemeMode(themeMode)
        Toast.makeText(this, R.string.options_saved, Toast.LENGTH_SHORT).show()
        if (themeMode != themeModeAtOpen) {
            // Nachtmodus-Override greift in attachBaseContext: nur dort wird
            // ein geänderter Modus aktiv, daher Neuaufbau der Activity.
            recreate()
        } else {
            showPanel(Panel.MENU)
        }
    }

    // ------------------------------------------------------------------
    // Highscore
    // ------------------------------------------------------------------

    private fun showScores() {
        val entries = highScores.load()
        ScoreListRenderer.render(listScores, entries, ::formatTime)
        textScoresEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        scrollScores.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
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
        newRecord = false
        displayedBoard = emptyList()
        cancelGameAnimations()
        overlayGameOver.visibility = View.GONE
        renderGame(session.snapshot(SystemClock.elapsedRealtime()), animateBoard = true)
        showPanel(Panel.GAME)
        startTicker()
    }

    private fun onCellTap(cell: BlockCellView) {
        if (!roundActive || session.isFinished) return
        val now = SystemClock.elapsedRealtime()
        val scoreBefore = session.snapshot(now).score
        val snapshot = session.tap(cell.value, now)
        cell.playTap()
        if (snapshot.score > scoreBefore) {
            background.shiftHue(Palette.HUE_STEP)
            popTarget()
            renderGame(snapshot, animateBoard = true)
        } else {
            cell.playError()
            renderGame(snapshot, animateBoard = false)
        }
        if (snapshot.finished) {
            onRoundFinished(snapshot)
        }
    }

    private fun onRoundFinished(snapshot: GameSession.GameSnapshot) {
        roundActive = false
        stopTicker()
        boardView.setCellsEnabled(false)
        if (!savedThisRound) {
            val entry = HighScoreEntry(
                playerName = settings.loadPlayerName(),
                boardSize = session.boardSize,
                score = snapshot.score,
                durationMillis = snapshot.elapsedMillis,
                finishedAtMillis = System.currentTimeMillis()
            )
            newRecord = highScores.add(entry).firstOrNull() == entry
            savedThisRound = true
        }
        showGameOver(snapshot, animate = true)
    }

    private fun showGameOver(snapshot: GameSession.GameSnapshot, animate: Boolean) {
        textFinalScore.text = getString(R.string.number, snapshot.score)
        textFinalTime.text = getString(R.string.game_over_time, formatTime(snapshot.elapsedMillis))
        textNewRecord.visibility = if (newRecord) View.VISIBLE else View.GONE
        cancelGameAnimations()
        overlayGameOver.visibility = View.VISIBLE
        // Spielfeld blendet aus statt abgedunkelt zu werden: am Ende bleibt
        // nur der Verlauf mit dem Ergebnis, wie ein eigener ruhiger Screen.
        if (animate) {
            overlayGameOver.alpha = 0f
            overlayGameOver.animate().alpha(1f)
                .setStartDelay(OVERLAY_DELAY_MS)
                .setDuration(OVERLAY_FADE_MS)
                .start()
            gameContent.animate().alpha(0f)
                .setStartDelay(OVERLAY_DELAY_MS)
                .setDuration(OVERLAY_FADE_MS)
                .start()
        } else {
            overlayGameOver.alpha = 1f
            gameContent.alpha = 0f
        }
    }

    private fun popTarget() {
        textTarget.animate().cancel()
        textTarget.scaleX = POP_SCALE
        textTarget.scaleY = POP_SCALE
        textTarget.animate().scaleX(1f).scaleY(1f)
            .setDuration(POP_MS)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun cancelGameAnimations() {
        textTarget.animate().cancel()
        textTarget.scaleX = 1f
        textTarget.scaleY = 1f
        overlayGameOver.animate().cancel()
        overlayGameOver.alpha = 1f
        gameContent.animate().cancel()
        gameContent.alpha = 1f
    }

    private fun renderGame(snapshot: GameSession.GameSnapshot, animateBoard: Boolean) {
        textTarget.text = getString(R.string.game_target, snapshot.target)
        textScore.text = getString(R.string.game_score, snapshot.score)
        missDots.setMisses(snapshot.misses)
        updateTime(snapshot.elapsedMillis)
        if (snapshot.board != displayedBoard) {
            boardView.setBoard(snapshot.board, session.boardSize, animateBoard)
            displayedBoard = snapshot.board
        }
        boardView.setCellsEnabled(roundActive)
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
        outState.putFloat(KEY_HUE, background.hue)
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
            outState.putBoolean(KEY_NEW_RECORD, newRecord)
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
        newRecord = state.getBoolean(KEY_NEW_RECORD, false)
        roundActive = !snapshot.finished

        renderGame(session.snapshot(now), animateBoard = false)
        showPanel(Panel.GAME)
        if (snapshot.finished) {
            // Wiederherstellung nach Ende: Anzeige ohne erneutes Speichern.
            showGameOver(snapshot, animate = false)
        }
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

    override fun onDestroy() {
        stopTicker()
        background.cancel()
        background.onHueChanged = null
        cancelGameAnimations()
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 1_000L
        private const val POP_SCALE = 1.15f
        private const val POP_MS = 280L
        private const val OVERLAY_DELAY_MS = 250L
        private const val OVERLAY_FADE_MS = 300L

        private const val KEY_BOARD = "switchsort.board"
        private const val KEY_TARGET = "switchsort.target"
        private const val KEY_SCORE = "switchsort.score"
        private const val KEY_MISSES = "switchsort.misses"
        private const val KEY_BOARD_SIZE = "switchsort.board_size"
        private const val KEY_ELAPSED = "switchsort.elapsed"
        private const val KEY_FINISHED = "switchsort.finished"
        private const val KEY_SAVED = "switchsort.saved"
        private const val KEY_NEW_RECORD = "switchsort.new_record"
        private const val KEY_HUE = "switchsort.hue"
    }
}
