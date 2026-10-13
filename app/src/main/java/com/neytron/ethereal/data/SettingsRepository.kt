package com.neytron.ethereal.data

import android.content.Context

object SettingsRepository {

    private const val PREFS = "ethereal_settings"

    // === Palette ===
    private const val KEY_PALETTE = "palette_id"

    // === Dock ===
    private const val KEY_DOCK = "dock_packages"
    private const val KEY_DOCK_ICON_SIZE = "dock_icon_size"
    private const val KEY_DOCK_BOTTOM_PADDING = "dock_bottom_padding"
    private const val KEY_DOCK_USE_OVERRIDE = "dock_use_override"
    private const val KEY_DOCK_OPACITY = "dock_opacity"
    private const val KEY_DOCK_BLUR = "dock_blur"
    private const val KEY_DOCK_SCROLLABLE = "dock_scrollable"
    private const val KEY_DOCK_ORDER = "dock_order"
    private const val KEY_DOCK_ICON_SHAPE = "dock_icon_shape"
    private const val KEY_DOCK_SHOW_BG = "dock_show_icon_bg"
    private const val KEY_DOCK_THEMED_ICONS = "dock_themed_icons"
    private const val KEY_ICON_PACK = "icon_pack_package"

    private const val DEFAULT_DOCK_ICON_SIZE = 50f
    private const val DEFAULT_DOCK_BOTTOM_PADDING = 32f

    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_WALLPAPER_SCALE_MODE = "wallpaper_scale_mode"
    private const val DEFAULT_WALLPAPER_SCALE_MODE = "CROP"

    // === Appearance ===
    private const val KEY_OPACITY = "panel_opacity"
    private const val KEY_BLUR = "panel_blur"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_FONT = "font_family"

    private const val DEFAULT_OPACITY = 0.95f
    private const val DEFAULT_FONT_SCALE = 1.0f

    // === Launch stats ===
    private const val KEY_LAUNCH_COUNT = "launch_count_"
    private const val KEY_LAST_LAUNCH = "last_launch_"

    // === ESearch ===
    private const val KEY_ESEARCH_USE_OVERRIDE = "esearch_use_override"
    private const val KEY_ESEARCH_OPACITY = "esearch_opacity"
    private const val KEY_ESEARCH_BLUR = "esearch_blur"
    private const val KEY_ESEARCH_WIDTH = "esearch_width"
    private const val KEY_ESEARCH_VISIBLE_ITEMS = "esearch_visible_items"
    private const val KEY_ESEARCH_AUTO_KEYBOARD = "esearch_auto_keyboard"

    private const val DEFAULT_ESEARCH_WIDTH = 0.75f
    private const val DEFAULT_ESEARCH_VISIBLE = 6


    // === Tiles (сетка) ===
    private const val KEY_TILE_SIZE = "tile_size"
    private const val KEY_TILE_COLUMNS = "tile_columns"
    private const val KEY_TILE_SPACING = "tile_spacing"
    private const val KEY_TILE_RADIUS = "tile_radius"

    private const val DEFAULT_TILE_SIZE = 64f
    private const val DEFAULT_TILE_COLUMNS = 4
    private const val DEFAULT_TILE_SPACING = 8f
    private const val DEFAULT_TILE_RADIUS = 16f


    private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"


    // === Virtual Apps (законсервировано) ===
    private const val KEY_VIRTUAL_DIALER_ENABLED = "virtual_dialer_enabled"

    // === Навигация ===
    private const val KEY_LAST_SPACE_ID = "last_space_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ============================================================
    // Palette
    // ============================================================

    fun getPaletteId(context: Context): String =
        prefs(context).getString(KEY_PALETTE, "ethereal_green") ?: "ethereal_green"

