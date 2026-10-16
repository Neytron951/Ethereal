package com.neytron.ethereal.tiles

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import android.media.MediaPlayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.editor.TileEditorState
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.FrostedGlassBox
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.toImageBitmap
import com.neytron.ethereal.weather.WeatherCodeMapper
import com.neytron.ethereal.weather.WeatherData
import com.neytron.ethereal.weather.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun TileCell(
    tile: Tile,
    tileSize: Dp,
    spacing: Dp,
    radius: Dp,
    palette: EtherealPalette,
    editorState: TileEditorState,
    installedApps: List<AppInfo>,
    themedIcons: Boolean,
    isTargetCell: Boolean = false,
    introReveal: Boolean = false,
    isIntroActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: (Tile) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val widthDp = tileSize * tile.w + spacing * (tile.w - 1)
    val heightDp = tileSize * tile.h + spacing * (tile.h - 1)

    val shape = RoundedCornerShape(radius)

    val dragging = editorState.draggingTile.value
    val isDragging = dragging?.id == tile.id
    val isEditing = editorState.editing.value
    val isSelected = editorState.selectedTile.value?.id == tile.id

    val offsetX = if (isDragging) editorState.dragOffsetPx.value.x else 0f
    val offsetY = if (isDragging) editorState.dragOffsetPx.value.y else 0f

    val isHiddenByIntro = isIntroActive
    var hasRevealed by remember(introReveal) { mutableStateOf(!introReveal) }
    LaunchedEffect(introReveal) {
        if (introReveal) {
            hasRevealed = false
            delay(120L + (tile.x + tile.y * 3) * 90L)
            hasRevealed = true
            try {
                val player = MediaPlayer.create(context, R.raw.click)
                player?.start()
                player?.setOnCompletionListener {
                    it.release()
                }
            } catch (_: Exception) {}
        }
    }

    val revealProgress by animateFloatAsState(
        targetValue = if (hasRevealed && !isHiddenByIntro) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "reveal"
    )

    val scale = (if (isDragging) 1.08f else 1f) * (if (isHiddenByIntro) 0.8f else (0.8f + 0.2f * revealProgress))
    val alpha = if (isHiddenByIntro) 0f else (if (isDragging) 0.9f else 1f) * revealProgress.coerceIn(0f, 1f)

    val currentEditorState = rememberUpdatedState(editorState)

    val appearAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 200),
        label = "appear"
    )

    val backgroundColor = when {
        isTargetCell -> palette.primary
        isSelected -> palette.primary
        tile.type == TileType.EMPTY -> Color.Transparent
        else -> palette.surfaceVariant
    }
    val backgroundOpacity = when {
        isTargetCell -> 0.25f
        isSelected -> 0.25f
        tile.type == TileType.EMPTY -> 0f
        else -> tile.style.backgroundOpacity
    }

    val contentScale = tile.style.contentScale

    FrostedGlassBox(
        blur = tile.style.backgroundBlur,
        opacity = backgroundOpacity,
        backgroundColor = backgroundColor,
        shape = shape,
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .zIndex(if (isDragging) 10f else 0f)
            .offset {
                IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
            }
            .graphicsLayer(alpha = appearAlpha)
            .scale(scale)
            .alpha(alpha)
            .then(
                when {
                    isTargetCell -> Modifier.border(2.dp, palette.primary, shape)
                    isSelected -> Modifier.border(2.dp, palette.primary, shape)
                    isEditing -> Modifier.border(1.dp, palette.outline.copy(alpha = 0.3f), shape)
                    else -> Modifier
                }
            )
            .clickable(
                enabled = isEditing || tile.clickable,
                onClick = onClick
            )
            .pointerInput(tile) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        if (!currentEditorState.value.editing.value) {
                            currentEditorState.value.enterEdit()
                        }
                        onDragStart(tile)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val current = currentEditorState.value.dragOffsetPx.value
                        onDrag(Offset(current.x + amount.x, current.y + amount.y))
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        when (tile.type) {
            TileType.EMPTY -> { }

            TileType.SETTINGS -> {
                val config = remember(tile.config) {
                    AppLauncherConfig.fromJson(tile.config)
                }
                val imageFile = remember(config.imageUri) {
                    config.imageUri?.let { File(it) }
                }

                if (imageFile != null && imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = stringResource(R.string.settings),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.settings_24dp),
                        contentDescription = stringResource(R.string.widget_icon_settings_description),
                        colorFilter = ColorFilter.tint(palette.textPrimary),
                        modifier = Modifier.size((tileSize.value * 0.5f * contentScale).dp)
                    )
                }
            }

            TileType.EQUALIZER -> AudioSpectrumTileContent(
                palette = palette,
                contentScale = contentScale,
                config = AudioSpectrumConfig.fromJson(tile.config)
            )

            TileType.APP -> {
                Text(
                    text = tile.payload?.take(1)?.uppercase() ?: "?",
                    color = palette.textPrimary,
                    fontSize = (tileSize.value * 0.35f * contentScale).sp
                )
            }

            TileType.ACTION -> {
                Text(
                    text = tile.payload ?: "?",
                    color = palette.textPrimary,
                    fontSize = (tileSize.value * 0.25f * contentScale).sp
                )
            }

            TileType.APP_LAUNCHER -> {
                AppLauncherContent(
                    tile = tile,
                    tileSize = tileSize,
                    palette = palette,
                    contentScale = contentScale,
                    installedApps = installedApps,
                    themedIcons = themedIcons
                )
            }

            TileType.WIDGET -> {
                when (tile.payload) {
                    "ethereal_clock" -> ClockTileContent(tileSize, palette, contentScale)
                    "ethereal_weather" -> WeatherTileContent(
                        context = context,
                        tile = tile,
                        tileSize = tileSize,
                        palette = palette,
                        contentScale = contentScale
                    )
                    "ethereal_search" -> SearchTileContent(tileSize, palette, contentScale)
                    "appwidget" -> {
                        val widgetId = com.neytron.ethereal.widgets
                            .EtherealAppWidgetHost.decodeWidgetId(tile.config)
                        if (widgetId != null) {
                            com.neytron.ethereal.widgets.WidgetHostView(
                                appWidgetId = widgetId,
                                frozen = isEditing,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        } else {
                            Text(
                                text = "?",
                                color = palette.textSecondary,
                                fontSize = (tileSize.value * 0.3f * contentScale).sp
                            )
                        }
                    }
                    else -> Text(
                        text = "W",
                        color = palette.textSecondary,
                        fontSize = (tileSize.value * 0.3f * contentScale).sp
                    )
                }
            }

            TileType.SPACE -> {
                val config = remember(tile.config) {
                    SpaceConfig.fromJson(tile.config)
                }
                SpaceTileContent(
                    config = config,
                    tileSize = tileSize,
                    palette = palette,
                    contentScale = contentScale
                )
            }
        }
    }
}

