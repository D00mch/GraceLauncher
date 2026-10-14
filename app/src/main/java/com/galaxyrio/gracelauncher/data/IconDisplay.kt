package com.galaxyrio.gracelauncher.data

import org.json.JSONObject

enum class IconDisplayStyle { Full, Dot, Hidden }

enum class IconDotColor { Artwork, Theme, Custom }

/** How list icons render; Hidden always keeps the row label visible. */
data class IconDisplaySettings(
    val style: IconDisplayStyle = IconDisplayStyle.Full,
    val dotColor: IconDotColor = IconDotColor.Artwork,
    /** Dot diameter in dp before the icon-size percentage is applied. */
    val dotSize: Int = 10,
    /** Legacy storage fields are retained; the UI uses the shared color source. */
    val customColor: IconColor? = null,
    // Kept separate from IconDesign so switching modes never changes the full icon.
    val dotShape: IconShape = IconShape.Circle,
    val dotCookieSides: Int = 4,
    val dotPebbleRoundness: Int = IconDesign.DefaultPebbleRoundness,
    val dotSquareCornerRadius: Int = 0,
) {
    val color: IconColor get() = when (dotColor) {
        IconDotColor.Artwork -> IconColor.FromApp
        IconDotColor.Theme -> IconColor.Theme
        IconDotColor.Custom -> customColor ?: IconColor.Theme
    }

    fun withColor(value: IconColor): IconDisplaySettings = when (value.source) {
        IconColorSource.Theme -> copy(dotColor = IconDotColor.Theme, customColor = null)
        IconColorSource.FromApp -> copy(dotColor = IconDotColor.Artwork, customColor = null)
        IconColorSource.Fixed -> copy(dotColor = IconDotColor.Custom, customColor = value)
    }

    val dotDesign: IconDesign get() = IconDesign(shape = dotShape, cookieSides = dotCookieSides,
        pebbleRoundness = dotPebbleRoundness, squareCornerRadius = dotSquareCornerRadius)

    fun withDotDesign(design: IconDesign): IconDisplaySettings = copy(dotShape = design.shape,
        dotCookieSides = design.cookieSides, dotPebbleRoundness = design.pebbleRoundness,
        dotSquareCornerRadius = design.squareCornerRadius).normalized()

    fun normalized(): IconDisplaySettings = copy(
        dotSize = dotSize.coerceIn(4, 32),
        dotShape = dotShape.takeUnless { it == IconShape.None } ?: IconShape.Circle,
        dotCookieSides = dotCookieSides.takeIf { it in IconDesign.CookieSides } ?: 4,
        dotPebbleRoundness = dotPebbleRoundness.coerceIn(0, 100),
        dotSquareCornerRadius = dotSquareCornerRadius.coerceIn(0, 100),
    )

    internal fun json(): JSONObject = normalized().let { value -> JSONObject()
        .put("style", value.style.name)
        .put("dotColor", value.dotColor.name)
        .put("dotSize", value.dotSize)
        .put("dotShape", value.dotShape.name)
        .put("dotCookieSides", value.dotCookieSides)
        .put("dotPebbleRoundness", value.dotPebbleRoundness)
        .put("dotSquareCornerRadius", value.dotSquareCornerRadius)
        .apply { value.customColor?.let { put("customColor", it.json()) } }
    }

    companion object {
        internal fun decode(json: String?): IconDisplaySettings = runCatching {
            JSONObject(json!!).let {
                IconDisplaySettings(
                    style = IconDisplayStyle.entries.firstOrNull { s -> s.name == it.optString("style") }
                        ?: IconDisplayStyle.Full,
                    dotColor = IconDotColor.entries.firstOrNull { c -> c.name == it.optString("dotColor") }
                        ?: IconDotColor.Artwork,
                    dotSize = it.optInt("dotSize", 10),
                    customColor = IconColor.decode(it.optJSONObject("customColor")),
                    dotShape = IconShape.entries.firstOrNull { shape -> shape.name == it.optString("dotShape") }
                        ?: IconShape.Circle,
                    dotCookieSides = it.optInt("dotCookieSides", 4),
                    dotPebbleRoundness = it.optInt("dotPebbleRoundness", IconDesign.DefaultPebbleRoundness),
                    dotSquareCornerRadius = it.optInt("dotSquareCornerRadius", 0),
                ).normalized()
            }
        }.getOrDefault(IconDisplaySettings())
    }
}
