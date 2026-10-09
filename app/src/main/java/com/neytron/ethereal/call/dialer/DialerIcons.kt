package com.neytron.ethereal.call.dialer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// Стрелка вверх (исходящий) ↗
// ============================================================

@Composable
fun ArrowUpRightIcon(
    color: Color,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        // Линия из левого-нижнего в правый-верхний
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.8f),
            end = Offset(w * 0.8f, h * 0.2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Стрелка вверх
        drawLine(
            color = color,
            start = Offset(w * 0.8f, h * 0.2f),
            end = Offset(w * 0.55f, h * 0.2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Стрелка вправо
        drawLine(
            color = color,
            start = Offset(w * 0.8f, h * 0.2f),
            end = Offset(w * 0.8f, h * 0.45f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Стрелка вниз-влево (входящий / пропущенный) ↙
// ============================================================

@Composable
fun ArrowDownLeftIcon(
    color: Color,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        // Линия из правого-верхнего в левый-нижний
        drawLine(
            color = color,
            start = Offset(w * 0.8f, h * 0.2f),
            end = Offset(w * 0.2f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Стрелка вниз
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.8f),
            end = Offset(w * 0.2f, h * 0.55f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Стрелка влево
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.8f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Трубка (кнопка "Позвонить")
// ============================================================

@Composable
fun DialerCallIcon(
    color: Color,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.14f

        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.2f)
            quadraticBezierTo(w * 0.15f, h * 0.15f, w * 0.15f, h * 0.35f)
            quadraticBezierTo(w * 0.25f, h * 0.75f, w * 0.65f, h * 0.85f)
            quadraticBezierTo(w * 0.85f, h * 0.85f, w * 0.8f, h * 0.75f)
            lineTo(w * 0.65f, h * 0.65f)
            lineTo(w * 0.5f, h * 0.7f)
            quadraticBezierTo(w * 0.35f, h * 0.65f, w * 0.3f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.35f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

// ============================================================
// Backspace (⌫)
// ============================================================

@Composable
fun BackspaceIcon(
    color: Color,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.08f

        // Прямоугольник со скошенным левым углом
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.75f)
            lineTo(w * 0.35f, h * 0.75f)
            lineTo(w * 0.15f, h * 0.5f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Крестик внутри
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.4f),
            end = Offset(w * 0.7f, h * 0.6f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.7f, h * 0.4f),
            end = Offset(w * 0.5f, h * 0.6f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Лупа (поиск)
// ============================================================

@Composable
fun SearchIcon(
    color: Color,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.1f
        val radius = w * 0.28f
        val center = Offset(w * 0.42f, h * 0.42f)

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.85f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Таб-иконки
// ============================================================

@Composable
fun TabCallsIcon(
    color: Color,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        // Простая трубка
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.2f)
            quadraticBezierTo(w * 0.15f, h * 0.15f, w * 0.15f, h * 0.35f)
            quadraticBezierTo(w * 0.25f, h * 0.75f, w * 0.65f, h * 0.85f)
            quadraticBezierTo(w * 0.85f, h * 0.85f, w * 0.8f, h * 0.75f)
            lineTo(w * 0.65f, h * 0.65f)
            lineTo(w * 0.5f, h * 0.7f)
            quadraticBezierTo(w * 0.35f, h * 0.65f, w * 0.3f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.35f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TabKeypadIcon(
    color: Color,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w * 0.09f
        val startX = w * 0.22f
        val startY = h * 0.22f
        val stepX = w * 0.28f
        val stepY = h * 0.28f

        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = color,
                    radius = radius,
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
// Аватарка-заглушка (если нет фото)
// ============================================================

@Composable
fun DefaultAvatarIcon(
    color: Color,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Голова
        drawCircle(
            color = color,
            radius = w * 0.18f,
            center = Offset(w / 2f, h * 0.35f)
        )
        // Тело
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.22f, h * 0.55f),
            size = Size(w * 0.56f, h * 0.5f)
        )
    }
}