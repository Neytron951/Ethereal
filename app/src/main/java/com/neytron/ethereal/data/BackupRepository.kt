package com.neytron.ethereal.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.neytron.ethereal.tiles.SpaceRepository
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TileLayoutRepository
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.tiles.TileType
import com.neytron.ethereal.ui.navigation.ScreenStack
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import com.neytron.ethereal.widgets.EtherealAppWidgetHost
import com.neytron.ethereal.widgets.WidgetHostManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID

object BackupRepository {

    /**
     * Экспортирует настройки, список пространств и все тайлы в JSON-строку.
     */
    fun exportToJson(
        context: Context,
        includeSettings: Boolean = true,
        includeSpacesAndLayouts: Boolean = true,
        includeWallpapersAndAssets: Boolean = true
    ): String {
        val root = JSONObject()

        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        // === 1) Настройки ===
        if (includeSettings) {
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
                put("icon_pack_package", SettingsRepository.getIconPackPackage(context) ?: JSONObject.NULL)

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
        } else {
            root.put("settings", JSONObject())
        }

        // === 2) Пространства ===
        val spacesList = if (includeSpacesAndLayouts) SpaceRepository.getAllSpaces(context) else emptyList()
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
        if (includeSpacesAndLayouts) {
            val rootTiles = TileLayoutRepository.getTiles(context, ScreenStack.ROOT_ID)
            layoutsObj.put(ScreenStack.ROOT_ID, serializeTiles(context, rootTiles))

            spacesList.forEach { space ->
                val tiles = TileLayoutRepository.getTiles(context, space.id)
                layoutsObj.put(space.id, serializeTiles(context, tiles))
            }
        } else {
            layoutsObj.put(ScreenStack.ROOT_ID, JSONArray())
        }
        root.put("layouts", layoutsObj)

        val assets = if (includeWallpapersAndAssets) collectAssets(context) else emptyList()
        val assetRefs = assets.mapNotNull { asset ->
            val src = asset.source ?: return@mapNotNull null
            val path = try { src.canonicalPath } catch (_: Exception) { src.absolutePath }
            if (path.isNullOrEmpty()) null else path to "ethereal-asset://${asset.id}"
        }.toMap()
        root.put("assets", JSONArray().apply {
            assets.forEach { asset ->
                put(JSONObject().apply {
                    put("id", asset.id)
                    put("kind", asset.kind)
                    put("extension", asset.extension)
                    put("sha256", asset.sha256)
                    put("data", Base64.encodeToString(asset.bytes, Base64.NO_WRAP))
                })
            }
        })
        val transformed = replaceAssetReferences(root, assetRefs) as JSONObject
        return transformed.toString(2)
    }

