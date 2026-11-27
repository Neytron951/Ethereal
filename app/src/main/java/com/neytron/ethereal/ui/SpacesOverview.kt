package com.neytron.ethereal.ui

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.AppLauncherConfig
import com.neytron.ethereal.tiles.ClockTileContent
import com.neytron.ethereal.tiles.SearchTileContent
import com.neytron.ethereal.tiles.SpaceConfig
import com.neytron.ethereal.tiles.SpaceTileContent
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TileAppIconContent
import com.neytron.ethereal.tiles.TileBorderStyle
import com.neytron.ethereal.tiles.TileGridMetrics
import com.neytron.ethereal.tiles.TileGridSpec
import com.neytron.ethereal.tiles.TileType
import com.neytron.ethereal.tiles.WeatherTileContent
import com.neytron.ethereal.tiles.tileBorderForShape
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.LocalAppearance
import com.neytron.ethereal.ui.theme.TileShape
import com.neytron.ethereal.ui.theme.flatPanelOpacity
import com.neytron.ethereal.ui.theme.tileShapeFor
import com.neytron.ethereal.ui.navigation.ScreenStack
import com.neytron.ethereal.utils.tileTexture
import com.neytron.ethereal.utils.toImageBitmap
import com.neytron.ethereal.widgets.EtherealAppWidgetHost
import android.appwidget.AppWidgetManager
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Одно пространство в мире. Позиция — мировые координаты в dp, те же,
 * что лежат в SpaceRepository (canvasX/canvasY). ROOT всегда в (0, 0).
 */
data class SpacePreview(
    val id: String,
    val name: String,
    val iconText: String?,
    val tiles: List<Tile>,
    val canvasX: Float,
    val canvasY: Float,
    val isRoot: Boolean = false,
    val parentId: String? = null
)

/** Радиус скругления «экрана» пространства в мире. */
private val SCREEN_CORNER = 26.dp

/** Насколько сильно можно отъехать камерой. */
const val SPACES_MIN_SCALE = 0.12f

/**
 * Кэш картинок системных виджетов: провайдер отдаёт готовое превью для пикеров,
 * им и пользуемся — это честнее, чем пустая плашка.
 */
private val widgetPreviewCache = mutableMapOf<Int, ImageBitmap?>()

/**
 * Камера мира пространств.
 *
 * scale — масштаб, offset — смещение в пикселях. Мировые координаты в dp,
 * точка мира p попадает на экран в p * scale + offset.
 *
 * Состояние общее для домашнего экрана и слоя мира: домашний экран — это
 * текущее пространство, поэтому он масштабируется той же камерой. Именно
 * поэтому жест «отдаление» не открывает оверлей, а отъезжает камерой.
 */
@Stable
class SpacesWorldState {

    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Обзор «зафиксирован»: палец отпущен, можно тапать по пространствам. */
    var open by mutableStateOf(false)

    /** Какое пространство тащат прямо сейчас (оно не должно ловить тапы). */
    var draggingId by mutableStateOf<String?>(null)

    /**
     * Сдвиг текущего пространства под пальцем во время перетаскивания (в пикселях).
     * Держим отдельно от позиции: пока палец ведёт, мир не пересобирается,
     * а позиция и камера фиксируются один раз — на отпускании.
     */
    var homeDragPx by mutableStateOf(Offset.Zero)

    val isZoomedOut: Boolean get() = scale < 0.999f || open

    /** Зум вокруг точки экрана + панорамирование (дельты в пикселях). */
    fun applyTransform(zoom: Float, focalPx: Offset, panPx: Offset) {
        val next = (scale * zoom).coerceIn(SPACES_MIN_SCALE, 1f)
        val k = if (scale <= 0.0001f) 1f else next / scale
        offset = Offset(
            focalPx.x - (focalPx.x - offset.x) * k + panPx.x,
            focalPx.y - (focalPx.y - offset.y) * k + panPx.y
        )
        scale = next
    }

    fun moveBy(panPx: Offset) {
        offset += panPx
    }

    fun setCamera(newScale: Float, newOffset: Offset) {
        scale = newScale.coerceIn(SPACES_MIN_SCALE, 1f)
        offset = newOffset
    }

    /** Возврат камеры точно на текущий экран. */
    fun reset() {
        scale = 1f
        offset = Offset.Zero
        open = false
        draggingId = null
        homeDragPx = Offset.Zero
    }
}

