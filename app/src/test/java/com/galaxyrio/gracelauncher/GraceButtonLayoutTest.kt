package com.galaxyrio.gracelauncher

import com.galaxyrio.gracelauncher.data.GraceButtonAlignment
import com.galaxyrio.gracelauncher.data.GraceButtonLayout
import com.galaxyrio.gracelauncher.data.GraceButtonSettings
import org.junit.Assert.*
import org.junit.Test

class GraceButtonLayoutTest {
    @Test fun defaultsPreserveThePreviousDesktopPlacement() {
        val bounds = GraceButtonLayout().bounds(400f, 800f, topInset = 24f, bottomInset = 28f)
        assertEquals(290f, bounds.left, .001f)
        assertEquals(696f, bounds.top, .001f)
        assertEquals(54f, bounds.size, .001f)
    }

    @Test fun centerIgnoresButPreservesTheSideDistance() {
        val layout = GraceButtonLayout(alignment = GraceButtonAlignment.Center, sideMargin = 73f)
        assertEquals(173f, layout.bounds(400f, 800f).left, .001f)
        assertEquals(73f, layout.copy(alignment = GraceButtonAlignment.Left).bounds(400f, 800f).left, .001f)
        assertEquals(273f, layout.copy(alignment = GraceButtonAlignment.Right).bounds(400f, 800f).left, .001f)
    }

    @Test fun dragUsesTheSameGeometryAsTheDesktopForBothSidesAndCenter() {
        val initial = GraceButtonLayout()
        for (left in listOf(18f, 173f, 300f)) {
            val moved = initial.movedTo(left, 420f, 400f, 800f, 24f, 28f)
            val bounds = moved.bounds(400f, 800f, 24f, 28f)
            assertEquals(left, bounds.left, .001f)
            assertEquals(420f, bounds.top, .001f)
        }
        assertEquals(GraceButtonAlignment.Left, initial.movedTo(18f, 420f, 400f, 800f).alignment)
        assertEquals(GraceButtonAlignment.Center, initial.movedTo(177f, 420f, 400f, 800f).alignment)
        assertEquals(GraceButtonAlignment.Right, initial.movedTo(300f, 420f, 400f, 800f).alignment)
    }

    @Test fun foldingOrResizingKeepsTheButtonInsideTheSafeArea() {
        val layout = GraceButtonLayout(sideMargin = 400f, bottomMargin = 900f, size = 120f)
        val bounds = layout.bounds(280f, 360f, 32f, 24f)
        assertEquals(0f, bounds.left, .001f)
        assertEquals(32f, bounds.top, .001f)
        assertTrue(bounds.left + bounds.size <= 280f)
        assertTrue(bounds.top + bounds.size <= 336f)
        val tiny = layout.bounds(40f, 80f, 30f, 30f)
        assertEquals(20f, tiny.size, .001f)
        assertEquals(30f, tiny.top, .001f)
    }

    @Test fun sizesAndInvalidNumbersAreNormalized() {
        val layout = GraceButtonLayout(sideMargin = Float.NaN, bottomMargin = Float.POSITIVE_INFINITY, size = Float.NaN).normalized()
        assertEquals(GraceButtonLayout(), layout)
        assertEquals(32f, GraceButtonLayout(size = -1f).normalized().size, .001f)
        assertEquals(120f, GraceButtonLayout(size = 1000f).normalized().size, .001f)
    }

    @Test fun changingAppearancePreservesActions() {
        val previous = GraceButtonSettings()
        val changed = previous.copy(layout = GraceButtonLayout(alignment = GraceButtonAlignment.Left))
        assertEquals(previous.tap, changed.tap)
        assertEquals(previous.longPress, changed.longPress)
        assertEquals(previous.swipeUp, changed.swipeUp)
        assertEquals(previous.animationsEnabled, changed.animationsEnabled)
    }
}
