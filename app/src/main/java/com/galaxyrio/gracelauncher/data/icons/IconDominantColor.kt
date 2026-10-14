package com.galaxyrio.gracelauncher.data.icons

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * Most-used color of an icon's visible pixels, bucketed at 4 bits per channel.
 * White-squircle icons (Gmail, Chrome…) dominate on neutral pixels, so a neutral
 * winner yields to the most populous saturated bucket; fully neutral icons
 * return null and the caller falls back to the theme color.
 */
internal object IconDominantColor {
    private const val SampleSize = 24

    fun of(icon: Bitmap): Int? {
        val sampled = if (icon.width > SampleSize || icon.height > SampleSize)
            Bitmap.createScaledBitmap(icon, SampleSize, SampleSize, true) else icon
        val pixels = IntArray(sampled.width * sampled.height)
        sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)
        if (sampled !== icon) sampled.recycle()
        val counts = IntArray(1 shl 12)
        for (pixel in pixels) {
            if (pixel.alpha < 128) continue
            counts[(pixel.red shr 4 shl 8) or (pixel.green shr 4 shl 4) or (pixel.blue shr 4)]++
        }
        // Local scratch — callers hit this concurrently from refresh (IO) and the
        // designer preview (main), so no shared FloatArray.
        val hsl = FloatArray(3)
        var dominant = -1
        var saturated = -1
        for (bucket in counts.indices) {
            if (counts[bucket] == 0) continue
            if (dominant < 0 || counts[bucket] > counts[dominant]) dominant = bucket
            if (!isNeutral(bucket, hsl) && (saturated < 0 || counts[bucket] > counts[saturated])) saturated = bucket
        }
        return when {
            dominant < 0 -> null
            !isNeutral(dominant, hsl) -> bucketColor(dominant)
            saturated >= 0 -> bucketColor(saturated)
            else -> null
        }
    }

    private fun isNeutral(bucket: Int, hsl: FloatArray): Boolean {
        ColorUtils.colorToHSL(bucketColor(bucket), hsl)
        return hsl[1] < 0.15f || hsl[2] !in 0.12f..0.88f
    }

    private fun bucketColor(bucket: Int): Int =
        android.graphics.Color.rgb(
            ((bucket shr 8) and 15) * 17,
            ((bucket shr 4) and 15) * 17,
            (bucket and 15) * 17,
        )
}