/** Параметры сетки, чтобы превью пространства считалось ровно как живой экран. */
data class SpaceGridParams(
    val tileSizeDp: Float,
    val spacingDp: Float,
    val radiusDp: Float,
    val defaultShape: TileShape,
    val fixedColumns: Int,
    val fixedRows: Int,
    val isLandscape: Boolean,
    val dockIconSizeDp: Float,
    val dockBottomPaddingDp: Float,
    val dockEnabled: Boolean,
    val themedIcons: Boolean,
    // Внешний вид дока — тот же, что в настройках, иначе миниатюра врёт.
    val dockPanelBackground: Boolean = true,
    val dockIconBorderStyle: TileBorderStyle = TileBorderStyle.LIQUID_GLASS,
    val dockIconBorderWidth: Float = 1f,
    val dockIconBorderOpacity: Float = 0.55f,
    val dockIconBackgroundOpacity: Float = 0.5f
)

/** Плавно перегоняет камеру в точку. */
suspend fun animateCameraTo(
    state: SpacesWorldState,
    targetScale: Float,
    targetOffset: Offset,
    durationMillis: Int = 380,
    easing: Easing = FastOutSlowInEasing
) {
    val startScale = state.scale
    val startOffset = state.offset
    animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = tween(durationMillis = durationMillis, easing = easing)
    ) { value, _ ->
        state.setCamera(
            newScale = startScale + (targetScale - startScale) * value,
            newOffset = Offset(
                startOffset.x + (targetOffset.x - startOffset.x) * value,
                startOffset.y + (targetOffset.y - startOffset.y) * value
            )
        )
    }
}

/**
 * Куда встать камере после жеста: масштаб + центр видимой области в пикселях
 * (в мировых координатах, уже со сдвигом на текущее пространство).
 *
 * Отдельно масштаб и центр — чтобы вызывающий мог не «отъезжать обратно»,
 * если человек свёл пальцы сильнее, чем нужно для кадра.
 */
data class SpaceCameraFit(val scale: Float, val centerPx: Offset)

/**
 * Камера «шаг назад»: в кадр попадает текущее пространство и его ближайшие
 * соседи (родитель и дети), но не весь мир.
 *
 * Раньше здесь был «уместить всё» — именно из-за него обзор превращался
 * в ленту мелких экранов. Теперь структура видна, а остальное — панорамированием.
 */
fun neighborhoodCamera(
    spaces: List<SpacePreview>,
    currentSpaceId: String,
    frameWidthDp: Float,
    frameHeightDp: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    density: Float
): SpaceCameraFit {
    val current = spaces.firstOrNull { it.id == currentSpaceId }
        ?: return SpaceCameraFit(1f, Offset.Zero)
    val originX = current.canvasX
    val originY = current.canvasY

    val childIds = spaces.asSequence()
        .filter { it.parentId == currentSpaceId }
        .map { it.id }
        .toSet()
    val parentId = current.parentId

    val neighborhood = spaces.filter { space ->
        space.id == currentSpaceId ||
            space.id in childIds ||
            (parentId != null && space.id == parentId)
    }

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    neighborhood.forEach { space ->
        val x = space.canvasX - originX
        val y = space.canvasY - originY
        minX = min(minX, x)
        minY = min(minY, y)
        maxX = max(maxX, x + frameWidthDp)
        maxY = max(maxY, y + frameHeightDp)
    }

    val worldW = ((maxX - minX) * density).coerceAtLeast(1f)
    val worldH = ((maxY - minY) * density).coerceAtLeast(1f)
    val fit = min(viewportWidthPx / worldW, viewportHeightPx / worldH) * 0.86f

    // Шаг назад должен быть заметным: даже если соседей рядом нет, отъезжаем
    // хотя бы до 0.56. Ниже 0.30 не мельчим — остальное панорамированием.
    val scale = min(fit, 0.56f).coerceAtLeast(0.30f)

    return SpaceCameraFit(
        scale = scale,
        centerPx = Offset(
            ((minX + maxX) / 2f) * density,
            ((minY + maxY) / 2f) * density
        )
    )
}

// ============================================================
// Слой мира (под домашним экраном)
// ============================================================

/**
 * Мир пространств: рисуется ПОД домашним экраном, поэтому домашний экран —
 * это просто текущее пространство, уменьшенное той же камерой.
 *
 * Показываем настоящие раскладки: иконки, формы, текстуры, подписи и док —
 * то есть ровно то, что человек увидит, если войдёт в пространство.
 */
