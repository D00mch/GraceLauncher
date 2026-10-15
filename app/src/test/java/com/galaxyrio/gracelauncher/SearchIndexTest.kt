package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.SearchName
import com.galaxyrio.gracelauncher.data.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SearchIndexTest {
    @Test
    fun yandexMatchesBothLatinSpellingsAndPrefixes() {
        val name = SearchName("Яндекс")
        for (query in listOf("Yandex", "Yandeks", "YANDEX", "Яндекс")) {
            assertEquals(query, 0, name.score(SearchQuery(query, fuzzy = true)))
        }
        for (query in listOf("Ya", "Ян")) {
            assertEquals(query, 2, name.score(SearchQuery(query, fuzzy = true)))
        }
    }

    @Test
    fun russianDigraphsAreSearchableWithLatinOrCyrillicInput() {
        for ((label, latin) in listOf("Юла" to "Yula", "Чат" to "Chat", "Ёлка" to "Yolka")) {
            val name = SearchName(label)
            assertEquals(label, 0, name.score(SearchQuery(latin, fuzzy = true)))
            assertEquals(label, 2, name.score(SearchQuery(latin.take(2), fuzzy = true)))
            assertEquals(label, 0, name.score(SearchQuery(label, fuzzy = true)))
        }
    }

    @Test
    fun russianNamesRetainInitialsSubstringAndSubsequenceMatching() {
        val name = SearchName("Яндекс Музыка")
        assertEquals(12, name.score(SearchQuery("ym", fuzzy = true)))
        assertEquals(8, name.score(SearchQuery("Muzyka", fuzzy = true)))
        assertNotNull(name.score(SearchQuery("ydx", fuzzy = true)))
        assertEquals(2, name.score(SearchQuery("Yandex", fuzzy = true)))
    }

    @Test
    fun xAliasHandlesCyrillicCaseWithoutRewritingLatinNames() {
        assertEquals(0, SearchName("ЯНДЕКС").score(SearchQuery("yandex", fuzzy = true)))
        assertNull(SearchName("Folks").score(SearchQuery("Folx", fuzzy = true)))
        assertNull(SearchName("Яндекс").score(SearchQuery("Google", fuzzy = true)))
    }

    @Test
    fun disablingFuzzySearchKeepsLiteralMatching() {
        val name = SearchName("Яндекс")
        assertEquals(0, name.score(SearchQuery("ЯНДЕКС", fuzzy = false)))
        assertEquals(2, name.score(SearchQuery("Ян", fuzzy = false)))
        assertNull(name.score(SearchQuery("Ya", fuzzy = false)))
        assertNull(name.score(SearchQuery("Yandex", fuzzy = false)))
        assertNull(name.score(SearchQuery("", fuzzy = true)))
    }
}
