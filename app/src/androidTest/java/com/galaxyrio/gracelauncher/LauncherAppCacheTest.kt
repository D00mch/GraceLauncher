package com.galaxyrio.gracelauncher

import android.content.ComponentName
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.UserHandle
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LauncherAppCacheTest {
    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(target.cacheDir, "startup-cache-test-${System.nanoTime()}").apply { mkdirs() }
    private val context = object : ContextWrapper(target) {
        override fun getNoBackupFilesDir() = directory
    }
    private fun icon(color: Int) = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        .apply { eraseColor(color) }.asImageBitmap()
    private fun app() = LauncherApp(ComponentName("test.app", "Main"), "Renamed", icon(Color.RED),
        monochromeIcon = icon(Color.WHITE), originalLabel = "Original", iconPackPackage = "test.pack",
        themeIconPackPackage = "test.theme", monochromeScale = 1.2f, isSystemApp = true, isAdaptiveIcon = true,
        firstInstallTime = 123L, englishLabel = "English", originalIconColor = Color.BLUE,
        iconDisplay = IconDisplaySettings(style = IconDisplayStyle.Dot, dotSize = 16))
    private fun snapshot(apps: List<LauncherApp> = listOf(app())) =
        CachedLauncherApps(apps, emptyList(), emptyList(), null)

    @After fun cleanUp() { directory.deleteRecursively() }

    @Test fun newInstanceRestoresRenderedAppsShortcutsAndFoldersWithoutAnIconProvider() = runBlocking {
        val app = app()
        val shortcut = LauncherShortcut("compose/email", app.packageName, "Compose", icon(Color.GREEN), app.componentName).asApp(app)
        val folder = LauncherFolder("favorites", "Favorites", listOf(app.key), FolderPlacement.Both).asApp()
        LauncherAppCache(context).save("appearance", CachedLauncherApps(listOf(app), listOf(shortcut), listOf(folder), app))
        val restored = requireNotNull(LauncherAppCache(context).load("appearance"))
        val actual = restored.apps.single()
        assertEquals(app.copy(icon = null, monochromeIcon = null), actual.copy(icon = null, monochromeIcon = null))
        assertEquals(Color.RED, actual.icon!!.asAndroidBitmap().getPixel(16, 16))
        assertEquals(Color.WHITE, actual.monochromeIcon!!.asAndroidBitmap().getPixel(16, 16))
        assertEquals(shortcut.key, restored.shortcuts.single().key)
        assertEquals("compose/email", restored.shortcuts.single().shortcut!!.id)
        assertEquals(Color.GREEN, restored.shortcuts.single().shortcut!!.icon!!.asAndroidBitmap().getPixel(16, 16))
        assertEquals(folder, restored.folders.single())
        assertNotNull(restored.graceButton?.icon)
    }

    @Test fun appearanceMismatchOrMissingFileFallsBackToLiveLoading() = runBlocking {
        val cache = LauncherAppCache(context)
        assertNull(cache.load("old"))
        cache.save("old", snapshot())
        assertNull(cache.load("changed settings or configuration"))
        assertNotNull(cache.load("old"))
    }

    @Test fun privateAndWorkRowsNeverEnterThePersonalStartupPreview() = runBlocking {
        val app = app()
        val user = UserHandle.getUserHandleForUid(1_000_000)
        val privateApp = app.copy(user = user, userSerial = 10, isPrivateSpace = true)
        val workApp = app.copy(user = user, userSerial = 11, isWorkProfile = true)
        val privateShortcut = LauncherShortcut("secret", app.packageName, "Secret", null, app.componentName,
            user = user, userSerial = 10, isPrivateSpace = true).asApp(privateApp)
        val rows = listOf(app, privateApp, workApp, privateShortcut)
        val cache = LauncherAppCache(context)
        cache.save("key", CachedLauncherApps(rows, rows, rows, privateApp))
        val restored = requireNotNull(cache.load("key"))
        assertEquals(listOf(app.key), restored.apps.map { it.key })
        assertEquals(listOf(app.key), restored.shortcuts.map { it.key })
        assertEquals(listOf(app.key), restored.folders.map { it.key })
        assertNull(restored.graceButton)
        assertFalse(File(directory, "launcher-apps.json").readText().contains("secret"))
    }

    @Test fun uninstalledOrDisabledActivitiesAndTheirShortcutsAreRemovedBeforeDisplay() {
        val app = app()
        val shortcut = LauncherShortcut("compose", app.packageName, "Compose", null, app.componentName).asApp(app)
        val cached = CachedLauncherApps(listOf(app), listOf(shortcut), emptyList(), null)
        assertTrue(cached.installed(emptySet()).apps.isEmpty())
        assertTrue(cached.installed(emptySet()).shortcuts.isEmpty())
        assertEquals(cached, cached.installed(setOf(app.componentName)))
    }

    @Test fun emptyLiveInventoryReplacesTheOldPreview() = runBlocking {
        val cache = LauncherAppCache(context)
        cache.save("key", snapshot())
        cache.save("key", snapshot(emptyList()))
        assertTrue(requireNotNull(LauncherAppCache(context).load("key")).apps.isEmpty())
    }

    @Test fun damagedCacheDoesNotPreventLiveLoading() = runBlocking {
        val cache = LauncherAppCache(context)
        cache.save("key", snapshot())
        File(directory, "launcher-apps.json").writeText("{truncated")
        assertNull(cache.load("key"))
        cache.save("key", snapshot())
        assertNotNull(cache.load("key"))
    }
}
