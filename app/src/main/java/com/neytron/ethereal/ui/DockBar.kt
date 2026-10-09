package com.neytron.ethereal.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.data.SettingsRepository
import com.neytron.ethereal.ui.theme.FrostedGlassBox
import com.neytron.ethereal.tiles.TilePreset
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.IconShape
import com.neytron.ethereal.ui.theme.LocalAppearance
import com.neytron.ethereal.ui.theme.LocalDockConfig
import com.neytron.ethereal.ui.theme.iconShapeFor
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.toImageBitmap
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DockBar(
    apps: List<AppInfo>,
    palette: EtherealPalette,
    onAppClick: (AppInfo) -> Unit,
    showPresets: Boolean = false,
    presets: List<TilePreset> = emptyList(),
    onPresetClick: (TilePreset) -> Unit = {},
    onSystemWidgetClick: () -> Unit = {},
    onCreateSpaceClick: () -> Unit = {},
    introFlyIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val config = LocalDockConfig.current
    val haptic = LocalHapticFeedback.current

    val effectiveOpacity = config.opacityOverride ?: appearance.opacity
    val effectiveBlur = config.blurOverride ?: appearance.blur
    val iconSize = config.iconSizeDp.dp
    val itemCount = if (showPresets) presets.size else apps.size
    val spacing = if (itemCount >= 5) 6.dp else 8.dp
    val horizontalPadding = if (itemCount >= 5) 12.dp else 16.dp

    val packageKey = remember(apps) { apps.map { it.packageName }.sorted().joinToString("|") }
    var order by remember(apps) { mutableStateOf(apps) }

    LaunchedEffect(packageKey) {
        val savedOrder = SettingsRepository.getDockOrder(context)
        val sorted = apps.sortedBy { app ->
            val idx = savedOrder.indexOf(app.packageName)
            if (idx == -1) Int.MAX_VALUE else idx
        }
        if (sorted.isNotEmpty()) {
            order = sorted
        }
    }

    var previousShowPresets by remember { mutableStateOf(showPresets) }
    LaunchedEffect(showPresets) {
        if (showPresets != previousShowPresets) {
            val count = minOf(if (showPresets) presets.size else order.size, 6)
            repeat(count) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(50)
            }
            previousShowPresets = showPresets
        }
    }

    if (showPresets && presets.isEmpty()) return
    if (!showPresets && order.isEmpty()) return

    FrostedGlassBox(
        blur = effectiveBlur,
        opacity = effectiveOpacity,
        backgroundColor = palette.background,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = showPresets,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                    } else {
                        (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                    }
                },
                contentAlignment = Alignment.Center,
                label = "dock_content"
            ) { showingPresets ->
                if (showingPresets) {
                    PresetsRow(
                        presets = presets,
                        palette = palette,
                        onPresetClick = onPresetClick,
                        onSystemWidgetClick = onSystemWidgetClick,
                        onCreateSpaceClick = onCreateSpaceClick
                    )
                } else {
                    AppsRow(
                        order = order,
                        palette = palette,
                        iconSize = iconSize,
                        spacing = spacing,
                        config = config,
                        introFlyIn = introFlyIn,
                        onAppClick = onAppClick,
                        onOrderChange = { newOrder -> order = newOrder },
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsRow(
    order: List<AppInfo>,
    palette: EtherealPalette,
    iconSize: Dp,
    spacing: Dp,
    config: com.neytron.ethereal.ui.theme.DockConfig,
    introFlyIn: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onOrderChange: (List<AppInfo>) -> Unit,
    context: android.content.Context
) {
    val scrollState = rememberScrollState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val stepPx = with(density) { (iconSize + spacing).toPx() }

    val targetIndex: Int? = if (draggingIndex == null || order.isEmpty()) null else {
        val from = draggingIndex!!
        val shift = (dragOffsetX / stepPx).roundToInt().coerceIn(-order.size, order.size)
        (from + shift).coerceIn(0, order.size - 1)
    }

    val rowModifier = if (config.scrollable) {
        Modifier.horizontalScroll(scrollState, enabled = draggingIndex == null)
    } else {
        Modifier
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        order.forEachIndexed { index, app ->
            key(app.packageName) {
                DockItem(
                    app = app,
                    index = index,
                    palette = palette,
                    iconSize = iconSize,
                    spacing = spacing,
                    iconShape = config.iconShape,
                    draggingIndex = draggingIndex,
                    themedIcons = config.themedIcons,
                    showBackground = config.showIconBackground,
                    targetIndex = targetIndex,
                    dragOffsetX = dragOffsetX,
                    introFlyIn = introFlyIn,
                    onClick = { onAppClick(app) },
                    onDragStart = { i ->
                        draggingIndex = i
                        dragOffsetX = 0f
                    },
                    onDrag = { amount -> dragOffsetX += amount },
                    onDragEnd = { finalOffset ->
                        val from = draggingIndex
                        if (from != null && order.isNotEmpty()) {
                            val shift = (finalOffset / stepPx).roundToInt().coerceIn(-order.size, order.size)
                            val to = (from + shift).coerceIn(0, order.size - 1)
                            if (to != from && from in order.indices && to in order.indices) {
                                val newOrder = order.toMutableList().apply {
                                    val item = removeAt(from)
                                    add(to, item)
                                }
                                onOrderChange(newOrder)
                                SettingsRepository.setDockOrder(context, newOrder.map { it.packageName })
                            }
                        }
                        draggingIndex = null
                        dragOffsetX = 0f
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetsRow(
    presets: List<TilePreset>,
    palette: EtherealPalette,
    onPresetClick: (TilePreset) -> Unit,
    onSystemWidgetClick: () -> Unit,
    onCreateSpaceClick: () -> Unit
) {
    // Каскадная анимация появления каждого элемента
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        revealed = false
        delay(30)
        revealed = true
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Элемент 0: System widget
            CascadeItem(revealed, 0) {
                SystemWidgetChip(
                    palette = palette,
                    onClick = onSystemWidgetClick
                )
            }

            // Элемент 1: Space
            CascadeItem(revealed, 1) {
                SpaceChip(
                    palette = palette,
                    onClick = onCreateSpaceClick
                )
            }

            // Элементы 2..N: Пресеты
            presets.forEachIndexed { index, preset ->
                CascadeItem(revealed, index + 2) {
                    PresetChip(
                        preset = preset,
                        palette = palette,
                        onClick = { onPresetClick(preset) }
                    )
                }
            }
        }
    }
}

/**
 * Обёртка для каскадного появления элемента.
 * Появляется с задержкой index * 50мс, плавно выезжая слева и проявляясь.
 */
@Composable
private fun CascadeItem(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    var itemVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 50L)
            itemVisible = true
        } else {
            itemVisible = false
        }
    }

    AnimatedVisibility(
        visible = itemVisible,
        enter = fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { -it / 2 }
                ),
        exit = fadeOut(animationSpec = tween(120))
    ) {
        content()
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

@Composable
private fun DockItem(
    app: AppInfo,
    index: Int,
    palette: EtherealPalette,
    iconSize: Dp,
    spacing: Dp,
    iconShape: IconShape,
    showBackground: Boolean,
    themedIcons: Boolean,
    draggingIndex: Int?,
    targetIndex: Int?,
    dragOffsetX: Float,
    introFlyIn: Boolean = false,
    onClick: () -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit
) {
    val isDragging = draggingIndex == index

    var hasAppeared by remember(introFlyIn) { mutableStateOf(!introFlyIn) }
    LaunchedEffect(introFlyIn) {
        if (introFlyIn) {
            hasAppeared = false
            delay(250L + index * 180L)
            hasAppeared = true
        }
    }

    val flyProgress by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flyProgress"
    )

    val currentScale = (if (isDragging) 1.25f else 1f) * (0.25f + 0.75f * flyProgress)
    val currentAlpha = flyProgress.coerceIn(0f, 1f)
    val currentOffsetY = (70f * (1f - flyProgress)).roundToInt()

    val shift by animateDpAsState(
        targetValue = when {
            draggingIndex == null || isDragging -> 0.dp
            draggingIndex < index && index <= (targetIndex ?: index) -> -(iconSize + spacing)
            draggingIndex > index && index >= (targetIndex ?: index) -> (iconSize + spacing)
            else -> 0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shift"
    )

    val density = LocalDensity.current

    val offsetX = if (isDragging) dragOffsetX else with(density) { shift.toPx() }

    val bmp = remember(app.packageName, themedIcons, palette.id) {
        if (themedIcons) {
            renderThemedIcon(app.icon, palette).asImageBitmap()
        } else {
            app.icon.toImageBitmap()
        }
    }

    val currentIndex by rememberUpdatedState(index)
    val currentDraggingIndex by rememberUpdatedState(draggingIndex)
    val currentDragOffsetX by rememberUpdatedState(dragOffsetX)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val borderPadding = 4.dp
    val actualIconSize = iconSize - borderPadding * 2

    val shape = iconShapeFor(iconShape, iconSize.value)
    val iconShapeComp = iconShapeFor(iconShape, actualIconSize.value)

    Box(
        modifier = Modifier
            .zIndex(if (isDragging) 10f else 0f)
            .offset { IntOffset(x = offsetX.roundToInt(), y = currentOffsetY) }
            .graphicsLayer(alpha = currentAlpha)
            .requiredSize(iconSize)
            .scale(currentScale)
            .pointerInput(app.packageName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart(currentIndex) },
                    onDrag = { change, amount ->
                        change.consume()
                        currentOnDrag(amount.x)
                    },
                    onDragEnd = { currentOnDragEnd(currentDragOffsetX) },
                    onDragCancel = { currentOnDragEnd(currentDragOffsetX) }
                )
            }
            .clickable(enabled = currentDraggingIndex == null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (showBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(palette.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = palette.outline.copy(alpha = 0.6f),
                        shape = shape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(actualIconSize)
                .clip(iconShapeComp),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(iconShapeComp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.label.take(1).uppercase(), color = palette.textPrimary)
                }
            }
        }
    }
}