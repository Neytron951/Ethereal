package com.neytron.ethereal.tiles

import org.json.JSONObject

enum class TileBorderStyle {
    NONE,
    OUTLINE,
    LIQUID_GLASS,
    LIQUID_GLASS_SENSORS,
    ACCENT,
}

/**
 * Плитки приложений по умолчанию создаются с крупной иконкой — 200%.
 * Меняется в редакторе плитки («Содержимое → Размер контента»).
 */
const val APP_LAUNCHER_DEFAULT_CONTENT_SCALE = 2f

/**
 * Процедурная текстура фона плитки.
 * Рисуется кодом (без картинок), поэтому масштабируется на любой размер.
 */
enum class TileTexture {
    NONE,
    GRAIN,
    DOTS,
    GRID,
    DIAGONAL,
    CARBON,
    AURORA;

    companion object {
        fun fromName(raw: String?): TileTexture =
            values().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NONE
    }
}

data class TileStyle(
    val backgroundOpacity: Float = 0.55f,
    val backgroundBlur: Float = 0f,
    val contentScale: Float = 1.0f,
    val themedIcon: Boolean? = null,
    val borderWidth: Float = 0f,
    val borderOpacity: Float = 0.3f,
    val borderStyle: TileBorderStyle = TileBorderStyle.NONE,
    val texture: TileTexture = TileTexture.NONE,
    val textureStrength: Float = 0.35f
)

data class Tile(
    val id: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val type: TileType,
    val payload: String? = null,
    val locked: Boolean = false,
    val style: TileStyle = TileStyle(),
    val clickable: Boolean = true,
    val config: String? = null
)

enum class TileType {
    EMPTY,
    SETTINGS,
    WIDGET,
    EQUALIZER,
    APP,
    ACTION,
    /** Виджет запуска приложения (с кастомным визуалом) */
    APP_LAUNCHER,
    /** Пространство — вложенный экран с собственной раскладкой */
    SPACE
}

data class TilePreset(
    val id: String,
    val label: String,
    val type: TileType,
    val w: Int = 1,
    val h: Int = 1,
    val payload: String? = null,
    val clickable: Boolean = true
)

/**
 * Config для APP_LAUNCHER.
 * Хранится в Tile.config как JSON.
 */
data class AppLauncherConfig(
    val packageName: String? = null,
    val imageUri: String? = null,
    val showLabel: Boolean = true,
    /** Показывать только иконку, без подложки плитки (по умолчанию включено). */
    val showOnlyForegroundIcon: Boolean = true
) {
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"packageName\":")
            if (packageName == null) append("null") else append("\"$packageName\"")
            append(",\"imageUri\":")
            if (imageUri == null) append("null") else append("\"$imageUri\"")
            append(",\"showLabel\":$showLabel")
            append(",\"showOnlyForegroundIcon\":$showOnlyForegroundIcon")
            append("}")
        }
    }

    companion object {
        fun fromJson(json: String?): AppLauncherConfig {
            if (json.isNullOrBlank()) return AppLauncherConfig()
            return try {
                val obj = JSONObject(json)
                AppLauncherConfig(
                    packageName = obj.optString("packageName", "").takeIf { it.isNotEmpty() },
                    imageUri = obj.optString("imageUri", "").takeIf { it.isNotEmpty() },
                    showLabel = obj.optBoolean("showLabel", true),
                    showOnlyForegroundIcon = obj.optBoolean("showOnlyForegroundIcon", true)
                )
            } catch (e: Exception) {
                AppLauncherConfig()
            }
        }
    }
}
