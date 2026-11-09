package com.neytron.ethereal.tiles.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.neytron.ethereal.tiles.Tile

/**
 * Состояние редактора тайлов.
 *
 * editing — включён ли режим редактирования.
 * draggingTile — какой тайл тащат.
 * dragOffsetPx — смещение пальца.
 * targetCell — куда тайл встанет (col, row).
 * selectedTile — какой тайл выбран для настройки (тап по нему).
 */
class TileEditorState {

    val editing: MutableState<Boolean> = mutableStateOf(false)
    val draggingTile: MutableState<Tile?> = mutableStateOf(null)
    val dragOffsetPx: MutableState<Offset> = mutableStateOf(Offset.Zero)
    val targetCell: MutableState<Pair<Int, Int>?> = mutableStateOf(null)
    val selectedTile: MutableState<Tile?> = mutableStateOf(null)
    val resizingTile: MutableState<Tile?> = mutableStateOf(null)
    val resizePreviewTiles: MutableState<List<Tile>?> = mutableStateOf(null)
    val displacedTileIds: MutableState<Set<String>> = mutableStateOf(emptySet())
    val newlyCreatedTileIds: MutableState<Set<String>> = mutableStateOf(emptySet())

    fun markNewlyCreated(id: String) {
        newlyCreatedTileIds.value = newlyCreatedTileIds.value + id
    }

    fun consumeNewlyCreated(id: String): Boolean {
        if (id in newlyCreatedTileIds.value) {
            newlyCreatedTileIds.value = newlyCreatedTileIds.value - id
            return true
        }
        return false
    }

    fun enterEdit() {
        editing.value = true
    }

    fun exitEdit() {
        editing.value = false
        draggingTile.value = null
        dragOffsetPx.value = Offset.Zero
        targetCell.value = null
        selectedTile.value = null
        resizingTile.value = null
        resizePreviewTiles.value = null
        displacedTileIds.value = emptySet()
    }

    fun startDrag(tile: Tile) {
        draggingTile.value = tile
        dragOffsetPx.value = Offset.Zero
        targetCell.value = Pair(tile.x, tile.y)
        displacedTileIds.value = emptySet()
    }

    fun updateDrag(offset: Offset) {
        dragOffsetPx.value = offset
    }

    fun endDrag() {
        draggingTile.value = null
        dragOffsetPx.value = Offset.Zero
        targetCell.value = null
    }

    suspend fun animateDropToZero(targetDeltaPx: Offset) {
        val currentOffset = dragOffsetPx.value
        val compensated = currentOffset - targetDeltaPx
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ) {
            dragOffsetPx.value = Offset(
                compensated.x * (1f - value),
                compensated.y * (1f - value)
            )
        }
    }

    fun startResize(tile: Tile, tiles: List<Tile>) {
        resizingTile.value = tile
        resizePreviewTiles.value = tiles
    }

    fun endResize() {
        resizingTile.value = null
        resizePreviewTiles.value = null
    }

    fun select(tile: Tile?) {
        selectedTile.value = tile
    }
}

@Composable
fun rememberTileEditorState(): TileEditorState {
    return remember { TileEditorState() }
}