    fun setPaletteId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_PALETTE, id).apply()
    }

    // ============================================================
    // Dock
    // ============================================================

    fun getDockPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_DOCK, null) ?: emptySet()

    fun setDockPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_DOCK, packages).apply()
    }

    fun getDockIconSize(context: Context): Float =
        prefs(context).getFloat(KEY_DOCK_ICON_SIZE, DEFAULT_DOCK_ICON_SIZE)
    fun setDockIconSize(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_DOCK_ICON_SIZE, value.coerceIn(36f, 72f)).apply()
    }

    fun getDockBottomPadding(context: Context): Float =
        prefs(context).getFloat(KEY_DOCK_BOTTOM_PADDING, DEFAULT_DOCK_BOTTOM_PADDING)
    fun setDockBottomPadding(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_DOCK_BOTTOM_PADDING, value.coerceIn(0f, 80f)).apply()
    }

    fun getDockUseOverride(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOCK_USE_OVERRIDE, false)
    fun setDockUseOverride(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOCK_USE_OVERRIDE, value).apply()
    }

    fun getDockOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_DOCK_OPACITY, 0.95f)
    fun setDockOpacity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_DOCK_OPACITY, value.coerceIn(0f, 1.0f)).apply()
    }

    fun getDockBlur(context: Context): Float =
        prefs(context).getFloat(KEY_DOCK_BLUR, 0f)
    fun setDockBlur(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_DOCK_BLUR, value.coerceIn(0f, 25f)).apply()
    }

    fun getDockScrollable(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOCK_SCROLLABLE, false)
    fun setDockScrollable(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOCK_SCROLLABLE, value).apply()
    }

    fun getDockOrder(context: Context): List<String> {
        val str = prefs(context).getString(KEY_DOCK_ORDER, null) ?: return emptyList()
        return if (str.isEmpty()) emptyList() else str.split("|")
    }
    fun setDockOrder(context: Context, order: List<String>) {
        prefs(context).edit().putString(KEY_DOCK_ORDER, order.joinToString("|")).apply()
    }

    fun getDockIconShape(context: Context): String =
        prefs(context).getString(KEY_DOCK_ICON_SHAPE, "SQUIRCLE_TIGHT") ?: "SQUIRCLE_TIGHT"
    fun setDockIconShape(context: Context, value: String) {
        prefs(context).edit().putString(KEY_DOCK_ICON_SHAPE, value).apply()
    }

    fun getDockShowIconBackground(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOCK_SHOW_BG, true)
    fun setDockShowIconBackground(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOCK_SHOW_BG, value).apply()
    }

    fun getDockThemedIcons(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOCK_THEMED_ICONS, false)
    fun setDockThemedIcons(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOCK_THEMED_ICONS, value).apply()
    }

    fun getIconPackPackage(context: Context): String? =
        prefs(context).getString(KEY_ICON_PACK, null)

    fun setIconPackPackage(context: Context, packageName: String?) {
        prefs(context).edit().putString(KEY_ICON_PACK, packageName).apply()
    }

    // ============================================================
    // Appearance
    // ============================================================

    fun getOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_OPACITY, DEFAULT_OPACITY)
    fun setOpacity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_OPACITY, value.coerceIn(0f, 1.0f)).apply()
    }

    fun getBlur(context: Context): Float =
        prefs(context).getFloat(KEY_BLUR, 0f)
    fun setBlur(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_BLUR, value.coerceIn(0f, 25f)).apply()
    }

    fun getFontScale(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE)
    fun setFontScale(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_FONT_SCALE, value.coerceIn(0.7f, 1.5f)).apply()
    }

    fun getFont(context: Context): String =
        prefs(context).getString(KEY_FONT, "DEFAULT") ?: "DEFAULT"
    fun setFont(context: Context, value: String) {
        prefs(context).edit().putString(KEY_FONT, value).apply()
    }

    // ============================================================
    // Launch stats
    // ============================================================

    fun recordLaunch(context: Context, packageName: String) {
        val p = prefs(context)
        val count = p.getInt(KEY_LAUNCH_COUNT + packageName, 0) + 1
        val now = System.currentTimeMillis()
        p.edit()
            .putInt(KEY_LAUNCH_COUNT + packageName, count)
            .putLong(KEY_LAST_LAUNCH + packageName, now)
            .apply()
    }

    fun getLaunchCount(context: Context, packageName: String): Int =
        prefs(context).getInt(KEY_LAUNCH_COUNT + packageName, 0)

    fun getLastLaunch(context: Context, packageName: String): Long =
        prefs(context).getLong(KEY_LAST_LAUNCH + packageName, 0L)

    // ============================================================
    // ESearch
    // ============================================================

    fun getESearchUseOverride(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ESEARCH_USE_OVERRIDE, false)
    fun setESearchUseOverride(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ESEARCH_USE_OVERRIDE, value).apply()
    }

    fun getESearchOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_ESEARCH_OPACITY, 0.95f)
    fun setESearchOpacity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_ESEARCH_OPACITY, value.coerceIn(0f, 1.0f)).apply()
    }

    fun getESearchBlur(context: Context): Float =
        prefs(context).getFloat(KEY_ESEARCH_BLUR, 0f)
    fun setESearchBlur(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_ESEARCH_BLUR, value.coerceIn(0f, 25f)).apply()
    }

    fun getESearchWidth(context: Context): Float =
        prefs(context).getFloat(KEY_ESEARCH_WIDTH, DEFAULT_ESEARCH_WIDTH)
    fun setESearchWidth(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_ESEARCH_WIDTH, value.coerceIn(0.5f, 0.95f)).apply()
    }

    fun getESearchVisibleItems(context: Context): Int =
        prefs(context).getInt(KEY_ESEARCH_VISIBLE_ITEMS, DEFAULT_ESEARCH_VISIBLE)
    fun setESearchVisibleItems(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_ESEARCH_VISIBLE_ITEMS, value.coerceIn(3, 12)).apply()
    }

    fun getESearchAutoKeyboard(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ESEARCH_AUTO_KEYBOARD, true)
    fun setESearchAutoKeyboard(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ESEARCH_AUTO_KEYBOARD, value).apply()
    }

    // ============================================================
    // Tiles (сетка)
    // ============================================================

    fun getTileSize(context: Context): Float =
        prefs(context).getFloat(KEY_TILE_SIZE, DEFAULT_TILE_SIZE)
    fun setTileSize(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_TILE_SIZE, value.coerceIn(32f, 96f)).apply()
    }

    fun getTileColumns(context: Context): Int =
        prefs(context).getInt(KEY_TILE_COLUMNS, DEFAULT_TILE_COLUMNS)
    fun setTileColumns(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_TILE_COLUMNS, value.coerceIn(3, 6)).apply()
    }

    fun getTileSpacing(context: Context): Float =
        prefs(context).getFloat(KEY_TILE_SPACING, DEFAULT_TILE_SPACING)
    fun setTileSpacing(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_TILE_SPACING, value.coerceIn(4f, 24f)).apply()
    }

    fun getTileRadius(context: Context): Float =
        prefs(context).getFloat(KEY_TILE_RADIUS, DEFAULT_TILE_RADIUS)
    fun setTileRadius(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_TILE_RADIUS, value.coerceIn(0f, 32f)).apply()
    }

    fun getWallpaperScaleMode(context: Context): String =
        prefs(context).getString(KEY_WALLPAPER_SCALE_MODE, DEFAULT_WALLPAPER_SCALE_MODE) ?: DEFAULT_WALLPAPER_SCALE_MODE

    fun setWallpaperScaleMode(context: Context, value: String) {
        prefs(context).edit().putString(KEY_WALLPAPER_SCALE_MODE, value).apply()
    }



    // ============================================================
    // Setup / Onboarding
    // ============================================================

    fun isSetupCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_COMPLETED, false)

    fun setSetupCompleted(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()
    }

    // ============================================================
    // Virtual Apps (законсервировано, но флаг оставлен)
    // ============================================================

    fun isVirtualDialerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VIRTUAL_DIALER_ENABLED, false)

    fun setVirtualDialerEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_VIRTUAL_DIALER_ENABLED, value).apply()
    }

    // ============================================================
    // Навигация
    // ============================================================

    /**
     * ID последнего открытого пространства.
     * Возвращает null, если ещё ни разу не сохранялось.
     * Используется при старте MainActivity для восстановления позиции.
     */
    fun getLastSpaceId(context: Context): String? =
        prefs(context).getString(KEY_LAST_SPACE_ID, null)

    fun setLastSpaceId(context: Context, spaceId: String) {
        prefs(context).edit().putString(KEY_LAST_SPACE_ID, spaceId).apply()
    }


    fun getAnalyticsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ANALYTICS_ENABLED, true)   // дефолт ВКЛ

    fun setAnalyticsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()
    }


}