@Composable
fun SpacesWorldLayer(
    spaces: List<SpacePreview>,
    currentSpaceId: String,
    state: SpacesWorldState,
    palette: EtherealPalette,
    frameWidth: Dp,
    frameHeight: Dp,
    grid: SpaceGridParams,
    installedApps: List<AppInfo>,
    wallpaperPath: String?,
    dockPackages: Set<String>,
    modifier: Modifier = Modifier
) {
    val origin = spaces.firstOrNull { it.id == currentSpaceId }
    val originX = origin?.canvasX ?: 0f
    val originY = origin?.canvasY ?: 0f

    // Обои декодируем ОДИН раз на все миниатюры. Раньше каждый экран грузил
    // свой AsyncImage, и было видно, как фон в рамках «подгружается» по очереди.
    val wallpaperBitmap by produceState<ImageBitmap?>(initialValue = null, wallpaperPath) {
        value = withContext(Dispatchers.IO) {
            wallpaperPath?.let { path ->
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(palette.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                    // Мир скрываем ТОЛЬКО когда камера действительно в покое.
                    // Раньше условие было «scale == 1», и в конце влёта в
                    // пространство (scale уже 1, а домашний экран ещё сдвинут)
                    // слой мира исчезал — экран на секунду становился чёрным,
                    // пока не подменится живой экран.
                    val atRest = state.scale > 0.9995f &&
                        state.offset.x == 0f && state.offset.y == 0f &&
                        state.homeDragPx.x == 0f && state.homeDragPx.y == 0f
                    alpha = if (atRest) 0f else 1f
                }
        ) {
            Connections(
                spaces = spaces,
                originX = originX,
                originY = originY,
                frameWidthDp = frameWidth.value,
                frameHeightDp = frameHeight.value,
                palette = palette
            )

            spaces.forEach { space ->
                // Текущее пространство — это настоящий домашний экран под этим слоем.
                if (space.id == currentSpaceId) return@forEach

                SpaceFrame(
                    space = space,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight,
                    grid = grid,
                    palette = palette,
                    installedApps = installedApps,
                    wallpaper = wallpaperBitmap,
                    dockPackages = dockPackages,
                    modifier = Modifier.offset(
                        x = (space.canvasX - originX).dp,
                        y = (space.canvasY - originY).dp
                    )
                )
            }
        }
    }
}

/**
 * Один настоящий экран пространства: обои, сетка плиток, док.
 * Ничего не заглушка — те же иконки и те же формы, что на живом экране.
 */
@Composable
private fun SpaceFrame(
    space: SpacePreview,
    frameWidth: Dp,
    frameHeight: Dp,
    grid: SpaceGridParams,
    palette: EtherealPalette,
    installedApps: List<AppInfo>,
    wallpaper: ImageBitmap?,
    dockPackages: Set<String>,
    modifier: Modifier = Modifier
) {
    val corner = RoundedCornerShape(SCREEN_CORNER)

    Box(
        modifier = modifier
            .size(frameWidth, frameHeight)
            .clip(corner)
            .background(palette.background)
    ) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
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

        SpaceGridContent(
            tiles = space.tiles,
            frameWidthDp = frameWidth.value,
            frameHeightDp = frameHeight.value,
            grid = grid,
            palette = palette,
            installedApps = installedApps
        )

        if (grid.dockEnabled && dockPackages.isNotEmpty()) {
            PreviewDock(
                packages = dockPackages,
                installedApps = installedApps,
                grid = grid,
                palette = palette,
                // Отступ снизу задаёт сам PreviewDock (он передаёт его в DockBar):
                // раньше он применялся и здесь, и там — док висел выше на один
                // dockBottomPadding и при входе «падал» на место.
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Тонкая рамка экрана: отделяет пространство от фона мира.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.10f), corner)
        )
    }
}

@Composable
private fun SpaceGridContent(
    tiles: List<Tile>,
    frameWidthDp: Float,
    frameHeightDp: Float,
    grid: SpaceGridParams,
    palette: EtherealPalette,
    installedApps: List<AppInfo>
) {
    val layoutColumns = remember(tiles) { tiles.maxOfOrNull { it.x + it.w }?.coerceAtLeast(1) ?: 1 }
    val layoutRows = remember(tiles) { tiles.maxOfOrNull { it.y + it.h }?.coerceAtLeast(1) ?: 1 }

    val spec = remember(tiles, frameWidthDp, frameHeightDp, grid, layoutColumns, layoutRows) {
        TileGridMetrics.compute(
            availableWidthDp = TileGridMetrics.availableWidthDp(frameWidthDp),
            availableHeightDp = TileGridMetrics.availableHeightDp(
                screenHeightDp = frameHeightDp,
                dockIconSizeDp = grid.dockIconSizeDp,
                dockBottomPaddingDp = grid.dockBottomPaddingDp,
                isLandscape = grid.isLandscape
            ),
            desiredTileSizeDp = grid.tileSizeDp,
            spacingDp = grid.spacingDp,
            isLandscape = grid.isLandscape,
            layoutColumns = layoutColumns,
            layoutRows = layoutRows,
            fixedColumns = grid.fixedColumns,
            fixedRows = grid.fixedRows
        )
    }

    val step = spec.cellStepDp
    val originX = (frameWidthDp - spec.totalWidthDp) / 2f
    val originY = TileGridMetrics.TOP_PADDING_DP

    Box(modifier = Modifier.fillMaxSize()) {
        tiles.forEach { tile ->
            PreviewTile(
                tile = tile,
                spec = spec,
                palette = palette,
                installedApps = installedApps,
                grid = grid,
                modifier = Modifier.offset(
                    x = (originX + tile.x * step).dp,
                    y = (originY + tile.y * step).dp
                )
            )
        }
    }
}

