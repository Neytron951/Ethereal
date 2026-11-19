package com.neytron.ethereal.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Color as AndroidColor

/**
 * Простой выбор цвета: квадрат насыщенность×яркость и полоса оттенка.
 *
 * Наружу отдаёт готовый [Color] — никаких пересчётов в другие цветовые
 * пространства: что нажал, то и вернулось.
 */
@Composable
fun ColorPicker(
    selected: Color,
    palette: EtherealPalette,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // Внутреннее состояние инициализируется один раз, чтобы маркер не «прыгал»
    // при каждом применении цвета.
    val start = remember { hsvOf(selected) }
    var hue by remember { mutableStateOf(start[0]) }
    var sat by remember { mutableStateOf(start[1]) }
    var value by remember { mutableStateOf(start[2]) }

    fun emit(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        value = v
        onPick(Color.hsv(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.hsv(((hue % 360f) + 360f) % 360f, 1f, 1f))
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        emit(hue, offset.x / size.width, 1f - offset.y / size.height)
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        emit(hue, change.position.x / size.width, 1f - change.position.y / size.height)
                        change.consume()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val x = sat.coerceIn(0f, 1f) * size.width
                val y = (1f - value.coerceIn(0f, 1f)) * size.height
                drawCircle(color = Color.Black, radius = 9f, center = Offset(x, y))
                drawCircle(color = Color.White, radius = 7f, center = Offset(x, y))
            }
        }

        Spacer(height = 10.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        emit(offset.x / size.width * 360f, sat, value)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        emit(change.position.x / size.width * 360f, sat, value)
                        change.consume()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                val steps = 24
                for (i in 0 until steps) {
                    val x0 = size.width * i / steps
                    val x1 = size.width * (i + 1) / steps
                    drawRect(
                        color = Color.hsv(i * 360f / steps, 1f, 1f),
                        topLeft = Offset(x0, 0f),
                        size = androidx.compose.ui.geometry.Size(x1 - x0 + 1f, size.height)
                    )
                }
            }
        }

        Spacer(height = 10.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Свотч показывает цвет, который реально применился к теме
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(selected)
                    .border(1.dp, palette.outline.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
            )
            Text(
                text = "#%02X%02X%02X".format(
                    (selected.red * 255).toInt(),
                    (selected.green * 255).toInt(),
                    (selected.blue * 255).toInt()
                ),
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.height(height))
}

private fun hsvOf(color: Color): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(
        AndroidColor.rgb(
            (color.red * 255).toInt().coerceIn(0, 255),
            (color.green * 255).toInt().coerceIn(0, 255),
            (color.blue * 255).toInt().coerceIn(0, 255)
        ),
        hsv
    )
    return hsv
}
