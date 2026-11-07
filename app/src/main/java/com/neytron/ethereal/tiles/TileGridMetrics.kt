package com.neytron.ethereal.tiles

/**
 * Геометрия сетки плиток: сколько ячеек влезает и какого размера должна быть плитка.
 * Значения в dp, чтобы одну и ту же логику могли использовать и экран, и репозиторий.
 */
data class TileGridSpec(
    val columns: Int,
    val rows: Int,
    val tileSizeDp: Float,
    val spacingDp: Float
) {
    val cellStepDp: Float get() = tileSizeDp + spacingDp
    val totalWidthDp: Float get() = cellStepDp * columns - spacingDp
    val totalHeightDp: Float get() = cellStepDp * rows - spacingDp
}

/**
 * Единый источник правды по раскладке сетки.
 *
 * Раньше геометрия была размазана по TilesScreen (padding'и, dockHeight, min 4 строки),
 * а генерация ландшафтной раскладки в репозитории считала колонки совсем из других данных
 * (настройка tile_columns = 4), из-за чего плитки в ландшафте сбивались в «остров»
 * и уменьшались до нечитаемых размеров.
 */
object TileGridMetrics {

    const val MAX_COLUMNS = 12
    const val MIN_TILE_DP = 24f
    const val MAX_TILE_DP = 160f

    /** В портрете всегда показываем минимум 4 строки (пустая нижняя часть — это нормально). */
    const val PORTRAIT_MIN_ROWS = 4

    // Отступы главного экрана
    const val EDGE_PADDING_DP = 16f
    const val TOP_PADDING_DP = 72f
    const val GAP_ABOVE_DOCK_DP = 16f
    const val DOCK_EXTRA_DP = 24f
    const val LANDSCAPE_DOCK_SCALE = 0.75f

    fun availableWidthDp(screenWidthDp: Float): Float =
        (screenWidthDp - EDGE_PADDING_DP * 2f).coerceAtLeast(MIN_TILE_DP)

    fun availableHeightDp(
        screenHeightDp: Float,
        dockIconSizeDp: Float,
        dockBottomPaddingDp: Float,
        isLandscape: Boolean
    ): Float {
        val dockIcon = if (isLandscape) {
            (dockIconSizeDp * LANDSCAPE_DOCK_SCALE).coerceAtLeast(24f)
        } else {
            dockIconSizeDp
        }
        val dockHeight = dockIcon + DOCK_EXTRA_DP
        return (screenHeightDp - TOP_PADDING_DP - GAP_ABOVE_DOCK_DP - dockHeight - dockBottomPaddingDp)
            .coerceAtLeast(MIN_TILE_DP)
    }

    /**
     * Считает сетку под доступную область с учётом того, сколько ячеек уже занято раскладкой.
     *
     * @param layoutColumns/minRows — сколько колонок/строк требует сама раскладка (меньше сетка быть не может)
     */
    fun compute(
        availableWidthDp: Float,
        availableHeightDp: Float,
        desiredTileSizeDp: Float,
        spacingDp: Float,
        isLandscape: Boolean,
        layoutColumns: Int,
        layoutRows: Int
    ): TileGridSpec {
        val spacing = spacingDp.coerceIn(0f, 32f)
        val desired = desiredTileSizeDp.coerceIn(MIN_TILE_DP, MAX_TILE_DP)

        val widthFit = ((availableWidthDp + spacing) / (desired + spacing)).toInt()
            .coerceIn(1, MAX_COLUMNS)
        val heightFit = ((availableHeightDp + spacing) / (desired + spacing)).toInt()
            .coerceAtLeast(1)

        var columns: Int = widthFit
        var rows: Int = heightFit

        // Раскладка не должна вылезать за сетку
        columns = maxOf(columns, layoutColumns).coerceIn(1, MAX_COLUMNS)
        rows = maxOf(rows, layoutRows).coerceAtLeast(1)
        // В портрете держим минимум 4 строки; в ландшафте высота дефицитная — лишние строки не навязываем.
        if (!isLandscape) rows = maxOf(rows, PORTRAIT_MIN_ROWS)

        var tile = tileSizeFor(availableWidthDp, availableHeightDp, columns, rows, spacing)

        // Добираем колонки, чтобы сетка занимала ширину, а не стояла «островом» по центру
        val columnsAtTile = ((availableWidthDp + spacing) / (tile + spacing)).toInt()
            .coerceIn(1, MAX_COLUMNS)
        if (columnsAtTile > columns) {
            columns = columnsAtTile
            tile = tileSizeFor(availableWidthDp, availableHeightDp, columns, rows, spacing)
        }

        // И строки, если после уточнения размера в высоту влезает больше
        val rowsAtTile = ((availableHeightDp + spacing) / (tile + spacing)).toInt().coerceAtLeast(1)
        if (rowsAtTile > rows) {
            rows = rowsAtTile
            tile = tileSizeFor(availableWidthDp, availableHeightDp, columns, rows, spacing)
        }

        return TileGridSpec(columns, rows, tile, spacing)
    }

