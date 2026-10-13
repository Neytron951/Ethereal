package com.neytron.ethereal.ui

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import com.neytron.ethereal.BuildConfig
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.data.UpdateInfo
import com.neytron.ethereal.ui.theme.AllPalettes
import com.neytron.ethereal.ui.theme.EtherealFont
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.IconShape
import com.neytron.ethereal.ui.theme.LocalAppearance
import com.neytron.ethereal.ui.theme.fontFamilyFor
import com.neytron.ethereal.ui.theme.iconShapeFor
import com.neytron.ethereal.utils.toImageBitmap

enum class SettingsTab(val labelRes: Int) {
    APPEARANCE(R.string.tab_appearance),
    WALLPAPER(R.string.tab_wallpaper),
    TILES(R.string.tab_tiles),
    ESEARCH(R.string.tab_esearch),
    DOCK(R.string.tab_dock),
    ABOUT(R.string.tab_about)
}

@Composable
fun SettingsScreen(
    apps: List<AppInfo>,
    currentPalette: EtherealPalette,
    currentDock: Set<String>,
    onPaletteChange: (EtherealPalette) -> Unit,
    onDockChange: (Set<String>) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    fontFamilyName: String,
    onFontFamilyChange: (String) -> Unit,
    wallpapers: List<java.io.File>,
    currentWallpaperPath: String?,
    wallpaperScaleMode: String,
    onWallpaperScaleModeChange: (String) -> Unit,
    onPickWallpaper: () -> Unit,
    onApplyWallpaper: (java.io.File) -> Unit,
    onApplyWallpaperLockScreen: (java.io.File) -> Unit,
    onApplyWallpaperEverywhere: (java.io.File) -> Unit,
    onDeleteWallpaper: (java.io.File) -> Unit,
    esearchUseOverride: Boolean,
    esearchOpacity: Float,
    esearchBlur: Float,
    esearchWidth: Float,
    esearchVisibleItems: Int,
    esearchAutoKeyboard: Boolean = true,
    onESearchUseOverrideChange: (Boolean) -> Unit,
    onESearchOpacityChange: (Float) -> Unit,
    onESearchBlurChange: (Float) -> Unit,
    onESearchWidthChange: (Float) -> Unit,
    onESearchVisibleItemsChange: (Int) -> Unit,
    onESearchAutoKeyboardChange: (Boolean) -> Unit = {},
    dockIconSize: Float,
    dockBottomPadding: Float,
    dockUseOverride: Boolean,
    dockOpacity: Float,
    dockBlur: Float,
    dockScrollable: Boolean,
    dockIconShape: String,
    dockShowIconBackground: Boolean,
    dockThemedIcons: Boolean,
    onDockThemedIconsChange: (Boolean) -> Unit,
    onDockIconSizeChange: (Float) -> Unit,
    onDockBottomPaddingChange: (Float) -> Unit,
    onDockUseOverrideChange: (Boolean) -> Unit,
    onDockOpacityChange: (Float) -> Unit,
    onDockBlurChange: (Float) -> Unit,
    onDockScrollableChange: (Boolean) -> Unit,
    onDockIconShapeChange: (String) -> Unit,
    onDockShowIconBackgroundChange: (Boolean) -> Unit,
    tileSize: Float,
    tileSpacing: Float,
    tileRadius: Float,
    onTileSizeChange: (Float) -> Unit,
    onTileSpacingChange: (Float) -> Unit,
    onTileRadiusChange: (Float) -> Unit,
    onResetTiles: () -> Unit,
    analyticsEnabled: Boolean,
    onAnalyticsEnabledChange: (Boolean) -> Unit,
    updateInfo: UpdateInfo?,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateOverlay: () -> Unit,
    onRestartOnboarding: (() -> Unit)? = null,
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = LocalAppearance.current
    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }
    var draftDock by remember(currentDock) { mutableStateOf(currentDock) }
    var drawerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = currentPalette.background.copy(alpha = appearance.opacity)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentPalette.surface.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { drawerOpen = !drawerOpen },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "☰",
                            color = currentPalette.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(selectedTab.labelRes),
                        color = currentPalette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        SettingsTab.WALLPAPER -> WallpaperTab(
                            palette = currentPalette,
                            wallpapers = wallpapers,
                            currentPath = currentWallpaperPath,
                            scaleMode = wallpaperScaleMode,
                            onScaleModeChange = onWallpaperScaleModeChange,
                            onPickWallpaper = onPickWallpaper,
                            onApply = onApplyWallpaper,
                            onApplyLockScreen = onApplyWallpaperLockScreen,
                            onApplyEverywhere = onApplyWallpaperEverywhere,
                            onDelete = onDeleteWallpaper
                        )
                        SettingsTab.APPEARANCE -> AppearanceTab(
                            currentPalette = currentPalette,
                            onPaletteChange = onPaletteChange,
                            onOpacityChange = onOpacityChange,
                            onBlurChange = onBlurChange,
                            onFontScaleChange = onFontScaleChange,
                            fontFamilyName = fontFamilyName,
                            onFontFamilyChange = onFontFamilyChange,
                            themedIcons = dockThemedIcons,
                            onThemedIconsChange = onDockThemedIconsChange
                        )
                        SettingsTab.TILES -> TilesTab(
                            palette = currentPalette,
                            tileSize = tileSize,
                            tileSpacing = tileSpacing,
                            tileRadius = tileRadius,
                            onTileSizeChange = onTileSizeChange,
                            onTileSpacingChange = onTileSpacingChange,
                            onTileRadiusChange = onTileRadiusChange,
                            onResetTiles = onResetTiles
                        )
                        SettingsTab.ESEARCH -> ESearchTab(
                            palette = currentPalette,
                            useOverride = esearchUseOverride,
                            opacity = esearchOpacity,
                            blur = esearchBlur,
                            width = esearchWidth,
                            visibleItems = esearchVisibleItems,
                            autoKeyboard = esearchAutoKeyboard,
                            onUseOverrideChange = onESearchUseOverrideChange,
                            onOpacityChange = onESearchOpacityChange,
                            onBlurChange = onESearchBlurChange,
                            onWidthChange = onESearchWidthChange,
                            onVisibleItemsChange = onESearchVisibleItemsChange,
                            onAutoKeyboardChange = onESearchAutoKeyboardChange
                        )
                        SettingsTab.DOCK -> DockTab(
                            apps = apps,
                            currentPalette = currentPalette,
                            draftDock = draftDock,
                            onToggle = { pkg ->
                                draftDock = if (draftDock.contains(pkg)) draftDock - pkg
                                else draftDock + pkg
                                onDockChange(draftDock)
                            },
                            iconSize = dockIconSize,
                            bottomPadding = dockBottomPadding,
                            useOverride = dockUseOverride,
                            opacity = dockOpacity,
                            blur = dockBlur,
                            scrollable = dockScrollable,
                            iconShape = dockIconShape,
                            showIconBackground = dockShowIconBackground,
                            onIconSizeChange = onDockIconSizeChange,
                            onBottomPaddingChange = onDockBottomPaddingChange,
                            onUseOverrideChange = onDockUseOverrideChange,
                            onOpacityChange = onDockOpacityChange,
                            onBlurChange = onDockBlurChange,
                            onScrollableChange = onDockScrollableChange,
                            onIconShapeChange = onDockIconShapeChange,
                            onShowIconBackgroundChange = onDockShowIconBackgroundChange
                        )
                        SettingsTab.ABOUT -> AboutTab(
                            palette = currentPalette,
                            onRestartOnboarding = onRestartOnboarding,
                            analyticsEnabled = analyticsEnabled,
                            onAnalyticsEnabledChange = onAnalyticsEnabledChange,
                            updateInfo = updateInfo,
                            updateChecking = updateChecking,
                            onCheckForUpdates = onCheckForUpdates,
                            onOpenUpdateOverlay = onOpenUpdateOverlay,
                            onExportBackup = onExportBackup,
                            onImportBackup = onImportBackup
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = drawerOpen,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { drawerOpen = false }
                ) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .background(currentPalette.background.copy(alpha = 0.98f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ethereal",
                            color = currentPalette.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                        )

                        SettingsTab.values().forEach { tab ->
                            SettingsMenuItem(
                                text = stringResource(tab.labelRes),
                                selected = selectedTab == tab,
                                palette = currentPalette,
                                onClick = {
                                    selectedTab = tab
                                    drawerOpen = false
                                }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        SettingsMenuItem(
                            text = stringResource(R.string.close),
                            palette = currentPalette,
                            onClick = onClose
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// ВКЛАДКА: ТЕМА
// ============================================================

@Composable
private fun AppearanceTab(
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    fontFamilyName: String,
    onFontFamilyChange: (String) -> Unit,
    themedIcons: Boolean,
    onThemedIconsChange: (Boolean) -> Unit
) {
    val appearance = LocalAppearance.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_appearance_section),
                color = currentPalette.textSecondary,
                fontSize = 12.sp
            )
        }
        item {
            ThemeDropdown(
                currentPalette = currentPalette,
                onPaletteChange = onPaletteChange
            )
        }
        item {
            FontDropdown(
                currentPalette = currentPalette,
                fontFamilyName = fontFamilyName,
                onFontFamilyChange = onFontFamilyChange
            )
        }
        item {
            SettingsToggle(
                title = stringResource(R.string.settings_dock_themed_icons),
                subtitle = stringResource(R.string.settings_dock_themed_icons_subtitle),
                checked = themedIcons,
                palette = currentPalette,
                onCheckedChange = onThemedIconsChange
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_panel_opacity),
                subtitle = stringResource(R.string.settings_panel_opacity_subtitle),
                value = appearance.opacity,
                range = 0f..1.0f,
                palette = currentPalette,
                onValueChange = onOpacityChange,
                format = { "${(it * 100).toInt()}%" }
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_panel_blur),
                subtitle = stringResource(R.string.settings_panel_blur_subtitle),
                value = appearance.blur,
                range = 0f..25f,
                palette = currentPalette,
                onValueChange = onBlurChange,
                format = { "${it.toInt()} dp" }
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_font_scale),
                subtitle = stringResource(R.string.settings_font_scale_subtitle),
                value = appearance.fontScale,
                range = 0.7f..1.5f,
                palette = currentPalette,
                onValueChange = onFontScaleChange,
                format = { "${(it * 100).toInt()}%" }
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ============================================================
// ВКЛАДКА: СЕТКА
// ============================================================

@Composable
private fun TilesTab(
    palette: EtherealPalette,
    tileSize: Float,
    tileSpacing: Float,
    tileRadius: Float,
    onTileSizeChange: (Float) -> Unit,
    onTileSpacingChange: (Float) -> Unit,
    onTileRadiusChange: (Float) -> Unit,
    onResetTiles: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_tiles_section),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_tile_size),
                subtitle = stringResource(R.string.settings_tile_size_subtitle),
                value = tileSize,
                range = 32f..96f,
                palette = palette,
                onValueChange = onTileSizeChange,
                format = { "${it.toInt()}dp" }
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_tile_spacing),
                subtitle = stringResource(R.string.settings_tile_spacing_subtitle),
                value = tileSpacing,
                range = 4f..24f,
                palette = palette,
                onValueChange = onTileSpacingChange,
                format = { "${it.toInt()}dp" }
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_tile_radius),
                subtitle = stringResource(R.string.settings_tile_radius_subtitle),
                value = tileRadius,
                range = 0f..32f,
                palette = palette,
                onValueChange = onTileRadiusChange,
                format = { "${it.toInt()}dp" }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary.copy(alpha = 0.15f))
                    .clickable { onResetTiles() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_reset_tiles),
                    color = palette.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ============================================================
