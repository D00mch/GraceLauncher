package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.LauncherSettings
import com.galaxyrio.gracelauncher.data.decodePaletteStyle
import com.galaxyrio.gracelauncher.data.launcherColorScheme
import com.materialkolor.PaletteStyle
import org.junit.Assert.*
import org.junit.Test

class ThemePaletteTest {
    @Test fun defaultsAndUnknownStoredStylesRemainTonalSpot() {
        assertEquals(PaletteStyle.TonalSpot, LauncherSettings().paletteStyle)
        assertEquals(PaletteStyle.TonalSpot, decodePaletteStyle("unknown"))
        PaletteStyle.entries.forEach { assertEquals(it, decodePaletteStyle(it.name)) }
    }

    @Test fun allSupportedStylesGenerateLightAndDarkSchemesAndChangeTheResult() {
        val seed = 0xFF6750A4.toInt()
        listOf(false, true).forEach { dark ->
            val schemes = PaletteStyle.entries.associateWith { launcherColorScheme(seed, dark, it) }
            assertEquals(9, schemes.size)
            val signatures = schemes.mapValues { (_, colors) ->
                listOf(colors.primary, colors.primaryContainer, colors.secondary, colors.tertiary, colors.surface)
            }
            assertEquals("Each style must change actual color roles, not just the scheme instance", 9, signatures.values.toSet().size)
            assertNotEquals(schemes[PaletteStyle.TonalSpot]!!.primary, schemes[PaletteStyle.Monochrome]!!.primary)
            assertNotEquals(schemes[PaletteStyle.TonalSpot]!!.primary, schemes[PaletteStyle.Expressive]!!.primary)
        }
    }
}
