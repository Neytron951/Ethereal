package com.neytron.ethereal.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileGridMetricsTest {

    private fun tile(id: String, x: Int, y: Int, w: Int = 1, h: Int = 1) = Tile(
        id = id,
        x = x,
        y = y,
        w = w,
        h = h,
        type = TileType.WIDGET
    )

    /** Раскладка-пример: 2x2 часы и погода, виджет 5x1, 3x1, четыре иконки. */
    private fun sampleLayout() = listOf(
        tile("clock", 0, 0, 2, 2),
        tile("weather", 2, 0, 2, 2),
        tile("space", 4, 0),
        tile("settings", 4, 1),
        tile("widget", 0, 2, 5, 1),
        tile("ime", 0, 3, 3, 1),
        tile("a1", 0, 4),
        tile("a2", 1, 4),
        tile("a3", 3, 4),
        tile("a4", 4, 4)
    )

    @Test
    fun landscapeGridFitsScreenWidthAndHeight() {
        // Телефон из бага: 804x360dp, density 640, dock 50dp, отступ 13.36dp
        val width = TileGridMetrics.availableWidthDp(804f)
        val height = TileGridMetrics.availableHeightDp(360f, 50f, 13.36f, isLandscape = true)

        val spec = TileGridMetrics.compute(
            availableWidthDp = width,
            availableHeightDp = height,
            desiredTileSizeDp = 56.7f,
            spacingDp = 9f,
            isLandscape = true,
            layoutColumns = 0,
            layoutRows = 0
        )

        assertTrue("плитка не должна быть крошечной: ${spec.tileSizeDp}", spec.tileSizeDp >= 45f)
        assertTrue("сетка шире экрана: ${spec.totalWidthDp} > $width", spec.totalWidthDp <= width + 0.5f)
        assertTrue("сетка выше экрана: ${spec.totalHeightDp} > $height", spec.totalHeightDp <= height + 0.5f)
        assertTrue("сетка должна занимать ширину", spec.totalWidthDp >= width * 0.9f)
    }

    @Test
    fun portraitGridKeepsFourRowsMinimum() {
        val width = TileGridMetrics.availableWidthDp(360f)
        val height = TileGridMetrics.availableHeightDp(804f, 50f, 13.36f, isLandscape = false)

        val spec = TileGridMetrics.compute(
            availableWidthDp = width,
            availableHeightDp = height,
            desiredTileSizeDp = 56.7f,
            spacingDp = 9f,
            isLandscape = false,
            layoutColumns = 5,
            layoutRows = 8
        )

        assertEquals(5, spec.columns)
        assertTrue("в портрете минимум 4 строки", spec.rows >= 4)
        assertTrue(spec.rows >= 8)
        assertTrue(spec.totalWidthDp <= width + 0.5f)
    }

    @Test
    fun projectionKeepsEveryTile() {
        val tiles = sampleLayout()
        val packed = TileGridMetrics.packTiles(tiles, columns = 11, rows = 3)

        assertNotNull("раскладка должна уместиться в 11x3", packed)
        assertEquals("ни одна плитка не должна потеряться", tiles.size, packed!!.size)
        assertEquals(tiles.map { it.id }.toSet(), packed.map { it.id }.toSet())
        assertTrue(TileGridMetrics.fitsGrid(packed, TileGridSpec(11, 3, 50f, 9f)))
    }

    @Test
    fun projectionGrowsRowsWhenCapacityIsNotEnough() {
        val tiles = sampleLayout()
        val projected = TileGridMetrics.projectTiles(tiles, columns = 4, startRows = 2)

        assertEquals(tiles.size, projected.size)
        val needRows = projected.maxOf { it.y + it.h }
        assertTrue(needRows >= 2)
        assertTrue(TileGridMetrics.fitsGrid(projected, TileGridSpec(4, needRows, 50f, 9f)))
    }

    @Test
    fun fittingLayoutIsNotTouchedByProjection() {
        val tiles = sampleLayout()
        val packed = TileGridMetrics.packTiles(tiles, columns = 11, rows = 3)!!
        val spec = TileGridSpec(11, 3, 50f, 9f)

        assertTrue(TileGridMetrics.fitsGrid(packed, spec))
        // Повторная перепаковка уже уложенной раскладки не должна ничего сдвигать
        val again = TileGridMetrics.projectTiles(packed, 11, 3)
        assertEquals(
            packed.associate { it.id to (it.x to it.y) },
            again.associate { it.id to (it.x to it.y) }
        )
    }

    @Test
    fun fitsGridRejectsOverlapsAndOutOfBounds() {
        val spec = TileGridSpec(4, 3, 50f, 9f)
        assertTrue(TileGridMetrics.fitsGrid(listOf(tile("a", 0, 0, 2, 1)), spec))
        assertTrue(!TileGridMetrics.fitsGrid(listOf(tile("a", 0, 0, 2, 1), tile("b", 1, 0)), spec))
        assertTrue(!TileGridMetrics.fitsGrid(listOf(tile("a", 3, 0, 2, 1)), spec))
        assertTrue(!TileGridMetrics.fitsGrid(listOf(tile("a", 0, 2, 1, 2)), spec))
    }
}