// ВКЛАДКА: ESEARCH
// ============================================================

@Composable
private fun ESearchTab(
    palette: EtherealPalette,
    useOverride: Boolean,
    opacity: Float,
    blur: Float,
    width: Float,
    visibleItems: Int,
    autoKeyboard: Boolean,
    onUseOverrideChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onWidthChange: (Float) -> Unit,
    onVisibleItemsChange: (Int) -> Unit,
    onAutoKeyboardChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_esearch_section),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
        item {
            SettingsToggle(
                title = stringResource(R.string.settings_esearch_auto_keyboard),
                subtitle = stringResource(R.string.settings_esearch_auto_keyboard_subtitle),
                checked = autoKeyboard,
                palette = palette,
                onCheckedChange = onAutoKeyboardChange
            )
        }
        item {
            SettingsToggle(
                title = stringResource(R.string.settings_esearch_override),
                subtitle = stringResource(R.string.settings_esearch_override_subtitle),
                checked = useOverride,
                palette = palette,
                onCheckedChange = onUseOverrideChange
            )
        }
        if (useOverride) {
            item {
                SettingsSlider(
                    title = stringResource(R.string.settings_esearch_opacity),
                    subtitle = stringResource(R.string.settings_esearch_opacity_subtitle),
                    value = opacity,
                    range = 0f..1.0f,
                    palette = palette,
                    onValueChange = onOpacityChange,
                    format = { "${(it * 100).toInt()}%" }
                )
            }
            item {
                SettingsSlider(
                    title = stringResource(R.string.settings_esearch_blur),
                    subtitle = stringResource(R.string.settings_esearch_blur_subtitle),
                    value = blur,
                    range = 0f..25f,
                    palette = palette,
                    onValueChange = onBlurChange,
                    format = { "${it.toInt()} dp" }
                )
            }
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_esearch_width),
                subtitle = stringResource(R.string.settings_esearch_width_subtitle),
                value = width,
                range = 0.5f..0.95f,
                palette = palette,
                onValueChange = onWidthChange,
                format = { "${(it * 100).toInt()}%" }
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_esearch_visible_items),
                subtitle = stringResource(R.string.settings_esearch_visible_items_subtitle),
                value = visibleItems.toFloat(),
                range = 3f..12f,
                palette = palette,
                onValueChange = { onVisibleItemsChange(it.toInt()) },
                format = { it.toInt().toString() }
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ============================================================
// ВКЛАДКА: DOCK
// ============================================================

