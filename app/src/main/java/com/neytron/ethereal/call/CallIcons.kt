package com.neytron.ethereal.call

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neytron.ethereal.ui.theme.EtherealPalette

// ============================================================
// Микрофон
// ============================================================

@Composable
fun MicIcon(
    muted: Boolean,
    palette: EtherealPalette,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val color = if (muted) palette.textSecondary else palette.textPrimary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val micWidth = w * 0.3f
        val micHeight = h * 0.55f
        val micX = (w - micWidth) / 2
        val micY = h * 0.15f

        drawRoundRect(
            color = color,
            topLeft = Offset(micX, micY),
            size = Size(micWidth, micHeight),
            cornerRadius = CornerRadius(micWidth / 2),
            style = Stroke(width = w * 0.08f)
        )

        drawLine(
            color = color,
            start = Offset(w / 2, micY + micHeight),
            end = Offset(w / 2, h * 0.85f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(w * 0.3f, h * 0.85f),
            end = Offset(w * 0.7f, h * 0.85f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )

        if (muted) {
            drawLine(
                color = Color(0xFFFF5555),
                start = Offset(w * 0.15f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.85f),
                strokeWidth = w * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

// ============================================================
// Динамик / Аудио-выход
// ============================================================

@Composable
fun SpeakerIcon(
    active: Boolean,
    palette: EtherealPalette,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val color = if (active) Color.Black else palette.textPrimary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.4f)
            lineTo(w * 0.3f, h * 0.4f)
            lineTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.5f, h * 0.8f)
            lineTo(w * 0.3f, h * 0.6f)
            lineTo(w * 0.1f, h * 0.6f)
            close()
        }
        drawPath(path = path, color = color)

        drawArc(
            color = color,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.35f, h * 0.25f),
            size = Size(w * 0.4f, h * 0.5f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.5f, h * 0.15f),
            size = Size(w * 0.5f, h * 0.7f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
    }
}

// ============================================================
// Удержание (пауза)
// ============================================================

@Composable
fun HoldIcon(
    active: Boolean,
    palette: EtherealPalette,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val color = if (active) Color.Black else palette.textPrimary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.25f, h * 0.2f),
            size = Size(w * 0.15f, h * 0.6f),
            cornerRadius = CornerRadius(w * 0.05f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.6f, h * 0.2f),
            size = Size(w * 0.15f, h * 0.6f),
            cornerRadius = CornerRadius(w * 0.05f)
        )
    }
}

// ============================================================
// Клавиатура
// ============================================================

@Composable
fun KeypadIcon(
    palette: EtherealPalette,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val color = palette.textPrimary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val dotRadius = w * 0.08f
        val startX = w * 0.2f
        val startY = h * 0.2f
        val stepX = w * 0.3f
        val stepY = h * 0.3f

        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = color,
                    radius = dotRadius,
                    center = Offset(
                        startX + col * stepX,
                        startY + row * stepY
                    )
                )
            }
        }
    }
}

// ============================================================
// Сбросить — простая трубка, повёрнутая вниз
// ============================================================

@Composable
fun CallEndIcon(
    palette: EtherealPalette,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val color = Color.White

        // Рисуем прямую трубку (горизонтальную) и поворачиваем на 135°
        val stroke = w * 0.18f
        val left = w * 0.2f
        val right = w * 0.8f
        val centerY = h * 0.5f

        // Основная часть трубки (прямая)
        drawLine(
            color = color,
            start = Offset(left + stroke, centerY - stroke / 2),
            end = Offset(right - stroke, centerY - stroke / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Концы трубки (загнутые вниз)
        drawLine(
            color = color,
            start = Offset(left + stroke / 2, centerY - stroke / 2),
            end = Offset(left + stroke / 2, centerY + stroke / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(right - stroke / 2, centerY - stroke / 2),
            end = Offset(right - stroke / 2, centerY + stroke / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Ответить — трубка, повёрнутая вверх
// ============================================================

@Composable
fun CallAnswerIcon(
    palette: EtherealPalette,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val color = Color.White

        val stroke = w * 0.18f
        val left = w * 0.2f
        val right = w * 0.8f
        val centerY = h * 0.5f

        drawLine(
            color = color,
            start = Offset(left + stroke, centerY + stroke / 2),
            end = Offset(right - stroke, centerY + stroke / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(left + stroke / 2, centerY - stroke / 2),
            end = Offset(left + stroke / 2, centerY + stroke / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(right - stroke / 2, centerY - stroke / 2),
            end = Offset(right - stroke / 2, centerY + stroke / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Аватар
// ============================================================

@Composable
fun AvatarIcon(
    palette: EtherealPalette,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val color = palette.primary

        drawCircle(
            color = color,
            radius = w * 0.18f,
            center = Offset(w / 2, h * 0.3f)
        )

        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.2f, h * 0.5f),
            size = Size(w * 0.6f, h * 0.5f)
        )
    }
}