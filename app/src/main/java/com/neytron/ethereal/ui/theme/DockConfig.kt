package com.neytron.ethereal.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class IconShape(val displayName: String) {
    CIRCLE("Круг"),
    SQUARE("Квадрат"),
    SQUIRCLE_TIGHT("Квадрокруг 1"),
    SQUIRCLE_ROUND("Квадрокруг 2"),
    WINDOW("Окно")
}

fun iconShapeFor(shape: IconShape, sizeDp: Float): Shape = when (shape) {
    IconShape.CIRCLE -> RoundedCornerShape(percent = 50)
    IconShape.SQUARE -> RoundedCornerShape(size = (sizeDp * 0.08f).dp)
    IconShape.SQUIRCLE_TIGHT -> RoundedCornerShape(percent = 25)
    IconShape.SQUIRCLE_ROUND -> RoundedCornerShape(percent = 40)
    IconShape.WINDOW -> CutCornerShape(percent = 25)
}

data class DockConfig(
    val iconSizeDp: Float = 52f,
    val bottomPaddingDp: Float = 32f,
    val opacityOverride: Float? = null,
    val blurOverride: Float? = null,
    val scrollable: Boolean = false,
    val iconShape: IconShape = IconShape.SQUIRCLE_TIGHT,
    val showIconBackground: Boolean = true,
    val themedIcons: Boolean = false

)

val LocalDockConfig = compositionLocalOf { DockConfig() }