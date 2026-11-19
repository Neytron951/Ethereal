package com.neytron.ethereal.ui.theme

import android.content.Context
import com.neytron.ethereal.data.SettingsRepository
import com.neytron.ethereal.tiles.SpaceRepository
import com.neytron.ethereal.tiles.TileBorderStyle
import com.neytron.ethereal.tiles.TileLayoutRepository
import com.neytron.ethereal.tiles.TileTexture
import com.neytron.ethereal.ui.navigation.ScreenStack

/**
 * Пресет оформления: один тап переключает весь вид лаунчера —
 * панели, блюр, доли прозрачности, рамки плиток, док (включая его элементы)
 * и поиск.
 */
enum class DesignPreset {
    LIQUID_GLASS,
    MATERIAL,
    WINDOWS_PHONE,
    NONE
}

data class DesignSpec(
    val uiStyle: UiStyle,
    val panelOpacity: Float,
    val panelBlur: Float,
    val tileBackgroundOpacity: Float,
    val tileBackgroundBlur: Float,
    val borderStyle: TileBorderStyle,
    val borderWidth: Float,
    val borderOpacity: Float,
    val tileRadius: Float,
    val tileShape: TileShape,
    // Элементы дока — тоже часть дизайна
    val dockIconBgOpacity: Float,
    val dockBorderStyle: TileBorderStyle,
    val dockBorderWidth: Float,
    val dockBorderOpacity: Float,
    val dockIconShape: IconShape,
    val esearchOpacity: Float,
    val esearchBlur: Float,
    val esearchBorderStyle: TileBorderStyle,
    val esearchBorderWidth: Float,
    val esearchBorderOpacity: Float
)

fun specFor(preset: DesignPreset): DesignSpec = when (preset) {
    DesignPreset.LIQUID_GLASS -> DesignSpec(
        uiStyle = UiStyle.GLASS,
        panelOpacity = 0.55f,
        panelBlur = 18f,
        tileBackgroundOpacity = 0.30f,
        tileBackgroundBlur = 10f,
        borderStyle = TileBorderStyle.LIQUID_GLASS,
        borderWidth = 1.2f,
        borderOpacity = 0.55f,
        tileRadius = 22f,
        tileShape = TileShape.AUTO,
        dockIconBgOpacity = 0.50f,
        dockBorderStyle = TileBorderStyle.LIQUID_GLASS,
        dockBorderWidth = 1.2f,
        dockBorderOpacity = 0.55f,
        // Ближе к кругу, а не к квадрату
        dockIconShape = IconShape.SQUIRCLE_ROUND,
        esearchOpacity = 0.32f,
        esearchBlur = 26f,
        esearchBorderStyle = TileBorderStyle.LIQUID_GLASS,
        esearchBorderWidth = 1.2f,
        esearchBorderOpacity = 0.55f
    )

    DesignPreset.MATERIAL -> DesignSpec(
        uiStyle = UiStyle.FLAT,
        panelOpacity = 1f,
        panelBlur = 0f,
        tileBackgroundOpacity = 1f,
        tileBackgroundBlur = 0f,
        borderStyle = TileBorderStyle.NONE,
        borderWidth = 0f,
        borderOpacity = 0f,
        tileRadius = 16f,
        tileShape = TileShape.AUTO,
        dockIconBgOpacity = 1f,
        dockBorderStyle = TileBorderStyle.NONE,
        dockBorderWidth = 0f,
        dockBorderOpacity = 0f,
        dockIconShape = IconShape.CIRCLE,
        esearchOpacity = 1f,
        esearchBlur = 0f,
        esearchBorderStyle = TileBorderStyle.NONE,
        esearchBorderWidth = 0f,
        esearchBorderOpacity = 0f
    )

    // Windows Phone: плоские плитки без скруглений, без блюра и без рамок.
    DesignPreset.WINDOWS_PHONE -> DesignSpec(
        uiStyle = UiStyle.FLAT,
        panelOpacity = 1f,
        panelBlur = 0f,
        tileBackgroundOpacity = 1f,
        tileBackgroundBlur = 0f,
        borderStyle = TileBorderStyle.NONE,
        borderWidth = 0f,
        borderOpacity = 0f,
        tileRadius = 0f,
        tileShape = TileShape.ROUNDED,
        dockIconBgOpacity = 1f,
        dockBorderStyle = TileBorderStyle.NONE,
        dockBorderWidth = 0f,
        dockBorderOpacity = 0f,
        dockIconShape = IconShape.SQUARE,
        esearchOpacity = 1f,
        esearchBlur = 0f,
        esearchBorderStyle = TileBorderStyle.NONE,
        esearchBorderWidth = 0f,
        esearchBorderOpacity = 0f
    )

    // NONE не применяется (см. applyDesignPreset) — значения только для полноты.
    DesignPreset.NONE -> DesignSpec(
        uiStyle = UiStyle.GLASS,
        panelOpacity = 0f,
        panelBlur = 0f,
        tileBackgroundOpacity = 0f,
        tileBackgroundBlur = 0f,
        borderStyle = TileBorderStyle.NONE,
        borderWidth = 0f,
        borderOpacity = 0f,
        tileRadius = 0f,
        tileShape = TileShape.AUTO,
        dockIconBgOpacity = 0f,
        dockBorderStyle = TileBorderStyle.NONE,
        dockBorderWidth = 0f,
        dockBorderOpacity = 0f,
        dockIconShape = IconShape.CIRCLE,
        esearchOpacity = 0f,
        esearchBlur = 0f,
        esearchBorderStyle = TileBorderStyle.NONE,
        esearchBorderWidth = 0f,
        esearchBorderOpacity = 0f
    )
}

