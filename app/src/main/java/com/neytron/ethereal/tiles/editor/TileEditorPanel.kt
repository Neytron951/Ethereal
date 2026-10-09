package com.neytron.ethereal.tiles.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.AppLauncherConfig
import com.neytron.ethereal.tiles.SpaceConfig
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TilePreset
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.tiles.TileType
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.utils.toImageBitmap
import com.neytron.ethereal.weather.CityResult
import com.neytron.ethereal.weather.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun TileEditorPanel(
    palette: EtherealPalette,
    presets: List<TilePreset>,
    selectedTile: Tile?,
    installedApps: List<AppInfo>,
    onPresetClick: (TilePreset) -> Unit,
    onSystemWidgetClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onDeleteTile: (Tile) -> Unit,
    onResizeTile: (Tile, Int, Int) -> Unit,
    onStyleChange: (Tile, TileStyle) -> Unit,
    onConfigChange: (Tile, String?) -> Unit,
    onPickImage: (Tile) -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(palette.surface.copy(alpha = 0.98f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Поглощаем клик */ }
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (selectedTile == null) {
            PresetsMode(
                palette = palette,
                presets = presets,
                onPresetClick = onPresetClick,
                onSystemWidgetClick = onSystemWidgetClick,
                onCreateSpaceClick = onCreateSpaceClick
            )
        } else {
            TileSettingsMode(
                palette = palette,
                tile = selectedTile,
                installedApps = installedApps,
                onResize = { w, h -> onResizeTile(selectedTile, w, h) },
                onStyleChange = { newStyle -> onStyleChange(selectedTile, newStyle) },
                onConfigChange = { newConfig -> onConfigChange(selectedTile, newConfig) },
                onPickImage = { onPickImage(selectedTile) },
                onDelete = { onDeleteTile(selectedTile) },
                onClose = onDeselect
            )
        }
    }
}

// ============================================================
// Режим пресетов
// ============================================================

@Composable
private fun PresetsMode(
    palette: EtherealPalette,
    presets: List<TilePreset>,
    onPresetClick: (TilePreset) -> Unit,
    onSystemWidgetClick: () -> Unit,
    onCreateSpaceClick: () -> Unit
) {
    Text(
        text = stringResource(R.string.tile_add_widget),
        color = palette.textPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SystemWidgetChip(
            palette = palette,
            onClick = onSystemWidgetClick
        )

        SpaceChip(
            palette = palette,
            onClick = onCreateSpaceClick
        )

        presets.forEach { preset ->
            PresetChip(
                preset = preset,
                palette = palette,
                onClick = { onPresetClick(preset) }
            )
        }
    }
}

