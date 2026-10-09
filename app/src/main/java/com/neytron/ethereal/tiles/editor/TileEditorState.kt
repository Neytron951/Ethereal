package com.neytron.ethereal.tiles.editor

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

    fun enterEdit() {
        editing.value = true
    }

    fun exitEdit() {
        editing.value = false
        draggingTile.value = null
        dragOffsetPx.value = Offset.Zero
        targetCell.value = null
        selectedTile.value = null
    }

    fun startDrag(tile: Tile) {
        draggingTile.value = tile
        dragOffsetPx.value = Offset.Zero
        targetCell.value = Pair(tile.x, tile.y)
    }

    fun updateDrag(offset: Offset) {
        dragOffsetPx.value = offset
    }

    fun endDrag() {
        draggingTile.value = null
        dragOffsetPx.value = Offset.Zero
        targetCell.value = null
    }

    fun select(tile: Tile?) {
        selectedTile.value = tile
    }
}

@Composable
fun rememberTileEditorState(): TileEditorState {
    return remember { TileEditorState() }
}