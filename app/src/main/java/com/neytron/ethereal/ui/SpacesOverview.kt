package com.neytron.ethereal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TileType
import com.neytron.ethereal.R
import com.neytron.ethereal.ui.theme.EtherealPalette
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

/** Карточка пространства в обзоре. */
data class SpacePreview(
    val id: String,
    val name: String,
    val iconText: String?,
    val tiles: List<Tile>,
    val canvasX: Float,
    val canvasY: Float,
    val isRoot: Boolean = false,
    /** Родительское пространство: в его раскладке лежит плитка-портал в это. */
    val parentId: String? = null
)

private const val CARD_W = 230f
private const val CARD_H = 480f
private const val MIN_SCALE = 0.20f
private const val MAX_SCALE = 1.45f

/**
 * Обзор пространств на «бесконечном канвасе».
 *
 * Открывается жестом «отдаление» с домашнего экрана. Карточки живут в собственных
 * координатах (canvasX/canvasY), камера умеет пан и зум, при отпускании жеста
 * подсказка показывает пространство, ближайшее к центру экрана.
 */
@Composable
fun SpacesOverview(
    visible: Boolean,
    spaces: List<SpacePreview>,
    currentSpaceId: String,
    palette: EtherealPalette,
    onOpenSpace: (String) -> Unit,
    onMoveSpace: (String, Float, Float) -> Unit = { _, _, _ -> },
    onCommitSpace: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDeleteOrphans: (List<String>) -> Unit = {},
    onDeleteSpace: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    if (spaces.isEmpty()) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var scale by remember { mutableStateOf(0.95f) }
    var camX by remember { mutableStateOf(0f) }
    var camY by remember { mutableStateOf(0f) }
    var centered by remember { mutableStateOf(false) }
    // Какая карточка тащится прямо сейчас — она держит фокус, камера не панорамируется
    var draggedId by remember { mutableStateOf<String?>(null) }
    var orphanDialogVisible by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<SpacePreview?>(null) }
    val latestSpaces by rememberUpdatedState(spaces)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background.copy(alpha = 0.97f))
                .pointerInput(Unit) {
                    // Один палец по пустому месту — пан камеры; по карточке — перетаскивание.
                    // Два пальца — пан и зум в любом месте.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = (down.position.x / density.density - camX) /
                            if (scale <= 0.01f) 1f else scale
                        val startY = (down.position.y / density.density - camY) /
                            if (scale <= 0.01f) 1f else scale
                        val startedOnCard = latestSpaces.any { sp ->
                            startX >= sp.canvasX && startX <= sp.canvasX + CARD_W &&
                                startY >= sp.canvasY && startY <= sp.canvasY + CARD_H
                        }

                        var lastCentroid: Offset? = null
                        var lastSpan = 0f

                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }

                            when {
                                pressed.size >= 2 -> {
                                    val centroid = pressed
                                        .map { it.position }
                                        .reduce { a, b -> a + b } / pressed.size.toFloat()
                                    val span = (pressed[0].position - pressed[1].position).getDistance()

                                    val previous = lastCentroid
                                    if (previous != null && lastSpan > 1f) {
                                        val cx = centroid.x / density.density
                                        val cy = centroid.y / density.density
                                        val pdx = (centroid.x - previous.x) / density.density
                                        val pdy = (centroid.y - previous.y) / density.density

                                        val newScale = (scale * (span / lastSpan))
                                            .coerceIn(MIN_SCALE, MAX_SCALE)
                                        val k = if (scale <= 0f) 1f else newScale / scale

                                        camX = cx - (cx - camX) * k + pdx
                                        camY = cy - (cy - camY) * k + pdy
                                        scale = newScale
                                    }

                                    lastCentroid = centroid
                                    lastSpan = span
                                }

                                pressed.size == 1 && !startedOnCard -> {
                                    val position = pressed[0].position
                                    val previous = lastCentroid
                                    if (previous != null) {
                                        camX += (position.x - previous.x) / density.density
                                        camY += (position.y - previous.y) / density.density
                                    }
                                    lastCentroid = position
                                    lastSpan = 0f
                                }

                                else -> {
                                    lastCentroid = null
                                    lastSpan = 0f
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val screenW = maxWidth.value
            val screenH = maxHeight.value

            // Камеру ставим на текущее пространство при первом открытии
            LaunchedEffect(visible, currentSpaceId, spaces.size) {
                if (visible && !centered) {
                    val target = spaces.firstOrNull { it.id == currentSpaceId } ?: spaces.first()
                    camX = screenW / 2f - (target.canvasX + CARD_W / 2f) * scale
                    camY = screenH / 2f - (target.canvasY + CARD_H / 2f) * scale
                    centered = true
                }
            }

            // Ближайшее к центру экрана пространство
            val focused = remember(spaces, camX, camY, scale, screenW, screenH, draggedId) {
                if (draggedId != null) {
                    spaces.firstOrNull { it.id == draggedId }
                } else spaces.minByOrNull { sp ->
                    val sx = camX + (sp.canvasX + CARD_W / 2f) * scale
                    val sy = camY + (sp.canvasY + CARD_H / 2f) * scale
                    val dx = sx - screenW / 2f
                    val dy = sy - screenH / 2f
                    dx * dx + dy * dy
                }
            }

            // Канвас
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = camX.dp.toPx()
                        translationY = camY.dp.toPx()
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                // «Нитки»: только прямые связи родитель → ребёнок (вассал вассала — не вассал)
                ConnectionsLayer(
                    spaces = spaces,
                    palette = palette,
                    modifier = Modifier.fillMaxSize()
                )

                spaces.forEach { preview ->
                    key(preview.id) {
                        SpaceCard(
                            preview = preview,
                            palette = palette,
                            focused = focused?.id == preview.id,
                            isCurrent = preview.id == currentSpaceId,
                            modifier = Modifier.size(width = CARD_W.dp, height = CARD_H.dp),
                            onMove = { x, y -> onMoveSpace(preview.id, x, y) },
                            onCommit = { x, y -> onCommitSpace(preview.id, x, y) },
                            onDraggingChange = { isDragging ->
                                draggedId = if (isDragging) preview.id else null
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuTarget = preview
                            },
                            clickEnabled = menuTarget == null,
                            onClick = {
                                // Страховка: отпускание после долгого удержания не открывает пространство
                                if (menuTarget == null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenSpace(preview.id)
                                }
                            }
                        )
                    }
                }
            }

            // Подсказка + кнопка открыть ближайшее
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.overview_hint),
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(palette.primary, palette.primary.copy(alpha = 0.82f))
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            focused?.let { onOpenSpace(it.id) }
                        }
                        .padding(horizontal = 22.dp, vertical = 13.dp)
                ) {
                    Text(
                        text = stringResource(R.string.overview_open_space, focused?.name ?: "Home"),
                        color = palette.background,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Закрыть
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Пространства без родителя: предлагаем убрать мусор, но показываем список
            val orphans = spaces.filter { !it.isRoot && it.parentId == null }
            if (orphans.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            orphanDialogVisible = true
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "${orphans.size} · ${stringResource(R.string.overview_orphans_badge)}",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (orphanDialogVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { orphanDialogVisible = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.surface)
                            .clickable(enabled = false, onClick = {})
                            .padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.overview_orphans_title),
                            color = palette.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.overview_orphans_body, orphans.size),
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        orphans.take(8).forEach { orphan ->
                            Text(
                                text = "• ${orphan.name}",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        if (orphans.size > 8) {
                            Text(
                                text = "+${orphans.size - 8}",
                                color = palette.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { orphanDialogVisible = false }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    color = palette.textPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.primary)
                                    .clickable {
                                        val ids = orphans.map { it.id }
                                        orphanDialogVisible = false
                                        onDeleteOrphans(ids)
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.overview_orphans_delete),
                                    color = palette.background,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Контекстное меню карточки (долгое удержание)
            menuTarget?.let { target ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { menuTarget = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
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
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    menuTarget = null
                                    onOpenSpace(target.id)
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
}

@Composable
private fun SpaceCard(
    preview: SpacePreview,
    palette: EtherealPalette,
    focused: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onMove: (Float, Float) -> Unit,
    onCommit: (Float, Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onLongClick: () -> Unit,
    clickEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val density = LocalDensity.current

    // Точка отсчёта для текущего перетаскивания: держим сами, чтобы карточка
    // шла ровно под пальцем и не «убегала» из-за пересчёта состояния.
    // ВАЖНО: pointerInput держит лямбду с первого кадра, поэтому читаем позицию
    // через rememberUpdatedState — иначе на старте жеста берётся устаревшая координата.
    val latestPreview by rememberUpdatedState(preview)
    var dragAnchor by remember(preview.id) { mutableStateOf(Offset(preview.canvasX, preview.canvasY)) }

    Column(
        modifier = modifier
            .offset(x = preview.canvasX.dp, y = preview.canvasY.dp)
            .pointerInput(preview.id) {
                // Долгое удержание — контекстное меню карточки (открыть / удалить)
                detectDragGesturesAfterLongPress(
                    onDragStart = { onLongClick() },
                    onDrag = { change, _ -> change.consume() },
                    onDragEnd = { },
                    onDragCancel = { }
                )
            }
            .pointerInput(preview.id) {
                detectDragGestures(
                    onDragStart = {
                        dragAnchor = Offset(latestPreview.canvasX, latestPreview.canvasY)
                        onDraggingChange(true)
                    },
                    onDragEnd = {
                        onCommit(dragAnchor.x, dragAnchor.y)
                        onDraggingChange(false)
                    },
                    onDragCancel = {
                        onCommit(dragAnchor.x, dragAnchor.y)
                        onDraggingChange(false)
                    }
                ) { change, delta ->
                    // delta приходит уже в координатах канваса (слой отмасштабирован),
                    // поэтому переводим только px → dp. Делить ещё и на scale нельзя.
                    val next = Offset(
                        dragAnchor.x + delta.x / density.density,
                        dragAnchor.y + delta.y / density.density
                    )
                    dragAnchor = next
                    onMove(next.x, next.y)
                    change.consume()
                }
            }
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.94f))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) palette.primary else Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            // Пока открыто меню, карточка не должна ловить отпускание пальца —
            // иначе вместо нажатия на кнопку меню открывается само пространство.
            .clickable(enabled = clickEnabled, onClick = onClick)
            .padding(10.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MiniGrid(tiles = preview.tiles, palette = palette, modifier = Modifier.fillMaxSize())
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.overview_now),
                        color = palette.background,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!preview.iconText.isNullOrBlank()) {
                Text(
                    text = preview.iconText,
                    fontSize = 16.sp,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = preview.name,
                color = if (focused) palette.primary else palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Силуэт раскладки: плитки как маленькие прямоугольники в своих ячейках. */
@Composable
private fun MiniGrid(
    tiles: List<Tile>,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.30f))
            .padding(6.dp)
    ) {
        val cols = (tiles.maxOfOrNull { it.x + it.w } ?: 1).coerceAtLeast(1)
        val rows = (tiles.maxOfOrNull { it.y + it.h } ?: 1).coerceAtLeast(1)
        val areaW = maxWidth.value
        val areaH = maxHeight.value

        // Ячейки квадратные (как в реальной сетке), а сам грид центрируем в карточке
        val cell = minOf(areaW / cols, areaH / rows)
        val gridW = cell * cols
        val gridH = cell * rows
        val offsetX = (areaW - gridW) / 2f
        val offsetY = (areaH - gridH) / 2f

        tiles.forEach { tile ->
            val isSpaceTile = tile.type == TileType.SPACE
            Box(
                modifier = Modifier
                    .offset(
                        x = (offsetX + tile.x * cell).dp,
                        y = (offsetY + tile.y * cell).dp
                    )
                    .size(
                        width = (tile.w * cell).dp,
                        height = (tile.h * cell).dp
                    )
                    .padding(1.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .alpha(if (isSpaceTile) 0.9f else 1f)
                    .background(
                        if (isSpaceTile) palette.primary.copy(alpha = 0.40f)
                        else palette.textSecondary.copy(alpha = 0.26f)
                    )
            )
        }
    }
}
/**
 * «Нитки» между пространствами: только прямые связи родитель → ребёнок.
 *
 * Линия выходит из края карточки-родителя и входит в край карточки-ребёнка
 * (а не из центра), изгиб у каждой связи свой — так «паутина» не сливается
 * в параллельные дуги и меньше пересекается. По ниткам бежит мягкий импульс.
 * Линии между «дедом» и «внуком» не появляются: вассал моего вассала не мой вассал.
 */
@Composable
private fun ConnectionsLayer(
    spaces: List<SpacePreview>,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val pulse by rememberInfiniteTransition(label = "web-pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "web-pulse-value"
    )

    Canvas(modifier = modifier) {
        val halfW = CARD_W / 2f * density.density
        val halfH = CARD_H / 2f * density.density

        val centers = spaces.associate { sp ->
            sp.id to Offset(
                (sp.canvasX + CARD_W / 2f) * density.density,
                (sp.canvasY + CARD_H / 2f) * density.density
            )
        }

        spaces.forEachIndexed { index, child ->
            val parentId = child.parentId ?: return@forEachIndexed
            val fromCenter = centers[parentId] ?: return@forEachIndexed
            val toCenter = centers[child.id] ?: return@forEachIndexed

            val dx = toCenter.x - fromCenter.x
            val dy = toCenter.y - fromCenter.y
            val length = kotlin.math.hypot(dx, dy)
            if (length < 1f) return@forEachIndexed

            val ux = dx / length
            val uy = dy / length

            // Точки на границах карточек: линия не прячется под ними
            val tFrom = kotlin.math.min(
                if (kotlin.math.abs(ux) > 0.0001f) halfW / kotlin.math.abs(ux) else Float.MAX_VALUE,
                if (kotlin.math.abs(uy) > 0.0001f) halfH / kotlin.math.abs(uy) else Float.MAX_VALUE
            )
            val tTo = kotlin.math.min(
                if (kotlin.math.abs(ux) > 0.0001f) halfW / kotlin.math.abs(ux) else Float.MAX_VALUE,
                if (kotlin.math.abs(uy) > 0.0001f) halfH / kotlin.math.abs(uy) else Float.MAX_VALUE
            )

            val from = Offset(fromCenter.x + ux * tFrom, fromCenter.y + uy * tFrom)
            val to = Offset(toCenter.x - ux * tTo, toCenter.y - uy * tTo)

            val span = kotlin.math.hypot(to.x - from.x, to.y - from.y)
            if (span < 8f) return@forEachIndexed

            // Мягкая «провисшая» дуга: обе контрольные точки ниже прямой.
            // Единый характер изгиба у всех связей — паутина выглядит спокойнее
            // и линии реже пересекаются, чем при случайном изгибе.
            val sag = span * 0.14f
            val nx = 0f
            val ny = sag

            val c1 = Offset(from.x + (to.x - from.x) / 3f + nx, from.y + (to.y - from.y) / 3f + ny)
            val c2 = Offset(from.x + (to.x - from.x) * 2f / 3f + nx, from.y + (to.y - from.y) * 2f / 3f + ny)

            val path = Path().apply {
                moveTo(from.x, from.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y)
            }

            val fade = (0.85f - span / 4200f).coerceIn(0.35f, 0.85f)

            // мягкое свечение под линией + чёткая линия сверху
            drawPath(
                path = path,
                color = palette.primary.copy(alpha = 0.12f * fade / 0.85f),
                style = Stroke(width = 7f * density.density, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = palette.primary.copy(alpha = 0.62f * fade / 0.85f),
                style = Stroke(width = 1.7f * density.density, cap = StrokeCap.Round)
            )

            // Бегущий импульс: у каждой нитки своя фаза, чтобы не мигали синхронно
            val phase = (pulse + (index * 0.17f)) % 1f
            val t = phase
            val mt = 1f - t
            val px = mt * mt * mt * from.x + 3 * mt * mt * t * c1.x + 3 * mt * t * t * c2.x + t * t * t * to.x
            val py = mt * mt * mt * from.y + 3 * mt * mt * t * c1.y + 3 * mt * t * t * c2.y + t * t * t * to.y

            drawCircle(
                color = palette.primary.copy(alpha = 0.9f * fade / 0.85f),
                radius = 2.6f * density.density,
                center = Offset(px, py)
            )
        }
    }
}