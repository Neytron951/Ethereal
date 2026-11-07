package com.neytron.ethereal.tiles

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import android.media.MediaPlayer
import android.graphics.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.geometry.Size
import coil.compose.AsyncImage
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.editor.TileEditorState
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.FrostedGlassBox
import com.neytron.ethereal.utils.DeviceTiltProvider
import com.neytron.ethereal.utils.screenBounds
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.tileBorder
import com.neytron.ethereal.utils.tileTexture
import com.neytron.ethereal.utils.toForegroundOnlyBitmap
import com.neytron.ethereal.utils.toImageBitmap
import com.neytron.ethereal.weather.WeatherCodeMapper
import com.neytron.ethereal.weather.WeatherData
import com.neytron.ethereal.weather.WeatherRepository
import com.neytron.ethereal.widgets.EtherealAppWidgetHost
import com.neytron.ethereal.widgets.WidgetHostView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
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
    showResizeIndicator: Boolean = true,
    isTargetCell: Boolean = false,
    introReveal: Boolean = false,
    isIntroActive: Boolean = false,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    onDragStart: (Tile) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onResizeStart: (Tile) -> Unit = {},
    onResize: (Tile, Offset) -> Unit = { _, _ -> },
    onResizeEnd: () -> Unit = {},
    onDeleteTile: (Tile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    var launchCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var contextMenuExpanded by remember(tile.id) { mutableStateOf(false) }

    val widthDp = tileSize * tile.w + spacing * (tile.w - 1)
    val heightDp = tileSize * tile.h + spacing * (tile.h - 1)

    val shape = RoundedCornerShape(radius)

    val dragging = editorState.draggingTile.value
    val isDragging = dragging?.id == tile.id
    val isEditing = editorState.editing.value
    val isSelected = editorState.selectedTile.value?.id == tile.id
    val canResizeTile = !tile.locked || tile.type == TileType.SETTINGS

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

    val currentTile = rememberUpdatedState(tile)
    val currentEditorState = rememberUpdatedState(editorState)
    val currentOnClick = rememberUpdatedState(onClick)
    val currentOnLongClick = rememberUpdatedState(onLongClick)
    val currentOnDragStart = rememberUpdatedState(onDragStart)
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    val currentOnResizeStart = rememberUpdatedState(onResizeStart)
    val currentOnResize = rememberUpdatedState(onResize)
    val currentOnResizeEnd = rememberUpdatedState(onResizeEnd)
    val currentOnDeleteTile = rememberUpdatedState(onDeleteTile)

    val appearAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 200),
        label = "appear"
    )

    val appLauncherConfig = remember(tile.type, tile.config) {
        if (tile.type == TileType.APP_LAUNCHER) AppLauncherConfig.fromJson(tile.config) else null
    }
    val appPackage = appLauncherConfig?.packageName

    // «Show only foreground icon»: у иконки не рисуется её background-слой,
    // остаётся только foreground (логотип). Подложка самой плитки не затрагивается.
    val foregroundOnlyIcon = appLauncherConfig?.showOnlyForegroundIcon == true &&
        appLauncherConfig.packageName != null

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
    val backgroundBlur = tile.style.backgroundBlur

    val contentScale = tile.style.contentScale

    val tilt = if (
        tile.style.borderStyle == TileBorderStyle.LIQUID_GLASS ||
        tile.style.borderStyle == TileBorderStyle.LIQUID_GLASS_SENSORS
    ) {
        DeviceTiltProvider.tilt.collectAsState().value
    } else {
        Pair(0f, 0f)
    }

    Box(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .zIndex(if (isDragging) 10f else 0f)
            .offset {
                IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
            }
            .onGloballyPositioned { launchCoordinates = it }
            .pointerInput(tile.id, tile.clickable, isEditing) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )

                    if (currentEditorState.value.resizingTile.value != null) return@awaitEachGesture

                    val isEditingNow = currentEditorState.value.editing.value
                    var dragStarted = false
                    var totalDrag = Offset.Zero
                    var isTap = false

                    if (isEditingNow) {
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    if (!dragStarted) isTap = true
                                    break
                                }
                                if (currentEditorState.value.resizingTile.value != null) break
                                val delta = change.position - change.previousPosition
                                if (delta != Offset.Zero) {
                                    totalDrag += delta
                                    if (!dragStarted && totalDrag.getDistance() > viewConfiguration.touchSlop) {
                                        dragStarted = true
                                        currentOnDragStart.value(currentTile.value)
                                    }
                                    if (dragStarted) {
                                        currentOnDrag.value(totalDrag)
                                        change.consume()
                                    }
                                }
                            }
                            if (dragStarted) {
                                currentOnDragEnd.value()
                            } else if (isTap && currentEditorState.value.resizingTile.value == null) {
                                currentOnClick.value(launchCoordinates?.screenBounds(rootView))
                            }
                        } catch (_: Exception) {
                            if (dragStarted) currentOnDragEnd.value()
                        }
                    } else {
                        var moved = false
                        val longPressed = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                    break
                                }
                            }
                            false
                        } ?: true

                        if (longPressed) {
                            contextMenuExpanded = true
                            currentOnLongClick.value()
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) break
                                    val delta = change.position - change.previousPosition
                                    if (delta != Offset.Zero) {
                                        totalDrag += delta
                                        if (!dragStarted && totalDrag.getDistance() > viewConfiguration.touchSlop) {
                                            dragStarted = true
                                            contextMenuExpanded = false
                                            if (!currentEditorState.value.editing.value) {
                                                currentEditorState.value.enterEdit()
                                            }
                                            currentOnDragStart.value(currentTile.value)
                                        }
                                        if (dragStarted) {
                                            currentOnDrag.value(totalDrag)
                                            change.consume()
                                        }
                                    }
                                }
                                if (dragStarted) currentOnDragEnd.value()
                            } catch (_: Exception) {
                                if (dragStarted) currentOnDragEnd.value()
                            }
                        } else if (
                            !moved &&
                            tile.clickable &&
                            !(tile.type == TileType.WIDGET && tile.payload == "appwidget")
                        ) {
                            currentOnClick.value(launchCoordinates?.screenBounds(rootView))
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = appearAlpha)
                .scale(scale)
                .alpha(alpha)
        ) {
            FrostedGlassBox(
                blur = backgroundBlur,
                opacity = backgroundOpacity,
                backgroundColor = backgroundColor,
                shape = shape,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when {
                            isTargetCell -> Modifier.border(2.dp, palette.primary, shape)
                            isSelected -> Modifier.border(2.dp, palette.primary, shape)
                            tile.style.borderStyle != TileBorderStyle.NONE -> Modifier.tileBorder(tile.style, palette, shape, tilt)
                            isEditing -> Modifier.border(1.dp, palette.outline.copy(alpha = 0.3f), shape)
                            else -> Modifier.tileBorder(tile.style, palette, shape, tilt)
                        }
                    )
                    .resizeCornerIndicator(isEditing, canResizeTile, palette, radius),
                contentModifier = Modifier
                    .fillMaxSize()
                    .tileTexture(tile.style, palette)
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
                            themedIcons = themedIcons,
                            foregroundOnlyIcon = foregroundOnlyIcon
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
                                val widgetId = EtherealAppWidgetHost.decodeWidgetId(tile.config)
                                if (widgetId != null) {
                                    WidgetHostView(
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

        var resizeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        if (isEditing && canResizeTile) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomEnd = radius))
                    .background(
                        if (showResizeIndicator) Color(0xFFE53935).copy(alpha = 0.45f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (showResizeIndicator) Color(0xFFE53935).copy(alpha = 0.8f)
                        else Color.Transparent,
                        RoundedCornerShape(topStart = 10.dp, bottomEnd = radius)
                    )
                    .onGloballyPositioned { resizeCoordinates = it }
                    .pointerInput(tile.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            currentOnResizeStart.value(currentTile.value)
                            val startRoot = resizeCoordinates?.localToRoot(down.position) ?: Offset.Zero
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    val currentRoot = resizeCoordinates?.localToRoot(change.position) ?: Offset.Zero
                                    val totalDrag = currentRoot - startRoot
                                    currentOnResize.value(currentTile.value, totalDrag)
                                    change.consume()
                                }
                            } finally {
                                currentOnResizeEnd.value()
                            }
                        }
                    }
            )
        }

        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false }
            ) {
                if (appPackage != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.esearch_app_info)) },
                        onClick = {
                            contextMenuExpanded = false
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:$appPackage")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tile_delete_tile)) },
                    onClick = {
                        contextMenuExpanded = false
                        currentOnDeleteTile.value(tile)
                    }
                )
                if (appPackage != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tile_uninstall_app)) },
                        onClick = {
                            contextMenuExpanded = false
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DELETE,
                                    Uri.parse("package:$appPackage")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }
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
    themedIcons: Boolean,
    foregroundOnlyIcon: Boolean = false
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

    val bmp = remember(app.packageName, app.icon, themedIcons, palette.id, foregroundOnlyIcon) {
        when {
            themedIcons -> renderThemedIcon(
                app.icon,
                palette,
                drawBackground = !foregroundOnlyIcon
            ).asImageBitmap()
            foregroundOnlyIcon -> app.icon.toForegroundOnlyBitmap()
            else -> app.icon.toImageBitmap()
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
    // Сами часы не перерисовываются: без тика время «замерзает» на моменте
    // первой композиции (например, показывает 12:37 в два часа ночи).
    val nowMs = rememberMinuteTick()
    val now = remember(nowMs) {
        java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    }
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
        while (true) {
            val data = withContext(Dispatchers.IO) {
                WeatherRepository.getWeather(context, tile.config)
            }
            if (data != null) weather = data
            // Репозиторий сам решает, брать кэш или идти в сеть (TTL 1 час),
            // но плитка больше не остаётся с данными «навсегда».
            delay(WEATHER_REFRESH_MS)
        }
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

private const val MINUTE_MS = 60_000L
private const val WEATHER_REFRESH_MS = 15L * 60_000L

/**
 * Тикает на границе каждой минуты.
 * Позволяет плиткам с часами обновляться сами, без внешних рекомпозиций.
 */
@Composable
private fun rememberMinuteTick(): Long {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMs = current
            val untilNextMinute = MINUTE_MS - (current % MINUTE_MS)
            delay(untilNextMinute.coerceAtLeast(16L) + 20L)
        }
    }
    return nowMs
}

private fun Modifier.resizeCornerIndicator(
    isEditing: Boolean,
    canResize: Boolean,
    palette: EtherealPalette,
    radius: Dp
): Modifier = if (isEditing && canResize) {
    this.drawWithCache {
        val radiusPx = radius.toPx()
        val strokeWidth = 3.dp.toPx()
        onDrawWithContent {
            drawContent()
            drawArc(
                color = palette.primary,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width - radiusPx * 2f, size.height - radiusPx * 2f),
                size = Size(radiusPx * 2f, radiusPx * 2f),
                style = Stroke(width = strokeWidth)
            )
        }
    }
} else {
    this
}