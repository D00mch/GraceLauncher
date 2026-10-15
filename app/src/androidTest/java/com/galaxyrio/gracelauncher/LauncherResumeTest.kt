package com.galaxyrio.gracelauncher

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyrio.gracelauncher.ui.LauncherViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class LauncherResumeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun returningHomeReusesLoadedAppsAndExplicitInvalidationStillReloads() = runBlocking {
        val store = ViewModelStore()
        lateinit var model: LauncherViewModel
        instrumentation.runOnMainSync {
            model = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(
                instrumentation.targetContext.applicationContext as Application))[LauncherViewModel::class.java]
        }
        try {
            awaitApps(model)
            instrumentation.runOnMainSync {
                val loaded = model.uiState.value.apps
                repeat(3) {
                    model.refreshApps(forceReload = false)
                    assertFalse("Returning Home must not start another inventory load", model.uiState.value.isLoadingApps)
                    assertSame(loaded, model.uiState.value.apps)
                }
                // Package/profile/appearance notifications use the default, forced path.
                model.refreshApps()
                assertTrue(model.uiState.value.isLoadingApps)
            }
            awaitApps(model)
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }

    private suspend fun awaitApps(model: LauncherViewModel) = withTimeout(30_000) {
        val state = model.uiState.first { !it.isLoadingSettings && !it.isLoadingApps }
        assertFalse(state.settingsLoadFailed)
        assertFalse(state.appLoadFailed)
    }
}