@Composable
private fun DockTab(
    apps: List<AppInfo>,
    currentPalette: EtherealPalette,
    draftDock: Set<String>,
    onToggle: (String) -> Unit,
    iconSize: Float,
    bottomPadding: Float,
    useOverride: Boolean,
    opacity: Float,
    blur: Float,
    scrollable: Boolean,
    iconShape: String,
    showIconBackground: Boolean,
    onIconSizeChange: (Float) -> Unit,
    onBottomPaddingChange: (Float) -> Unit,
    onUseOverrideChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onScrollableChange: (Boolean) -> Unit,
    onIconShapeChange: (String) -> Unit,
    onShowIconBackgroundChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_dock_section),
                color = currentPalette.textSecondary,
                fontSize = 12.sp
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_dock_icon_size),
                subtitle = stringResource(R.string.settings_dock_icon_size_subtitle),
                value = iconSize,
                range = 36f..72f,
                palette = currentPalette,
                onValueChange = onIconSizeChange,
                format = { "${it.toInt()}dp" }
            )
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_dock_bottom_padding),
                subtitle = stringResource(R.string.settings_dock_bottom_padding_subtitle),
                value = bottomPadding,
                range = 0f..80f,
                palette = currentPalette,
                onValueChange = onBottomPaddingChange,
                format = { "${it.toInt()}dp" }
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentPalette.surfaceVariant.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_dock_icon_shape),
                    color = currentPalette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_dock_icon_shape_subtitle),
                    color = currentPalette.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconShape.values().forEach { shape ->
                        val selected = shape.name == iconShape
                        ShapePreview(
                            shape = shape,
                            selected = selected,
                            palette = currentPalette,
                            onClick = { onIconShapeChange(shape.name) }
                        )
                    }
                }
            }
        }
        item {
            SettingsToggle(
                title = stringResource(R.string.settings_dock_show_bg),
                subtitle = stringResource(R.string.settings_dock_show_bg_subtitle),
                checked = showIconBackground,
                palette = currentPalette,
                onCheckedChange = onShowIconBackgroundChange
            )
        }
        item {
            SettingsToggle(
                title = stringResource(R.string.settings_dock_scrollable),
                subtitle = stringResource(R.string.settings_dock_scrollable_subtitle),
                checked = scrollable,
                palette = currentPalette,
                onCheckedChange = onScrollableChange
            )
        }
        item {
            SettingsToggle(
                title = stringResource(R.string.settings_dock_override),
                subtitle = stringResource(R.string.settings_dock_override_subtitle),
                checked = useOverride,
                palette = currentPalette,
                onCheckedChange = onUseOverrideChange
            )
        }
        if (useOverride) {
            item {
                SettingsSlider(
                    title = stringResource(R.string.settings_dock_opacity),
                    subtitle = stringResource(R.string.settings_dock_opacity_subtitle),
                    value = opacity,
                    range = 0f..1.0f,
                    palette = currentPalette,
                    onValueChange = onOpacityChange,
                    format = { "${(it * 100).toInt()}%" }
                )
            }
            item {
                SettingsSlider(
                    title = stringResource(R.string.settings_dock_blur),
                    subtitle = stringResource(R.string.settings_dock_blur_subtitle),
                    value = blur,
                    range = 0f..25f,
                    palette = currentPalette,
                    onValueChange = onBlurChange,
                    format = { "${it.toInt()} dp" }
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(currentPalette.outline.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_dock_apps_count, draftDock.size),
                color = currentPalette.textSecondary,
                fontSize = 12.sp
            )
        }
        items(apps, key = { it.packageName }) { app ->
            DockAppRow(
                app = app,
                selected = draftDock.contains(app.packageName),
                palette = currentPalette,
                onToggle = { onToggle(app.packageName) }
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ShapePreview(
    shape: IconShape,
    selected: Boolean,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    val previewSize = 44.dp
    val compShape = iconShapeFor(shape, previewSize.value)

    Box(
        modifier = Modifier
            .size(previewSize)
            .clip(compShape)
            .background(
                if (selected) palette.primary.copy(alpha = 0.25f)
                else palette.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.primary
                else palette.outline.copy(alpha = 0.5f),
                shape = compShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(previewSize * 0.35f)
                .clip(compShape)
                .background(
                    if (selected) palette.primary else palette.textSecondary
                )
        )
    }
}

@Composable
private fun DockAppRow(
    app: AppInfo,
    selected: Boolean,
    palette: EtherealPalette,
    onToggle: () -> Unit
) {
    val bg = if (selected) palette.primary.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (selected) palette.primary else palette.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bmp = remember(app.packageName) { app.icon.toImageBitmap() }
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = app.label,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = app.label,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text("✓", color = palette.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================
// ВКЛАДКА: О ПРОГРАММЕ
// ============================================================

@Composable
private fun AboutTab(
    palette: EtherealPalette,
    onRestartOnboarding: (() -> Unit)? = null,
    analyticsEnabled: Boolean,
    onAnalyticsEnabledChange: (Boolean) -> Unit,
    updateInfo: UpdateInfo?,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateOverlay: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val deviceInfo = remember { getDeviceInfo(context) }
    val uriHandler = LocalUriHandler.current
    var versionClicks by remember { mutableIntStateOf(0) }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // === ПЛАШКА ОБНОВЛЕНИЯ (над логотипом) ===
        if (updateInfo != null && updateInfo.hasUpdate) {
            item {
                UpdateBadge(
                    palette = palette,
                    updateInfo = updateInfo,
                    updateChecking = updateChecking,
                    onCheckForUpdates = onCheckForUpdates,
                    onOpenUpdateOverlay = onOpenUpdateOverlay
                )
            }
        }

        item {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Ethereal",
                modifier = Modifier.size(180.dp)
            )
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ethereal",
                    color = palette.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        versionClicks++
                        if (versionClicks >= 5) {
                            versionClicks = 0
                            onRestartOnboarding?.invoke()
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            val tiles = listOf(
                deviceInfo.manufacturer to stringResource(R.string.settings_about_manufacturer),
                deviceInfo.model to stringResource(R.string.settings_about_model),
                deviceInfo.androidVersion to stringResource(R.string.settings_about_android),
                deviceInfo.processor to stringResource(R.string.settings_about_processor),
                deviceInfo.refreshRate to stringResource(R.string.settings_about_refresh_rate),
                deviceInfo.storage to stringResource(R.string.settings_about_storage),
                deviceInfo.ram to stringResource(R.string.settings_about_ram),
                deviceInfo.display to stringResource(R.string.settings_about_display)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tiles.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { (value, label) ->
                            InfoTile(
                                label = label,
                                value = value,
                                palette = palette,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.4f))
                    .clickable {
                        uriHandler.openUri("https://github.com/Neytron951")
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_about_developed_by) + " ",
                    color = palette.textSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "Neytron951",
                    color = palette.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // === Кнопка ручной проверки обновлений ===
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.4f))
                    .clickable(enabled = !updateChecking) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCheckForUpdates()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (updateChecking)
                        stringResource(R.string.update_checking)
                    else
                        stringResource(R.string.update_check_manual),
                    color = if (updateChecking) palette.textSecondary else palette.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_analytics_title),
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_analytics_subtitle),
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = analyticsEnabled,
                    onCheckedChange = onAnalyticsEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.primary,
                        checkedTrackColor = palette.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.outline
                    )
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_backup_section),
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                SettingsButton(
                    title = stringResource(R.string.settings_backup_export),
                    palette = palette,
                    onClick = onExportBackup
                )
                SettingsButton(
                    title = stringResource(R.string.settings_backup_import),
                    palette = palette,
                    onClick = onImportBackup
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_about_license),
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun UpdateBadge(
    palette: EtherealPalette,
    updateInfo: UpdateInfo,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateOverlay: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.primary.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = palette.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenUpdateOverlay()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↓",
                color = palette.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.update_available_format,
                    updateInfo.latestVersion.display()
                ),
                color = palette.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.update_available_subtitle),
                color = palette.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Text(
            text = "›",
            color = palette.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = palette.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================
// Хелперы About
// ============================================================

private data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val processor: String,
    val refreshRate: String,
    val storage: String,
    val ram: String,
    val display: String
)

private fun getDeviceInfo(context: android.content.Context): DeviceInfo {
    val manufacturer = android.os.Build.MANUFACTURER
        .replaceFirstChar { it.uppercase() }
    val model = android.os.Build.MODEL
    val androidStr = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"

    val processor = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val soc = android.os.Build.SOC_MODEL
            if (!soc.isNullOrBlank() && soc != android.os.Build.UNKNOWN) {
                soc
            } else {
                "${Runtime.getRuntime().availableProcessors()} cores"
            }
        } else {
            val cores = Runtime.getRuntime().availableProcessors()
            val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "—"
            "$cores cores · $abi"
        }
    } catch (e: Exception) {
        "—"
    }

    val refreshRate = try {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        val rate = display?.refreshRate ?: 60f
        String.format("%.0f Hz", rate)
    } catch (_: Exception) {
        "—"
    }

    val storageStr = try {
        val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalGb = totalBytes.toDouble() / (1024 * 1024 * 1024)
        val availableGb = availableBytes.toDouble() / (1024 * 1024 * 1024)
        String.format("%.0f GB / %.0f GB", availableGb, totalGb)
    } catch (e: Exception) {
        "—"
    }

    val ramStr = try {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalGb = mi.totalMem.toDouble() / (1024 * 1024 * 1024)
        String.format("%.1f GB", totalGb)
    } catch (e: Exception) {
        "—"
    }

    val displayStr = try {
        val metrics = context.resources.displayMetrics
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonal = kotlin.math.sqrt(
            (widthInches * widthInches + heightInches * heightInches).toDouble()
        )
        String.format("%.1f\"", diagonal)
    } catch (e: Exception) {
        "—"
    }

    return DeviceInfo(
        manufacturer = manufacturer,
        model = model,
        androidVersion = androidStr,
        processor = processor,
        refreshRate = refreshRate,
        storage = storageStr,
        ram = ramStr,
        display = displayStr
    )
}

