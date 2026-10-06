package de.dhbw.switchsort.score

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsNormalizerTest {

    @Test
    fun `normaler Name bleibt erhalten`() {
        val result = SettingsNormalizer.validateNewPlayerName("Dominik")
        assertEquals(SettingsNormalizer.NameValidation.Valid("Dominik"), result)
    }

    @Test
    fun `fuehrende und nachfolgende Leerzeichen werden getrimmt`() {
        val result = SettingsNormalizer.validateNewPlayerName("  Anna  ")
        assertEquals(SettingsNormalizer.NameValidation.Valid("Anna"), result)
    }

    @Test
    fun `leerer Name wird zu Gast`() {
        assertEquals(SettingsNormalizer.NameValidation.Valid("Gast"),
            SettingsNormalizer.validateNewPlayerName(""))
        assertEquals(SettingsNormalizer.NameValidation.Valid("Gast"),
            SettingsNormalizer.validateNewPlayerName("   "))
    }

    @Test
    fun `Name mit genau 24 Zeichen ist gueltig`() {
        val name24 = "a".repeat(24)
        assertEquals(SettingsNormalizer.NameValidation.Valid(name24),
            SettingsNormalizer.validateNewPlayerName(name24))
    }

    @Test
    fun `Name mit 25 Zeichen ist ungueltig`() {
        val name25 = "a".repeat(25)
        assertTrue(SettingsNormalizer.validateNewPlayerName(name25)
            is SettingsNormalizer.NameValidation.TooLong)
    }

    @Test
    fun `Leerzeichen zaehlen nach Trimmen nicht zur Laengenpruefung`() {
        // 24 fachs + drumherum Leerzeichen -> nach Trim genau 24 -> gueltig
        val result = SettingsNormalizer.validateNewPlayerName("  " + "b".repeat(24) + "  ")
        assertEquals(SettingsNormalizer.NameValidation.Valid("b".repeat(24)), result)
    }

    @Test
    fun `gespeicherter leerer Wert faellt auf Gast zurueck`() {
        assertEquals("Gast", SettingsNormalizer.normalizeStoredPlayerName(null))
        assertEquals("Gast", SettingsNormalizer.normalizeStoredPlayerName(""))
        assertEquals("Gast", SettingsNormalizer.normalizeStoredPlayerName("   "))
    }

    @Test
    fun `gespeicherter zu langer Wert faellt auf Gast zurueck`() {
        assertEquals("Gast", SettingsNormalizer.normalizeStoredPlayerName("x".repeat(25)))
    }

    @Test
    fun `gespeicherter gueltiger Wert wird getrimmt uebernommen`() {
        assertEquals("Lena", SettingsNormalizer.normalizeStoredPlayerName(" Lena "))
    }

    @Test
    fun `gueltige Feldgroessen bleiben erhalten`() {
        assertEquals(3, SettingsNormalizer.normalizeBoardSize(3))
        assertEquals(4, SettingsNormalizer.normalizeBoardSize(4))
        assertEquals(5, SettingsNormalizer.normalizeBoardSize(5))
    }

    @Test
    fun `ungueltige Feldgroessen fallen auf 3 zurueck`() {
        assertEquals(3, SettingsNormalizer.normalizeBoardSize(2))
        assertEquals(3, SettingsNormalizer.normalizeBoardSize(6))
        assertEquals(3, SettingsNormalizer.normalizeBoardSize(0))
        assertEquals(3, SettingsNormalizer.normalizeBoardSize(null))
    }
}
