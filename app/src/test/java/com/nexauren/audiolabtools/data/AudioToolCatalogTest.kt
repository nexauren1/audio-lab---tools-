package com.nexauren.audiolabtools.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioToolCatalogTest {
    @Test
    fun catalogueContainsExactlyThirtyEightUniqueTools() {
        assertEquals(38, AudioToolCatalog.tools.size)
        assertEquals(
            AudioToolCatalog.tools.size,
            AudioToolCatalog.tools.map { it.id }.toSet().size
        )
    }

    @Test
    fun everyPublishedCatalogueEntryHasAProcessingOperation() {
        assertTrue(AudioToolCatalog.tools.all { it.isAvailable })
    }

    @Test
    fun coreAudioOperationsRemainInTheCatalogue() {
        val ids = AudioToolCatalog.tools.map { it.id }.toSet()
        listOf(
            "trim-audio",
            "join-audio",
            "convert-audio",
            "sample-rate",
            "normalize-wav",
            "amplify-wav",
            "remove-silence",
            "dynamic-compressor",
            "audio-info",
            "compare-audio",
            "edit-metadata",
            "audio-compress",
            "speed-audio",
            "nightcore",
            "pitch-audio"
        ).forEach { assertTrue("Missing tool: $it", it in ids) }
    }

    @Test
    fun appLocaleSelectionRecognizesEnglishAndPortuguese() {
        assertTrue(isPortuguese(Locale("pt", "PT")))
        assertFalse(isPortuguese(Locale.ENGLISH))
    }
}
