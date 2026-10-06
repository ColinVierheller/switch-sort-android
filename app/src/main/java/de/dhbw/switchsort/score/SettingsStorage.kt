package de.dhbw.switchsort.score

/**
 * Minimale Storage-Abstraktion für Spieleinstellungen. Die Android-Umsetzung
 * liegt in [AppSettingsRepository] (SharedPreferences); die reine Logik bleibt
 * in [SettingsNormalizer] JVM-testbar.
 */
interface SettingsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
}
