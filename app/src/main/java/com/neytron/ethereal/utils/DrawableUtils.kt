package com.neytron.ethereal.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb

/**
 * Конвертирует Drawable в ImageBitmap.
 *
 * @param sizePx размер итогового Bitmap
 * @param bgTint если не null — фон иконки заливается этим цветом
 * @param fgTint если не null — логотип иконки красится в этот цвет
 */
fun Drawable.toImageBitmap(
    sizePx: Int = 256,
    bgTint: androidx.compose.ui.graphics.Color? = null,
    fgTint: androidx.compose.ui.graphics.Color? = null
): ImageBitmap? {
    return when (this) {
        is BitmapDrawable -> {
            // BitmapDrawable не поддерживает tint-разделение —
            // если нужен tint, применяем ко всей иконке
            if (bgTint != null || fgTint != null) {
                val tint = fgTint ?: bgTint
                val bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(bmp)
                val paint = android.graphics.Paint().apply {
                    colorFilter = PorterDuffColorFilter(
                        tint!!.toArgb(),
                        PorterDuff.Mode.SRC_IN
                    )
                }
                canvas.drawBitmap(bmp, 0f, 0f, paint)
                bmp.asImageBitmap()
            } else {
                bitmap.asImageBitmap()
            }
        }
        is AdaptiveIconDrawable -> renderAdaptive(this, sizePx, bgTint, fgTint)
        else -> renderNormal(this)
    }
}

private fun renderAdaptive(
    drawable: AdaptiveIconDrawable,
    sizePx: Int,
    bgTint: androidx.compose.ui.graphics.Color?,
    fgTint: androidx.compose.ui.graphics.Color?
): ImageBitmap? {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bg: Drawable? = try { drawable.background } catch (e: Exception) { null }
    val fg: Drawable? = try { drawable.foreground } catch (e: Exception) { null }

    if (bg == null && fg == null) {
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    // === 1) Фон ===
    if (bgTint != null) {
        // Заливаем сплошным цветом, игнорируя оригинальный слой
        canvas.drawColor(bgTint.toArgb())
    } else {
        bg?.let {
            it.setBounds(0, 0, sizePx, sizePx)
            it.draw(canvas)
        }
    }

    // === 2) Логотип ===
    fg?.let {
        val scaleFactor = 1.5f
        canvas.save()
        canvas.translate(sizePx / 2f, sizePx / 2f)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(-sizePx / 2f, -sizePx / 2f)

        if (fgTint != null) {
            // Красим логотип в один цвет
            it.setColorFilter(
                PorterDuffColorFilter(fgTint.toArgb(), PorterDuff.Mode.SRC_IN)
            )
        }

        it.setBounds(0, 0, sizePx, sizePx)
        it.draw(canvas)

        // Сбрасываем filter
        it.setColorFilter(null)

        canvas.restore()
    }

    return bitmap.asImageBitmap()
}

private fun renderNormal(drawable: Drawable): ImageBitmap? {
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)

    return bitmap.asImageBitmap()
}