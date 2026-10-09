package com.neytron.ethereal.tiles

import android.content.Context
import com.neytron.ethereal.ui.navigation.ScreenStack
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object TileLayoutRepository {

    private const val PREFS = "ethereal_settings"
    private const val KEY_TILES_ROOT = "tiles_layout"
    private const val KEY_TILES_SPACE_PREFIX = "tiles_space_"
    private const val KEY_COLUMNS = "tiles_columns"

    const val DEFAULT_COLUMNS = 4

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Ключ для раскладки.
     * Root = старый ключ tiles_layout (совместимость).
     * Пространство = tiles_space_<spaceId>.
     */
    private fun keyFor(spaceId: String): String =
        if (spaceId == ScreenStack.ROOT_ID) KEY_TILES_ROOT
        else KEY_TILES_SPACE_PREFIX + spaceId

    // ============================================================
    // Колонки (глобально, не по пространствам)
    // ============================================================

    fun getColumns(context: Context): Int =
        prefs(context).getInt(KEY_COLUMNS, DEFAULT_COLUMNS)

    fun setColumns(context: Context, columns: Int) {
        prefs(context).edit().putInt(KEY_COLUMNS, columns.coerceIn(3, 12)).apply()
    }

    // ============================================================
    // Тайлы
    // ============================================================

    /**
     * Читает раскладку для указанного пространства.
     * Root: если ключ пуст → defaultTiles() (совместимость со старыми установками).
     * Пространство: если ключ пуст → emptyList() (новое пустое пространство).
     */
    fun getTiles(context: Context, spaceId: String): List<Tile> {
        val key = keyFor(spaceId)
        val raw = prefs(context).getString(key, null)

        if (raw == null) {
            return if (spaceId == ScreenStack.ROOT_ID) defaultTiles() else emptyList()
        }

        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Tile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val styleObj = obj.optJSONObject("style")
                list.add(
                    Tile(
                        id = obj.getString("id"),
                        x = obj.getInt("x"),
                        y = obj.getInt("y"),
                        w = obj.getInt("w"),
                        h = obj.getInt("h"),
                        type = try {
                            TileType.valueOf(obj.getString("type"))
                        } catch (e: Exception) {
                            TileType.EMPTY
                        },
                        payload = obj.optString("payload").takeIf { it.isNotEmpty() },
                        locked = obj.optBoolean("locked", false),
                        style = if (styleObj != null) {
                            TileStyle(
                                backgroundOpacity = styleObj.optDouble("bgOpacity", 0.55).toFloat(),
                                backgroundBlur = styleObj.optDouble("bgBlur", 0.0).toFloat(),
                                contentScale = styleObj.optDouble("contentScale", 1.0).toFloat(),
                                themedIcon = if (styleObj.has("themedIcon")) styleObj.optBoolean("themedIcon") else null
                            )
                        } else TileStyle(),
                        clickable = obj.optBoolean("clickable", true),
                        config = obj.optString("config").takeIf { it.isNotEmpty() }
                    )
                )
            }
            list.ifEmpty {
                if (spaceId == ScreenStack.ROOT_ID) defaultTiles() else emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (spaceId == ScreenStack.ROOT_ID) defaultTiles() else emptyList()
        }
    }

    fun setTiles(context: Context, spaceId: String, tiles: List<Tile>) {
        val key = keyFor(spaceId)
        val arr = JSONArray()
        tiles.forEach { tile ->
            val styleObj = JSONObject().apply {
                put("bgOpacity", tile.style.backgroundOpacity.toDouble())
                put("bgBlur", tile.style.backgroundBlur.toDouble())
                put("contentScale", tile.style.contentScale.toDouble())
                if (tile.style.themedIcon != null) {
                    put("themedIcon", tile.style.themedIcon)
                }
            }
            val obj = JSONObject().apply {
                put("id", tile.id)
                put("x", tile.x)
                put("y", tile.y)
                put("w", tile.w)
                put("h", tile.h)
                put("type", tile.type.name)
                put("payload", tile.payload ?: "")
                put("locked", tile.locked)
                put("style", styleObj)
                put("clickable", tile.clickable)
                put("config", tile.config ?: "")
            }
            arr.put(obj)
        }
        prefs(context).edit().putString(key, arr.toString()).apply()
    }

    /**
     * Полная очистка раскладки пространства.
     * Для root — сбрасывает на defaultTiles() (settings-плитка).
     * Для пространства — удаляет ключ.
     */
    fun clearTiles(context: Context, spaceId: String) {
        if (spaceId == ScreenStack.ROOT_ID) {
            setTiles(context, spaceId, defaultTiles())
        } else {
            prefs(context).edit().remove(keyFor(spaceId)).apply()
        }
    }

    // ============================================================
    // Дефолт
    // ============================================================

    fun defaultTiles(): List<Tile> {
        return listOf(
            Tile(
                id = "settings-tile",
                x = 3,
                y = 0,
                w = 1,
                h = 1,
                type = TileType.SETTINGS,
                locked = true,
                clickable = true
            )
        )
    }

    fun resetToDefault(context: Context, spaceId: String) {
        setTiles(context, spaceId, if (spaceId == ScreenStack.ROOT_ID) defaultTiles() else emptyList())
    }

    // ============================================================
    // Утилиты
    // ============================================================

    fun isAreaFree(
        tiles: List<Tile>,
        x: Int, y: Int, w: Int, h: Int,
        ignoreId: String? = null
    ): Boolean {
        for (tile in tiles) {
            if (tile.id == ignoreId) continue
            val overlapX = x < tile.x + tile.w && x + w > tile.x
            val overlapY = y < tile.y + tile.h && y + h > tile.y
            if (overlapX && overlapY) return false
        }
        return true
    }

    fun normalizeTiles(tiles: List<Tile>, columns: Int): List<Tile> {
        return smartNormalizeTiles(tiles, columns, 100)
    }

    /**
     * Умная нормализация сетки: гарантирует, что ни один тайл не вылетит за пределы (columns, rows),
     * не превысит допустимую ширину/высоту и не перекроет другой тайл.
     * При уменьшении сетки элементы аккуратно сдвигаются на свободные ячейки.
     */
    fun smartNormalizeTiles(
        tiles: List<Tile>,
        columns: Int,
        rows: Int
    ): List<Tile> {
        val safeCols = columns.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)

        val result = mutableListOf<Tile>()
        val sorted = tiles.sortedWith(compareBy({ it.y }, { it.x }))

        for (tile in sorted) {
            val newW = tile.w.coerceIn(1, safeCols)
            val newH = tile.h.coerceIn(1, safeRows)

            val fitsX = tile.x + newW <= safeCols
            val fitsY = tile.y + newH <= safeRows
            val currentSpotFree = isAreaFree(result, tile.x, tile.y, newW, newH)

            if (fitsX && fitsY && currentSpotFree) {
                result.add(tile.copy(w = newW, h = newH))
            } else {
                val startX = tile.x.coerceIn(0, (safeCols - newW).coerceAtLeast(0))
                val startY = tile.y.coerceIn(0, (safeRows - newH).coerceAtLeast(0))

                val bestSpot = findClosestFreeSpot(
                    placed = result,
                    columns = safeCols,
                    rows = safeRows,
                    w = newW,
                    h = newH,
                    preferredX = startX,
                    preferredY = startY
                )

                result.add(
                    tile.copy(
                        x = bestSpot.first,
                        y = bestSpot.second,
                        w = newW,
                        h = newH
                    )
                )
            }
        }

        return result
    }

    fun placeTileWithDisplacement(
        tiles: List<Tile>,
        draggingId: String,
        newX: Int,
        newY: Int,
        columns: Int,
        rows: Int
    ): List<Tile>? {
        val dragging = tiles.firstOrNull { it.id == draggingId } ?: return null
        val newW = dragging.w
        val newH = dragging.h

        val overlapping = tiles.filter { tile ->
            if (tile.id == draggingId) return@filter false
            val overlapX = newX < tile.x + tile.w && newX + newW > tile.x
            val overlapY = newY < tile.y + tile.h && newY + newH > tile.y
            overlapX && overlapY
        }

        if (overlapping.isEmpty()) {
            return tiles.map { if (it.id == draggingId) it.copy(x = newX, y = newY) else it }
        }

        val updated = tiles.map { tile ->
            if (tile.id == draggingId) tile.copy(x = newX, y = newY) else tile
        }.toMutableList()

        for (ol in overlapping) {
            val withoutOl = updated.filter { it.id != ol.id }
            val freeSpot = findClosestFreeSpot(
                placed = withoutOl,
                columns = columns,
                rows = rows,
                w = ol.w,
                h = ol.h,
                preferredX = dragging.x,
                preferredY = dragging.y
            )
            val idx = updated.indexOfFirst { it.id == ol.id }
            if (idx >= 0) {
                updated[idx] = ol.copy(x = freeSpot.first, y = freeSpot.second)
            }
        }

        return smartNormalizeTiles(updated, columns, rows)
    }

    private fun findClosestFreeSpot(
        placed: List<Tile>,
        columns: Int,
        rows: Int,
        w: Int,
        h: Int,
        preferredX: Int,
        preferredY: Int
    ): Pair<Int, Int> {
        val maxSearchY = maxOf(rows, preferredY + 1)
        val maxColStart = maxOf(0, columns - w)
        val xRange = (preferredX..maxColStart) + (0 until preferredX.coerceAtMost(maxColStart + 1))

        for (y in preferredY until maxSearchY + 10) {
            for (x in xRange) {
                if (x in 0..maxColStart) {
                    if (isAreaFree(placed, x, y, w, h)) {
                        return Pair(x, y)
                    }
                }
            }
        }

        for (y in 0 until preferredY) {
            for (x in xRange) {
                if (x in 0..maxColStart) {
                    if (isAreaFree(placed, x, y, w, h)) {
                        return Pair(x, y)
                    }
                }
            }
        }

        return Pair(0, 0)
    }

    fun findFreeSpot(
        tiles: List<Tile>,
        columns: Int,
        rows: Int,
        w: Int,
        h: Int
    ): Pair<Int, Int>? {
        val maxRow = (rows - h).coerceAtLeast(0)
        val maxCol = (columns - w).coerceAtLeast(0)
        for (y in 0..maxRow) {
            for (x in 0..maxCol) {
                if (isAreaFree(tiles, x, y, w, h)) {
                    return Pair(x, y)
                }
            }
        }
        return null
    }

    fun findBestSpotOrShrink(
        tiles: List<Tile>,
        columns: Int,
        rows: Int,
        initialW: Int,
        initialH: Int
    ): Triple<Int, Int, Pair<Int, Int>>? {
        var targetW = initialW.coerceIn(1, columns)
        var targetH = initialH.coerceIn(1, rows)

        while (targetW >= 1 && targetH >= 1) {
            val spot = findFreeSpot(tiles, columns, rows, targetW, targetH)
            if (spot != null) {
                return Triple(targetW, targetH, spot)
            }
            if (targetW > 1 && targetW >= targetH) {
                targetW--
            } else if (targetH > 1) {
                targetH--
            } else {
                break
            }
        }
        return null
    }

    fun addTileFromPreset(
        context: Context,
        spaceId: String,
        tiles: List<Tile>,
        preset: TilePreset,
        columns: Int,
        rows: Int
    ): List<Tile>? {
        val spot = findFreeSpot(tiles, columns, rows, preset.w, preset.h)
            ?: return null

        val newTile = Tile(
            id = UUID.randomUUID().toString(),
            x = spot.first,
            y = spot.second,
            w = preset.w,
            h = preset.h,
            type = preset.type,
            payload = preset.payload,
            locked = false,
            clickable = preset.clickable,
            config = null
        )

        val updated = tiles + newTile
        setTiles(context, spaceId, updated)
        return updated
    }

    fun addTile(
        context: Context,
        spaceId: String,
        tiles: List<Tile>,
        newTile: Tile,
        columns: Int,
        rows: Int
    ): List<Tile>? {
        val spot = findFreeSpot(tiles, columns, rows, newTile.w, newTile.h)
            ?: return null

        val placed = newTile.copy(x = spot.first, y = spot.second)
        val updated = tiles + placed
        setTiles(context, spaceId, updated)
        return updated
    }

    fun removeTile(
        context: Context,
        spaceId: String,
        tiles: List<Tile>,
        tileId: String
    ): List<Tile>? {
        val target = tiles.firstOrNull { it.id == tileId } ?: return null
        if (target.locked) return null

        val updated = tiles.filterNot { it.id == tileId }
        setTiles(context, spaceId, updated)
        return updated
    }

    fun resizeTile(
        context: Context,
        spaceId: String,
        tiles: List<Tile>,
        tileId: String,
        newW: Int,
        newH: Int,
        columns: Int,
        rows: Int
    ): List<Tile>? {
        val target = tiles.firstOrNull { it.id == tileId } ?: return null

        val w = newW.coerceAtLeast(1)
        val h = newH.coerceAtLeast(1)

        if (target.x + w > columns) return null
        if (target.y + h > rows) return null
        if (!isAreaFree(tiles, target.x, target.y, w, h, ignoreId = tileId)) return null

        val updated = tiles.map { tile ->
            if (tile.id == tileId) tile.copy(w = w, h = h) else tile
        }
        setTiles(context, spaceId, updated)
        return updated
    }

    fun updateStyle(
        context: Context,
        spaceId: String,
        tiles: List<Tile>,
        tileId: String,
        newStyle: TileStyle
    ): List<Tile>? {
        val target = tiles.firstOrNull { it.id == tileId } ?: return null
        val updated = tiles.map { tile ->
            if (tile.id == tileId) tile.copy(style = newStyle) else tile
        }
        setTiles(context, spaceId, updated)
        return updated
    }

    fun updateConfig(
        context: Context,
        spaceId: String,
        tiles: List<Tile>,
        tileId: String,
        newConfig: String?
    ): List<Tile>? {
        val target = tiles.firstOrNull { it.id == tileId } ?: return null
        val updated = tiles.map { tile ->
            if (tile.id == tileId) tile.copy(config = newConfig) else tile
        }
        setTiles(context, spaceId, updated)
        return updated
    }
}