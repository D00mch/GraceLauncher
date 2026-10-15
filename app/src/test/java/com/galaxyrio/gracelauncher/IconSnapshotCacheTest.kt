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

    @Test fun entryCountIsBounded() {
        val cache = IconSnapshotCache<Int, String>(2)
        cache.put("a", 1, "a")
        cache.put("b", 1, "b")
        cache.put("c", 1, "c")
        assertEquals("c", cache.get("c", 1))
        assertEquals(2, listOf("a", "b", "c").count { cache.get(it, 1) != null })
    }

    @Test fun bitmapBudgetIsEnforcedBeforeTheCountLimit() {
        val cache = IconSnapshotCache<Int, ByteArray>(capacity = 100, maxBytes = 10, sizeOf = { it.size })
        cache.put("a", 1, ByteArray(4))
        cache.put("b", 1, ByteArray(4))
        cache.put("c", 1, ByteArray(4))
        assertNotNull(cache.get("c", 1))
        assertEquals(2, listOf("a", "b", "c").count { cache.get(it, 1) != null })
    }

    @Test fun replacingAnIconReleasesItsOldBudgetAndOversizedImagesAreNotRetained() {
        val cache = IconSnapshotCache<Int, ByteArray>(maxBytes = 10, sizeOf = { it.size })
        cache.put("a", 1, ByteArray(8))
        cache.put("a", 2, ByteArray(2))
        cache.put("b", 1, ByteArray(8))
        assertNotNull(cache.get("a", 2))
        assertNotNull(cache.get("b", 1))
        cache.put("huge", 1, ByteArray(11))
        assertNull(cache.get("huge", 1))
        assertNotNull(cache.get("a", 2))
        assertNotNull(cache.get("b", 1))
    }

    @Test fun fullCatalogScansReuseIconsEvenWhenTheCatalogExceedsEitherLimit() {
        val iconBytes = 144 * 144 * 4
        for ((apps, budget) in listOf(200 to 16 * 1024 * 1024, 203 to 16 * 1024 * 1024, 257 to Int.MAX_VALUE)) {
            val cache = IconSnapshotCache<Int, Int>(maxBytes = budget, sizeOf = { iconBytes })
            val capacity = minOf(256, budget / iconBytes)
            repeat(5) { pass ->
                var hits = 0
                repeat(apps) { key ->
                    if (cache.get("app:$key", 1) != null) hits++ else cache.put("app:$key", 1, key)
                }
                if (pass > 0) {
                    if (apps <= capacity) assertEquals(apps, hits)
                    else assertTrue("$apps apps, pass $pass: only $hits hits", hits >= capacity - 1)
                }
            }
        }
    }
}
