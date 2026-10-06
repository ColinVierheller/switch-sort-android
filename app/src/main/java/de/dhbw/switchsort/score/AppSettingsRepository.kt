package de.dhbw.switchsort.score

import android.content.Context

/**
 * Android-Anbindung der Spieleinstellungen über private SharedPreferences
 * (Datei "switchsort_settings"). Die gesamte Normalisierungslogik liegt in
 * [SettingsNormalizer] und ist JVM-testbar; hier steckt nur die Persistenz.
 */
class AppSettingsRepository(context: Context) : SettingsStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        /** Privater Named-Preferences-Dateiname. */
        const val PREFS_NAME = "switchsort_settings"

        /** Schlüssel: Spielername (String). */
        const val KEY_PLAYER_NAME = "player_name"

        /** Schlüssel: Feldgröße 3/4/5 (Int). */
        const val KEY_BOARD_SIZE = "board_size"
    }

    /** Geladener, normalisierter Spielername; ungültige Alt-Werte → "Gast". */
    fun loadPlayerName(): String =
        SettingsNormalizer.normalizeStoredPlayerName(getString(KEY_PLAYER_NAME))

    /** Geladene, normalisierte Feldgröße; ungültige Alt-Werte → 3. */
    fun loadBoardSize(): Int =
        SettingsNormalizer.normalizeBoardSize(getInt(KEY_BOARD_SIZE))

    /**
     * Validiert und speichert den Spielernamen. Liefert das
     * Validierungsergebnis; bei [SettingsNormalizer.NameValidation.TooLong]
     * wird nichts geschrieben, damit die UI einen Fehler anzeigen kann.
     */
    fun savePlayerName(input: String): SettingsNormalizer.NameValidation {
        val result = SettingsNormalizer.validateNewPlayerName(input)
        if (result is SettingsNormalizer.NameValidation.Valid) {
            putString(KEY_PLAYER_NAME, result.name)
        }
        return result
    }

    /** Speichert die normalisierte Feldgröße. */
    fun saveBoardSize(size: Int) {
        putInt(KEY_BOARD_SIZE, SettingsNormalizer.normalizeBoardSize(size))
    }

    // SettingsStorage -------------------------------------------------------

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getInt(key: String): Int? =
        if (prefs.contains(key)) prefs.getInt(key, SettingsNormalizer.DEFAULT_BOARD_SIZE) else null

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
}