@Composable
private fun SystemWidgetChip(
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .requiredSize(width = 72.dp, height = 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.primary.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = palette.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "+",
            fontSize = 30.sp,
            color = palette.primary,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.system),
            color = palette.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SpaceChip(
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .requiredSize(width = 72.dp, height = 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.primary.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = palette.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.screen_24dp),
            contentDescription = stringResource(R.string.space),
            colorFilter = ColorFilter.tint(palette.primary),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.space),
            color = palette.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PresetChip(
    preset: TilePreset,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    val iconRes: Int? = when (preset.id) {
        "clock" -> R.drawable.clock_24dp
        "weather" -> R.drawable.cloud_24dp
        "app_launcher" -> R.drawable.apps_24dp
        else -> null
    }

    val localizedLabel = when (preset.id) {
        "clock" -> stringResource(R.string.tile_clock)
        "weather" -> stringResource(R.string.tile_weather)
        "app_launcher" -> stringResource(R.string.tile_app)
        else -> preset.label
    }

    Column(
        modifier = Modifier
            .requiredSize(width = 72.dp, height = 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = localizedLabel,
                colorFilter = ColorFilter.tint(palette.textPrimary),
                modifier = Modifier.size(28.dp)
            )
        } else {
            Text(
                text = localizedLabel.take(1),
                fontSize = 24.sp,
                color = palette.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = localizedLabel,
            color = palette.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================
// Режим настроек тайла
// ============================================================

@Composable
private fun TileSettingsMode(
    palette: EtherealPalette,
    tile: Tile,
    installedApps: List<AppInfo>,
    onResize: (Int, Int) -> Unit,
    onStyleChange: (TileStyle) -> Unit,
    onConfigChange: (String?) -> Unit,
    onPickImage: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tileDisplayName(tile),
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (tile.locked) {
                    stringResource(R.string.tile_size_locked_format, tile.w, tile.h)
                } else {
                    stringResource(R.string.tile_size_format, tile.w, tile.h)
                },
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    ResizeRow(
        label = stringResource(R.string.editor_width),
        value = tile.w,
        palette = palette,
        onMinus = { onResize(tile.w - 1, tile.h) },
        onPlus = { onResize(tile.w + 1, tile.h) }
    )
    Spacer(modifier = Modifier.height(6.dp))
    ResizeRow(
        label = stringResource(R.string.editor_height),
        value = tile.h,
        palette = palette,
        onMinus = { onResize(tile.w, tile.h - 1) },
        onPlus = { onResize(tile.w, tile.h + 1) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (tile.type == TileType.SPACE) {
        SpaceSettings(
            palette = palette,
            tile = tile,
            onConfigChange = onConfigChange,
            onPickImage = onPickImage
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (tile.payload == "ethereal_weather") {
        CitySearchField(
            palette = palette,
            currentConfig = tile.config,
            onCitySelected = { cityResult ->
                onConfigChange(cityResult?.toConfig())
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (tile.type == TileType.APP_LAUNCHER) {
        AppLauncherSettings(
            palette = palette,
            tile = tile,
            installedApps = installedApps,
            onConfigChange = onConfigChange,
            onPickImage = onPickImage
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (tile.type == TileType.WIDGET) {
        AppLauncherSettings(
            palette = palette,
            tile = tile,
            installedApps = installedApps,
            onConfigChange = onConfigChange,
            onPickImage = { },
            showExtraSettings = false
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (tile.type == TileType.SETTINGS) {
        TileCustomImageSettings(
            palette = palette,
            tile = tile,
            onConfigChange = onConfigChange,
            onPickImage = onPickImage
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    StyleSlider(
        label = stringResource(R.string.editor_opacity),
        value = tile.style.backgroundOpacity,
        range = 0f..1f,
        palette = palette,
        onValueChange = { v -> onStyleChange(tile.style.copy(backgroundOpacity = v)) },
        format = { "${(it * 100).toInt()}%" }
    )
    Spacer(modifier = Modifier.height(6.dp))
    StyleSlider(
        label = stringResource(R.string.editor_blur),
        value = tile.style.backgroundBlur,
        range = 0f..25f,
        palette = palette,
        onValueChange = { v -> onStyleChange(tile.style.copy(backgroundBlur = v)) },
        format = { "${it.toInt()} dp" }
    )
    Spacer(modifier = Modifier.height(6.dp))
    StyleSlider(
        label = stringResource(R.string.editor_content_scale),
        value = tile.style.contentScale,
        range = 0.5f..2.0f,
        palette = palette,
        onValueChange = { v -> onStyleChange(tile.style.copy(contentScale = v)) },
        format = { "${(it * 100).toInt()}%" }
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (!tile.locked) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.primary.copy(alpha = 0.15f))
                .clickable(onClick = onDelete)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.delete),
                color = palette.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.3f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.cannot_delete_widget),
                color = palette.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

// ============================================================
// Настройки SPACE
// ============================================================

@Composable
private fun SpaceSettings(
    palette: EtherealPalette,
    tile: Tile,
    onConfigChange: (String?) -> Unit,
    onPickImage: () -> Unit
) {
    val config = remember(tile.config) {
        SpaceConfig.fromJson(tile.config)
    }

    var nameValue by remember(tile.id) { mutableStateOf(config.name) }
    var iconTextValue by remember(tile.id) { mutableStateOf(config.iconText ?: "") }

    LaunchedEffect(config.name) { nameValue = config.name }
    LaunchedEffect(config.iconText) { iconTextValue = config.iconText ?: "" }

    LaunchedEffect(nameValue, iconTextValue) {
        delay(400)
        if (nameValue != config.name || iconTextValue != (config.iconText ?: "")) {
            val newConfig = config.copy(
                name = nameValue.ifBlank { "Space" },
                iconText = iconTextValue.takeIf { it.isNotEmpty() }
            )
            onConfigChange(newConfig.toJson())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.editor_space_title),
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.editor_space_name),
            color = palette.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (nameValue.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_space_name_hint),
                    color = palette.textSecondary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = nameValue,
                onValueChange = { nameValue = it },
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(palette.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.editor_space_icon_text),
            color = palette.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (iconTextValue.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_space_icon_text_hint),
                    color = palette.textSecondary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = iconTextValue,
                onValueChange = { newValue ->
                    iconTextValue = newValue.take(2)
                },
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(palette.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.editor_space_icon_image),
            color = palette.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (config.iconUri != null) stringResource(R.string.editor_app_image_set) else stringResource(R.string.editor_app_image_not_set),
            color = palette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onPickImage)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (config.iconUri != null) stringResource(R.string.replace) else stringResource(R.string.select),
                    color = palette.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (config.iconUri != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surfaceVariant.copy(alpha = 0.6f))
                        .clickable {
                            val newConfig = config.copy(iconUri = null)
                            onConfigChange(newConfig.toJson())
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.remove),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.space_show_label),
                color = palette.textSecondary,
                fontSize = 13.sp
            )
            Switch(
                checked = config.showLabel,
                onCheckedChange = { checked ->
                    val newConfig = config.copy(showLabel = checked)
                    onConfigChange(newConfig.toJson())
                }
            )
        }
    }
}

// ============================================================
// Настройки APP_LAUNCHER
// ============================================================

@Composable
private fun AppLauncherSettings(
    palette: EtherealPalette,
    tile: Tile,
    installedApps: List<AppInfo>,
    onConfigChange: (String?) -> Unit,
    onPickImage: () -> Unit,
    showExtraSettings: Boolean = true
) {
    val config = remember(tile.config) {
        AppLauncherConfig.fromJson(tile.config)
    }

    var showAppPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.tile_app),
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        val selectedApp = installedApps.firstOrNull { it.packageName == config.packageName }
        val labelText = selectedApp?.label ?: stringResource(R.string.editor_app_choose)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.surface)
                .clickable { showAppPicker = !showAppPicker }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = labelText,
                    color = if (selectedApp != null) palette.textPrimary else palette.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (showAppPicker) "▲" else "▼",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (showAppPicker) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.surface)
            ) {
                LazyColumn {
                    items(installedApps, key = { it.packageName }) { app ->
                        AppPickerRow(
                            app = app,
                            selected = app.packageName == config.packageName,
                            palette = palette,
                            onClick = {
                                val newConfig = config.copy(packageName = app.packageName)
                                onConfigChange(newConfig.toJson())
                                showAppPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showExtraSettings) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.editor_app_show_label),
                color = palette.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = config.showLabel,
                onCheckedChange = { checked ->
                    val newConfig = config.copy(showLabel = checked)
                    onConfigChange(newConfig.toJson())
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.primary,
                    checkedTrackColor = palette.primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = palette.textSecondary,
                    uncheckedTrackColor = palette.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_app_image),
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (config.imageUri != null) {
                    stringResource(R.string.editor_app_image_set)
                } else {
                    stringResource(R.string.editor_app_image_not_set)
                },
                color = palette.textSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.primary.copy(alpha = 0.15f))
                        .clickable(onClick = onPickImage)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (config.imageUri != null) {
                            stringResource(R.string.replace)
                        } else {
                            stringResource(R.string.select)
                        },
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (config.imageUri != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.6f))
                            .clickable {
                                val newConfig = config.copy(imageUri = null)
                                onConfigChange(newConfig.toJson())
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.remove),
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCustomImageSettings(
    palette: EtherealPalette,
    tile: Tile,
    onConfigChange: (String?) -> Unit,
    onPickImage: () -> Unit
) {
    val config = remember(tile.config) {
        AppLauncherConfig.fromJson(tile.config)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.editor_app_image),
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (config.imageUri != null) {
                stringResource(R.string.editor_app_image_set)
            } else {
                stringResource(R.string.editor_app_image_not_set)
            },
            color = palette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onPickImage)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (config.imageUri != null) {
                        stringResource(R.string.replace)
                    } else {
                        stringResource(R.string.select)
                    },
                    color = palette.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (config.imageUri != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surfaceVariant.copy(alpha = 0.6f))
                        .clickable {
                            val newConfig = config.copy(imageUri = null)
                            onConfigChange(newConfig.toJson())
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.remove),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppInfo,
    selected: Boolean,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    val bg = if (selected) palette.primary.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (selected) palette.primary else palette.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
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
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = app.label,
            color = textColor,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text("✓", color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================
// Поле ввода города с автоподсказками
// ============================================================

@Composable
private fun CitySearchField(
    palette: EtherealPalette,
    currentConfig: String?,
    onCitySelected: (CityResult?) -> Unit
) {
    val currentCity = remember(currentConfig) {
        CityResult.parseConfig(currentConfig)
    }

    var query by remember(currentCity) {
        mutableStateOf(currentCity?.name ?: "")
    }
    var suggestions by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length < 2) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }

        if (currentCity != null && query == currentCity.name) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }

        isLoading = true
        delay(400)

        val results = withContext(Dispatchers.IO) {
            LocationProvider.searchCities(query, limit = 5)
        }
        suggestions = results
        showSuggestions = results.isNotEmpty()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.editor_location),
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (currentCity != null) {
                Text(
                    text = stringResource(R.string.reset),
                    color = palette.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            query = ""
                            onCitySelected(null)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (currentCity != null) {
                stringResource(R.string.editor_location_current, currentCity.name)
            } else {
                stringResource(R.string.editor_location_empty)
            },
            color = palette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_location_hint),
                    color = palette.textSecondary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { newValue ->
                    query = newValue
                },
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(palette.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showSuggestions && suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.surface)
            ) {
                suggestions.forEach { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = city.name
                                showSuggestions = false
                                onCitySelected(city)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = city.displayName(),
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Компоненты
// ============================================================

@Composable
private fun ResizeRow(
    label: String,
    value: Int,
    palette: EtherealPalette,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        ResizeButton(text = "−", palette = palette, onClick = onMinus)
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                color = palette.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        ResizeButton(text = "+", palette = palette, onClick = onPlus)
    }
}

@Composable
private fun ResizeButton(
    text: String,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.8f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StyleSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    palette: EtherealPalette,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = format(value),
                color = palette.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
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

// ============================================================
// Хелперы
// ============================================================

@Composable
private fun tileDisplayName(tile: Tile): String = when {
    tile.type == TileType.SPACE -> stringResource(R.string.tile_space)
    tile.payload == "ethereal_clock" -> stringResource(R.string.tile_clock)
    tile.payload == "ethereal_weather" -> stringResource(R.string.tile_weather)
    tile.payload == "ethereal_search" -> stringResource(R.string.tile_search)
    tile.type == TileType.SETTINGS -> stringResource(R.string.settings)
    tile.type == TileType.APP_LAUNCHER -> stringResource(R.string.tile_app_launcher)
    tile.type == TileType.APP -> tile.payload ?: stringResource(R.string.tile_app)
    tile.type == TileType.WIDGET -> stringResource(R.string.tile_widget)
    tile.type == TileType.ACTION -> stringResource(R.string.tile_action)
    else -> stringResource(R.string.tile_tile)
}

fun defaultPresets(): List<TilePreset> = listOf(
    TilePreset(
        id = "clock",
        label = "Clock",
        type = TileType.WIDGET,
        w = 2, h = 1,
        payload = "ethereal_clock",
        clickable = true
    ),
    TilePreset(
        id = "weather",
        label = "Weather",
        type = TileType.WIDGET,
        w = 2, h = 1,
        payload = "ethereal_weather",
        clickable = false
    ),
    TilePreset(
        id = "app_launcher",
        label = "Application",
        type = TileType.APP_LAUNCHER,
        w = 1, h = 1,
        payload = null,
        clickable = true
    )
)