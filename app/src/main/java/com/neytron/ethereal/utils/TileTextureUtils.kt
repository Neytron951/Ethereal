package com.neytron.ethereal.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.tiles.TileTexture
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlin.math.max
import kotlin.random.Random

/**
 * Процедурные текстуры фона плитки.
 *
 * Рисуются кодом поверх цвета подложки и под контентом: ничего не весит,
 * не тянет картинки из ресурсов и автоматически подстраивается под размер плитки.
 * [TileStyle.textureStrength] задаёт насыщенность (0..1).
 */
fun Modifier.tileTexture(
    style: TileStyle,
    palette: EtherealPalette
): Modifier {
    val texture = style.texture
    if (texture == TileTexture.NONE || style.textureStrength <= 0f) return this

    val strength = style.textureStrength.coerceIn(0f, 1f)
    // На светлых темах «светлая» текстура не видна — берём затемнение и наоборот.
    val tint = if (palette.isLight) Color.Black else Color.White

    return this.drawBehind {
        val width = size.width
        val height = size.height
        val diagonal = max(width, height) * 1.6f

        when (texture) {
            TileTexture.GRAIN -> {
                val cell = 13.dp.toPx()
                val count = ((width * height) / (cell * cell)).toInt().coerceIn(120, 2200)
                val random = Random((width.toInt() * 31 + height.toInt() * 17 + 7))
                val points = List(count) {
                    Offset(random.nextFloat() * width, random.nextFloat() * height)
                }
                drawPoints(
                    points = points,
                    pointMode = PointMode.Points,
                    color = tint.copy(alpha = 0.15f * strength),
                    strokeWidth = 1.1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            TileTexture.DOTS -> {
                val step = 9.dp.toPx()
                val radius = 1.1.dp.toPx()
                val color = tint.copy(alpha = 0.20f * strength)
                var y = step / 2f
                while (y < height) {
                    var x = step / 2f
                    while (x < width) {
                        drawCircle(color = color, radius = radius, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }

            TileTexture.GRID -> {
                val step = 12.dp.toPx()
                val stroke = 0.7.dp.toPx()
                val color = tint.copy(alpha = 0.18f * strength)
                var x = 0f
                while (x <= width) {
                    drawLine(color, Offset(x, 0f), Offset(x, height), stroke)
                    x += step
                }
                var y = 0f
                while (y <= height) {
                    drawLine(color, Offset(0f, y), Offset(width, y), stroke)
                    y += step
                }
            }

            TileTexture.DIAGONAL -> {
                val step = 11.dp.toPx()
                val stroke = 3.5.dp.toPx()
                val color = tint.copy(alpha = 0.16f * strength)
                rotate(degrees = 45f) {
                    var x = -diagonal
                    while (x <= diagonal) {
                        drawLine(color, Offset(x, -diagonal), Offset(x, diagonal), stroke)
                        x += step
                    }
                }
            }

            TileTexture.CARBON -> {
                val step = 7.dp.toPx()
                val stroke = 1.6.dp.toPx()
                val light = tint.copy(alpha = 0.13f * strength)
                val dark = Color.Black.copy(alpha = 0.10f * strength)
                rotate(degrees = 45f) {
                    var x = -diagonal
                    while (x <= diagonal) {
                        drawLine(light, Offset(x, -diagonal), Offset(x, diagonal), stroke)
                        x += step * 2f
                    }
                }
                rotate(degrees = -45f) {
                    var x = -diagonal
                    while (x <= diagonal) {
                        drawLine(dark, Offset(x, -diagonal), Offset(x, diagonal), stroke)
                        x += step * 2f
                    }
                }
            }

            TileTexture.AURORA -> {
                val radius = max(width, height)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.primary.copy(alpha = 0.20f * strength), Color.Transparent),
                        center = Offset(width * 0.15f, height * 0.10f),
                        radius = radius * 0.85f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.secondary.copy(alpha = 0.16f * strength), Color.Transparent),
                        center = Offset(width * 0.85f, height * 0.90f),
                        radius = radius * 0.75f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.10f * strength), Color.Transparent),
                        center = Offset(width * 0.60f, height * 0.25f),
                        radius = radius * 0.55f
                    )
                )
            }

            TileTexture.NONE -> Unit
        }
    }
}
