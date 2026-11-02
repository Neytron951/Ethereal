package com.neytron.ethereal.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.neytron.ethereal.tiles.TileBorderStyle

data class ESearchConfig(
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
