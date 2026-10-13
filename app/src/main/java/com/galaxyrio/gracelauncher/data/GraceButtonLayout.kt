package com.galaxyrio.gracelauncher.data

import kotlin.math.abs

enum class GraceButtonAlignment { Left, Center, Right }

/** Distances are dp from the safe viewport's sides/bottom, not from the icon's center. */
data class GraceButtonLayout(
    val alignment: GraceButtonAlignment = GraceButtonAlignment.Right,
    val sideMargin: Float = 56f,
    val bottomMargin: Float = 22f,
    val size: Float = 54f,
) {
    fun normalized() = copy(
        sideMargin = sideMargin.takeIf { it.isFinite() }?.coerceIn(0f, 2000f) ?: 56f,
        bottomMargin = bottomMargin.takeIf { it.isFinite() }?.coerceIn(0f, 2000f) ?: 22f,
        size = size.takeIf { it.isFinite() }?.coerceIn(32f, 120f) ?: 54f,
    )

    /** Shared by the desktop and its scaled preview, including small/folded windows. */
    fun bounds(width: Float, height: Float, topInset: Float = 0f, bottomInset: Float = 0f): GraceButtonBounds {
        val value = normalized()
        val safeWidth = width.coerceAtLeast(0f)
        val safeTop = topInset.coerceIn(0f, height.coerceAtLeast(0f))
        val safeBottom = (height - bottomInset).coerceAtLeast(safeTop)
        val side = minOf(value.size, safeWidth, safeBottom - safeTop)
        val travelX = safeWidth - side
        val left = when (value.alignment) {
            GraceButtonAlignment.Left -> value.sideMargin
            GraceButtonAlignment.Center -> travelX / 2f
            GraceButtonAlignment.Right -> travelX - value.sideMargin
        }.coerceIn(0f, travelX)
        return GraceButtonBounds(left, (safeBottom - side - value.bottomMargin).coerceIn(safeTop, safeBottom - side), side)
    }

    /** Drag freely; snap only near center, otherwise anchor to the nearer side. */
    fun movedTo(left: Float, top: Float, width: Float, height: Float,
        topInset: Float = 0f, bottomInset: Float = 0f): GraceButtonLayout {
        val value = normalized()
        val bounds = value.bounds(width, height, topInset, bottomInset)
        val travelX = (width - bounds.size).coerceAtLeast(0f)
        val x = left.coerceIn(0f, travelX)
        val alignment = when {
            abs(x - travelX / 2f) <= 8f -> GraceButtonAlignment.Center
            x < travelX / 2f -> GraceButtonAlignment.Left
            else -> GraceButtonAlignment.Right
        }
        return value.copy(alignment = alignment,
            sideMargin = when (alignment) {
                GraceButtonAlignment.Left -> x
                GraceButtonAlignment.Right -> travelX - x
                GraceButtonAlignment.Center -> value.sideMargin
            },
            bottomMargin = (height - bottomInset - bounds.size - top)
                .coerceIn(0f, (height - topInset - bottomInset - bounds.size).coerceAtLeast(0f)),
        )
    }
}

data class GraceButtonBounds(val left: Float, val top: Float, val size: Float)
