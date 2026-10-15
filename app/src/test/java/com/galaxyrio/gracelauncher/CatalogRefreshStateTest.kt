package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.CatalogRefreshState
import org.junit.Assert.*
import org.junit.Test

class CatalogRefreshStateTest {
    @Test fun repeatedResumesReuseASuccessfullyLoadedCatalog() {
        val state = CatalogRefreshState<String>()
        val request = state.pending("same apps")!!
        assertTrue(state.complete(request, "same apps"))
        repeat(20) { assertNull(state.pending("same apps")) }
    }

    @Test fun packageChangesRequireAReloadEvenWithTheSameConfiguration() {
        val state = CatalogRefreshState<String>()
        state.complete(state.pending("configuration")!!, "configuration")
        state.invalidate()
        assertNotNull(state.pending("configuration"))
    }

    @Test fun configurationAndDayChangesAreDetectedWithoutABroadcast() {
        val state = CatalogRefreshState<Pair<String, Int>>()
        state.complete(state.pending("en" to 1)!!, "en" to 1)
        assertNotNull(state.pending("fr" to 1))
        assertNotNull(state.pending("en" to 2))
    }

    @Test fun changesDuringALoadCannotBeMarkedCleanByTheOldResult() {
        val state = CatalogRefreshState<String>()
        val old = state.pending("apps")!!
        repeat(3) { state.invalidate() }
        assertFalse(state.isCurrent(old, "apps"))
        assertFalse(state.complete(old, "apps"))
        val latest = state.pending("apps")!!
        assertTrue(state.complete(latest, "apps"))
        assertNull(state.pending("apps"))
    }

    @Test fun changedInputsCannotPublishAnOldResultAndFailuresRemainRetryable() {
        val state = CatalogRefreshState<String>()
        val old = state.pending("old theme")!!
        assertFalse(state.complete(old, "new theme"))
        val retry = state.pending("new theme")!!
        // A failed load never calls complete; the next resume must retry it.
        assertEquals(retry, state.pending("new theme"))
    }
}
