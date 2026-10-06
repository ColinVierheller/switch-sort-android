package de.dhbw.switchsort.score

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Speichert bis zu 10 Highscore-Einträge lokal in privaten SharedPreferences
 * (Datei "switchsort_highscores", ein Schlüssel [KEY_ENTRIES]).
 *
 * Serialisierung: Java-Serializable über [ObjectOutputStream]/[ObjectInputStream],
 * Base64-kodiert. Die Daten stammen ausschließlich aus dieser App selbst
 * (private Datei, keine externe Quelle); defekte oder fremde Inhalte werden
 * verworfen und der Schlüssel bereinigt.
 */
class HighScoreRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "switchsort_highscores"
        const val KEY_ENTRIES = "entries"
    }

    /**
     * Lädt die sortierten Top-10-Einträge. Beschädigte, manipulierte oder
     * inkompatible Daten (falsche Klassen, kaputtes Base64/Serialisierungsstrom,
     * abgelehnter Lesezugriff) werden verworfen und der Schlüssel entfernt;
     * das Ergebnis ist dann eine leere Liste statt eines Absturzes.
     */
    fun load(): List<HighScoreEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            decode(raw)
        } catch (e: ClassCastException) {
            clearCorrupted()
        } catch (e: ClassNotFoundException) {
            clearCorrupted()
        } catch (e: IOException) {
            clearCorrupted()
        } catch (e: SecurityException) {
            clearCorrupted()
        } catch (e: IllegalArgumentException) {
            // u. a. ungültiges Base64
            clearCorrupted()
        }
    }

    /**
     * Fügt einen Eintrag hinzu, sortiert, kürzt auf die Top 10 und persistiert
     * das Ergebnis. Liefert die neue Rangliste (zur sofortigen Anzeige).
     */
    fun add(entry: HighScoreEntry): List<HighScoreEntry> {
        val updated = HighScoreRanking.insert(load(), entry)
        persist(updated)
        return updated
    }

    private fun persist(entries: List<HighScoreEntry>) {
        prefs.edit().putString(KEY_ENTRIES, encode(entries)).apply()
    }

    private fun encode(entries: List<HighScoreEntry>): String {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { oos ->
            oos.writeObject(ArrayList(entries))
        }
        return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    }

    private fun decode(raw: String): List<HighScoreEntry> {
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        ObjectInputStream(ByteArrayInputStream(bytes)).use { ois ->
            val obj = ois.readObject()
            if (obj !is List<*>) {
                throw ClassCastException("Unerwarteter Typ: ${obj?.javaClass?.name}")
            }
            // Wirft ClassCastException bei Fremdtypen → wird oben bereinigt.
            return obj.map { it as HighScoreEntry }
        }
    }

    private fun clearCorrupted(): List<HighScoreEntry> {
        prefs.edit().remove(KEY_ENTRIES).apply()
        return emptyList()
    }
}
