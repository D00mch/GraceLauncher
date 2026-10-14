package com.galaxyrio.gracelauncher.data

import android.content.res.Resources
import android.os.Build
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/** The system accent is a seed, not a finished scheme, so every style works in dynamic mode too. */
internal fun launcherThemeSeed(resources: Resources, dynamic: Boolean, fallback: Int): Int =
    if (dynamic && Build.VERSION.SDK_INT >= 31) resources.getColor(android.R.color.system_accent1_500, null)
    else fallback

/** UI, previews and rasterized icons must use the same palette algorithm and spec. */
internal fun launcherColorScheme(seed: Int, dark: Boolean, style: PaletteStyle) = dynamicColorScheme(
    seedColor = Color(seed), isDark = dark, style = style, specVersion = ColorSpec.SpecVersion.SPEC_2025,
)
