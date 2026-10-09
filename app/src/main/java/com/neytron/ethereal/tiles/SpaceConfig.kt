package com.neytron.ethereal.tiles

import org.json.JSONObject

/**
 * Конфиг плитки-пространства.
 * Хранится в Tile.config как JSON.
 *
 * Иконка: либо emoji/текст (iconText), либо путь к картинке (iconUri).
 * Если оба заданы — приоритет у iconUri.
 */
data class SpaceConfig(
    val name: String = "Space",
    val iconUri: String? = null,
    val iconText: String? = null,
    val showLabel: Boolean = true
) {
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"name\":\"")
            append(name.replace("\\", "\\\\").replace("\"", "\\\""))
            append("\",\"iconUri\":")
            if (iconUri == null) append("null") else append("\"$iconUri\"")
            append(",\"iconText\":")
            if (iconText == null) append("null") else append("\"${iconText.replace("\"", "\\\"")}\"")
            append(",\"showLabel\":$showLabel")
            append("}")
        }
    }

    companion object {
        fun fromJson(json: String?): SpaceConfig {
            if (json.isNullOrBlank()) return SpaceConfig()
            return try {
                val obj = JSONObject(json)
                SpaceConfig(
                    name = obj.optString("name", "Space").ifBlank { "Space" },
                    iconUri = obj.optString("iconUri", "").takeIf { it.isNotEmpty() },
                    iconText = obj.optString("iconText", "").takeIf { it.isNotEmpty() },
                    showLabel = obj.optBoolean("showLabel", true)
                )
            } catch (e: Exception) {
                SpaceConfig()
            }
        }
    }
}