    fun tileSizeFor(
        availableWidthDp: Float,
        availableHeightDp: Float,
        columns: Int,
        rows: Int,
        spacingDp: Float
    ): Float {
        val cols = columns.coerceAtLeast(1)
        val rws = rows.coerceAtLeast(1)
        val byWidth = (availableWidthDp - spacingDp * (cols - 1)) / cols
        val byHeight = (availableHeightDp - spacingDp * (rws - 1)) / rws
        return minOf(byWidth, byHeight).coerceIn(MIN_TILE_DP, MAX_TILE_DP)
    }

    /** Все плитки влезают в сетку и не пересекаются. */
    fun fitsGrid(tiles: List<Tile>, spec: TileGridSpec): Boolean {
        if (tiles.isEmpty()) return true
        val cols = spec.columns.coerceAtLeast(1)
        val rows = spec.rows.coerceAtLeast(1)
        val occupied = Array(rows) { BooleanArray(cols) }
        for (tile in tiles) {
            if (tile.x < 0 || tile.y < 0) return false
            if (tile.w < 1 || tile.h < 1) return false
            if (tile.x + tile.w > cols || tile.y + tile.h > rows) return false
            for (dy in 0 until tile.h) {
                for (dx in 0 until tile.w) {
                    if (occupied[tile.y + dy][tile.x + dx]) return false
                    occupied[tile.y + dy][tile.x + dx] = true
                }
            }
        }
        return true
    }

    /**
     * Пытается разложить плитки в сетку [columns]×[rows], сохраняя по возможности
     * исходные координаты. Возвращает null, если ёмкости не хватает.
     */
    fun packTiles(tiles: List<Tile>, columns: Int, rows: Int): List<Tile>? {
        val cols = columns.coerceAtLeast(1)
        val rws = rows.coerceAtLeast(1)
        if (tiles.isEmpty()) return emptyList()
        if (tiles.any { it.w > cols || it.h > rws }) {
            // Плитка шире/выше сетки — такие сначала ужимаем, но только если это возможно
            return packTiles(tiles, cols, rws, allowShrink = true)
        }
        return packTiles(tiles, cols, rws, allowShrink = false)
    }

    private fun packTiles(
        tiles: List<Tile>,
        cols: Int,
        rws: Int,
        allowShrink: Boolean
    ): List<Tile>? {
        val occupied = Array(rws) { BooleanArray(cols) }
        val ordered = tiles.sortedWith(compareBy({ it.y }, { it.x }))
        val result = ArrayList<Tile>(ordered.size)

        for (tile in ordered) {
            val w = if (allowShrink) tile.w.coerceIn(1, cols) else tile.w
            val h = if (allowShrink) tile.h.coerceIn(1, rws) else tile.h
            if (w > cols || h > rws) return null

            val preferX = tile.x.coerceIn(0, cols - w)
            val preferY = tile.y.coerceIn(0, rws - h)
            val spot = findSpot(occupied, cols, rws, w, h, preferX, preferY) ?: return null
            for (dy in 0 until h) {
                for (dx in 0 until w) {
                    occupied[spot.second + dy][spot.first + dx] = true
                }
            }
            result.add(tile.copy(x = spot.first, y = spot.second, w = w, h = h))
        }
        return result
    }

    /**
     * Перепаковка «во что бы то ни стало»: если в [rows] не влезает — добавляем строки,
     * пока всё не поместится. Ни одна плитка не теряется.
     */
    fun projectTiles(tiles: List<Tile>, columns: Int, startRows: Int): List<Tile> {
        if (tiles.isEmpty()) return tiles
        val cols = columns.coerceAtLeast(1)
        val minRows = maxOf(
            startRows.coerceAtLeast(1),
            tiles.maxOfOrNull { it.h }?.coerceAtLeast(1) ?: 1
        )
        var rows = minRows
        while (rows <= 64) {
            packTiles(tiles, cols, rows)?.let { return it }
            rows++
        }
        // Совсем крайний случай: плитки больше сетки по ширине — ужимаем ширину
        return tiles.map { it.copy(w = it.w.coerceAtMost(cols)) }
    }

    private fun findSpot(
        occupied: Array<BooleanArray>,
        cols: Int,
        rws: Int,
        w: Int,
        h: Int,
        preferX: Int,
        preferY: Int
    ): Pair<Int, Int>? {
        if (isFree(occupied, cols, rws, w, h, preferX, preferY)) return preferX to preferY
        for (y in 0..rws - h) {
            for (x in 0..cols - w) {
                if (isFree(occupied, cols, rws, w, h, x, y)) return x to y
            }
        }
        return null
    }

    private fun isFree(
        occupied: Array<BooleanArray>,
        cols: Int,
        rws: Int,
        w: Int,
        h: Int,
        x: Int,
        y: Int
    ): Boolean {
        if (x < 0 || y < 0 || x + w > cols || y + h > rws) return false
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                if (occupied[y + dy][x + dx]) return false
            }
        }
        return true
    }
}
