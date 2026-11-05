package com.neytron.ethereal.tiles

import android.graphics.Rect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.editor.TileEditorState
import com.neytron.ethereal.ui.theme.EtherealPalette

@Composable
fun TileGrid(
    tiles: List<Tile>,
    columns: Int,
    rows: Int,
    tileSize: Dp,
    spacing: Dp,
    radius: Dp,
    palette: EtherealPalette,
    editorState: TileEditorState,
    installedApps: List<AppInfo>,
    themedIcons: Boolean,
    showResizeIndicator: Boolean = false,
    introReveal: Boolean = false,
    isIntroActive: Boolean = false,
    isTransitioning: Boolean = false,
    pivotFractionX: Float = 0.5f,
    pivotFractionY: Float = 0.5f,
    oldTiles: List<Tile> = emptyList(),
    incomingTiles: List<Tile> = emptyList(),
    isPop: Boolean = false,
    transitionProgress: Float = 1f,
    onTileClick: (Tile, Rect?) -> Unit = { _, _ -> },
    onTileLongClick: (Tile) -> Unit = {},
    onDragStart: (Tile) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onResizeStart: (Tile) -> Unit = {},
    onResize: (Tile, Offset) -> Unit = { _, _ -> },
    onResizeEnd: () -> Unit = {},
    onDeleteTile: (Tile) -> Unit = {},
    onEmptyTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEditing = editorState.editing.value
    val resizePreviewTiles = editorState.resizePreviewTiles.value
    val displayedTiles = (resizePreviewTiles ?: tiles).filter { it.y < rows }
    val displayIncomingTiles = if (isTransitioning) incomingTiles.filter { it.y < rows } else displayedTiles
    val cellSize = tileSize + spacing
    val totalWidth = cellSize * columns - spacing
    val totalHeight = cellSize * rows - spacing

    val dragging = editorState.draggingTile.value
    val occupied: Array<BooleanArray> = remember(displayedTiles, columns, rows, dragging) {
        Array(rows) { BooleanArray(columns) { false } }.also { mask ->
            displayedTiles.forEach { tile ->
                if (dragging == null || tile.id != dragging.id) {
                    for (dy in 0 until tile.h) {
                        for (dx in 0 until tile.w) {
                            val y = tile.y + dy
                            val x = tile.x + dx
                            if (y in 0 until rows && x in 0 until columns) {
                                mask[y][x] = true
                            }
                        }
                    }
                }
            }
        }
    }

    val transformOrigin = TransformOrigin(pivotFractionX.coerceIn(0f, 1f), pivotFractionY.coerceIn(0f, 1f))

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(totalWidth)
                .height(totalHeight)
        ) {
            // Пустые клетки
            for (row in 0 until rows) {
                for (col in 0 until columns) {
                    if (!occupied[row][col]) {
                        EmptyCell(
                            visible = isEditing && !isTransitioning,
                            tileSize = tileSize,
                            radius = radius,
                            palette = palette,
                            onClick = onEmptyTap,
                            modifier = Modifier.offset(
                                x = cellSize * col,
                                y = cellSize * row
                            )
                        )
                    }
                }
            }

            // Тень (footprint) целевой позиции при перетаскивании
            if (isEditing && dragging != null && editorState.targetCell.value != null) {
                val target = editorState.targetCell.value!!
                val targetX = cellSize * target.first
                val targetY = cellSize * target.second
                val targetW = cellSize * dragging.w - spacing
                val targetH = cellSize * dragging.h - spacing

                Box(
                    modifier = Modifier
                        .offset(x = targetX, y = targetY)
                        .size(width = targetW, height = targetH)
                        .clip(RoundedCornerShape(radius))
                        .background(palette.primary.copy(alpha = 0.22f))
                        .border(1.5.dp, palette.primary, RoundedCornerShape(radius))
                )
            }

            if (isTransitioning) {
                if (isPop) {
                    // POP: Старые тайлы втягиваются в pivot
                    val oldScale = 1f - transitionProgress * 0.8f
                    val oldAlpha = (1f - transitionProgress).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(10f)
                            .graphicsLayer(
                                scaleX = oldScale,
                                scaleY = oldScale,
                                alpha = oldAlpha,
                                transformOrigin = transformOrigin
                            )
                    ) {
                        oldTiles.filter { it.y < rows }.forEach { tile ->
                            key("pop_old_${tile.id}") {
                                TileCell(
                                    tile = tile,
                                    tileSize = tileSize,
                                    spacing = spacing,
                                    radius = radius,
                                    palette = palette,
                                    editorState = editorState,
                                    installedApps = installedApps,
                                    themedIcons = themedIcons,
                                    showResizeIndicator = showResizeIndicator,
                                    onClick = { },
                                    onLongClick = {},
                                    onDragStart = {},
                                    onDrag = {},
                                    onDragEnd = {},
                                    modifier = Modifier.offset(
                                        x = cellSize * tile.x,
                                        y = cellSize * tile.y
                                    )
                                )
                            }
                        }
                    }

                    // POP: Новые тайлы расширяются из pivot
                    val newScale = 1.4f - transitionProgress * 0.4f
                    val newAlpha = transitionProgress.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(5f)
                            .graphicsLayer(
                                scaleX = newScale,
                                scaleY = newScale,
                                alpha = newAlpha,
                                transformOrigin = transformOrigin
                            )
                    ) {
                        displayIncomingTiles.forEach { tile ->
                            key("pop_new_${tile.id}") {
                                TileCell(
                                    tile = tile,
                                    tileSize = tileSize,
                                    spacing = spacing,
                                    radius = radius,
                                    palette = palette,
                                    editorState = editorState,
                                    installedApps = installedApps,
                                    themedIcons = themedIcons,
                                    showResizeIndicator = showResizeIndicator,
                                    onClick = { },
                                    onLongClick = {},
                                    onDragStart = {},
                                    onDrag = {},
                                    onDragEnd = {},
                                    modifier = Modifier.offset(
                                        x = cellSize * tile.x,
                                        y = cellSize * tile.y
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // PUSH: Старые тайлы разлетаются от pivot
                    val oldScale = 1f + transitionProgress * 1.2f
                    val oldAlpha = (1f - transitionProgress * 1.2f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(10f)
                            .graphicsLayer(
                                scaleX = oldScale,
                                scaleY = oldScale,
                                alpha = oldAlpha,
                                transformOrigin = transformOrigin
                            )
                    ) {
                        oldTiles.filter { it.y < rows }.forEach { tile ->
                            key("push_old_${tile.id}") {
                                TileCell(
                                    tile = tile,
                                    tileSize = tileSize,
                                    spacing = spacing,
                                    radius = radius,
                                    palette = palette,
                                    editorState = editorState,
                                    installedApps = installedApps,
                                    themedIcons = themedIcons,
                                    showResizeIndicator = showResizeIndicator,
                                    onClick = { },
                                    onLongClick = {},
                                    onDragStart = {},
                                    onDrag = {},
                                    onDragEnd = {},
                                    modifier = Modifier.offset(
                                        x = cellSize * tile.x,
                                        y = cellSize * tile.y
                                    )
                                )
                            }
                        }
                    }

                    // PUSH: Новые тайлы вырастают из pivot
                    val newScale = 0.2f + transitionProgress * 0.8f
                    val newAlpha = transitionProgress.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(5f)
                            .graphicsLayer(
                                scaleX = newScale,
                                scaleY = newScale,
                                alpha = newAlpha,
                                transformOrigin = transformOrigin
                            )
                    ) {
                        displayIncomingTiles.forEach { tile ->
                            key("push_new_${tile.id}") {
                                TileCell(
                                    tile = tile,
                                    tileSize = tileSize,
                                    spacing = spacing,
                                    radius = radius,
                                    palette = palette,
                                    editorState = editorState,
                                    installedApps = installedApps,
                                    themedIcons = themedIcons,
                                    showResizeIndicator = showResizeIndicator,
                                    onClick = { },
                                    onLongClick = {},
                                    onDragStart = {},
                                    onDrag = {},
                                    onDragEnd = {},
                                    modifier = Modifier.offset(
                                        x = cellSize * tile.x,
                                        y = cellSize * tile.y
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Тайлы в обычном режиме
                val displacedTileIds = editorState.displacedTileIds.value
                displayedTiles.forEach { tile ->
                    val originalTile = tiles.firstOrNull { it.id == tile.id }
                    val movedByResize = resizePreviewTiles != null &&
                        originalTile != null &&
                        (originalTile.x != tile.x || originalTile.y != tile.y)
                    val isDisplacedNeighbor = displacedTileIds.contains(tile.id)
                    val animSpec = if (isDisplacedNeighbor) {
                        tween<Dp>(durationMillis = 140)
                    } else {
                        tween<Dp>(durationMillis = 0)
                    }

                    key(tile.id) {
                        val tileX by animateDpAsState(
                            targetValue = cellSize * tile.x,
                            animationSpec = animSpec,
                            label = "resizePreviewX"
                        )
                        val tileY by animateDpAsState(
                            targetValue = cellSize * tile.y,
                            animationSpec = animSpec,
                            label = "resizePreviewY"
                        )
                        TileCell(
                            tile = tile,
                            tileSize = tileSize,
                            spacing = spacing,
                            radius = radius,
                            palette = palette,
                            editorState = editorState,
                            installedApps = installedApps,
                            themedIcons = themedIcons,
                            showResizeIndicator = showResizeIndicator,
                            isTargetCell = movedByResize || isTargetCell(tile, editorState),
                            introReveal = introReveal,
                            isIntroActive = isIntroActive,
                            onClick = { bounds -> onTileClick(tile, bounds) },
                            onLongClick = { onTileLongClick(tile) },
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onResizeStart = onResizeStart,
                            onResize = onResize,
                            onResizeEnd = onResizeEnd,
                            onDeleteTile = onDeleteTile,
                            modifier = Modifier.offset(
                                x = tileX,
                                y = tileY
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCell(
    visible: Boolean,
    tileSize: Dp,
    radius: Dp,
    palette: EtherealPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (visible) 1f else 0f
    val bg = palette.surfaceVariant.copy(alpha = 0.08f * alpha)
    val borderColor = palette.outline.copy(alpha = 0.25f * alpha)

    Box(
        modifier = modifier
            .size(tileSize)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .border(0.5.dp, borderColor, RoundedCornerShape(radius))
    )
}

private fun isTargetCell(tile: Tile, editorState: TileEditorState): Boolean {
    val target = editorState.targetCell.value ?: return false
    val dragging = editorState.draggingTile.value ?: return false
    if (dragging.id == tile.id) return false
    return target.first == tile.x && target.second == tile.y
}