// ============================================================
// ОБЩИЕ КОМПОНЕНТЫ
// ============================================================

@Composable
private fun SettingsMenuItem(
    text: String,
    palette: EtherealPalette,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bg = if (selected) palette.primary.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (selected) palette.primary else palette.textSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    palette: EtherealPalette,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String = { it.toString() }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = format(value),
                color = palette.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.primary,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: EtherealPalette,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.primary,
                checkedTrackColor = palette.primary.copy(alpha = 0.4f),
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.outline
            )
        )
    }
}

@Composable
private fun WallpaperTab(
    palette: EtherealPalette,
    wallpapers: List<java.io.File>,
    currentPath: String?,
    scaleMode: String,
    onScaleModeChange: (String) -> Unit,
    onPickWallpaper: () -> Unit,
    onApply: (java.io.File) -> Unit,
    onApplyLockScreen: (java.io.File) -> Unit,
    onApplyEverywhere: (java.io.File) -> Unit,
    onDelete: (java.io.File) -> Unit
) {
    var pendingFile by remember { mutableStateOf<java.io.File?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.settings_wallpaper_title),
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_wallpaper_subtitle),
            color = palette.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        SettingsButton(
            title = stringResource(R.string.settings_wallpaper_add),
            palette = palette,
            onClick = onPickWallpaper
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.wallpaper_scale_mode_title),
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modes = listOf(
                "CROP" to R.string.wallpaper_scale_crop,
                "FIT" to R.string.wallpaper_scale_fit,
                "STRETCH" to R.string.wallpaper_scale_stretch,
                "CENTER" to R.string.wallpaper_scale_center
            )
            modes.forEach { (modeKey, labelRes) ->
                val isSelected = scaleMode == modeKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) palette.primary.copy(alpha = 0.2f)
                            else palette.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) palette.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onScaleModeChange(modeKey) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(labelRes),
                        color = if (isSelected) palette.primary else palette.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (wallpapers.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_wallpaper_empty),
                color = palette.textSecondary,
                fontSize = 13.sp
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(wallpapers.size, key = { wallpapers[it].absolutePath }) { index ->
                    val file = wallpapers[index]
                    WallpaperThumb(
                        file = file,
                        isCurrent = file.absolutePath == currentPath,
                        palette = palette,
                        onApply = { pendingFile = file },
                        onDelete = { onDelete(file) }
                    )
                }
            }
        }
    }

    if (pendingFile != null) {
        val file = pendingFile!!
        AlertDialog(
            onDismissRequest = { pendingFile = null },
            title = {
                Text(
                    text = stringResource(R.string.settings_wallpaper_apply_title),
                    color = palette.textPrimary
                )
            },
            containerColor = palette.surface,
            text = {
                Column {
                    WallpaperActionRow(
                        text = stringResource(R.string.settings_wallpaper_apply_home),
                        palette = palette,
                        onClick = {
                            onApply(file)
                            pendingFile = null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WallpaperActionRow(
                        text = stringResource(R.string.settings_wallpaper_apply_lock),
                        palette = palette,
                        onClick = {
                            onApplyLockScreen(file)
                            pendingFile = null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WallpaperActionRow(
                        text = stringResource(R.string.settings_wallpaper_apply_everywhere),
                        palette = palette,
                        onClick = {
                            onApplyEverywhere(file)
                            pendingFile = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingFile = null }) {
                    Text(stringResource(R.string.cancel), color = palette.primary)
                }
            }
        )
    }
}

@Composable
private fun WallpaperActionRow(
    text: String,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = text, color = palette.textPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun WallpaperThumb(
    file: java.io.File,
    isCurrent: Boolean,
    palette: EtherealPalette,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(file.absolutePath) {
        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant)
            .border(
                width = if (isCurrent) 2.dp else 0.dp,
                color = if (isCurrent) palette.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onApply)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_wallpaper_active),
                    color = palette.background,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(
    title: String,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.primary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = palette.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThemeDropdown(
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(currentPalette.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            color = currentPalette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.settings_theme_subtitle),
            color = currentPalette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentPalette.surface)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaletteDot(currentPalette.background)
                    PaletteDot(currentPalette.surface)
                    PaletteDot(currentPalette.primary)
                    PaletteDot(currentPalette.secondary)
                    PaletteDot(currentPalette.textPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = currentPalette.name,
                    color = currentPalette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "▼",
                    color = currentPalette.textSecondary,
                    fontSize = 12.sp
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(currentPalette.surface)
                    .heightIn(max = 400.dp)
            ) {
                AllPalettes.forEach { palette ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PaletteDot(palette.background)
                                    PaletteDot(palette.surface)
                                    PaletteDot(palette.primary)
                                    PaletteDot(palette.secondary)
                                    PaletteDot(palette.textPrimary)
                                }
                                Text(
                                    text = palette.name,
                                    color = if (palette.id == currentPalette.id)
                                        currentPalette.primary
                                    else currentPalette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (palette.id == currentPalette.id)
                                        FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onPaletteChange(palette)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
    )
}

@Composable
private fun FontDropdown(
    currentPalette: EtherealPalette,
    fontFamilyName: String,
    onFontFamilyChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val currentFont = try {
        EtherealFont.valueOf(fontFamilyName)
    } catch (e: Exception) {
        EtherealFont.DEFAULT
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(currentPalette.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_font),
            color = currentPalette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.settings_font_subtitle),
            color = currentPalette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentPalette.surface)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentFont.displayName,
                    color = currentPalette.textPrimary,
                    fontSize = 14.sp,
                    fontFamily = fontFamilyFor(currentFont),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "▼",
                    color = currentPalette.textSecondary,
                    fontSize = 12.sp
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(currentPalette.surface)
                    .heightIn(max = 400.dp)
            ) {
                EtherealFont.values().forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = font.displayName,
                                color = if (font == currentFont)
                                    currentPalette.primary
                                else currentPalette.textPrimary,
                                fontSize = 14.sp,
                                fontFamily = fontFamilyFor(font),
                                fontWeight = if (font == currentFont)
                                    FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onFontFamilyChange(font.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}