// ============================================================
// APP_LAUNCHER
// ============================================================

@Composable
private fun AppLauncherContent(
    tile: Tile,
    tileSize: Dp,
    palette: EtherealPalette,
    contentScale: Float,
    installedApps: List<AppInfo>,
    themedIcons: Boolean
) {
    val config = remember(tile.config) {
        AppLauncherConfig.fromJson(tile.config)
    }

    val app = remember(config.packageName, installedApps) {
        installedApps.firstOrNull { it.packageName == config.packageName }
    }

    val imageFile = remember(config.imageUri) {
        config.imageUri?.let { File(it) }
    }

    if (imageFile != null && imageFile.exists()) {
        coil.compose.AsyncImage(
            model = imageFile,
            contentDescription = app?.label ?: stringResource(R.string.tile_app),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (app == null) {
        Text(
            text = "?",
            color = palette.textSecondary,
            fontSize = (tileSize.value * 0.35f * contentScale).sp
        )
        return
    }

    val bmp = remember(app.packageName, app.icon, themedIcons, palette.id) {
        if (themedIcons) {
            renderThemedIcon(app.icon, palette).asImageBitmap()
        } else {
            app.icon.toImageBitmap()
        }
    }

    if (tile.w == 1 && tile.h == 1) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = app.label,
                modifier = Modifier.size((tileSize.value * 0.65f * contentScale).dp)
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = app.label,
                    modifier = Modifier.size((tileSize.value * 0.45f * contentScale).dp)
                )
            }
            if (config.showLabel) {
                Text(
                    text = app.label,
                    color = palette.textPrimary,
                    fontSize = (tileSize.value * 0.2f * contentScale).sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================
// Прочие контенты
// ============================================================

@Composable
private fun ClockTileContent(tileSize: Dp, palette: EtherealPalette, contentScale: Float) {
    val now = java.util.Calendar.getInstance()
    val hour = now.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = now.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    val dayOfWeek = when (now.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> stringResource(R.string.day_mon)
        java.util.Calendar.TUESDAY -> stringResource(R.string.day_tue)
        java.util.Calendar.WEDNESDAY -> stringResource(R.string.day_wed)
        java.util.Calendar.THURSDAY -> stringResource(R.string.day_thu)
        java.util.Calendar.FRIDAY -> stringResource(R.string.day_fri)
        java.util.Calendar.SATURDAY -> stringResource(R.string.day_sat)
        else -> stringResource(R.string.day_sun)
    }
    val day = now.get(java.util.Calendar.DAY_OF_MONTH)
    val month = (now.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$hour:$minute",
            color = palette.primary,
            fontSize = (tileSize.value * 0.42f * contentScale).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$dayOfWeek, $day.$month",
            color = palette.textSecondary,
            fontSize = (tileSize.value * 0.18f * contentScale).sp
        )
    }
}

@Composable
private fun WeatherTileContent(
    context: android.content.Context,
    tile: Tile,
    tileSize: Dp,
    palette: EtherealPalette,
    contentScale: Float
) {
    var weather by remember(tile.id) {
        mutableStateOf<WeatherData?>(null)
    }

    LaunchedEffect(tile.id, tile.config) {
        val data = withContext(Dispatchers.IO) {
            WeatherRepository.getWeather(context, tile.config)
        }
        weather = data
    }

    val data: WeatherData = weather ?: WeatherData.EMPTY

    val fallbackCity = stringResource(R.string.tile_weather)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (data.weatherCode >= 0) WeatherCodeMapper.iconFor(data.weatherCode) else "🌤",
            fontSize = (tileSize.value * 0.4f * contentScale).sp
        )
        Column {
            Text(
                text = if (data.weatherCode >= 0) "${data.temperature}°" else "--°",
                color = palette.primary,
                fontSize = (tileSize.value * 0.4f * contentScale).sp
            )
            Text(
                text = if (data.city.isNotBlank()) data.city else fallbackCity,
                color = palette.textSecondary,
                fontSize = (tileSize.value * 0.16f * contentScale).sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchTileContent(tileSize: Dp, palette: EtherealPalette, contentScale: Float) {
    val searchLabel = stringResource(R.string.tile_search)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🔍",
            fontSize = (tileSize.value * 0.35f * contentScale).sp
        )
        Text(
            text = searchLabel,
            color = palette.textSecondary,
            fontSize = (tileSize.value * 0.22f * contentScale).sp
        )
    }
}