    /**
     * Импортирует настройки, пространства и тайлы из JSON-строки.
     * Возвращает true при успешном восстановлении.
     */
    fun importFromJson(
        context: Context,
        jsonString: String,
        includeSettings: Boolean = true,
        includeSpacesAndLayouts: Boolean = true,
        includeWallpapersAndAssets: Boolean = true
    ): Boolean {
        var transaction: AssetRestoreTransaction? = null
        return try {
            require(jsonString.length <= 180 * 1024 * 1024) { "Backup file is too large" }
            val source = JSONObject(jsonString)
            val version = source.optInt("version", -1)
            require(version in 1..2) { "Unsupported backup version: $version" }

            val assets = if (includeWallpapersAndAssets) parseAssets(source.optJSONArray("assets")) else emptyList()
            val assetTargets = assets.associate { asset ->
                "ethereal-asset://${asset.id}" to targetForAsset(context, asset)
            }
            val assetPathReferences = assetTargets.mapValues { (_, file) -> file.absolutePath }
            val root = replaceAssetReferences(source, assetPathReferences) as JSONObject

            val preferences = context.getSharedPreferences("ethereal_settings", Context.MODE_PRIVATE)
            val editor = preferences.edit()

            if (includeSettings && root.has("settings")) {
                val settings = root.getJSONObject("settings")
                if (settings.length() > 0) {
                    validateSettings(settings)
                    applySettings(editor, settings)
                }
            }

            if (includeSpacesAndLayouts && root.has("spaces") && root.has("layouts")) {
                val spacesArray = root.getJSONArray("spaces")
                val spaces = parseSpaces(spacesArray)
                val layouts = parseLayouts(context, root.getJSONObject("layouts"), spaces)
                editor.putString("spaces_meta", serializeSpaces(spaces).toString())

                val validSpaceIds = spaces.mapTo(mutableSetOf()) { it.id }
                preferences.all.keys
                    .filter { it.startsWith("tiles_space_") && it.removePrefix("tiles_space_") !in validSpaceIds }
                    .forEach(editor::remove)
                layouts.forEach { (spaceId, tiles) ->
                    val key = if (spaceId == ScreenStack.ROOT_ID) "tiles_layout" else "tiles_space_$spaceId"
                    editor.putString(key, serializeTiles(context, tiles).toString())
                }
            }

            if (includeWallpapersAndAssets && assets.isNotEmpty()) {
                transaction = restoreAssets(context, assets, assetTargets)
            }

            if (!editor.commit()) {
                transaction?.rollback()
                false
            } else {
                transaction?.finish()
                true
            }
        } catch (e: Exception) {
            transaction?.rollback()
            e.printStackTrace()
            false
        }
    }

    private fun serializeTiles(context: Context, tiles: List<Tile>): JSONArray {
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
                if (tile.type == TileType.WIDGET) {
                    val widgetId = EtherealAppWidgetHost.decodeWidgetId(tile.config)
                    val providerName = widgetId?.let {
                        try {
                            AppWidgetManager.getInstance(context).getAppWidgetInfo(it)?.provider?.flattenToString()
                        } catch (_: Exception) { null }
                    }
                    if (!providerName.isNullOrEmpty()) {
                        put("widgetProvider", providerName)
                    }
                }
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

    private fun deserializeTiles(context: Context, arr: JSONArray): List<Tile> {
        val list = mutableListOf<Tile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val styleObj = obj.optJSONObject("style")
            val type = TileType.valueOf(obj.getString("type"))
            var config = obj.optString("config").takeIf { it.isNotEmpty() }

            if (type == TileType.WIDGET) {
                val providerName = obj.optString("widgetProvider").takeIf { it.isNotEmpty() }
                if (providerName != null) {
                    try {
                        val host = WidgetHostManager.getHost(context)
                        val newWidgetId = host.allocateAppWidgetId()
                        val component = ComponentName.unflattenFromString(providerName)
                        if (component != null) {
                            val manager = AppWidgetManager.getInstance(context)
                            manager.bindAppWidgetIdIfAllowed(newWidgetId, component)
                        }
                        config = EtherealAppWidgetHost.encodeWidgetId(newWidgetId)
                    } catch (_: Exception) {}
                }
            }

            list.add(
                Tile(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    x = obj.optInt("x", 0),
                    y = obj.optInt("y", 0),
                    w = obj.optInt("w", 1),
                    h = obj.optInt("h", 1),
                    type = type,
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
                    config = config
                )
            )
        }
        return list
    }

    private data class BackupAsset(
        val id: String,
        val kind: String,
        val extension: String,
        val sha256: String,
        val bytes: ByteArray,
        val source: File? = null
    )

    private data class AssetRestoreTransaction(
        val addedFiles: List<File>,
        val currentWallpaper: File?,
        val previousWallpaper: File?
    ) {
        fun rollback() {
            addedFiles.forEach { it.delete() }
            if (currentWallpaper != null) {
                if (previousWallpaper?.exists() == true) {
                    previousWallpaper.copyTo(currentWallpaper, overwrite = true)
                } else {
                    currentWallpaper.delete()
                }
            }
            previousWallpaper?.delete()
        }

        fun finish() {
            previousWallpaper?.delete()
        }
    }

