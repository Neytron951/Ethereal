package com.neytron.ethereal.tiles

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Реестр всех пространств.
 * Хранит метаданные (id, name, iconUri, iconText, createdAt) + умеет чистить раскладки.
 *
 * Раскладки (tiles) хранит TileLayoutRepository по ключу tiles_space_<id>.
 * Здесь — только метаданные, чтобы знать, какие пространства вообще существуют.
 */
object SpaceRepository {

    private const val PREFS = "ethereal_settings"
    private const val KEY_SPACES_META = "spaces_meta"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ============================================================
    // Метаданные
    // ============================================================

    data class SpaceMeta(
        val id: String,
        val name: String,
        val iconUri: String?,
        val iconText: String?,
        val createdAt: Long
    )

    fun getAllSpaces(context: Context): List<SpaceMeta> {
        val raw = prefs(context).getString(KEY_SPACES_META, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SpaceMeta>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SpaceMeta(
                        id = obj.getString("id"),
                        name = obj.optString("name", "Space").ifBlank { "Space" },
                        iconUri = obj.optString("iconUri", "").takeIf { it.isNotEmpty() },
                        iconText = obj.optString("iconText", "").takeIf { it.isNotEmpty() },
                        createdAt = obj.optLong("createdAt", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getSpace(context: Context, spaceId: String): SpaceMeta? =
        getAllSpaces(context).firstOrNull { it.id == spaceId }

    fun exists(context: Context, spaceId: String): Boolean =
        getSpace(context, spaceId) != null

    private fun saveAll(context: Context, list: List<SpaceMeta>) {
        val arr = JSONArray()
        list.forEach { meta ->
            val obj = JSONObject().apply {
                put("id", meta.id)
                put("name", meta.name)
                put("iconUri", meta.iconUri ?: "")
                put("iconText", meta.iconText ?: "")
                put("createdAt", meta.createdAt)
            }
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_SPACES_META, arr.toString()).apply()
    }

    // ============================================================
    // CRUD
    // ============================================================

    /**
     * Создаёт пространство и пустую раскладку для него.
     * Возвращает spaceId.
     */
    fun createSpace(
        context: Context,
        name: String,
        iconUri: String?,
        iconText: String?
    ): String {
        val id = UUID.randomUUID().toString()
        val meta = SpaceMeta(
            id = id,
            name = name.ifBlank { "Space" },
            iconUri = iconUri,
            iconText = iconText,
            createdAt = System.currentTimeMillis()
        )
        val current = getAllSpaces(context).toMutableList()
        current.add(meta)
        saveAll(context, current)

        // Пустая раскладка
        TileLayoutRepository.setTiles(context, id, emptyList())

        return id
    }

    /**
     * Обновляет имя/иконку пространства.
     * Раскладку НЕ трогает.
     */
    fun updateSpace(
        context: Context,
        spaceId: String,
        name: String,
        iconUri: String?,
        iconText: String?
    ): Boolean {
        val current = getAllSpaces(context).toMutableList()
        val index = current.indexOfFirst { it.id == spaceId }
        if (index < 0) return false

        current[index] = current[index].copy(
            name = name.ifBlank { "Space" },
            iconUri = iconUri,
            iconText = iconText
        )
        saveAll(context, current)
        return true
    }

    /**
     * Удаляет пространство: метаданные + раскладку.
     * Виджеты внутри нужно удалить ДО вызова этого метода (см. MainActivity).
     */
    fun deleteSpace(context: Context, spaceId: String): Boolean {
        val current = getAllSpaces(context).toMutableList()
        val removed = current.removeAll { it.id == spaceId }
        if (!removed) return false

        saveAll(context, current)

        // Чистим раскладку
        TileLayoutRepository.clearTiles(context, spaceId)

        return true
    }
}