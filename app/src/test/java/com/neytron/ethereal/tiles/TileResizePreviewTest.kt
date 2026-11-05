package com.neytron.ethereal.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileResizePreviewTest {

    @Test
    fun resizeMovesOverlappedTileToNearestFreeCells() {
        val target = tile("target", x = 0, y = 0)
        val neighbor = tile("neighbor", x = 1, y = 0)

        val preview = TileLayoutRepository.previewResizeWithDisplacement(
            tiles = listOf(target, neighbor),
            tileId = target.id,
            newW = 2,
            newH = 1,
            columns = 4,
            rows = 2
        )

        assertEquals(2, preview?.first { it.id == target.id }?.w)
        assertEquals(2, preview?.first { it.id == neighbor.id }?.x)
        assertEquals(0, preview?.first { it.id == neighbor.id }?.y)
    }

    @Test
    fun resizeIsRejectedWhenOverlappedTileCannotMove() {
        val target = tile("target", x = 0, y = 0)
        val neighbor = tile("neighbor", x = 1, y = 0)

        val preview = TileLayoutRepository.previewResizeWithDisplacement(
            tiles = listOf(target, neighbor),
            tileId = target.id,
            newW = 2,
            newH = 1,
            columns = 2,
            rows = 1
        )

        assertNull(preview)
    }

    private fun tile(id: String, x: Int, y: Int) = Tile(
        id = id,
        x = x,
        y = y,
        w = 1,
        h = 1,
        type = TileType.APP_LAUNCHER
    )
}
