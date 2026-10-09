package com.neytron.ethereal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.compose.AsyncImage
import java.io.File
import com.neytron.ethereal.data.AnalyticsHelper
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.data.AppRepository
import com.neytron.ethereal.data.SettingsRepository
import com.neytron.ethereal.data.UpdateCheckWorker
import com.neytron.ethereal.data.UpdateInfo
import com.neytron.ethereal.data.UpdateRepository
import com.neytron.ethereal.data.WallpaperRepository
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TileLayoutRepository
import com.neytron.ethereal.tiles.TilesScreen
import com.neytron.ethereal.tiles.TilePreset
import com.neytron.ethereal.tiles.editor.defaultPresets
import com.neytron.ethereal.tiles.editor.rememberTileEditorState
import com.neytron.ethereal.ui.DockBar
import com.neytron.ethereal.ui.ESearchOverlay
import com.neytron.ethereal.ui.OnboardingOverlay
import com.neytron.ethereal.ui.SettingsScreen
import com.neytron.ethereal.ui.UpdateOverlay
import com.neytron.ethereal.ui.navigation.ScreenStack
import com.neytron.ethereal.ui.theme.AppearanceState
import com.neytron.ethereal.ui.theme.DockConfig
import com.neytron.ethereal.ui.theme.ESearchConfig
import com.neytron.ethereal.ui.theme.EtherealFont
import com.neytron.ethereal.ui.theme.EtherealLauncherTheme
import com.neytron.ethereal.ui.theme.IconShape
import com.neytron.ethereal.ui.theme.LocalAppearance
import com.neytron.ethereal.ui.theme.LocalDockConfig
import com.neytron.ethereal.ui.theme.LocalESearchConfig
import com.neytron.ethereal.ui.theme.LocalWallpaperPath
import com.neytron.ethereal.ui.theme.LocalWallpaperScaleMode
import com.neytron.ethereal.ui.theme.fontFamilyFor
import com.neytron.ethereal.ui.theme.paletteById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.neytron.ethereal.widgets.WidgetHostManager

class MainActivity : ComponentActivity() {

    var onHomePressed: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                }
                add(GifDecoder.Factory())
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        // === Планирование фоновой проверки обновлений ===
        UpdateCheckWorker.schedule(applicationContext)

        setContent {
            EtherealApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
            onHomePressed?.invoke()
        }
    }

    override fun onStart() {
        super.onStart()
        WidgetHostManager.startListening(this)
    }

    override fun onStop() {
        super.onStop()
        WidgetHostManager.stopListening(this)
    }
}

