package com.neytron.ethereal.ui

import android.content.Context
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.AppLauncherConfig
import com.neytron.ethereal.tiles.Tile
import com.neytron.ethereal.tiles.TileLayoutRepository
import com.neytron.ethereal.tiles.TileType
import java.util.UUID

/** Стартовая раскладка, которую новичок выбирает в онбординге. */
enum class StarterLayout { EMPTY, MINIMAL, EVERYDAY }

/**
 * Собирает стартовый набор плиток для Home.
 *
 * По умолчанию — пустой пресет (как будто ничего не выбирали). Остальные пресеты
 * заполняют сетку приложениями по алфавиту и не трогают уже существующие плитки:
 * если на Home что-то есть, мы ничего не перезаписываем.
 */
fun buildStarterTiles(
    apps: List<AppInfo>,
    layout: StarterLayout,
    columns: Int,
    rows: Int,
    chosenPackages: List<String> = emptyList()
): List<Tile> {
    if (layout == StarterLayout.EMPTY) return emptyList()

    val cols = columns.coerceAtLeast(1)
    val maxRows = rows.coerceAtLeast(1)
    val appCount = when (layout) {
        StarterLayout.MINIMAL -> 6
        StarterLayout.EVERYDAY -> 12
        StarterLayout.EMPTY -> 0
    }

    // Если пользователь выбрал приложения сам — берём их, иначе первые по алфавиту
    val pool = if (chosenPackages.isNotEmpty()) {
        chosenPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    } else {
        apps.sortedBy { it.label.lowercase() }
    }
    val sortedApps = pool.take(appCount)

    val tiles = mutableListOf<Tile>()

    // Плитка настроек — «якорь» интерфейса, стоит в левом верхнем углу
    tiles.add(
        Tile(
            id = "settings-tile",
            x = 0,
            y = 0,
            w = 1,
            h = 1,
            type = TileType.SETTINGS,
            locked = true,
            clickable = true
        )
    )

    var placed = 0
    outer@ for (y in 0 until maxRows) {
        for (x in 0 until cols) {
            if (placed >= sortedApps.size) break@outer
            if (tiles.any { it.x == x && it.y == y }) continue
            val app = sortedApps[placed]
            tiles.add(
                Tile(
                    id = UUID.randomUUID().toString(),
                    x = x,
                    y = y,
                    w = 1,
                    h = 1,
                    type = TileType.APP_LAUNCHER,
                    payload = app.packageName,
                    locked = false,
                    clickable = true,
                    config = AppLauncherConfig(packageName = app.packageName).toJson()
                )
            )
            placed++
        }
    }

    return tiles
}

/** Применяет выбранный пресет к Home, если там ещё пусто. */
fun applyStarterLayout(
    context: Context,
    apps: List<AppInfo>,
    layout: StarterLayout,
    columns: Int,
    rows: Int,
    chosenPackages: List<String> = emptyList()
): List<Tile>? {
    if (layout == StarterLayout.EMPTY) return null

    val existing = TileLayoutRepository.getTiles(context, com.neytron.ethereal.ui.navigation.ScreenStack.ROOT_ID)
    val hasContent = existing.any { it.type != TileType.SETTINGS }
    if (hasContent) return null

    val tiles = buildStarterTiles(apps, layout, columns, rows, chosenPackages)
    if (tiles.isEmpty()) return null

    TileLayoutRepository.setTiles(
        context,
        com.neytron.ethereal.ui.navigation.ScreenStack.ROOT_ID,
        tiles
    )
    return tiles
}
