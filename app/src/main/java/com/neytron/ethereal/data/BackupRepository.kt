package com.neytron.ethereal.data

import android.content.Context
import com.neytron.ethereal.tiles.SpaceRepository
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TileLayoutRepository
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.tiles.TileType
import com.neytron.ethereal.ui.navigation.ScreenStack
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BackupRepository {

    /**
     * Экспортирует настройки, список пространств и все тайлы в JSON-строку.
     */
    fun exportToJson(context: Context): String {
        val root = JSONObject()

        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // === 1) Настройки ===
        val settingsObj = JSONObject().apply {
            put("palette_id", SettingsRepository.getPaletteId(context))
            put("font_family", SettingsRepository.getFont(context))
            put("font_scale", SettingsRepository.getFontScale(context))
            put("panel_opacity", SettingsRepository.getOpacity(context))
            put("panel_blur", SettingsRepository.getBlur(context))

            val dockSet = JSONArray()
            SettingsRepository.getDockPackages(context).forEach { dockSet.put(it) }
            put("dock_packages", dockSet)

            put("dock_icon_size", SettingsRepository.getDockIconSize(context))
            put("dock_bottom_padding", SettingsRepository.getDockBottomPadding(context))
            put("dock_use_override", SettingsRepository.getDockUseOverride(context))
            put("dock_opacity", SettingsRepository.getDockOpacity(context))
            put("dock_blur", SettingsRepository.getDockBlur(context))
            put("dock_scrollable", SettingsRepository.getDockScrollable(context))
            put("dock_icon_shape", SettingsRepository.getDockIconShape(context))
            put("dock_show_icon_bg", SettingsRepository.getDockShowIconBackground(context))
            put("dock_themed_icons", SettingsRepository.getDockThemedIcons(context))

            put("esearch_use_override", SettingsRepository.getESearchUseOverride(context))
            put("esearch_opacity", SettingsRepository.getESearchOpacity(context))
            put("esearch_blur", SettingsRepository.getESearchBlur(context))
            put("esearch_width", SettingsRepository.getESearchWidth(context))
            put("esearch_visible_items", SettingsRepository.getESearchVisibleItems(context))
            put("esearch_auto_keyboard", SettingsRepository.getESearchAutoKeyboard(context))

            put("tile_size", SettingsRepository.getTileSize(context))
            put("tile_spacing", SettingsRepository.getTileSpacing(context))
            put("tile_radius", SettingsRepository.getTileRadius(context))
            put("tile_columns", SettingsRepository.getTileColumns(context))

            put("wallpaper_scale_mode", SettingsRepository.getWallpaperScaleMode(context))
            put("analytics_enabled", SettingsRepository.getAnalyticsEnabled(context))
        }
        root.put("settings", settingsObj)

        // === 2) Пространства ===
        val spacesList = SpaceRepository.getAllSpaces(context)
        val spacesArr = JSONArray()
        spacesList.forEach { space ->
            spacesArr.put(JSONObject().apply {
                put("id", space.id)
                put("name", space.name)
                put("iconUri", space.iconUri ?: "")
                put("iconText", space.iconText ?: "")
                put("createdAt", space.createdAt)
            })
        }
        root.put("spaces", spacesArr)

        // === 3) Раскладки тайлов ===
        val layoutsObj = JSONObject()

        // Root
        val rootTiles = TileLayoutRepository.getTiles(context, ScreenStack.ROOT_ID)
        layoutsObj.put(ScreenStack.ROOT_ID, serializeTiles(rootTiles))

        // Custom spaces
        spacesList.forEach { space ->
            val tiles = TileLayoutRepository.getTiles(context, space.id)
            layoutsObj.put(space.id, serializeTiles(tiles))
        }

        root.put("layouts", layoutsObj)

        return root.toString(2)
    }

    /**
     * Импортирует настройки, пространства и тайлы из JSON-строки.
     * Возвращает true при успешном восстановлении.
     */
    fun importFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            // === 1) Настройки ===
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")

                if (s.has("palette_id")) SettingsRepository.setPaletteId(context, s.getString("palette_id"))
                if (s.has("font_family")) SettingsRepository.setFont(context, s.getString("font_family"))
                if (s.has("font_scale")) SettingsRepository.setFontScale(context, s.getDouble("font_scale").toFloat())
                if (s.has("panel_opacity")) SettingsRepository.setOpacity(context, s.getDouble("panel_opacity").toFloat())
                if (s.has("panel_blur")) SettingsRepository.setBlur(context, s.getDouble("panel_blur").toFloat())

                if (s.has("dock_packages")) {
                    val arr = s.getJSONArray("dock_packages")
                    val set = mutableSetOf<String>()
                    for (i in 0 until arr.length()) set.add(arr.getString(i))
                    SettingsRepository.setDockPackages(context, set)
                }

                if (s.has("dock_icon_size")) SettingsRepository.setDockIconSize(context, s.getDouble("dock_icon_size").toFloat())
                if (s.has("dock_bottom_padding")) SettingsRepository.setDockBottomPadding(context, s.getDouble("dock_bottom_padding").toFloat())
                if (s.has("dock_use_override")) SettingsRepository.setDockUseOverride(context, s.getBoolean("dock_use_override"))
                if (s.has("dock_opacity")) SettingsRepository.setDockOpacity(context, s.getDouble("dock_opacity").toFloat())
                if (s.has("dock_blur")) SettingsRepository.setDockBlur(context, s.getDouble("dock_blur").toFloat())
                if (s.has("dock_scrollable")) SettingsRepository.setDockScrollable(context, s.getBoolean("dock_scrollable"))
                if (s.has("dock_icon_shape")) SettingsRepository.setDockIconShape(context, s.getString("dock_icon_shape"))
                if (s.has("dock_show_icon_bg")) SettingsRepository.setDockShowIconBackground(context, s.getBoolean("dock_show_icon_bg"))
                if (s.has("dock_themed_icons")) SettingsRepository.setDockThemedIcons(context, s.getBoolean("dock_themed_icons"))

                if (s.has("esearch_use_override")) SettingsRepository.setESearchUseOverride(context, s.getBoolean("esearch_use_override"))
                if (s.has("esearch_opacity")) SettingsRepository.setESearchOpacity(context, s.getDouble("esearch_opacity").toFloat())
                if (s.has("esearch_blur")) SettingsRepository.setESearchBlur(context, s.getDouble("esearch_blur").toFloat())
                if (s.has("esearch_width")) SettingsRepository.setESearchWidth(context, s.getDouble("esearch_width").toFloat())
                if (s.has("esearch_visible_items")) SettingsRepository.setESearchVisibleItems(context, s.getInt("esearch_visible_items"))
                if (s.has("esearch_auto_keyboard")) SettingsRepository.setESearchAutoKeyboard(context, s.getBoolean("esearch_auto_keyboard"))

                if (s.has("tile_size")) SettingsRepository.setTileSize(context, s.getDouble("tile_size").toFloat())
                if (s.has("tile_spacing")) SettingsRepository.setTileSpacing(context, s.getDouble("tile_spacing").toFloat())
                if (s.has("tile_radius")) SettingsRepository.setTileRadius(context, s.getDouble("tile_radius").toFloat())
                if (s.has("tile_columns")) SettingsRepository.setTileColumns(context, s.getInt("tile_columns"))

                if (s.has("wallpaper_scale_mode")) SettingsRepository.setWallpaperScaleMode(context, s.getString("wallpaper_scale_mode"))
                if (s.has("analytics_enabled")) SettingsRepository.setAnalyticsEnabled(context, s.getBoolean("analytics_enabled"))
            }

            // === 2) Пространства ===
            if (root.has("spaces")) {
                val spacesArr = root.getJSONArray("spaces")
                val spacesList = mutableListOf<SpaceRepository.SpaceMeta>()
                for (i in 0 until spacesArr.length()) {
                    val obj = spacesArr.getJSONObject(i)
                    spacesList.add(
                        SpaceRepository.SpaceMeta(
                            id = obj.getString("id"),
                            name = obj.optString("name", "Space").ifBlank { "Space" },
                            iconUri = obj.optString("iconUri", "").takeIf { it.isNotEmpty() },
                            iconText = obj.optString("iconText", "").takeIf { it.isNotEmpty() },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                // Записываем пространства в SharedPreferences напрямую через JSONArray
                val prefs = context.getSharedPreferences("ethereal_settings", Context.MODE_PRIVATE)
                val outSpaces = JSONArray()
                spacesList.forEach { meta ->
                    outSpaces.put(JSONObject().apply {
                        put("id", meta.id)
                        put("name", meta.name)
                        put("iconUri", meta.iconUri ?: "")
                        put("iconText", meta.iconText ?: "")
                        put("createdAt", meta.createdAt)
                    })
                }
                prefs.edit().putString("spaces_meta", outSpaces.toString()).apply()
            }

            // === 3) Раскладки ===
            if (root.has("layouts")) {
                val layoutsObj = root.getJSONObject("layouts")
                val keys = layoutsObj.keys()
                while (keys.hasNext()) {
                    val spaceId = keys.next()
                    val arr = layoutsObj.getJSONArray(spaceId)
                    val tiles = deserializeTiles(arr)
                    TileLayoutRepository.setTiles(context, spaceId, tiles)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun serializeTiles(tiles: List<Tile>): JSONArray {
        val arr = JSONArray()
        tiles.forEach { tile ->
            arr.put(JSONObject().apply {
                put("id", tile.id)
                put("x", tile.x)
                put("y", tile.y)
                put("w", tile.w)
                put("h", tile.h)
                put("type", tile.type.name)
                put("payload", tile.payload ?: "")
                put("locked", tile.locked)
                put("clickable", tile.clickable)
                put("config", tile.config ?: "")
                put("style", JSONObject().apply {
                    put("bgOpacity", tile.style.backgroundOpacity)
                    put("bgBlur", tile.style.backgroundBlur)
                    put("contentScale", tile.style.contentScale)
                    if (tile.style.themedIcon != null) {
                        put("themedIcon", tile.style.themedIcon)
                    }
                })
            })
        }
        return arr
    }

    private fun deserializeTiles(arr: JSONArray): List<Tile> {
        val list = mutableListOf<Tile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val styleObj = obj.optJSONObject("style")
            list.add(
                Tile(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    x = obj.optInt("x", 0),
                    y = obj.optInt("y", 0),
                    w = obj.optInt("w", 1),
                    h = obj.optInt("h", 1),
                    type = try { TileType.valueOf(obj.getString("type")) } catch (_: Exception) { TileType.EMPTY },
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
        return list
    }
}