package com.neytron.ethereal.tiles

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.neytron.ethereal.data.AppRepository
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
    isDockEnabled: Boolean,
    tiles: List<Tile>,
    onTilesChange: (List<Tile>) -> Unit,
    openWidgetPickerRequest: Int,
    onWidgetPickerRequestHandled: () -> Unit,
    showWidgetPickerSheet: Boolean,
    onWidgetPickerVisibilityChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeUp: () -> Unit,
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

    val tileSize: Dp = tileSizeDp.dp
    val spacing: Dp = spacingDp.dp
    val radius: Dp = radiusDp.dp

    val edgePadding = 16.dp
    val topPadding = 72.dp
    val gapAboveDock = 16.dp

    val dockHeight = dockIconSizeDp.dp + 24.dp
    val dockBottomPadding = dockBottomPaddingDp.dp

    val screenWidthDp = configuration.screenWidthDp.dp
    val availableWidth = screenWidthDp - edgePadding * 2

    val autoColumns = remember(tileSizeDp, spacingDp, availableWidth) {
        val ts = tileSizeDp
        val sp = spacingDp
        if (ts <= 0f) 1
        else {
            val n = ((availableWidth.value + sp) / (ts + sp)).toInt()
            n.coerceIn(1, 12)
        }
    }

    val screenHeightDp = configuration.screenHeightDp.dp

    val autoRows = remember(
        tileSizeDp, spacingDp, screenHeightDp,
        dockHeight, dockBottomPadding, topPadding, gapAboveDock
    ) {
        val availableHeight =
            screenHeightDp - topPadding - gapAboveDock - dockHeight - dockBottomPadding
        val ts = tileSizeDp
        val sp = spacingDp
        if (ts <= 0f || availableHeight.value <= 0f) 0
        else {
            val n = ((availableHeight.value + sp) / (ts + sp)).toInt()
            n.coerceAtLeast(0)
        }
    }

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
                val newTiles = TileLayoutRepository.getTiles(context, currentSpaceId)
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
                TileLayoutRepository.setTiles(context, currentSpaceId, normalized)
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
            newConfig = newConfig
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
        val dragging = editorState.draggingTile.value
        if (dragging == null) {
            editorState.endDrag()
        } else {
            val offset = editorState.dragOffsetPx.value
            val dCol = (offset.x / cellSizePx).roundToInt()
            val dRow = (offset.y / cellSizePx).roundToInt()

            val newX = (dragging.x + dCol).coerceIn(0, autoColumns - dragging.w)
            val newY = (dragging.y + dRow).coerceIn(0, (autoRows - dragging.h).coerceAtLeast(0))

            val updated = TileLayoutRepository.placeTileWithDisplacement(
                tiles = tiles,
                draggingId = dragging.id,
                newX = newX,
                newY = newY,
                columns = autoColumns,
                rows = autoRows
            )

            val finalTiles = if (updated != null && updated != tiles) {
                onTilesChange(updated)
                TileLayoutRepository.setTiles(context, currentSpaceId, updated)
                updated
            } else {
                tiles
            }

            if (selectedTile?.id == dragging.id) {
                val updatedSelected = finalTiles.firstOrNull { it.id == dragging.id }
                editorState.select(updatedSelected)
            }

            editorState.dragOffsetPx.value = Offset.Zero
            editorState.targetCell.value = null
            editorState.draggingTile.value = null
        }
    }

    val handlePresetClick: (TilePreset) -> Unit = { preset ->
        val updated = TileLayoutRepository.addTileFromPreset(
            context = context,
            spaceId = currentSpaceId,
            tiles = tiles,
            preset = preset,
            columns = autoColumns,
            rows = autoRows
        )
        if (updated != null) {
            onTilesChange(updated)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(noSpaceMsg)
            }
        }
    }

    val spectrumPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingSpectrumPreset
        pendingSpectrumPreset = null
        if (granted && pending != null) {
            handlePresetClick(pending)
        } else if (!granted) {
            scope.launch { snackbarHostState.showSnackbar(audioPermissionDeniedMsg) }
        }
    }

    val handlePresetSelection: (TilePreset) -> Unit = { preset ->
        if (
            preset.type == TileType.EQUALIZER &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingSpectrumPreset = preset
            showSpectrumPermissionExplanation = true
        } else {
            handlePresetClick(preset)
        }
    }

    LaunchedEffect(handlePresetClick) {
        onPresetClickReady(handlePresetSelection)
    }

    val handleDeleteTile: (Tile) -> Unit = { tile ->
        if (tile.type == TileType.SPACE && !tile.payload.isNullOrEmpty()) {
            val spaceId = tile.payload!!
            val innerTiles = TileLayoutRepository.getTiles(context, spaceId)
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
            tileId = tile.id
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
                rows = autoRows
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

    val handleStyleChange: (Tile, TileStyle) -> Unit = { tile, newStyle ->
        val updated = TileLayoutRepository.updateStyle(
            context = context,
            spaceId = currentSpaceId,
            tiles = tiles,
            tileId = tile.id,
            newStyle = newStyle
        )
        if (updated != null) {
            onTilesChange(updated)
            editorState.select(updated.first { it.id == tile.id })
        }
    }

    val handleTileClick: (Tile) -> Unit = { tile ->
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
                            incomingTilesState = TileLayoutRepository.getTiles(context, spaceId)
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
                        AppRepository.launchApp(context, pkg)
                    }
                }

                TileType.WIDGET -> {
                    val config = AppLauncherConfig.fromJson(tile.config)
                    if (config.packageName != null) {
                        AppRepository.launchApp(context, config.packageName)
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
                introReveal = introReveal,
                isIntroActive = isIntroActive,
                isTransitioning = isTransitioning,
                pivotFractionX = transitionPivotFractionX,
                pivotFractionY = transitionPivotFractionY,
                oldTiles = oldTilesState,
                incomingTiles = incomingTilesState,
                isPop = isPopState,
                transitionProgress = transitionProgress,
                onTileClick = { tile ->
                    if (isEditing) editorState.select(tile)
                    else handleTileClick(tile)
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
                onDeselect = { editorState.select(null) }
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

        if (showSpectrumPermissionExplanation) {
            AlertDialog(
                onDismissRequest = {
                    showSpectrumPermissionExplanation = false
                    pendingSpectrumPreset = null
                },
                title = { androidx.compose.material3.Text(stringResource(R.string.audio_spectrum_permission_title)) },
                text = { androidx.compose.material3.Text(stringResource(R.string.audio_spectrum_permission_rationale)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSpectrumPermissionExplanation = false
                            spectrumPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    ) {
                        androidx.compose.material3.Text(stringResource(R.string.continue_label))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSpectrumPermissionExplanation = false
                            pendingSpectrumPreset = null
                        }
                    ) {
                        androidx.compose.material3.Text(stringResource(R.string.cancel))
                    }
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