/** Плитка в превью: та же форма, подложка, текстура и иконка, что на экране. */
@Composable
private fun PreviewTile(
    tile: Tile,
    spec: TileGridSpec,
    palette: EtherealPalette,
    installedApps: List<AppInfo>,
    grid: SpaceGridParams,
    modifier: Modifier = Modifier
) {
    if (tile.type == TileType.EMPTY) return

    val flat = LocalAppearance.current.isFlat
    val shape = tileShapeFor(tile.style.shape, grid.radiusDp, grid.defaultShape)
    val widthDp = spec.tileSizeDp * tile.w + spec.spacingDp * (tile.w - 1)
    val heightDp = spec.tileSizeDp * tile.h + spec.spacingDp * (tile.h - 1)

    // Прозрачность считаем ТЕМ ЖЕ правилом, что живая плитка: в плоском стиле
    // поверхность плотнее (flatPanelOpacity), иначе миниатюра выглядит прозрачнее.
    val opacity = tile.style.backgroundOpacity.coerceIn(0f, 1f)
    val effectiveOpacity = if (flat) flatPanelOpacity(opacity) else opacity

    // Рамку рисуем тем же кодом, что и живая плитка (учитывает форму и стиль).
    // Наклон не подмешиваем: в миниатюре он не нужен и не должен дёргать композицию.
    val borderModifier = if (flat) {
        Modifier
    } else {
        tileBorderForShape(tile.style, palette, shape, tilt = 0f to 0f)
    }

    Box(modifier = modifier.size(widthDp.dp, heightDp.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(palette.surfaceVariant.copy(alpha = effectiveOpacity))
                // Текстура есть ВСЕГДА, как в TileCell: раньше в плоском стиле
                // миниатюра её теряла, и это было видно при переходе.
                .tileTexture(tile.style, palette)
                .then(borderModifier)
        ) {
            PreviewTileContent(
                tile = tile,
                tileSizeDp = spec.tileSizeDp,
                palette = palette,
                installedApps = installedApps,
                themedIcons = grid.themedIcons
            )
        }
    }
}

@Composable
private fun PreviewTileContent(
    tile: Tile,
    tileSizeDp: Float,
    palette: EtherealPalette,
    installedApps: List<AppInfo>,
    themedIcons: Boolean
) {
    val contentScale = tile.style.contentScale

    // Живая плитка центрирует контент внутри FrostedGlassBox — повторяем это,
    // иначе часы, погода и поиск в миниатюре прилипают к левому верхнему углу.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (tile.type) {
            TileType.APP_LAUNCHER -> {
                val config = remember(tile.config) { AppLauncherConfig.fromJson(tile.config) }
                TileAppIconContent(
                    tile = tile,
                    tileSize = tileSizeDp.dp,
                    palette = palette,
                    contentScale = contentScale,
                    installedApps = installedApps,
                    themedIcons = themedIcons,
                    foregroundOnlyIcon = config.showOnlyForegroundIcon && config.packageName != null
                )
            }

            TileType.APP -> {
                Text(
                    text = tile.payload?.take(1)?.uppercase() ?: "?",
                    color = palette.textPrimary,
                    fontSize = (tileSizeDp * 0.35f * contentScale).sp
                )
            }

            TileType.ACTION -> {
                Text(
                    text = tile.payload ?: "?",
                    color = palette.textPrimary,
                    fontSize = (tileSizeDp * 0.22f * contentScale).sp
                )
            }

            TileType.SPACE -> {
                val config = remember(tile.config) { SpaceConfig.fromJson(tile.config) }
                SpaceTileContent(
                    config = config,
                    tileSize = tileSizeDp.dp,
                    palette = palette,
                    contentScale = contentScale
                )
            }

            TileType.SETTINGS -> {
                Image(
                    painter = painterResource(id = R.drawable.settings_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(palette.textPrimary),
                    modifier = Modifier.size((tileSizeDp * 0.5f * contentScale).dp)
                )
            }

            TileType.WIDGET -> PreviewWidgetContent(
                tile = tile,
                tileSizeDp = tileSizeDp,
                palette = palette,
                contentScale = contentScale
            )

            TileType.EQUALIZER -> PreviewSpectrum(palette = palette)

            TileType.EMPTY -> Unit
        }
    }
}

