package com.neytron.ethereal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Форма плитки.
 *
 * AUTO — «как в сетке»: берётся глобальная форма из настроек (и её радиус скругления).
 * Остальные значения — конкретная форма, выбранная для плитки.
 */
enum class TileShape {
    AUTO,
    ROUNDED,
    SQUIRCLE,
    CIRCLE,
    PILL,
    ARCH,
    LEAF,
    CLOVER,
    CUT,
    DIAMOND;

    companion object {
        fun fromName(raw: String?): TileShape =
            values().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AUTO

        /** Формы, которые имеют смысл как глобальный дефолт (без AUTO). */
        val explicit: List<TileShape> = values().filter { it != AUTO }
    }
}

/**
 * Собирает [Shape] для плитки.
 *
 * @param shape форма плитки (AUTO → [defaultShape])
 * @param radiusDp радиус скругления для ROUNDED (глобальная настройка сетки)
 * @param defaultShape глобальная форма из настроек
 */
fun tileShapeFor(
    shape: TileShape,
    radiusDp: Float,
    defaultShape: TileShape = TileShape.ROUNDED
): Shape {
    val resolved = when (shape) {
        TileShape.AUTO -> if (defaultShape == TileShape.AUTO) TileShape.ROUNDED else defaultShape
        else -> shape
    }
    return when (resolved) {
        TileShape.AUTO, TileShape.ROUNDED -> RoundedCornerShape(radiusDp.dp)
        else -> TilePathShape(resolved, radiusDp)
    }
}

/** Форма из произвольного пути: суперэллипс, арка, лист, клевер, срез, ромб, капсула. */
private class TilePathShape(
    private val type: TileShape,
    private val radiusDp: Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        val radiusPx = with(density) { radiusDp.dp.toPx() }.coerceIn(0f, min(w, h) / 2f)

        val path = when (type) {
            TileShape.SQUIRCLE -> squirclePath(w, h)
            TileShape.CIRCLE -> Path().apply { addOval(Rect(0f, 0f, w, h)) }
            TileShape.PILL -> Path().apply {
                addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(min(w, h) / 2f)))
            }
            TileShape.ARCH -> archPath(w, h)
            TileShape.LEAF -> leafPath(w, h)
            TileShape.CLOVER -> cloverPath(w, h)
            TileShape.CUT -> cutPath(w, h)
            TileShape.DIAMOND -> diamondPath(w, h)
            else -> Path().apply {
                addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(radiusPx)))
            }
        }
        return Outline.Generic(path)
    }
}

/** Настоящий суперэллипс (|x|^n + |y|^n = 1), n = 4 — «squircle», как у иконок iOS/Material. */
private fun squirclePath(w: Float, h: Float, exponent: Float = 4f, steps: Int = 128): Path {
    val path = Path()
    val cx = w / 2f
    val cy = h / 2f
    val e = 2f / exponent
    for (i in 0..steps) {
        val t = (2.0 * Math.PI * i / steps).toFloat()
        val ct = cos(t)
        val st = sin(t)
        val x = cx + cx * ct.sign * abs(ct).pow(e)
        val y = cy + cy * st.sign * abs(st).pow(e)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Арка: верх — полукруг, низ — прямой. */
private fun archPath(w: Float, h: Float): Path {
    val path = Path()
    val r = minOf(w / 2f, h)
    path.moveTo(0f, h)
    path.lineTo(0f, r)
    path.arcTo(Rect(0f, 0f, w, 2 * r), 180f, 180f, false)
    path.lineTo(w, h)
    path.close()
    return path
}

/** Лист: два противоположных угла скруглены сильно, два других — прямые. */
private fun leafPath(w: Float, h: Float): Path {
    val r = min(w, h) * 0.45f
    return Path().apply {
        addRoundRect(
            RoundRect(
                Rect(0f, 0f, w, h),
                topLeft = CornerRadius(r),
                topRight = CornerRadius(0f),
                bottomRight = CornerRadius(r),
                bottomLeft = CornerRadius(0f)
            )
        )
    }
}

/** Клевер: четыре лепестка, модуляция радиуса по углу. */
private fun cloverPath(
    w: Float,
    h: Float,
    lobes: Int = 4,
    amplitude: Float = 0.16f,
    steps: Int = 180
): Path {
    val path = Path()
    val cx = w / 2f
    val cy = h / 2f
    val norm = 1f + amplitude
    for (i in 0..steps) {
        val t = (2.0 * Math.PI * i / steps).toFloat()
        val r = (1f + amplitude * cos(lobes * t)) / norm
        val x = cx + cx * r * cos(t)
        val y = cy + cy * r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Восьмиугольник со срезанными углами. */
private fun cutPath(w: Float, h: Float): Path {
    val c = min(w, h) * 0.22f
    return Path().apply {
        moveTo(c, 0f)
        lineTo(w - c, 0f)
        lineTo(w, c)
        lineTo(w, h - c)
        lineTo(w - c, h)
        lineTo(c, h)
        lineTo(0f, h - c)
        lineTo(0f, c)
        close()
    }
}

/** Ромб. */
private fun diamondPath(w: Float, h: Float): Path = Path().apply {
    moveTo(w / 2f, 0f)
    lineTo(w, h / 2f)
    lineTo(w / 2f, h)
    lineTo(0f, h / 2f)
    close()
}
