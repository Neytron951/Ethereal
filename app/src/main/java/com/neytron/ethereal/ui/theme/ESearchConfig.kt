package com.neytron.ethereal.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.neytron.ethereal.tiles.TileBorderStyle

/**
 * Визуальный стиль ESearch.
 * Меняет не только цвета, но и саму подачу результатов.
 */
enum class ESearchStyle {
    /** Классика: строка поиска + вертикальный список приложений. */
    DEFAULT,

    /** Секции: лучший результат, недавние, все приложения. */
    CATEGORIES,

    /** Сетка иконок как в drawer'е. */
    GRID,

    /** Плотный текстовый список без иконок — максимум строк на экране. */
    COMPACT;

    companion object {
        fun fromName(raw: String?): ESearchStyle =
            values().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DEFAULT
    }
}

data class ESearchConfig(
    val style: ESearchStyle = ESearchStyle.DEFAULT,
    val opacityOverride: Float? = null,
    val blurOverride: Float? = null,
    val widthFraction: Float = 0.75f,
    val heightFraction: Float = 0.6f,
    val visibleItems: Int = 6,
    val placeholder: String = "Поиск",
    val themedIcons: Boolean = false,
    val autoShowKeyboard: Boolean = true,
    val borderWidth: Float = 0f,
    val borderOpacity: Float = 0.3f,
    val borderStyle: TileBorderStyle = TileBorderStyle.NONE
)

val LocalESearchConfig = compositionLocalOf { ESearchConfig() }