    private fun collectAssets(context: Context): List<BackupAsset> {
        val files = mutableListOf<Pair<File, String>>()
        val currentWallpaper = WallpaperRepository.getCurrentFile(context)
        if (currentWallpaper.isFile) files += currentWallpaper to "current_wallpaper"

        File(context.filesDir, "tile_images").listFiles()
            ?.filter { it.isFile }
            ?.forEach { files += it to "tile_image" }
        File(context.filesDir, "space_icons").listFiles()
            ?.filter { it.isFile }
            ?.forEach { files += it to "space_icon" }

        var totalBytes = 0L
        return files.mapNotNull { (file, kind) ->
            try {
                if (!file.exists() || !file.isFile) return@mapNotNull null
                val len = file.length()
                if (len > 32L * 1024 * 1024) return@mapNotNull null
                totalBytes += len
                if (totalBytes > 128L * 1024 * 1024) return@mapNotNull null
                val bytes = file.readBytes()
                val compressedBytes = compressImageBytes(bytes)
                BackupAsset(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    extension = "jpg",
                    sha256 = sha256(compressedBytes),
                    bytes = compressedBytes,
                    source = file
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun compressImageBytes(bytes: ByteArray, maxDim: Int = 1024, quality: Int = 80): ByteArray {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            val w = options.outWidth
            val h = options.outHeight
            if (w <= 0 || h <= 0) return bytes

            var sampleSize = 1
            var scaleW = w
            var scaleH = h
            while (scaleW > maxDim || scaleH > maxDim) {
                scaleW /= 2
                scaleH /= 2
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return bytes

            val finalBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                val fw = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                val fh = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, fw, fh, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            if (finalBitmap != bitmap) finalBitmap.recycle()
            bitmap.recycle()
            stream.toByteArray()
        } catch (_: Exception) {
            bytes
        }
    }

    private fun parseAssets(array: JSONArray?): List<BackupAsset> {
        if (array == null) return emptyList()
        val result = mutableListOf<BackupAsset>()
        val ids = mutableSetOf<String>()
        var totalBytes = 0L
        var currentWallpaperCount = 0

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            require(id.matches(Regex("[0-9a-fA-F-]{36}")) && ids.add(id)) { "Invalid or duplicate asset id" }
            val kind = item.getString("kind")
            require(kind in setOf("current_wallpaper", "wallpaper", "tile_image", "space_icon")) { "Unknown asset type" }
            if (kind == "current_wallpaper") currentWallpaperCount++
            require(currentWallpaperCount <= 1) { "Multiple current wallpapers" }
            val extension = item.getString("extension").lowercase()
            require(extension in setOf("jpg", "jpeg", "png", "webp", "gif")) { "Invalid asset extension" }
            val expectedHash = item.getString("sha256")
            require(expectedHash.matches(Regex("[0-9a-f]{64}"))) { "Invalid asset checksum" }
            val encoded = item.getString("data")
            require(encoded.length <= 45 * 1024 * 1024) { "An encoded image is too large" }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            totalBytes += bytes.size
            require(bytes.size <= 32 * 1024 * 1024 && totalBytes <= 128 * 1024 * 1024) {
                "Backup images exceed the import size limit"
            }
            require(sha256(bytes) == expectedHash) { "Asset checksum mismatch" }
            result += BackupAsset(id, kind, extension, expectedHash, bytes)
        }
        return result
    }

    private fun targetForAsset(context: Context, asset: BackupAsset): File {
        val dir = when (asset.kind) {
            "current_wallpaper" -> return WallpaperRepository.getCurrentFile(context)
            "wallpaper" -> File(context.filesDir, "wallpapers")
            "space_icon" -> File(context.filesDir, "space_icons")
            else -> File(context.filesDir, "tile_images")
        }
        if (!dir.exists()) dir.mkdirs()

        // Дедупликация: если файл с таким же хешем уже существует, переиспользовать его
        val existingFile = dir.listFiles()?.firstOrNull { file ->
            file.isFile && try {
                sha256(file.readBytes()) == asset.sha256
            } catch (_: Exception) { false }
        }

        if (existingFile != null) {
            return existingFile
        }

        val prefix = when (asset.kind) {
            "wallpaper" -> "wallpaper_user_restore_"
            "space_icon" -> "space_restore_"
            else -> "tile_restore_"
        }
        return File(dir, "$prefix${asset.id}.${asset.extension}")
    }

    private fun restoreAssets(
        context: Context,
        assets: List<BackupAsset>,
        references: Map<String, File>
    ): AssetRestoreTransaction {
        val addedFiles = mutableListOf<File>()
        val current = assets.firstOrNull { it.kind == "current_wallpaper" }
            ?.let { references["ethereal-asset://${it.id}"] }
        val previous = current?.takeIf { it.exists() }?.let {
            File(context.cacheDir, "wallpaper_before_restore_${UUID.randomUUID()}").also { backup ->
                it.copyTo(backup, overwrite = true)
            }
        }

        try {
            assets.forEach { asset ->
                val destination = references.getValue("ethereal-asset://${asset.id}")
                destination.parentFile?.let { parent ->
                    if (!parent.exists() && !parent.mkdirs()) {
                        error("Cannot create image directory")
                    }
                }
                if (destination != current && !destination.exists()) {
                    addedFiles += destination
                }
                destination.outputStream().use { it.write(asset.bytes) }
            }
            return AssetRestoreTransaction(addedFiles, current, previous)
        } catch (e: Exception) {
            addedFiles.forEach { it.delete() }
            if (current != null) {
                if (previous?.exists() == true) previous.copyTo(current, overwrite = true)
                else current.delete()
            }
            previous?.delete()
            throw e
        }
    }

    private fun validateSettings(settings: JSONObject) {
        val stringKeys = listOf(
            "palette_id", "font_family", "dock_icon_shape", "wallpaper_scale_mode"
        )
        stringKeys.forEach { key ->
            if (settings.has(key)) require(settings.get(key) is String) { "Invalid setting: $key" }
        }
        if (settings.has("wallpaper_scale_mode")) {
            require(settings.getString("wallpaper_scale_mode") in setOf("CROP", "FIT", "STRETCH", "CENTER"))
        }
        if (settings.has("icon_pack_package") && !settings.isNull("icon_pack_package")) {
            val packageName = settings.get("icon_pack_package")
            require(packageName is String && packageName.matches(Regex("[A-Za-z0-9_.]{1,255}"))) {
                "Invalid icon pack"
            }
        }

        val floatRanges = mapOf(
            "font_scale" to (0.7..1.5),
            "panel_opacity" to (0.0..1.0),
            "panel_blur" to (0.0..25.0),
            "dock_icon_size" to (36.0..72.0),
            "dock_bottom_padding" to (0.0..80.0),
            "dock_opacity" to (0.0..1.0),
            "dock_blur" to (0.0..25.0),
            "esearch_opacity" to (0.0..1.0),
            "esearch_blur" to (0.0..25.0),
            "esearch_width" to (0.5..0.95),
            "tile_size" to (32.0..96.0),
            "tile_spacing" to (4.0..24.0),
            "tile_radius" to (0.0..32.0)
        )
        floatRanges.forEach { (key, range) ->
            if (settings.has(key)) {
                val raw = settings.get(key)
                require(raw is Number) { "Invalid setting: $key" }
                val value = raw.toDouble()
                require(value.isFinite() && value in range) { "Out-of-range setting: $key" }
            }
        }

        val booleanKeys = listOf(
            "dock_use_override", "dock_scrollable", "dock_show_icon_bg",
            "dock_themed_icons", "esearch_use_override", "esearch_auto_keyboard",
            "analytics_enabled"
        )
        booleanKeys.forEach { key ->
            if (settings.has(key)) require(settings.get(key) is Boolean) { "Invalid setting: $key" }
        }

        val integerRanges = mapOf("esearch_visible_items" to (3..12), "tile_columns" to (3..12))
        integerRanges.forEach { (key, range) ->
            if (settings.has(key)) {
                val raw = settings.get(key)
                require(raw is Number && raw.toInt().toDouble() == raw.toDouble() && raw.toInt() in range) {
                    "Invalid setting: $key"
                }
            }
        }
        if (settings.has("dock_packages")) {
            val packages = settings.getJSONArray("dock_packages")
            for (index in 0 until packages.length()) {
                val packageName = packages.get(index)
                require(packageName is String && packageName.matches(Regex("[A-Za-z0-9_.]{1,255}"))) {
                    "Invalid dock package"
                }
            }
        }
    }

    private fun applySettings(editor: android.content.SharedPreferences.Editor, settings: JSONObject) {
        val strings = mapOf(
            "palette_id" to "palette_id",
            "font_family" to "font_family",
            "dock_icon_shape" to "dock_icon_shape",
            "wallpaper_scale_mode" to "wallpaper_scale_mode"
        )
        strings.forEach { (source, target) ->
            if (settings.has(source)) editor.putString(target, settings.getString(source))
        }
        if (settings.has("icon_pack_package")) {
            if (settings.isNull("icon_pack_package")) editor.remove("icon_pack_package")
            else editor.putString("icon_pack_package", settings.getString("icon_pack_package"))
        }

        val floats = mapOf(
            "font_scale" to "font_scale",
            "panel_opacity" to "panel_opacity",
            "panel_blur" to "panel_blur",
            "dock_icon_size" to "dock_icon_size",
            "dock_bottom_padding" to "dock_bottom_padding",
            "dock_opacity" to "dock_opacity",
            "dock_blur" to "dock_blur",
            "esearch_opacity" to "esearch_opacity",
            "esearch_blur" to "esearch_blur",
            "esearch_width" to "esearch_width",
            "tile_size" to "tile_size",
            "tile_spacing" to "tile_spacing",
            "tile_radius" to "tile_radius"
        )
        floats.forEach { (source, target) ->
            if (settings.has(source)) editor.putFloat(target, settings.getDouble(source).toFloat())
        }

        val booleans = mapOf(
            "dock_use_override" to "dock_use_override",
            "dock_scrollable" to "dock_scrollable",
            "dock_show_icon_bg" to "dock_show_icon_bg",
            "dock_themed_icons" to "dock_themed_icons",
            "esearch_use_override" to "esearch_use_override",
            "esearch_auto_keyboard" to "esearch_auto_keyboard",
            "analytics_enabled" to "analytics_enabled"
        )
        booleans.forEach { (source, target) ->
            if (settings.has(source)) editor.putBoolean(target, settings.getBoolean(source))
        }

        val integers = mapOf(
            "esearch_visible_items" to "esearch_visible_items",
            "tile_columns" to "tile_columns"
        )
        integers.forEach { (source, target) ->
            if (settings.has(source)) editor.putInt(target, settings.getInt(source))
        }
        settings.optJSONArray("dock_packages")?.let { packages ->
            val values = buildSet {
                for (index in 0 until packages.length()) add(packages.getString(index))
            }
            editor.putStringSet("dock_packages", values)
        }
    }

    private fun parseSpaces(array: JSONArray): List<SpaceRepository.SpaceMeta> {
        val spaces = mutableListOf<SpaceRepository.SpaceMeta>()
        val ids = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            require(id.isNotBlank() && id.length <= 128 && id != ScreenStack.ROOT_ID && ids.add(id)) {
                "Invalid or duplicate space id"
            }
            require(item.optString("name", "Space").length <= 256)
            require(item.optString("iconText", "").length <= 8)
            spaces += SpaceRepository.SpaceMeta(
                id = id,
                name = item.optString("name", "Space").ifBlank { "Space" },
                iconUri = item.optString("iconUri", "").takeIf { it.isNotEmpty() },
                iconText = item.optString("iconText", "").takeIf { it.isNotEmpty() },
                createdAt = item.optLong("createdAt", System.currentTimeMillis())
            )
        }
        return spaces
    }

    private fun parseLayouts(
        context: Context,
        layouts: JSONObject,
        spaces: List<SpaceRepository.SpaceMeta>
    ): Map<String, List<Tile>> {
        val allowedIds = spaces.mapTo(mutableSetOf()) { it.id }.apply { add(ScreenStack.ROOT_ID) }
        val keys = layouts.keys().asSequence().toSet()
        require(ScreenStack.ROOT_ID in keys && keys.all { it in allowedIds }) {
            "Backup layouts do not match its spaces"
        }
        require(spaces.all { it.id in keys }) { "A space layout is missing" }

        return keys.associateWith { spaceId ->
            val array = layouts.getJSONArray(spaceId)
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                require(item.has("id") && item.has("x") && item.has("y") &&
                        item.has("w") && item.has("h") && item.has("type")) {
                    "Incomplete tile data"
                }
                require(item.getInt("x") >= 0 && item.getInt("y") >= 0 &&
                        item.getInt("x") <= 10_000 && item.getInt("y") <= 10_000 &&
                        item.getInt("w") in 1..12 && item.getInt("h") in 1..10_000 &&
                        item.getString("id").isNotBlank() && item.getString("id").length <= 128) {
                    "Invalid tile bounds"
                }
                TileType.valueOf(item.getString("type"))
                item.optJSONObject("style")?.let { style ->
                    validateTileStyle(style, "bgOpacity", 0.0..1.0)
                    validateTileStyle(style, "bgBlur", 0.0..25.0)
                    validateTileStyle(style, "contentScale", 0.5..2.0)
                }
            }
            deserializeTiles(context, array)
        }
    }

    private fun serializeSpaces(spaces: List<SpaceRepository.SpaceMeta>): JSONArray =
        JSONArray().apply {
            spaces.forEach { space ->
                put(JSONObject().apply {
                    put("id", space.id)
                    put("name", space.name)
                    put("iconUri", space.iconUri ?: "")
                    put("iconText", space.iconText ?: "")
                    put("createdAt", space.createdAt)
                })
            }
        }

    private fun validateTileStyle(style: JSONObject, key: String, range: ClosedRange<Double>) {
        if (!style.has(key)) return
        val raw = style.get(key)
        require(raw is Number && raw.toDouble().isFinite() && raw.toDouble() in range) {
            "Invalid tile style: $key"
        }
    }

    private fun replaceAssetReferences(value: Any, references: Map<String, String>): Any =
        when (value) {
            is JSONObject -> JSONObject().apply {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, rewriteValue(value.get(key), references))
                }
            }
            is JSONArray -> JSONArray().apply {
                for (index in 0 until value.length()) put(rewriteValue(value.get(index), references))
            }
            else -> value
        }

    private fun rewriteValue(value: Any, references: Map<String, String>): Any =
        when (value) {
            is JSONObject -> replaceAssetReferences(value, references)
            is JSONArray -> replaceAssetReferences(value, references)
            is String -> {
                var res = references[value] ?: value
                if (res == value) {
                    for ((path, ref) in references) {
                        if (res.contains(path)) {
                            res = res.replace(path, ref)
                        }
                    }
                }
                res
            }
            else -> value
        }

    private fun containsAssetReference(value: Any): Boolean = when (value) {
        is JSONObject -> {
            val keys = value.keys()
            keys.asSequence().any { containsAssetReference(value.get(it)) }
        }
        is JSONArray -> (0 until value.length()).any { containsAssetReference(value.get(it)) }
        is String -> value.contains("ethereal-asset://")
        else -> false
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
}