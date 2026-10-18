package com.galaxyrio.gracelauncher.data

import org.json.JSONObject

/** One hosted widget. Zero height means the provider's default; zero offset means home default. */
data class HomeLayout(
    val topOffsetDp: Float = 0f,
    val widgetId: Int = -1,
    val widgetProvider: String? = null,
    val widgetLabel: String? = null,
    val widgetHeightDp: Int = 0,
    /** Per-widget presentation in app/folder pop-ups; the desktop grid owns its own size. */
    val popupWidthPercent: Int = 100,
    val popupFullWidth: Boolean = false,
    val popupBackground: Boolean = false,
    val popupContentScale: Int = 100,
) {
    val hasWidget: Boolean get() = widgetId >= 0

    fun withoutWidget() = HomeLayout(topOffsetDp = topOffsetDp)

    fun encode(): String = JSONObject().apply {
        put("topOffsetDp", topOffsetDp.takeIf { it.isFinite() }?.coerceIn(-1200f, 1200f) ?: 0f)
        put("widgetId", widgetId)
        put("widgetProvider", widgetProvider ?: JSONObject.NULL)
        put("widgetLabel", widgetLabel ?: JSONObject.NULL)
        put("widgetHeightDp", widgetHeightDp.coerceIn(0, 1600))
        put("popupWidthPercent", popupWidthPercent.coerceIn(20, 100))
        put("popupFullWidth", popupFullWidth)
        put("popupBackground", popupBackground)
        put("popupContentScale", popupContentScale.coerceIn(50, 150))
    }.toString()

    companion object {
        fun decode(value: String?): HomeLayout = runCatching {
            val data = JSONObject(value ?: "{}")
            HomeLayout(
                topOffsetDp = data.optDouble("topOffsetDp", 0.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-1200f, 1200f) ?: 0f,
                widgetId = data.optInt("widgetId", -1),
                widgetProvider = data.optString("widgetProvider").takeUnless { it.isBlank() || it == "null" },
                widgetLabel = data.optString("widgetLabel").takeUnless { it.isBlank() || it == "null" },
                widgetHeightDp = data.optInt("widgetHeightDp", 0).coerceIn(0, 1600),
                popupWidthPercent = data.optInt("popupWidthPercent", 100).coerceIn(20, 100),
                popupFullWidth = data.optBoolean("popupFullWidth", false),
                popupBackground = data.optBoolean("popupBackground", false),
                popupContentScale = data.optInt("popupContentScale", 100).coerceIn(50, 150),
            )
        }.getOrDefault(HomeLayout())
    }
}
