package de.dhbw.switchsort.score

/**
 * Reine, JVM-testbare Normalisierungs- und Validierungslogik für die
 * Spieleinstellungen. Kein Android-Bezug; die SharedPreferences-Anbindung
 * erfolgt in [AppSettingsRepository].
 */
object SettingsNormalizer {

    const val DEFAULT_PLAYER_NAME = "Gast"
    const val MAX_PLAYER_NAME_LENGTH = 24
    const val DEFAULT_BOARD_SIZE = 3
    val VALID_BOARD_SIZES = setOf(3, 4, 5)

    /** Darstellungsmodus: 0 = System, 1 = Hell, 2 = Dunkel. */
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    const val DEFAULT_THEME_MODE = THEME_SYSTEM
    val VALID_THEME_MODES = setOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK)

    /** Ergebnis der Validierung einer neuen UI-Eingabe. */
    sealed interface NameValidation {
        /** Gültiger, getrimmter Name (leer/blank wurde zu [DEFAULT_PLAYER_NAME] ersetzt). */
        data class Valid(val name: String) : NameValidation

        /** Eingabe ist nach Trimmen länger als [MAX_PLAYER_NAME_LENGTH] Zeichen. */
        data object TooLong : NameValidation
    }

    /**
     * Validiert eine neue Eingabe aus dem Optionsdialog. Leere/blank Eingaben
     * werden auf [DEFAULT_PLAYER_NAME] abgebildet; zu lange Namen werden als
     * [NameValidation.TooLong] zurückgewiesen und dürfen nicht still gespeichert
     * werden.
     */
    fun validateNewPlayerName(input: String?): NameValidation {
        val trimmed = input.orEmpty().trim()
        return when {
            trimmed.isEmpty() -> NameValidation.Valid(DEFAULT_PLAYER_NAME)
            trimmed.length > MAX_PLAYER_NAME_LENGTH -> NameValidation.TooLong
            else -> NameValidation.Valid(trimmed)
        }
    }

    /**
     * Normalisiert einen bereits persistierten Wert (ggf. aus einer alten
     * App-Version). Ungültige Werte (leer, zu lang) fallen ohne Fehlermeldung
     * auf [DEFAULT_PLAYER_NAME] zurück.
     */
    fun normalizeStoredPlayerName(raw: String?): String {
        return when (val result = validateNewPlayerName(raw)) {
            is NameValidation.Valid -> result.name
            NameValidation.TooLong -> DEFAULT_PLAYER_NAME
        }
    }

    /**
     * Normalisiert eine gespeicherte oder gewählte Feldgröße. Nur 3, 4 und 5
     * sind erlaubt; alles andere fällt auf [DEFAULT_BOARD_SIZE] zurück.
     */
    fun normalizeBoardSize(value: Int?): Int =
        if (value != null && value in VALID_BOARD_SIZES) value else DEFAULT_BOARD_SIZE

    /**
     * Normalisiert einen gespeicherten oder gewählten Darstellungsmodus.
     * Nur System/Hell/Dunkel sind erlaubt; alles andere fällt auf
     * [DEFAULT_THEME_MODE] zurück.
     */
    fun normalizeThemeMode(value: Int?): Int =
        if (value != null && value in VALID_THEME_MODES) value else DEFAULT_THEME_MODE
}
