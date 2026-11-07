package com.neytron.ethereal.tiles

import android.graphics.Rect
import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AnalyticsHelper
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.editor.CreateSpaceDialog
import com.neytron.ethereal.tiles.editor.TileEditorPanel
import com.neytron.ethereal.tiles.editor.TileEditorState
import com.neytron.ethereal.tiles.editor.defaultPresets
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.weather.WeatherRepository
import com.neytron.ethereal.widgets.EtherealAppWidgetHost
import com.neytron.ethereal.widgets.WidgetHostManager
import com.neytron.ethereal.widgets.WidgetPickerBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun TilesScreen(
    palette: EtherealPalette,
    tileSizeDp: Float,
    spacingDp: Float,
    radiusDp: Float,
    dockIconSizeDp: Float,
    dockBottomPaddingDp: Float,
    installedApps: List<AppInfo>,
    themedIcons: Boolean,
    showResizeIndicator: Boolean = false,
    isDockEnabled: Boolean,
    tiles: List<Tile>,
    onTilesChange: (List<Tile>) -> Unit,
    openWidgetPickerRequest: Int,
    onWidgetPickerRequestHandled: () -> Unit,
    showWidgetPickerSheet: Boolean,
    onWidgetPickerVisibilityChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeUp: () -> Unit,
    onLaunchApp: (String, Rect?, TileStyle?, Float, Boolean) -> Unit,
    editorState: TileEditorState,
    currentSpaceId: String,
    onOpenSpace: (String) -> Unit,
    showCreateSpaceDialog: Boolean,
    onShowCreateSpaceDialogChange: (Boolean) -> Unit,
    introReveal: Boolean = false,
    isIntroActive: Boolean = false,
    onPresetClickReady: ((TilePreset) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val noSpaceMsg = stringResource(R.string.no_space_for_widget)
    val cannotDeleteMsg = stringResource(R.string.cannot_delete_widget)
    val cannotResizeMsg = stringResource(R.string.cannot_resize_widget)
    val widgetAddFailedMsg = stringResource(R.string.widget_add_failed)
    val imageSaveFailedMsg = stringResource(R.string.image_save_failed)
    val spaceNotFoundMsg = stringResource(R.string.space_not_found)
    val widgetDeniedNotDefaultMsg = stringResource(R.string.widget_denied_not_default_launcher)
    val widgetDeniedSystemMsg = stringResource(R.string.widget_denied_system)
    val audioPermissionDeniedMsg = stringResource(R.string.audio_spectrum_permission_denied)

    val edgePadding = TileGridMetrics.EDGE_PADDING_DP.dp
    val topPadding = TileGridMetrics.TOP_PADDING_DP.dp
    val gapAboveDock = TileGridMetrics.GAP_ABOVE_DOCK_DP.dp

    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // DockBar в ландшафте рисует иконки мельче — резервируем ровно столько, сколько он занимает.
    val dockIconDp = if (isLandscape) {
        (dockIconSizeDp * TileGridMetrics.LANDSCAPE_DOCK_SCALE).coerceAtLeast(24f)
    } else {
        dockIconSizeDp
    }
    val dockHeight = (dockIconDp + TileGridMetrics.DOCK_EXTRA_DP).dp
    val dockBottomPadding = dockBottomPaddingDp.dp

    val availableWidthDp = TileGridMetrics.availableWidthDp(configuration.screenWidthDp.toFloat())
    val availableHeightDp = TileGridMetrics.availableHeightDp(
        screenHeightDp = configuration.screenHeightDp.toFloat(),
        dockIconSizeDp = dockIconSizeDp,
        dockBottomPaddingDp = dockBottomPaddingDp,
        isLandscape = isLandscape
    )

    val layoutColumns = remember(tiles) { tiles.maxOfOrNull { it.x + it.w }?.coerceAtLeast(1) ?: 1 }
    val layoutRows = remember(tiles) { tiles.maxOfOrNull { it.y + it.h }?.coerceAtLeast(1) ?: 1 }

    val gridSpec = remember(
        availableWidthDp, availableHeightDp, tileSizeDp, spacingDp,
        isLandscape, layoutColumns, layoutRows
    ) {
        TileGridMetrics.compute(
            availableWidthDp = availableWidthDp,
            availableHeightDp = availableHeightDp,
            desiredTileSizeDp = tileSizeDp,
            spacingDp = spacingDp,
            isLandscape = isLandscape,
            layoutColumns = layoutColumns,
            layoutRows = layoutRows
        )
    }

    val autoColumns = gridSpec.columns
    val autoRows = gridSpec.rows
    val tileSize: Dp = gridSpec.tileSizeDp.dp
    val spacing: Dp = spacingDp.dp
    val radius: Dp = radiusDp.dp

    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingBindWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingConfigureWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingImageTile by remember { mutableStateOf<Tile?>(null) }
    var pendingSpectrumPreset by remember { mutableStateOf<TilePreset?>(null) }
    var showSpectrumPermissionExplanation by remember { mutableStateOf(false) }

    var isTransitioning by remember { mutableStateOf(false) }
    var transitionPivotFractionX by remember { mutableStateOf(0.5f) }
    var transitionPivotFractionY by remember { mutableStateOf(0.5f) }
    var oldTilesState by remember { mutableStateOf<List<Tile>>(emptyList()) }
    var incomingTilesState by remember { mutableStateOf<List<Tile>>(emptyList()) }
    var isPopState by remember { mutableStateOf(false) }

    var targetProgress by remember { mutableStateOf(0f) }
    val transitionProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        finishedListener = { value ->
            if (value == 1f && isTransitioning) {
                isTransitioning = false
                oldTilesState = emptyList()
                isPopState = false
                targetProgress = 0f
            }
        },
        label = "transitionProgress"
    )

    var previousSpaceId by remember { mutableStateOf(currentSpaceId) }

    LaunchedEffect(currentSpaceId) {
        if (currentSpaceId != previousSpaceId) {
            val oldSpaceId = previousSpaceId
            previousSpaceId = currentSpaceId

            if (!isTransitioning) {
                val newTiles = TileLayoutRepository.getTiles(context, currentSpaceId, isLandscape, gridSpec)
                incomingTilesState = newTiles

                val matchingTile = newTiles.firstOrNull { it.type == TileType.SPACE && it.payload == oldSpaceId }
                transitionPivotFractionX = if (matchingTile != null) {
                    (matchingTile.x + matchingTile.w / 2f) / autoColumns.coerceAtLeast(1).toFloat()
                } else {
                    0.5f
                }
                transitionPivotFractionY = if (matchingTile != null) {
                    (matchingTile.y + matchingTile.h / 2f) / autoRows.coerceAtLeast(1).toFloat()
                } else {
                    0.5f
                }

                oldTilesState = tiles
                isPopState = true
                isTransitioning = true
                targetProgress = 1f
            }
        }
    }

    LaunchedEffect(openWidgetPickerRequest) {
        if (openWidgetPickerRequest > 0) {
            onWidgetPickerVisibilityChange(true)
            onWidgetPickerRequestHandled()
        }
    }

    val cellSizePx = with(density) { (tileSize + spacing).toPx() }

    LaunchedEffect(autoColumns, autoRows) {
        if (autoColumns > 0 && autoRows > 0 && tiles.isNotEmpty()) {
            val normalized = TileLayoutRepository.smartNormalizeTiles(
                tiles = tiles,
                columns = autoColumns,
                rows = autoRows
            )
            if (normalized != tiles) {
                onTilesChange(normalized)
                TileLayoutRepository.setTiles(context, currentSpaceId, normalized, isLandscape)
            }
        }
    }

    val isEditing = editorState.editing.value
    val selectedTile = editorState.selectedTile.value

    val handleConfigChange: (Tile, String?) -> Unit = { tile, newConfig ->
        if (tile.type == TileType.SPACE && !tile.payload.isNullOrEmpty()) {
            val spaceConfig = SpaceConfig.fromJson(newConfig)
            SpaceRepository.updateSpace(
                context = context,
                spaceId = tile.payload!!,
                name = spaceConfig.name,
                iconUri = spaceConfig.iconUri,
                iconText = spaceConfig.iconText
            )
        }

        val updated = TileLayoutRepository.updateConfig(
            context = context,
            spaceId = currentSpaceId,
            tiles = tiles,
            tileId = tile.id,
            newConfig = newConfig,
            isLandscape = isLandscape
        )
        if (updated != null) {
            onTilesChange(updated)
            editorState.select(updated.first { it.id == tile.id })

            if (tile.payload == "ethereal_weather") {
                WeatherRepository.clearCache(context)
                scope.launch {
                    withContext(Dispatchers.IO) {
                        WeatherRepository.fetchWeather(context, newConfig)
                    }
                }
            }
        }
    }

    val handleDrop: () -> Unit = {
        val resizingTile = editorState.resizingTile.value
        if (resizingTile != null) {
            val preview = editorState.resizePreviewTiles.value
            if (preview != null && preview != tiles) {
                onTilesChange(preview)
                TileLayoutRepository.setTiles(context, currentSpaceId, preview, isLandscape)
                if (editorState.selectedTile.value?.id == resizingTile.id) {
                    editorState.select(preview.firstOrNull { it.id == resizingTile.id })
                }
            }
            editorState.endResize()
        } else {
            val dragging = editorState.draggingTile.value
            if (dragging == null) {
                editorState.endDrag()
            } else {
                val offset = editorState.dragOffsetPx.value
                val dCol = (offset.x / cellSizePx).roundToInt()
                val dRow = (offset.y / cellSizePx).roundToInt()

                val newX = (dragging.x + dCol).coerceIn(0, autoColumns - dragging.w)
                val newY = (dragging.y + dRow).coerceIn(0, (autoRows - dragging.h).coerceAtLeast(0))

                val oldMap = tiles.associateBy { it.id }
                val updated = TileLayoutRepository.placeTileWithDisplacement(
                    tiles = tiles,
                    draggingId = dragging.id,
                    newX = newX,
                    newY = newY,
                    columns = autoColumns,
                    rows = autoRows
                )

                val finalTiles = if (updated != null && updated != tiles) {
                    val displaced = updated.filter { t ->
                        t.id != dragging.id && oldMap[t.id]?.let { it.x != t.x || it.y != t.y } == true
                    }.mapTo(mutableSetOf()) { it.id }
                    editorState.displacedTileIds.value = displaced

                    onTilesChange(updated)
                    TileLayoutRepository.setTiles(context, currentSpaceId, updated, isLandscape)
                    updated
                } else {
                    editorState.displacedTileIds.value = emptySet()
                    tiles
                }

                val finalDragging = finalTiles.firstOrNull { it.id == dragging.id } ?: dragging
                val deltaPx = Offset(
                    (finalDragging.x - dragging.x) * cellSizePx,
                    (finalDragging.y - dragging.y) * cellSizePx
                )

                scope.launch {
                    editorState.animateDropToZero(deltaPx)
                    editorState.endDrag()
                }

                editorState.targetCell.value = null
                if (selectedTile?.id == dragging.id) {
                    val updatedSelected = finalTiles.firstOrNull { it.id == dragging.id }
                    editorState.select(updatedSelected)
                }
            }
        }
    }

    val handlePresetClick: (TilePreset) -> Unit = { preset ->
        val updated = TileLayoutRepository.addTileFromPreset(
            context = context,
            spaceId = currentSpaceId,
            tiles = tiles,
            preset = preset,
            columns = autoColumns,
            rows = autoRows,
            isLandscape = isLandscape
        )
        if (updated != null) {
            onTilesChange(updated)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(noSpaceMsg)
            }
        }
    }

    val handlePresetSelection: (TilePreset) -> Unit = { preset ->
        handlePresetClick(preset)
    }

    LaunchedEffect(handlePresetClick) {
        onPresetClickReady(handlePresetSelection)
    }

    val handleDeleteTile: (Tile) -> Unit = { tile ->
        if (tile.type == TileType.SPACE && !tile.payload.isNullOrEmpty()) {
            val spaceId = tile.payload!!
            val innerTiles = TileLayoutRepository.getTiles(context, spaceId, isLandscape)
            innerTiles.forEach { inner ->
                if (inner.type == TileType.WIDGET) {
                    val wid = EtherealAppWidgetHost.decodeWidgetId(inner.config)
                    if (wid != null) {
                        WidgetHostManager.removeWidget(context, wid)
                    }
                }
                if (inner.type == TileType.APP_LAUNCHER || inner.type == TileType.SETTINGS) {
                    val cfg = AppLauncherConfig.fromJson(inner.config)
                    cfg.imageUri?.let { path ->
                        try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                if (inner.type == TileType.SPACE) {
                    val innerSpaceCfg = SpaceConfig.fromJson(inner.config)
                    innerSpaceCfg.iconUri?.let { path ->
                        try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
            SpaceRepository.deleteSpace(context, spaceId)
        }

        val widgetId = EtherealAppWidgetHost.decodeWidgetId(tile.config)
        if (widgetId != null) {
            WidgetHostManager.removeWidget(context, widgetId)
        }

        if (tile.type == TileType.APP_LAUNCHER) {
            val config = AppLauncherConfig.fromJson(tile.config)
            config.imageUri?.let { path ->
                try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
            }
        }

        if (tile.type == TileType.SPACE) {
            val cfg = SpaceConfig.fromJson(tile.config)
            cfg.iconUri?.let { path ->
                try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
            }
        }

        val updated = TileLayoutRepository.removeTile(
            context = context,
            spaceId = currentSpaceId,
            tiles = tiles,
            tileId = tile.id,
            isLandscape = isLandscape
        )
        if (updated != null) {
            onTilesChange(updated)
            editorState.select(null)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(cannotDeleteMsg)
            }
        }
    }

    val handleResizeTile: (Tile, Int, Int) -> Unit = { tile, newW, newH ->
        if (!tile.locked || tile.type == TileType.SETTINGS) {
            val updated = TileLayoutRepository.resizeTile(
                context = context,
                spaceId = currentSpaceId,
                tiles = tiles,
                tileId = tile.id,
                newW = newW,
                newH = newH,
                columns = autoColumns,
                rows = autoRows,
                isLandscape = isLandscape
            )
            if (updated != null) {
                onTilesChange(updated)
                editorState.select(updated.first { it.id == tile.id })
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(cannotResizeMsg)
                }
            }
        }
    }

    val handleResizeStart: (Tile) -> Unit = { tile ->
        val latestTile = tiles.firstOrNull { it.id == tile.id } ?: tile
        editorState.startResize(latestTile, tiles)
    }

    val handleResize: (Tile, Offset) -> Unit = { tile, offset ->
        val latestTile = tiles.firstOrNull { it.id == tile.id } ?: tile
        val base = editorState.resizingTile.value ?: latestTile
        val maxWidth = (autoColumns - base.x).coerceAtLeast(1)
        // Высота ограничена реальной сеткой: иначе плитка «уезжает» за экран и пропадает из грида
        val maxHeight = (autoRows - base.y).coerceAtLeast(1)
        val stepX = cellSizePx
        val stepY = cellSizePx
        val newW = (base.w + (offset.x / stepX).roundToInt()).coerceIn(1, maxWidth)
        val newH = (base.h + (offset.y / stepY).roundToInt()).coerceIn(1, maxHeight)
        val preview = TileLayoutRepository.previewResizeWithDisplacement(
            tiles = tiles,
            tileId = base.id,
            newW = newW,
            newH = newH,
            columns = autoColumns,
            rows = autoRows
        )
        if (preview != null) {
            editorState.resizePreviewTiles.value = preview
        }
    }

    val handleStyleChange: (Tile, TileStyle) -> Unit = { tile, newStyle ->
        val updated = TileLayoutRepository.updateStyle(
            context = context,
            spaceId = currentSpaceId,
            tiles = tiles,
            tileId = tile.id,
            newStyle = newStyle,
            isLandscape = isLandscape
        )
        if (updated != null) {
            onTilesChange(updated)
            editorState.select(updated.first { it.id == tile.id })
        }
    }

    val handleTileClick: (Tile, Rect?) -> Unit = { tile, sourceBounds ->
        if (tile.clickable) {
            when (tile.type) {
                TileType.SETTINGS -> onOpenSettings()

                TileType.SPACE -> {
                    val spaceId = tile.payload
                    if (!spaceId.isNullOrEmpty() && SpaceRepository.exists(context, spaceId)) {
                        if (!isTransitioning) {
                            transitionPivotFractionX = (tile.x + tile.w / 2f) / autoColumns.coerceAtLeast(1).toFloat()
                            transitionPivotFractionY = (tile.y + tile.h / 2f) / autoRows.coerceAtLeast(1).toFloat()
                            oldTilesState = tiles
                            incomingTilesState = TileLayoutRepository.getTiles(context, spaceId, isLandscape, gridSpec)
                            isPopState = false
                            isTransitioning = true
                            targetProgress = 1f

                            previousSpaceId = spaceId
                            onOpenSpace(spaceId)
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(spaceNotFoundMsg)
                        }
                    }
                }

                TileType.APP_LAUNCHER -> {
                    val config = AppLauncherConfig.fromJson(tile.config)
                    config.packageName?.let { pkg ->
                        val iconScale = if (tile.w == 1 && tile.h == 1) {
                            0.65f * tile.style.contentScale
                        } else {
                            0.45f * tile.style.contentScale
                        }
                        onLaunchApp(pkg, sourceBounds, tile.style, iconScale, config.showOnlyForegroundIcon)
                    }
                }

                TileType.WIDGET -> {
                    val config = AppLauncherConfig.fromJson(tile.config)
                    if (config.packageName != null) {
                        val iconScale = if (tile.w == 1 && tile.h == 1) {
                            0.65f * tile.style.contentScale
                        } else {
                            0.45f * tile.style.contentScale
                        }
                        onLaunchApp(config.packageName, sourceBounds, tile.style, iconScale, config.showOnlyForegroundIcon)
                    } else {
                        when (tile.payload) {
                            "ethereal_clock" -> {
                                try {
                                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            "ethereal_weather" -> {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/search?q=weather")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    }
                }

                else -> { }
            }
        }
    }

    val currentTiles by rememberUpdatedState(tiles)
    val currentColumns by rememberUpdatedState(autoColumns)
    val currentRows by rememberUpdatedState(autoRows)
    val currentSpaceIdState by rememberUpdatedState(currentSpaceId)

    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingConfigureWidgetId
        pendingConfigureWidgetId = null

        if (result.resultCode == Activity.RESULT_OK && widgetId != null) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = manager.getAppWidgetInfo(widgetId)
            if (provider != null) {
                val updated = addSystemWidgetToGrid(
                    context = context,
                    spaceId = currentSpaceIdState,
                    widgetId = widgetId,
                    provider = provider,
                    tiles = currentTiles,
                    columns = currentColumns,
                    rows = currentRows
                )
                updated?.let {
                    onTilesChange(it)
                    AnalyticsHelper.logWidgetAdded(context)
                } ?: scope.launch {
                    snackbarHostState.showSnackbar(noSpaceMsg)
                }
            } else {
                WidgetHostManager.removeWidget(context, widgetId)
            }
        } else {
            widgetId?.let { WidgetHostManager.removeWidget(context, it) }
        }
    }

    val processBoundWidget: (Int, AppWidgetProviderInfo) -> Unit = { widgetId, provider ->
        if (provider.configure != null) {
            pendingConfigureWidgetId = widgetId
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = provider.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            try {
                configureLauncher.launch(configureIntent)
            } catch (e: Exception) {
                val updated = addSystemWidgetToGrid(
                    context = context,
                    spaceId = currentSpaceIdState,
                    widgetId = widgetId,
                    provider = provider,
                    tiles = currentTiles,
                    columns = currentColumns,
                    rows = currentRows
                )
                updated?.let {
                    onTilesChange(it)
                    AnalyticsHelper.logWidgetAdded(context)
                }
            }
        } else {
            val updated = addSystemWidgetToGrid(
                context = context,
                spaceId = currentSpaceIdState,
                widgetId = widgetId,
                provider = provider,
                tiles = currentTiles,
                columns = currentColumns,
                rows = currentRows
            )
            updated?.let {
                onTilesChange(it)
                AnalyticsHelper.logWidgetAdded(context)
            } ?: scope.launch {
                snackbarHostState.showSnackbar(noSpaceMsg)
            }
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingBindWidgetId
        pendingBindWidgetId = null

        if (result.resultCode == Activity.RESULT_OK && widgetId != null) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = manager.getAppWidgetInfo(widgetId)

            if (provider != null) {
                processBoundWidget(widgetId, provider)
            } else {
                WidgetHostManager.removeWidget(context, widgetId)
            }
        } else {
            widgetId?.let { WidgetHostManager.removeWidget(context, it) }

            val defaultHome = isDefaultLauncher(context)
            val msg = if (!defaultHome) widgetDeniedNotDefaultMsg else widgetDeniedSystemMsg

            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val tile = pendingImageTile
        pendingImageTile = null

        if (uri != null && tile != null) {
            val savedPath = copyImageToInternal(context, uri)
            if (savedPath != null) {
                val newConfig = if (tile.type == TileType.SPACE) {
                    val spaceCfg = SpaceConfig.fromJson(tile.config)
                    spaceCfg.copy(iconUri = savedPath).toJson()
                } else {
                    val cfg = AppLauncherConfig.fromJson(tile.config)
                    cfg.copy(imageUri = savedPath).toJson()
                }
                handleConfigChange(tile, newConfig)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(imageSaveFailedMsg)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = edgePadding,
                    end = edgePadding,
                    top = topPadding,
                    bottom = gapAboveDock + dockHeight + dockBottomPadding
                )
                .pointerInput(isEditing, selectedTile) {
                    detectTapGestures(
                        onTap = {
                            if (isEditing && selectedTile != null) {
                                editorState.select(null)
                            }
                        },
                        onLongPress = {
                            if (!isEditing) editorState.enterEdit()
                        }
                    )
                }
                .pointerInput(isEditing) {
                    if (!isEditing) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { if (totalDrag < -150f) onSwipeUp() },
                            onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount }
                        )
                    }
                }
        ) {
            TileGrid(
                tiles = tiles,
                columns = autoColumns,
                rows = autoRows,
                tileSize = tileSize,
                spacing = spacing,
                radius = radius,
                palette = palette,
                editorState = editorState,
                installedApps = installedApps,
                themedIcons = themedIcons,
                showResizeIndicator = showResizeIndicator,
                introReveal = introReveal,
                isIntroActive = isIntroActive,
                isTransitioning = isTransitioning,
                pivotFractionX = transitionPivotFractionX,
                pivotFractionY = transitionPivotFractionY,
                oldTiles = oldTilesState,
                incomingTiles = incomingTilesState,
                isPop = isPopState,
                transitionProgress = transitionProgress,
                onTileClick = { tile, sourceBounds ->
                    if (isEditing) editorState.select(tile)
                    else handleTileClick(tile, sourceBounds)
                },
                onTileLongClick = { },
                onDragStart = { tile ->
                    val latestTile = tiles.firstOrNull { it.id == tile.id } ?: tile
                    editorState.startDrag(latestTile)
                },
                onDrag = { offset ->
                    editorState.updateDrag(offset)
                    val dragging = editorState.draggingTile.value
                    if (dragging != null) {
                        val dCol = (offset.x / cellSizePx).roundToInt()
                        val dRow = (offset.y / cellSizePx).roundToInt()
                        val newX = (dragging.x + dCol).coerceIn(0, autoColumns - dragging.w)
                        val newY = (dragging.y + dRow).coerceIn(0, (autoRows - dragging.h).coerceAtLeast(0))
                        val free = TileLayoutRepository.isAreaFree(
                            tiles = tiles, x = newX, y = newY,
                            w = dragging.w, h = dragging.h, ignoreId = dragging.id
                        )
                        editorState.targetCell.value =
                            if (free) Pair(newX, newY) else Pair(dragging.x, dragging.y)
                    }
                },
                onDragEnd = { handleDrop() },
                onResizeStart = handleResizeStart,
                onResize = handleResize,
                onResizeEnd = { handleDrop() },
                onDeleteTile = { handleDeleteTile(it) },
                onEmptyTap = { }
            )
        }

        // === ПАНЕЛЬ РЕДАКТОРА ПЛИТКИ — только когда выбрана плитка ===
        // Пресеты теперь живут только в DockBar (showPresetsInDock в MainActivity).
        AnimatedVisibility(
            visible = isEditing && selectedTile != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            TileEditorPanel(
                palette = palette,
                presets = emptyList(),
                selectedTile = selectedTile,
                installedApps = installedApps,
                onPresetClick = { preset -> handlePresetClick(preset) },
                onSystemWidgetClick = {
                    onWidgetPickerVisibilityChange(true)
                },
                onCreateSpaceClick = {
                    onShowCreateSpaceDialogChange(true)
                },
                onDeleteTile = { tile -> handleDeleteTile(tile) },
                onResizeTile = { tile, w, h -> handleResizeTile(tile, w, h) },
                onStyleChange = { tile, style -> handleStyleChange(tile, style) },
                onConfigChange = { tile, config -> handleConfigChange(tile, config) },
                onPickImage = { tile ->
                    pendingImageTile = tile
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onDeselect = { editorState.select(null) },
                onBatchCreateAppTiles = { chosenApps, batchConfig ->
                    val mutableTiles = tiles.toMutableList()
                    chosenApps.forEach { app ->
                        val best = TileLayoutRepository.findBestSpotOrShrink(
                            tiles = mutableTiles,
                            columns = autoColumns,
                            rows = autoRows,
                            initialW = batchConfig.w,
                            initialH = batchConfig.h
                        )
                        if (best != null) {
                            val (finalW, finalH, spot) = best
                            val newTile = Tile(
                                id = UUID.randomUUID().toString(),
                                x = spot.first,
                                y = spot.second,
                                w = finalW,
                                h = finalH,
                                type = TileType.APP_LAUNCHER,
                                style = batchConfig.style,
                                config = AppLauncherConfig(packageName = app.packageName, showLabel = batchConfig.showLabel).toJson()
                            )
                            mutableTiles.add(newTile)
                        }
                    }
                    TileLayoutRepository.setTiles(context, currentSpaceId, mutableTiles, isLandscape)
                    onTilesChange(mutableTiles)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp)
        )

        if (showWidgetPickerSheet) {
            WidgetPickerBottomSheet(
                palette = palette,
                onWidgetSelected = { selectedProvider ->
                    onWidgetPickerVisibilityChange(false)
                    try {
                        val host = WidgetHostManager.getHost(context)
                        val widgetId = host.allocateAppWidgetId()

                        val manager = AppWidgetManager.getInstance(context)
                        val allowed = manager.bindAppWidgetIdIfAllowed(
                            widgetId,
                            selectedProvider.provider
                        )

                        if (allowed) {
                            processBoundWidget(widgetId, selectedProvider)
                        } else {
                            pendingBindWidgetId = widgetId
                            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, selectedProvider.provider)
                            }
                            bindLauncher.launch(bindIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("EtherealWidget", "onWidgetSelected: ERROR", e)
                        scope.launch {
                            snackbarHostState.showSnackbar(widgetAddFailedMsg)
                        }
                    }
                },
                onDismiss = { onWidgetPickerVisibilityChange(false) }
            )
        }

        if (showCreateSpaceDialog) {
            CreateSpaceDialog(
                palette = palette,
                onDismiss = { onShowCreateSpaceDialogChange(false) },
                onCreate = { name, iconUri, iconText ->
                    val newId = SpaceRepository.createSpace(
                        context = context,
                        name = name,
                        iconUri = iconUri,
                        iconText = iconText
                    )

                    AnalyticsHelper.logSpaceCreated(context)

                    val spaceMeta = SpaceRepository.getSpace(context, newId)
                    if (spaceMeta != null) {
                        val newTile = Tile(
                            id = java.util.UUID.randomUUID().toString(),
                            x = 0, y = 0,
                            w = 1, h = 1,
                            type = TileType.SPACE,
                            payload = newId,
                            locked = false,
                            clickable = true,
                            config = SpaceConfig(
                                name = spaceMeta.name,
                                iconUri = spaceMeta.iconUri,
                                iconText = spaceMeta.iconText
                            ).toJson()
                        )
                        val updated = TileLayoutRepository.addTile(
                            context = context,
                            spaceId = currentSpaceIdState,
                            tiles = currentTiles,
                            newTile = newTile,
                            columns = currentColumns,
                            rows = currentRows
                        )
                        if (updated != null) {
                            onTilesChange(updated)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(noSpaceMsg)
                            }
                        }
                    }

                    onShowCreateSpaceDialogChange(false)
                }
            )
        }
    }
}

private fun addSystemWidgetToGrid(
    context: android.content.Context,
    spaceId: String,
    widgetId: Int,
    provider: AppWidgetProviderInfo,
    tiles: List<Tile>,
    columns: Int,
    rows: Int
): List<Tile>? {
    val reqW = ((provider.minWidth + 69) / 70).coerceAtLeast(1)
    val reqH = ((provider.minHeight + 69) / 70).coerceAtLeast(1)

    val best = TileLayoutRepository.findBestSpotOrShrink(
        tiles = tiles,
        columns = columns,
        rows = rows,
        initialW = reqW,
        initialH = reqH
    )

    if (best == null) return null

    val (finalW, finalH, spot) = best

    val newTile = Tile(
        id = java.util.UUID.randomUUID().toString(),
        x = spot.first, y = spot.second,
        w = finalW, h = finalH,
        type = TileType.WIDGET,
        payload = "appwidget",
        locked = false, clickable = true,
        config = EtherealAppWidgetHost.encodeWidgetId(widgetId)
    )

    val updated = tiles + newTile
    TileLayoutRepository.setTiles(context, spaceId, updated)
    return updated
}

private fun reflowTilesLeftToRight(tiles: List<Tile>, columns: Int): List<Tile> {
    val sorted = tiles.sortedWith(compareBy({ it.y }, { it.x }))
    val result = mutableListOf<Tile>()
    var currentX = 0
    var currentY = 0

    for (tile in sorted) {
        val w = tile.w.coerceAtMost(columns)
        val h = tile.h

        if (currentX + w > columns) {
            currentX = 0
            currentY++
        }

        result.add(tile.copy(x = currentX, y = currentY))
        currentX += w
        if (currentX >= columns) {
            currentX = 0
            currentY++
        }
    }
    return result
}

private fun copyImageToInternal(context: android.content.Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "tile_images")
        if (!dir.exists()) dir.mkdirs()

        val mimeType = context.contentResolver.getType(uri)
        val extension = when {
            mimeType == "image/gif" -> "gif"
            mimeType == "image/png" -> "png"
            mimeType == "image/webp" -> "webp"
            mimeType != null -> MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType) ?: "jpg"
            else -> "jpg"
        }

        val file = File(dir, "tile_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun isDefaultLauncher(context: Context): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == context.packageName
    } catch (e: Exception) {
        false
    }
}