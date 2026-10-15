package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.sectionForLabel
import com.galaxyrio.gracelauncher.data.appSortKey
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSectionTest {
    @Test
    fun latinLabelsAreGroupedCaseInsensitively() {
        assertEquals("C", sectionForLabel("chrome"))
        assertEquals("N", sectionForLabel("  Notes"))
    }

    @Test
    fun accentedLabelsUseTheirLatinSection() {
        assertEquals("E", sectionForLabel("Éditeur"))
    }

    @Test
    fun numericAndEmptyLabelsUseFallbackSection() {
        assertEquals("#", sectionForLabel("1Password"))
        assertEquals("#", sectionForLabel(""))
    }

    @Test
    fun fullWidthLatinLabelsUseTheirLatinSection() {
        assertEquals("C", sectionForLabel("Ｃａｍｅｒａ"))
    }

    @Test
    fun russianLabelsUseFamiliarLatinSpellingsAndSections() {
        mapOf(
            "Яндекс" to "yandeks", "Юла" to "yula", "Чат" to "chat",
            "Ёлка" to "yolka", "Йога" to "yoga", "Журнал" to "zhurnal",
            "Хром" to "khrom", "Центр" to "tsentr", "Шахматы" to "shakhmaty",
            "Щит" to "shchit", "Объект" to "obekt", "Связь" to "svyaz",
        ).forEach { (label, key) ->
            assertEquals(label, key, appSortKey(label))
            assertEquals(label, key.first().uppercase(), sectionForLabel(label))
        }
    }

    @Test
    fun russianTransliterationHandlesCaseNormalizationAndMixedLabels() {
        assertEquals("yandeks", appSortKey("ЯНДЕКС"))
        assertEquals("yandeks", appSortKey("яндекс"))
        assertEquals("yolka", appSortKey("Е\u0308лка"))
        assertEquals("yandeksmusic", appSortKey("  Яндекс Music  "))
    }
}
