package com.neytron.ethereal.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.neytron.ethereal.tiles.TileBorderStyle
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.ui.theme.EtherealPalette

fun Modifier.tileBorder(
    tileStyle: TileStyle,
    palette: EtherealPalette,
    shape: RoundedCornerShape
): Modifier {
    val bWidth = tileStyle.borderWidth
    val bOpacity = tileStyle.borderOpacity
    val bStyle = tileStyle.borderStyle

    if (bWidth <= 0f && bStyle == TileBorderStyle.NONE) return this

    val effectiveWidth = if (bWidth > 0f) bWidth.dp else 1.5.dp

    return when (bStyle) {
        TileBorderStyle.LIQUID_GLASS -> {
            val tilt = DeviceTiltProvider.tilt.value
            this.drawWithCache {
                val outerWidth = size.width
                val outerHeight = size.height
                val bWidthPx = effectiveWidth.toPx()
                val radiusPx = shape.topStart.toPx(size, this)
                val cornerRadius = CornerRadius(radiusPx, radiusPx)

                onDrawWithContent {
                    drawContent()

                    // 1. Outer convex specular highlight (top-left light source reacting to tilt)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (0.85f * bOpacity).coerceIn(0f, 1f)),
                                Color.White.copy(alpha = (0.20f * bOpacity).coerceIn(0f, 1f)),
                                Color.Transparent
                            ),
                            start = Offset(-tilt.first * 1.5f, -tilt.second * 1.5f),
                            end = Offset(outerWidth * 0.7f, outerHeight * 0.7f)
                        ),
                        size = size,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = bWidthPx)
                    )

                    // 2. Inner refraction bevel (bottom-right depth shadow)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = (0.4f * bOpacity).coerceIn(0f, 1f)),
                                Color.White.copy(alpha = (0.3f * bOpacity).coerceIn(0f, 1f))
                            ),
                            start = Offset(outerWidth * 0.3f, outerHeight * 0.3f),
                            end = Offset(outerWidth + tilt.first * 1.5f, outerHeight + tilt.second * 1.5f)
                        ),
                        size = size,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = bWidthPx * 0.7f)
                    )
                }
            }
        }
        TileBorderStyle.OUTLINE -> {
            this.border(effectiveWidth, palette.outline.copy(alpha = bOpacity), shape)
        }
        TileBorderStyle.ACCENT -> {
            this.border(effectiveWidth, palette.primary.copy(alpha = bOpacity), shape)
        }
        TileBorderStyle.CUSTOM -> {
            this.border(effectiveWidth, palette.primary.copy(alpha = bOpacity), shape)
        }
        TileBorderStyle.NONE -> {
            if (bWidth > 0f) {
                this.border(bWidth.dp, palette.outline.copy(alpha = bOpacity), shape)
            } else this
        }
        else -> this
    }
}
