package com.galaxyrio.gracelauncher

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Process
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.data.*
import com.galaxyrio.gracelauncher.data.icons.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersonalIconCachingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun bitmap(color: Int) = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
        .apply { eraseColor(color) }.asImageBitmap()
    private fun app() = LauncherApp(ComponentName("test.personal.icon", "Main"), "Original", bitmap(Color.GREEN),
        iconSourceRevision = "revision-1")
    private fun design(color: Int) = ItemIcon.Theme.copy(design = IconDesign(
        themeIcons = true, themeUnsupportedIcons = true, foreground = IconColor(color)))

    @Test fun inventoryRefreshReusesSourcePixelsUntilThatPackageChanges() = runBlocking {
        val repository = AppRepository(context)
        fun List<LauncherApp>.ownApp() = single { it.packageName == context.packageName }
        val first = repository.loadApps().ownApp()
        val again = repository.loadApps().ownApp()
        assertNotNull(first.icon)
        assertSame(first.icon, again.icon)
        assertEquals(first.iconSourceRevision, again.iconSourceRevision)

        repository.invalidateProfileApps(Process.myUserHandle(), listOf(context.packageName))
        val updated = repository.loadApps().ownApp()
        assertNotEquals(first.iconSourceRevision, updated.iconSourceRevision)
    }

    @Test fun unchangedArtworkReusesTheRenderWithoutRestoringAnOldLabel() = runBlocking {
        val store = ItemIconStore(context, IconPackRepository(context))
        val original = app()
        val style = design(Color.RED)
        val settings = LauncherSettings(iconDesign = style)
        val first = store.applyDesign(original, null, style, settings)
        val again = store.applyDesign(original.copy(label = "Renamed", icon = bitmap(Color.GREEN)), null, style, settings)
        assertSame(first.icon, again.icon)
        assertEquals("Renamed", again.label)
        assertEquals(Color.RED, again.icon!!.asAndroidBitmap().getPixel(72, 72))
    }

    @Test fun newSourceAndPerAppDesignsInvalidateOnlyTheAffectedRender() = runBlocking {
        val store = ItemIconStore(context, IconPackRepository(context))
        val original = app()
        val red = design(Color.RED)
        val settings = LauncherSettings(iconDesign = red)
        val first = store.applyDesign(original, null, red, settings)
        val newSource = store.applyDesign(original.copy(iconSourceRevision = "revision-2"), null, red, settings)
        assertNotSame(first.icon, newSource.icon)
        val blue = store.applyDesign(original.copy(iconSourceRevision = "revision-2"), design(Color.BLUE), red, settings)
        assertEquals(Color.BLUE, blue.icon!!.asAndroidBitmap().getPixel(72, 72))
    }

    @Test fun originalLayersAreSharedAcrossDesignsButNotSourceRevisions() = runBlocking {
        val repository = AppRepository(context)
        val own = repository.loadApps().single { it.packageName == context.packageName }
        val store = ItemIconStore(context, IconPackRepository(context))
        val first = store.layers(own, ItemIcon.System, LauncherSettings(), 144)
        assertNotNull(first)
        assertSame(first, store.layers(own, ItemIcon.System, LauncherSettings(), 144))
        assertNotSame(first, store.layers(own.copy(iconSourceRevision = "updated"), ItemIcon.System, LauncherSettings(), 144))
    }
}