@Composable
fun EtherealApp() {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var dockPackages by remember { mutableStateOf(SettingsRepository.getDockPackages(context)) }
    var paletteId by remember { mutableStateOf(SettingsRepository.getPaletteId(context)) }
    var opacity by remember { mutableStateOf(SettingsRepository.getOpacity(context)) }
    var blur by remember { mutableStateOf(SettingsRepository.getBlur(context)) }
    var fontScale by remember { mutableStateOf(SettingsRepository.getFontScale(context)) }
    var fontFamilyName by remember { mutableStateOf(SettingsRepository.getFont(context)) }

    var isSetupCompleted by remember {
        mutableStateOf(SettingsRepository.isSetupCompleted(context))
    }

    var analyticsEnabled by remember {
        mutableStateOf(SettingsRepository.getAnalyticsEnabled(context))
    }

    var esearchUseOverride by remember { mutableStateOf(SettingsRepository.getESearchUseOverride(context)) }
    var esearchOpacity by remember { mutableStateOf(SettingsRepository.getESearchOpacity(context)) }
    var esearchBlur by remember { mutableStateOf(SettingsRepository.getESearchBlur(context)) }
    var esearchWidth by remember { mutableStateOf(SettingsRepository.getESearchWidth(context)) }
    var esearchVisibleItems by remember { mutableStateOf(SettingsRepository.getESearchVisibleItems(context)) }

    var tileSize by remember { mutableStateOf(SettingsRepository.getTileSize(context)) }
    var tileSpacing by remember { mutableStateOf(SettingsRepository.getTileSpacing(context)) }
    var tileRadius by remember { mutableStateOf(SettingsRepository.getTileRadius(context)) }

    var searchVisible by remember { mutableStateOf(false) }
    var isIntroRoulette by remember { mutableStateOf(false) }
    var isIntroActive by remember { mutableStateOf(false) }
    var isTilesRevealing by remember { mutableStateOf(false) }
    var hasAnimatedStartup by remember { mutableStateOf(false) }

    LaunchedEffect(isTilesRevealing) {
        if (isTilesRevealing) {
            delay(1800L)
            isTilesRevealing = false
        }
    }
    var settingsVisible by remember { mutableStateOf(false) }
    var wallpaperList by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentWallpaperPath by remember { mutableStateOf<String?>(null) }
    var wallpaperScaleMode by remember { mutableStateOf(SettingsRepository.getWallpaperScaleMode(context)) }

    var dockIconSize by remember { mutableStateOf(SettingsRepository.getDockIconSize(context)) }
    var dockBottomPadding by remember { mutableStateOf(SettingsRepository.getDockBottomPadding(context)) }
    var dockUseOverride by remember { mutableStateOf(SettingsRepository.getDockUseOverride(context)) }
    var dockOpacity by remember { mutableStateOf(SettingsRepository.getDockOpacity(context)) }
    var dockBlur by remember { mutableStateOf(SettingsRepository.getDockBlur(context)) }
    var dockScrollable by remember { mutableStateOf(SettingsRepository.getDockScrollable(context)) }
    var dockIconShape by remember { mutableStateOf(SettingsRepository.getDockIconShape(context)) }
    var dockShowIconBackground by remember { mutableStateOf(SettingsRepository.getDockShowIconBackground(context)) }
    var dockThemedIcons by remember { mutableStateOf(SettingsRepository.getDockThemedIcons(context)) }

    var tiles by remember { mutableStateOf<List<Tile>>(emptyList()) }
    var openWidgetPickerRequest by remember { mutableStateOf(0) }
    var showWidgetPickerSheet by remember { mutableStateOf(false) }
    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var presetClickHandler by remember { mutableStateOf<((TilePreset) -> Unit)?>(null) }

    // === Состояние системы обновлений ===
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateChecking by remember { mutableStateOf(false) }
    var showUpdateOverlay by remember { mutableStateOf(false) }

    val esearchPlaceholder = stringResource(R.string.esearch_placeholder)

    val tileEditorState = rememberTileEditorState()

    val screenStack = remember {
        ScreenStack(ScreenStack.ROOT_ID)
    }
    val currentSpaceId = screenStack.current

    val activity = context as? MainActivity

    // === Проверка обновлений при старте (из кэша, без сети) ===
    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) {
            UpdateRepository.getUpdateInfo(
                context = context,
                currentVersion = BuildConfig.VERSION_NAME,
                forceRefresh = false
            )
        }
        updateInfo = cached
    }

    // === Перехват кнопки Домой (Home) ===
    DisposableEffect(activity) {
        activity?.onHomePressed = {
            val isAnyOverlayOpen = showWidgetPickerSheet ||
                    showCreateSpaceDialog ||
                    settingsVisible ||
                    searchVisible ||
                    tileEditorState.editing.value ||
                    showUpdateOverlay ||
                    screenStack.canGoBack

            if (isAnyOverlayOpen) {
                showWidgetPickerSheet = false
                showCreateSpaceDialog = false
                settingsVisible = false
                searchVisible = false
                showUpdateOverlay = false
                if (tileEditorState.selectedTile.value != null) tileEditorState.select(null)
                if (tileEditorState.editing.value) tileEditorState.exitEdit()
                screenStack.resetToRoot()
            } else if (screenStack.canGoBack) {
                screenStack.resetToRoot()
            }
        }
        onDispose {
            activity?.onHomePressed = null
        }
    }

    // === Analytics: онбординг начинается ===
    LaunchedEffect(isSetupCompleted) {
        if (!isSetupCompleted) {
            AnalyticsHelper.logOnboardingBegin(context)
        }
    }

    // === Analytics: вход в режим редактирования ===
    LaunchedEffect(tileEditorState.editing.value) {
        if (tileEditorState.editing.value) {
            AnalyticsHelper.logEditModeEntered(context)
        }
    }

    // === Загрузка данных при старте ===
    LaunchedEffect(Unit) {
        val loadedApps = withContext(Dispatchers.IO) {
            AppRepository.getInstalledApps(context)
        }
        apps = loadedApps

        withContext(Dispatchers.IO) {
            WallpaperRepository.installBuiltinWallpapers(
                context,
                listOf(
                    R.drawable.builtin_wallpaper_1,
                    R.drawable.builtin_wallpaper_2,
                    R.drawable.builtin_wallpaper_3,
                    R.drawable.builtin_wallpaper_4,
                    R.drawable.builtin_wallpaper_5
                )
            )
        }

        wallpaperList = withContext(Dispatchers.IO) {
            WallpaperRepository.getUserWallpapers(context)
        }

        if (WallpaperRepository.hasWallpaper(context)) {
            val path = withContext(Dispatchers.IO) {
                val file = WallpaperRepository.getCurrentFile(context)
                if (file.exists()) file.absolutePath else null
            }
            currentWallpaperPath = path
        }
    }

    // === Firebase Analytics: инициализация и применение настройки ===
    LaunchedEffect(analyticsEnabled) {
        try {
            com.google.firebase.FirebaseApp.initializeApp(context)
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                .setAnalyticsCollectionEnabled(analyticsEnabled)
        } catch (_: Exception) {
        }
    }

    // === Загрузка tiles при смене пространства ===
    LaunchedEffect(currentSpaceId) {
        tiles = emptyList()

        tileEditorState.exitEdit()
        tileEditorState.select(null)

        tiles = withContext(Dispatchers.IO) {
            TileLayoutRepository.getTiles(context, currentSpaceId)
        }
        if (isSetupCompleted && !hasAnimatedStartup) {
            hasAnimatedStartup = true
            isTilesRevealing = true
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedFile = WallpaperRepository.saveUserWallpaper(context, uri)
            if (savedFile != null) {
                wallpaperList = WallpaperRepository.getUserWallpapers(context)
                currentWallpaperPath = savedFile.absolutePath
            }
        }
    }

    val palette = paletteById(paletteId)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = palette.isLight
            controller.isAppearanceLightNavigationBars = palette.isLight
        }
    }

    val fontFamily = try {
        fontFamilyFor(EtherealFont.valueOf(fontFamilyName))
    } catch (e: Exception) {
        null
    }

    val appearance = AppearanceState(
        opacity = opacity,
        blur = blur,
        fontScale = fontScale,
        fontFamily = fontFamily
    )

    val esearchConfig = ESearchConfig(
        opacityOverride = if (esearchUseOverride) esearchOpacity else null,
        blurOverride = if (esearchUseOverride) esearchBlur else null,
        widthFraction = esearchWidth,
        heightFraction = 0.6f,
        visibleItems = esearchVisibleItems,
        placeholder = esearchPlaceholder,
        themedIcons = dockThemedIcons
    )

    val dockConfig = DockConfig(
        iconSizeDp = dockIconSize,
        bottomPaddingDp = dockBottomPadding,
        opacityOverride = if (dockUseOverride) dockOpacity else null,
        blurOverride = if (dockUseOverride) dockBlur else null,
        scrollable = dockScrollable,
        iconShape = try {
            IconShape.valueOf(dockIconShape)
        } catch (e: Exception) {
            IconShape.SQUIRCLE_TIGHT
        },
        showIconBackground = dockShowIconBackground,
        themedIcons = dockThemedIcons
    )

    val density = LocalDensity.current

    // === ЕДИНЫЙ BackHandler ===
    val isAnyOverlayOpen = showWidgetPickerSheet ||
            showCreateSpaceDialog ||
            settingsVisible ||
            searchVisible ||
            tileEditorState.editing.value ||
            showUpdateOverlay ||
            screenStack.canGoBack

    BackHandler(enabled = true) {
        if (isAnyOverlayOpen) {
            when {
                showUpdateOverlay -> showUpdateOverlay = false
                showWidgetPickerSheet -> showWidgetPickerSheet = false
                showCreateSpaceDialog -> showCreateSpaceDialog = false
                settingsVisible -> settingsVisible = false
                searchVisible -> {
                    searchVisible = false
                    isIntroRoulette = false
                    isIntroActive = false
                }
                tileEditorState.selectedTile.value != null -> tileEditorState.select(null)
                tileEditorState.editing.value -> tileEditorState.exitEdit()
                screenStack.canGoBack -> screenStack.pop()
            }
        } else {
            if (screenStack.canGoBack) {
                screenStack.resetToRoot()
            }
            // Если ничего не открыто и мы на главном экране — перехватываем «Назад»,
            // чтобы система не пересоздавала и не закрывала лаунчер.
        }
    }

    val showPresetsInDock = tileEditorState.editing.value && tileEditorState.selectedTile.value == null
    val isTileSelected = tileEditorState.selectedTile.value != null

    // === DockBar виден, когда: ===
    // - не выбрана плитка (иначе показываем TileEditorPanel с настройками),
    // - не открыт пикер виджетов,
    // - не открыт диалог создания пространства.
    //
    // В режиме редактирования с невыбранной плиткой DockBar показывает пресеты (showPresetsInDock).
    // В обычном режиме — приложения.
    // Пресеты в TileEditorPanel больше не дублируются (он показывает только настройки выбранной плитки).
    val dockVisible = !isTileSelected &&
            !showWidgetPickerSheet &&
            !showCreateSpaceDialog

    // === Ручная проверка обновлений (для кнопки в About) ===
    val updateScope = rememberCoroutineScope()
    val handleCheckForUpdates: () -> Unit = {
        if (!updateChecking) {
            updateChecking = true
            updateScope.launch {
                val info = withContext(Dispatchers.IO) {
                    UpdateRepository.getUpdateInfo(
                        context = context,
                        currentVersion = BuildConfig.VERSION_NAME,
                        forceRefresh = true
                    )
                }
                updateInfo = info
                updateChecking = false
            }
        }
    }

    CompositionLocalProvider(
        LocalAppearance provides appearance,
        LocalESearchConfig provides esearchConfig,
        LocalDockConfig provides dockConfig,
        LocalWallpaperPath provides currentWallpaperPath,
        LocalWallpaperScaleMode provides wallpaperScaleMode,
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * appearance.fontScale
        )
    ) {
        EtherealLauncherTheme(palette = palette, fontFamily = fontFamily) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
            ) {
                if (currentWallpaperPath != null) {
                    val contentScaleMode = when (wallpaperScaleMode) {
                        "FIT" -> ContentScale.Fit
                        "STRETCH" -> ContentScale.FillBounds
                        "CENTER" -> ContentScale.Inside
                        else -> ContentScale.Crop
                    }
                    AsyncImage(
                        model = File(currentWallpaperPath!!),
                        contentDescription = null,
                        contentScale = contentScaleMode,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(palette.background, palette.surface)
                                )
                            )
                    )
                }

                TilesScreen(
                    palette = palette,
                    tileSizeDp = tileSize,
                    spacingDp = tileSpacing,
                    radiusDp = tileRadius,
                    dockIconSizeDp = dockIconSize,
                    dockBottomPaddingDp = dockBottomPadding,
                    installedApps = apps,
                    themedIcons = dockThemedIcons,
                    isDockEnabled = dockPackages.isNotEmpty(),
                    tiles = tiles,
                    onTilesChange = { newTiles ->
                        tiles = newTiles
                    },
                    openWidgetPickerRequest = openWidgetPickerRequest,
                    onWidgetPickerRequestHandled = {
                        openWidgetPickerRequest = 0
                    },
                    showWidgetPickerSheet = showWidgetPickerSheet,
                    onWidgetPickerVisibilityChange = { visible ->
                        showWidgetPickerSheet = visible
                    },
                    onOpenSettings = { settingsVisible = true },
                    onSwipeUp = {
                        searchVisible = true
                        AnalyticsHelper.logESearchOpened(context)
                    },
                    editorState = tileEditorState,
                    currentSpaceId = currentSpaceId,
                    onOpenSpace = { spaceId ->
                        screenStack.push(spaceId)
                    },
                    showCreateSpaceDialog = showCreateSpaceDialog,
                    onShowCreateSpaceDialogChange = { v -> showCreateSpaceDialog = v },
                    introReveal = isTilesRevealing,
                    isIntroActive = isIntroActive,
                    onPresetClickReady = { handler ->
                        presetClickHandler = handler
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    visible = dockVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    val dockApps = apps.filter { dockPackages.contains(it.packageName) }
                    DockBar(
                        apps = dockApps,
                        palette = palette,
                        onAppClick = { app ->
                            SettingsRepository.recordLaunch(context, app.packageName)
                            AppRepository.launchApp(context, app.packageName)
                        },
                        showPresets = showPresetsInDock,
                        presets = defaultPresets(),
                        onPresetClick = { preset ->
                            presetClickHandler?.invoke(preset)
                        },
                        onSystemWidgetClick = {
                            openWidgetPickerRequest++
                        },
                        onCreateSpaceClick = {
                            showCreateSpaceDialog = true
                        },
                        introFlyIn = isIntroRoulette,
                        modifier = Modifier
                            .padding(bottom = dockBottomPadding.dp)
                    )
                }
            }

            ESearchOverlay(
                visible = searchVisible,
                apps = apps,
                palette = palette,
                onAppClick = { app ->
                    SettingsRepository.recordLaunch(context, app.packageName)
                    AppRepository.launchApp(context, app.packageName)
                    searchVisible = false
                    isIntroRoulette = false
                    isIntroActive = false
                },
                onDismiss = {
                    searchVisible = false
                    isIntroRoulette = false
                    isIntroActive = false
                },
                rouletteMode = isIntroRoulette,
                onRouletteComplete = {
                    searchVisible = false
                    isIntroActive = false
                    isIntroRoulette = false
                    isTilesRevealing = true
                }
            )

            AnimatedVisibility(
                visible = settingsVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 6 })
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { settingsVisible = false }
                    )

                    SettingsScreen(
                        apps = apps,
                        currentPalette = palette,
                        currentDock = dockPackages,
                        onPaletteChange = { newPalette ->
                            paletteId = newPalette.id
                            SettingsRepository.setPaletteId(context, newPalette.id)
                            AnalyticsHelper.logPaletteSelected(context, newPalette.id)
                        },
                        onDockChange = { newDock ->
                            dockPackages = newDock
                            SettingsRepository.setDockPackages(context, newDock)
                        },
                        onOpacityChange = { newOpacity ->
                            opacity = newOpacity
                            SettingsRepository.setOpacity(context, newOpacity)
                        },
                        onBlurChange = { newBlur ->
                            blur = newBlur
                            SettingsRepository.setBlur(context, newBlur)
                        },
                        onFontScaleChange = { newScale ->
                            fontScale = newScale
                            SettingsRepository.setFontScale(context, newScale)
                        },
                        fontFamilyName = fontFamilyName,
                        onFontFamilyChange = { v ->
                            fontFamilyName = v
                            SettingsRepository.setFont(context, v)
                            AnalyticsHelper.logFontSelected(context, v)
                        },

                        wallpapers = wallpaperList,
                        currentWallpaperPath = currentWallpaperPath,
                        wallpaperScaleMode = wallpaperScaleMode,
                        onWallpaperScaleModeChange = { mode ->
                            wallpaperScaleMode = mode
                            SettingsRepository.setWallpaperScaleMode(context, mode)
                        },
                        onPickWallpaper = {
                            wallpaperPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onApplyWallpaper = { file ->
                            WallpaperRepository.applyAsCurrent(context, file)
                            currentWallpaperPath = file.absolutePath
                            AnalyticsHelper.logWallpaperChanged(context)
                        },
                        onApplyWallpaperLockScreen = { file ->
                            WallpaperRepository.applyToLockScreen(context, file)
                        },
                        onApplyWallpaperEverywhere = { file ->
                            WallpaperRepository.applyEverywhere(context, file)
                            currentWallpaperPath = file.absolutePath
                            AnalyticsHelper.logWallpaperChanged(context)
                        },
                        onDeleteWallpaper = { file ->
                            WallpaperRepository.deleteUserWallpaper(context, file)
                            wallpaperList = WallpaperRepository.getUserWallpapers(context)
                            val current = WallpaperRepository.getCurrentFile(context)
                            currentWallpaperPath = if (current.exists()) current.absolutePath else null
                        },

                        esearchUseOverride = esearchUseOverride,
                        esearchOpacity = esearchOpacity,
                        esearchBlur = esearchBlur,
                        esearchWidth = esearchWidth,
                        esearchVisibleItems = esearchVisibleItems,
                        onESearchUseOverrideChange = { v ->
                            esearchUseOverride = v
                            SettingsRepository.setESearchUseOverride(context, v)
                        },
                        onESearchOpacityChange = { v ->
                            esearchOpacity = v
                            SettingsRepository.setESearchOpacity(context, v)
                        },
                        onESearchBlurChange = { v ->
                            esearchBlur = v
                            SettingsRepository.setESearchBlur(context, v)
                        },
                        onESearchWidthChange = { v ->
                            esearchWidth = v
                            SettingsRepository.setESearchWidth(context, v)
                        },
                        onESearchVisibleItemsChange = { v ->
                            esearchVisibleItems = v
                            SettingsRepository.setESearchVisibleItems(context, v)
                        },

                        dockIconSize = dockIconSize,
                        dockBottomPadding = dockBottomPadding,
                        dockUseOverride = dockUseOverride,
                        dockOpacity = dockOpacity,
                        dockBlur = dockBlur,
                        dockScrollable = dockScrollable,
                        onDockIconSizeChange = { v ->
                            dockIconSize = v
                            SettingsRepository.setDockIconSize(context, v)
                        },
                        onDockBottomPaddingChange = { v ->
                            dockBottomPadding = v
                            SettingsRepository.setDockBottomPadding(context, v)
                        },
                        onDockUseOverrideChange = { v ->
                            dockUseOverride = v
                            SettingsRepository.setDockUseOverride(context, v)
                        },
                        onDockOpacityChange = { v ->
                            dockOpacity = v
                            SettingsRepository.setDockOpacity(context, v)
                        },
                        onDockBlurChange = { v ->
                            dockBlur = v
                            SettingsRepository.setDockBlur(context, v)
                        },
                        onDockScrollableChange = { v ->
                            dockScrollable = v
                            SettingsRepository.setDockScrollable(context, v)
                        },
                        dockIconShape = dockIconShape,
                        onDockIconShapeChange = { v ->
                            dockIconShape = v
                            SettingsRepository.setDockIconShape(context, v)
                        },
                        dockShowIconBackground = dockShowIconBackground,
                        onDockShowIconBackgroundChange = { v ->
                            dockShowIconBackground = v
                            SettingsRepository.setDockShowIconBackground(context, v)
                        },
                        dockThemedIcons = dockThemedIcons,
                        onDockThemedIconsChange = { v ->
                            dockThemedIcons = v
                            SettingsRepository.setDockThemedIcons(context, v)
                            AnalyticsHelper.logThemedIconsToggled(context, v)
                        },

                        tileSize = tileSize,
                        tileSpacing = tileSpacing,
                        tileRadius = tileRadius,
                        onTileSizeChange = { v ->
                            tileSize = v
                            SettingsRepository.setTileSize(context, v)
                            val current = TileLayoutRepository.getTiles(context, currentSpaceId)
                            val normalized = TileLayoutRepository.normalizeTiles(current, 12)
                            TileLayoutRepository.setTiles(context, currentSpaceId, normalized)
                            tiles = normalized
                        },
                        onTileSpacingChange = { v ->
                            tileSpacing = v
                            SettingsRepository.setTileSpacing(context, v)
                        },
                        onTileRadiusChange = { v ->
                            tileRadius = v
                            SettingsRepository.setTileRadius(context, v)
                        },
                        onResetTiles = {
                            TileLayoutRepository.resetToDefault(context, currentSpaceId)
                            tiles = TileLayoutRepository.getTiles(context, currentSpaceId)
                        },
                        onRestartOnboarding = {
                            SettingsRepository.setSetupCompleted(context, false)
                            isSetupCompleted = false
                            settingsVisible = false
                        },
                        onClose = { settingsVisible = false },

                        analyticsEnabled = analyticsEnabled,
                        onAnalyticsEnabledChange = { value ->
                            analyticsEnabled = value
                            SettingsRepository.setAnalyticsEnabled(context, value)
                        },

                        updateInfo = updateInfo,
                        updateChecking = updateChecking,
                        onCheckForUpdates = handleCheckForUpdates,
                        onOpenUpdateOverlay = { showUpdateOverlay = true },

                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }

            // === Оверлей обновления ===
            UpdateOverlay(
                visible = showUpdateOverlay,
                info = updateInfo,
                palette = palette,
                onDismiss = { showUpdateOverlay = false }
            )

            OnboardingOverlay(
                visible = !isSetupCompleted,
                apps = apps,
                palette = palette,
                fontFamilyName = fontFamilyName,
                dockPackages = dockPackages,
                onPaletteChange = { newPaletteId ->
                    paletteId = newPaletteId
                    SettingsRepository.setPaletteId(context, newPaletteId)
                    AnalyticsHelper.logPaletteSelected(context, newPaletteId)
                },
                onFontChange = { newFontName ->
                    fontFamilyName = newFontName
                    SettingsRepository.setFont(context, newFontName)
                    AnalyticsHelper.logFontSelected(context, newFontName)
                },
                onDockPackagesChange = { newDock ->
                    dockPackages = newDock
                    SettingsRepository.setDockPackages(context, newDock)
                },
                onComplete = {
                    SettingsRepository.setSetupCompleted(context, true)
                    isSetupCompleted = true
                    isIntroActive = true
                    isIntroRoulette = true
                    searchVisible = true
                    AnalyticsHelper.logOnboardingComplete(context)
                }
            )
        }
    }
}