/**
 * Применяет пресет ко всему: настройки + стили уже существующих плиток.
 * Вызывать в IO-корутине: перезаписывает раскладки всех пространств.
 */
fun applyDesignPreset(context: Context, preset: DesignPreset) {
    // NONE — это «без пресета»: ничего не перезаписываем, у человека остаётся
    // его собственный дизайн. Важно для тех, кто обновился со старой версии.
    if (preset == DesignPreset.NONE) {
        SettingsRepository.setDesignPreset(context, preset)
        return
    }

    val spec = specFor(preset)

    SettingsRepository.setUiStyle(context, spec.uiStyle)
    SettingsRepository.setOpacity(context, spec.panelOpacity)
    SettingsRepository.setBlur(context, spec.panelBlur)
    SettingsRepository.setTileRadius(context, spec.tileRadius)
    SettingsRepository.setTileShape(context, spec.tileShape)

    // Док и его элементы
    SettingsRepository.setDockShowIconBackground(context, spec.dockIconBgOpacity > 0f)
    SettingsRepository.setDockIconBackgroundOpacity(context, spec.dockIconBgOpacity)
    SettingsRepository.setDockIconBorderStyle(context, spec.dockBorderStyle)
    SettingsRepository.setDockIconBorderWidth(context, spec.dockBorderWidth.coerceAtLeast(0.5f))
    SettingsRepository.setDockIconBorderOpacity(context, spec.dockBorderOpacity)
    SettingsRepository.setDockIconShape(context, spec.dockIconShape.name)

    SettingsRepository.setESearchUseOverride(context, true)
    SettingsRepository.setESearchOpacity(context, spec.esearchOpacity)
    SettingsRepository.setESearchBlur(context, spec.esearchBlur)
    SettingsRepository.setESearchBorderStyle(context, spec.esearchBorderStyle)
    SettingsRepository.setESearchBorderWidth(context, spec.esearchBorderWidth)
    SettingsRepository.setESearchBorderOpacity(context, spec.esearchBorderOpacity)

    SettingsRepository.setDesignPreset(context, preset)

    // Перекрашиваем уже существующие плитки: пропорции и размер контента
    // пользователя не трогаем — только «стекло/плоскость/форма».
    val owners = listOf(ScreenStack.ROOT_ID) + SpaceRepository.getAllSpaces(context).map { it.id }
    owners.forEach { ownerId ->
        val tiles = TileLayoutRepository.getTiles(context, ownerId)
        if (tiles.isEmpty()) return@forEach

        val updated = tiles.map { tile ->
            tile.copy(
                style = tile.style.copy(
                    backgroundOpacity = spec.tileBackgroundOpacity,
                    backgroundBlur = spec.tileBackgroundBlur,
                    borderStyle = spec.borderStyle,
                    borderWidth = spec.borderWidth,
                    borderOpacity = spec.borderOpacity,
                    texture = TileTexture.NONE,
                    shape = spec.tileShape
                )
            )
        }
        TileLayoutRepository.setTiles(context, ownerId, updated)
    }
}