package com.galaxyrio.gracelauncher.data

import org.json.JSONObject

enum class IconDisplayStyle { Full, Dot, Hidden }

enum class IconDotColor { Artwork, Theme, Custom }

/** How list icons render; Hidden is disallowed with hidden favorite names. */
data class IconDisplaySettings(
    val style: IconDisplayStyle = IconDisplayStyle.Full,
    val dotColor: IconDotColor = IconDotColor.Artwork,
    /** Dot diameter in dp before the icon-size percentage is applied. */
    val dotSize: Int = 10,
    /** Custom dot color; supports the designer's fixed/dynamic/theme color types. */
    val customColor: IconColor? = null,
) {
    internal fun json(): JSONObject = JSONObject()
        .put("style", style.name)
        .put("dotColor", dotColor.name)
        .put("dotSize", dotSize)
        .apply { customColor?.let { put("customColor", it.json()) } }

    companion object {
        internal fun decode(json: String?): IconDisplaySettings = runCatching {
            JSONObject(json!!).let {
                IconDisplaySettings(
                    style = IconDisplayStyle.entries.firstOrNull { s -> s.name == it.optString("style") }
                        ?: IconDisplayStyle.Full,
                    dotColor = IconDotColor.entries.firstOrNull { c -> c.name == it.optString("dotColor") }
                        ?: IconDotColor.Artwork,
                    dotSize = it.optInt("dotSize", 10).coerceIn(4, 32),
                    customColor = IconColor.decode(it.optJSONObject("customColor")),
                )
            }
        }.getOrDefault(IconDisplaySettings())
    }
}