/**
 * Виджеты в мире. Встроенные (часы, погода, поиск) — это те же живые
 * composables, что и на экране, поэтому миниатюра совпадает с оригиналом.
 *
 * Системный Android-виджет захостить дважды нельзя, поэтому берём картинку,
 * которую провайдер сам отдаёт для пикеров (previewImage) — это ровно то,
 * что человек видит в списке виджетов, а не пустой квадрат.
 */
@Composable
private fun PreviewWidgetContent(
    tile: Tile,
    tileSizeDp: Float,
    palette: EtherealPalette,
    contentScale: Float
) {
    val context = LocalContext.current

    when (tile.payload) {
        "ethereal_clock" -> ClockTileContent(tileSizeDp.dp, palette, contentScale)

        "ethereal_weather" ->
            WeatherTileContent(context, tile, tileSizeDp.dp, palette, contentScale)

        "ethereal_search" -> SearchTileContent(tileSizeDp.dp, palette, contentScale)

        "appwidget" -> {
            val widgetId = remember(tile.config) {
                EtherealAppWidgetHost.decodeWidgetId(tile.config)
            }
            if (widgetId == null) {
                WidgetPlaceholder(palette)
            } else {
                val preview by produceState<ImageBitmap?>(
                    initialValue = widgetPreviewCache[widgetId],
                    widgetId
                ) {
                    if (value == null) {
                        val loaded = withContext(Dispatchers.IO) {
                            runCatching {
                                // Тот же путь, что в пикере виджетов: провайдер сам
                                // отдаёт картинку предпросмотра.
                                AppWidgetManager.getInstance(context)
                                    .getAppWidgetInfo(widgetId)
                                    ?.loadPreviewImage(context, 0)
                                    ?.toImageBitmap()
                            }.getOrNull()
                        }
                        widgetPreviewCache[widgetId] = loaded
                        value = loaded
                    }
                }
                val bitmap = preview
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WidgetPlaceholder(palette)
                }
            }
        }

        else -> WidgetPlaceholder(palette)
    }
}

