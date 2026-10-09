package com.neytron.ethereal.tiles

data class TileStyle(
    val backgroundOpacity: Float = 0.55f,
    val backgroundBlur: Float = 0f,
    val contentScale: Float = 1.0f,
    val themedIcon: Boolean? = null
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
    val showLabel: Boolean = true
) {
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"packageName\":")
            if (packageName == null) append("null") else append("\"$packageName\"")
            append(",\"imageUri\":")
            if (imageUri == null) append("null") else append("\"$imageUri\"")
            append(",\"showLabel\":$showLabel")
            append("}")
        }
    }

    companion object {
        fun fromJson(json: String?): AppLauncherConfig {
            if (json.isNullOrBlank()) return AppLauncherConfig()
            return try {
                val obj = org.json.JSONObject(json)
                AppLauncherConfig(
                    packageName = obj.optString("packageName", "").takeIf { it.isNotEmpty() },
                    imageUri = obj.optString("imageUri", "").takeIf { it.isNotEmpty() },
                    showLabel = obj.optBoolean("showLabel", true)
                )
            } catch (e: Exception) {
                AppLauncherConfig()
            }
        }
    }
}