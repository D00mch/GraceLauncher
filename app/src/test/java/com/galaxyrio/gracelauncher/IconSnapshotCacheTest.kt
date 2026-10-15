package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.icons.IconSnapshotCache
import org.junit.Assert.*
import org.junit.Test

class IconSnapshotCacheTest {
    private data class Signature(val source: String = "original", val packs: List<String> = emptyList(),
        val theme: String = "light", val override: String? = null)

    @Test fun reopeningAnUnchangedProfileReusesTheSameResult() {
        val cache = IconSnapshotCache<Signature, Any>()
        val result = Any()
        cache.put("profile:10:app", Signature(), result)
        repeat(20) { assertSame(result, cache.get("profile:10:app", Signature())) }
    }

    @Test fun allVisualInputsInvalidateTheOldResult() {
        val cache = IconSnapshotCache<Signature, Any>()
        val original = Signature(packs = listOf("pack-a", "pack-b"))
        cache.put("profile:10:app", original, Any())
        listOf(original.copy(source = "updated-app"), original.copy(packs = listOf("pack-b", "pack-a")),
            original.copy(packs = emptyList()), original.copy(theme = "dark"), original.copy(override = "custom"))
            .forEach { assertNull(cache.get("profile:10:app", it)) }
    }

    @Test fun changingOneIconDoesNotInvalidateOtherProfilesOrApps() {
        val cache = IconSnapshotCache<Signature, Any>()
        val other = Any()
        cache.put("profile:10:app", Signature(), Any())
        cache.put("profile:11:app", Signature(), other)
        cache.put("profile:10:app", Signature(override = "custom"), Any())
        assertNull(cache.get("profile:10:app", Signature()))
        assertSame(other, cache.get("profile:11:app", Signature()))
        assertNull(cache.get("personal:app", Signature()))
    }

    @Test fun onlyTheLatestAppearanceIsRetainedPerItem() {
        val cache = IconSnapshotCache<Signature, Any>(2)
        val other = Any()
        val latest = Any()
        cache.put("other", Signature(), other)
        repeat(20) { cache.put("app", Signature(theme = "$it"), latest) }
        assertNull(cache.get("app", Signature(theme = "18")))
        assertSame(latest, cache.get("app", Signature(theme = "19")))
        assertSame(other, cache.get("other", Signature()))
    }

    @Test fun memoryIsBoundedAndRecentlyUsedEntriesSurvive() {
        val cache = IconSnapshotCache<Int, String>(2)
        cache.put("a", 1, "a")
        cache.put("b", 1, "b")
        assertEquals("a", cache.get("a", 1))
        cache.put("c", 1, "c")
        assertNull(cache.get("b", 1))
        assertEquals("a", cache.get("a", 1))
        assertEquals("c", cache.get("c", 1))
    }
}