/** Нейтральный «виджет» одной линией, если превью от провайдера нет. */
@Composable
private fun WidgetPlaceholder(palette: EtherealPalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.surface.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        val tint = palette.textSecondary.copy(alpha = 0.5f)
        Canvas(modifier = Modifier.fillMaxSize(0.46f)) {
            val stroke = 1.5.dp.toPx()
            drawRoundRect(
                color = tint,
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = stroke)
            )
            drawLine(
                color = tint,
                start = Offset(0f, size.height * 0.42f),
                end = Offset(size.width, size.height * 0.42f),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
private fun PreviewSpectrum(palette: EtherealPalette) {
    val heights = remember { listOf(0.35f, 0.65f, 0.45f, 0.85f, 0.55f, 0.30f, 0.70f) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (heights.size * 2f + 1f)
        heights.forEachIndexed { index, value ->
            val left = barWidth * (1f + index * 2f)
            val barHeight = size.height * value
            drawRoundRect(
                color = palette.primary.copy(alpha = 0.75f),
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** Док пространства: тот же DockBar, что на живом экране, только без реакций. */
@Composable
private fun PreviewDock(
    packages: Set<String>,
    installedApps: List<AppInfo>,
    grid: SpaceGridParams,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val dockApps = remember(packages, installedApps) {
        installedApps.filter { packages.contains(it.packageName) }
    }
    if (dockApps.isEmpty()) return

    Box(modifier = modifier.navigationBarsPadding()) {
        DockBar(
            apps = dockApps,
            palette = palette,
            // Миниатюра не интерактивна: клики — пустышки.
            onAppClick = { _, _ -> },
            showPresets = false,
            panelBackground = grid.dockPanelBackground,
            iconBorderStyle = grid.dockIconBorderStyle,
            iconBorderWidth = grid.dockIconBorderWidth,
            iconBorderOpacity = grid.dockIconBorderOpacity,
            iconBackgroundOpacity = grid.dockIconBackgroundOpacity,
            modifier = Modifier.padding(bottom = grid.dockBottomPaddingDp.dp)
        )
    }
}

// ============================================================
// Нитки между пространствами
// ============================================================

@Composable
private fun Connections(
    spaces: List<SpacePreview>,
    originX: Float,
    originY: Float,
    frameWidthDp: Float,
    frameHeightDp: Float,
    palette: EtherealPalette
) {
    if (spaces.size < 2) return

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    spaces.forEach { space ->
        val x = space.canvasX - originX
        val y = space.canvasY - originY
        minX = min(minX, x)
        minY = min(minY, y)
        maxX = max(maxX, x + frameWidthDp)
        maxY = max(maxY, y + frameHeightDp)
    }

    val boundsW = (maxX - minX).coerceAtLeast(1f)
    val boundsH = (maxY - minY).coerceAtLeast(1f)

    // Нитки живые: по ним бежит импульс — как в прежнем обзоре.
    val pulse by rememberInfiniteTransition(label = "space-web").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "space-web-phase"
    )

    Canvas(
        modifier = Modifier
            .offset(x = minX.dp, y = minY.dp)
            .size(boundsW.dp, boundsH.dp)
    ) {
        val halfW = frameWidthDp / 2f * density
        val halfH = frameHeightDp / 2f * density
        // Мировые dp → локальные px этого канваса
        fun px(space: SpacePreview): Offset = Offset(
            (space.canvasX - originX - minX) * density,
            (space.canvasY - originY - minY) * density
        )

        val centers = spaces.associate { space ->
            val topLeft = px(space)
            space.id to Offset(topLeft.x + halfW, topLeft.y + halfH)
        }

        spaces.forEachIndexed { index, child ->
            val parentId = child.parentId ?: return@forEachIndexed
            val fromCenter = centers[parentId] ?: return@forEachIndexed
            val toCenter = centers[child.id] ?: return@forEachIndexed

            val dx = toCenter.x - fromCenter.x
            val dy = toCenter.y - fromCenter.y
            val length = hypot(dx, dy)
            if (length < 1f) return@forEachIndexed

            val ux = dx / length
            val uy = dy / length

            val tFrom = min(
                if (abs(ux) > 0.0001f) halfW / abs(ux) else Float.MAX_VALUE,
                if (abs(uy) > 0.0001f) halfH / abs(uy) else Float.MAX_VALUE
            )

            val from = Offset(fromCenter.x + ux * tFrom, fromCenter.y + uy * tFrom)
            val to = Offset(toCenter.x - ux * tFrom, toCenter.y - uy * tFrom)
            val span = hypot(to.x - from.x, to.y - from.y)
            if (span < 8f) return@forEachIndexed

            val sag = span * 0.12f
            val c1 = Offset(
                from.x + (to.x - from.x) / 3f,
                from.y + (to.y - from.y) / 3f + sag
            )
            val c2 = Offset(
                from.x + (to.x - from.x) * 2f / 3f,
                from.y + (to.y - from.y) * 2f / 3f + sag
            )
            val path = Path().apply {
                moveTo(from.x, from.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y)
            }

            drawPath(
                path = path,
                color = palette.primary.copy(alpha = 0.10f),
                style = Stroke(width = 6f * density, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = palette.primary.copy(alpha = 0.42f),
                style = Stroke(width = 1.4f * density, cap = StrokeCap.Round)
            )

            // Бегущий по нитке импульс, у каждой нитки своя фаза.
            val t = (pulse + index * 0.17f) % 1f
            val mt = 1f - t
            val px = mt * mt * mt * from.x + 3 * mt * mt * t * c1.x +
                3 * mt * t * t * c2.x + t * t * t * to.x
            val py = mt * mt * mt * from.y + 3 * mt * mt * t * c1.y +
                3 * mt * t * t * c2.y + t * t * t * to.y

            drawCircle(
                color = palette.primary.copy(alpha = 0.85f),
                radius = 2.6f * density,
                center = Offset(px, py)
            )
        }
    }
}

// ============================================================
// Оверлей: HUD и жесты (над домашним экраном)
// ============================================================

@Composable
fun SpacesOverlay(
    spaces: List<SpacePreview>,
    currentSpaceId: String,
    state: SpacesWorldState,
    palette: EtherealPalette,
    frameWidth: Dp,
    frameHeight: Dp,
    onOpenSpace: (String) -> Unit,
    onMoveSpace: (String, Float, Float) -> Unit,
    onCommitSpace: (String, Float, Float) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (spaces.isEmpty()) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val origin = spaces.firstOrNull { it.id == currentSpaceId }
    val originX = origin?.canvasX ?: 0f
    val originY = origin?.canvasY ?: 0f
    val originState by rememberUpdatedState(Offset(originX, originY))
    val spacesState by rememberUpdatedState(spaces)
    // Жест живёт в pointerInput(Unit) и держит лямбды с первого кадра,
    // поэтому зовём всегда актуальные — иначе «залипают» currentSpaceId и колбэки.
    val onOpenSpaceState by rememberUpdatedState(onOpenSpace)
    val onMoveSpaceState by rememberUpdatedState(onMoveSpace)
    val onCommitSpaceState by rememberUpdatedState(onCommitSpace)
    val onDismissState by rememberUpdatedState(onDismiss)

    var menuTarget by remember { mutableStateOf<SpacePreview?>(null) }

    // Открытие: камера летит в пространство. Раскладка цели уже загружена
    // (её показало превью), поэтому подмена на живой экран совпадает —
    // затемнение не нужно, и чёрного кадра не бывает.
    fun flyInto(space: SpacePreview) {
        scope.launch {
            state.open = false
            state.draggingId = null
            val dx = (space.canvasX - originState.x) * density.density
            val dy = (space.canvasY - originState.y) * density.density
            try {
                animateCameraTo(state, 1f, Offset(-dx, -dy), durationMillis = 420)
                onOpenSpaceState(space.id)
                // Один кадр с превью ещё на экране: за него успевает собраться
                // живой экран нового пространства, поэтому подмена не мигает.
                withFrameNanos { }
                state.reset()
            } finally {
                state.homeDragPx = Offset.Zero
            }
        }
    }

    // Закрытие анимирует владелец камеры (MainActivity): тот же трансформ
    // двигает и домашний экран, поэтому логика живёт рядом с ним.
    fun closeWorld() {
        onDismissState()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Перетаскивание/тап работают только когда камера уже «зафиксирована».
        if (state.open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val slop = 10f * density.density

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            fun hitTest(position: Offset): SpacePreview? {
                                val s = state.scale
                                if (s <= 0.001f) return null
                                val worldDp = Offset(
                                    (position.x - state.offset.x) / s / density.density,
                                    (position.y - state.offset.y) / s / density.density
                                )
                                val frameW = frameWidth.value
                                val frameH = frameHeight.value
                                return spacesState.lastOrNull { space ->
                                    val x = space.canvasX - originState.x
                                    val y = space.canvasY - originState.y
                                    worldDp.x >= x && worldDp.x <= x + frameW &&
                                        worldDp.y >= y && worldDp.y <= y + frameH
                                }
                            }

                            val hit = hitTest(down.position)
                            // Текущее пространство перетаскивается отдельным режимом:
                            // его сдвиг под пальцем живёт в state.homeDragPx, а позиция
                            // и камера фиксируются один раз на отпускании — иначе
                            // на каждое движение пересобиралась бы вся раскладка мира.
                            val isCurrentSpaceHit = hit != null && hit.id == currentSpaceId
                            val dragTarget = hit?.takeIf { it.id != currentSpaceId }
                            var frameStart = if (dragTarget != null) {
                                Offset(dragTarget.canvasX, dragTarget.canvasY)
                            } else Offset.Zero
                            // Последняя позиция под пальцем. Фиксируем её сами, а не
                            // из состояния: иначе на отпускании можно сохранить
                            // позицию на кадр назад (или старую, если рекомпозиция отстала).
                            var lastPosition = frameStart
                            var dragStartWorld = Offset.Zero
                            var dragging = false
                            var movingCurrent = false
                            var moved = false
                            var longPressed = false
                            var lastCentroid: Offset? = null
                            var lastSpan = 0f
                            val downTime = down.uptimeMillis

                            if (dragTarget != null) {
                                dragStartWorld = Offset(
                                    (down.position.x - state.offset.x) / state.scale / density.density,
                                    (down.position.y - state.offset.y) / state.scale / density.density
                                )
                            }

                            var finished = false
                            while (!finished) {
                                val canLongPress = dragTarget != null &&
                                    !dragging && !longPressed && !moved
                                val heldMs = android.os.SystemClock.uptimeMillis() - downTime
                                val remaining = viewConfiguration.longPressTimeoutMillis - heldMs

                                // Долгое удержание: если палец стоит на месте, события
                                // не приходят, поэтому ждём с таймаутом (как в TileCell).
                                val event: PointerEvent? = if (canLongPress && remaining > 0) {
                                    withTimeoutOrNull(remaining) { awaitPointerEvent() }
                                        ?: run {
                                            longPressed = true
                                            haptic.performHapticFeedback(
                                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                            )
                                            menuTarget = dragTarget
                                            null
                                        }
                                } else {
                                    awaitPointerEvent()
                                }

                                if (event == null) continue

                                val pressed = event.changes.filter { it.pressed }

                                if (pressed.size >= 2) {
                                    // Два пальца — камера: зум вокруг центроида + пан.
                                    val centroid = pressed
                                        .map { it.position }
                                        .reduce { a, b -> a + b } / pressed.size.toFloat()
                                    val span = (pressed[0].position - pressed[1].position).getDistance()

                                    val previous = lastCentroid
                                    if (previous != null && lastSpan > 1f) {
                                        state.applyTransform(
                                            zoom = (span / lastSpan).coerceIn(0.5f, 2f),
                                            focalPx = centroid,
                                            panPx = centroid - previous
                                        )
                                    }
                                    lastCentroid = centroid
                                    lastSpan = span
                                    dragging = false
                                    moved = true
                                } else if (pressed.size == 1) {
                                    val change = pressed[0]
                                    val delta = change.position - change.previousPosition
                                    val previous = lastCentroid
                                    val fromDown = change.position - down.position

                                    if (isCurrentSpaceHit && !longPressed && !movingCurrent &&
                                        fromDown.getDistance() > slop
                                    ) {
                                        movingCurrent = true
                                        state.draggingId = currentSpaceId
                                    }

                                    if (movingCurrent) {
                                        // Только визуальный сдвиг: позиция и камера
                                        // пересчитаются один раз на отпускании.
                                        state.homeDragPx = fromDown
                                        change.consume()
                                    } else if (dragTarget != null && !longPressed && !dragging &&
                                        fromDown.getDistance() > slop
                                    ) {
                                        dragging = true
                                        state.draggingId = dragTarget.id
                                    }

                                    if (dragging && dragTarget != null) {
                                        val worldNow = Offset(
                                            (change.position.x - state.offset.x) / state.scale / density.density,
                                            (change.position.y - state.offset.y) / state.scale / density.density
                                        )
                                        val worldDelta = worldNow - dragStartWorld
                                        lastPosition = Offset(
                                            frameStart.x + worldDelta.x,
                                            frameStart.y + worldDelta.y
                                        )
                                        onMoveSpaceState(
                                            dragTarget.id,
                                            lastPosition.x,
                                            lastPosition.y
                                        )
                                        change.consume()
                                    } else if (dragTarget == null && !isCurrentSpaceHit && previous != null) {
                                        state.moveBy(delta)
                                        change.consume()
                                    }

                                    if (delta.getDistance() > 2f) moved = true
                                    lastCentroid = change.position
                                    lastSpan = 0f
                                } else {
                                    lastCentroid = null
                                    lastSpan = 0f
                                }

                                if (event.changes.none { it.pressed }) finished = true
                            }

                            state.draggingId = null

                            if (movingCurrent && hit != null) {
                                val safeScale = state.scale.coerceAtLeast(0.05f)
                                val worldDx = state.homeDragPx.x / (safeScale * density.density)
                                val worldDy = state.homeDragPx.y / (safeScale * density.density)
                                val newX = hit.canvasX + worldDx
                                val newY = hit.canvasY + worldDy
                                // Позиция и камера применяются в одном кадре: экран
                                // остаётся под пальцем, соседи не прыгают.
                                onMoveSpaceState(hit.id, newX, newY)
                                state.setCamera(safeScale, state.offset + state.homeDragPx)
                                state.homeDragPx = Offset.Zero
                                onCommitSpaceState(hit.id, newX, newY)
                            } else if (dragging && dragTarget != null) {
                                onCommitSpaceState(dragTarget.id, lastPosition.x, lastPosition.y)
                            } else if (!moved && !longPressed) {
                                if (hit == null || hit.id == currentSpaceId) {
                                    closeWorld()
                                } else {
                                    haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                                    )
                                    flyInto(hit)
                                }
                            }
                        }
                    }
            )
        }

        // HUD: только когда камера уже зафиксирована. Во время жеста его нет —
        // иначе оверлей пересобирался бы на каждое движение пальцев.
        if (state.open) {
            Text(
                text = stringResource(R.string.world_hint),
                color = palette.textSecondary.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 26.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.45f))
                    .clickable { closeWorld() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Контекстное меню пространства
        menuTarget?.let { target ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { menuTarget = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surface)
                        .clickable(enabled = false, onClick = {})
                        .padding(20.dp)
                ) {
                    Text(
                        text = target.name,
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                menuTarget = null
                                flyInto(target)
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.overview_menu_open),
                            color = palette.textPrimary,
                            fontSize = 14.sp
                        )
                    }

                    if (!target.isRoot) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.primary)
                                .clickable {
                                    menuTarget = null
                                    onDeleteSpace(target.id)
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.overview_menu_delete),
                                color = palette.background,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { menuTarget = null }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
