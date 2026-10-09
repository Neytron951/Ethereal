package com.neytron.ethereal.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlin.math.abs
import kotlin.math.pow

/**
 * Покраска иконки в стиль Material You / EtherealTheme.
 *
 * Уровень 1: Использование системного слоя <monochrome> в AdaptiveIconDrawable (Android 13+).
 * Уровень 2: Умное извлечение монохромного силуэта из foreground слоя AdaptiveIconDrawable.
 * Уровень 3: Извлечение силуэта из legacy Drawable (PNG/Vector).
 */
fun renderThemedIcon(
    drawable: Drawable,
    palette: EtherealPalette,
    sizePx: Int = 256,
): Bitmap {

    // ============ Уровень 1: <monochrome> (Android 13+) ============
    if ((Build.VERSION.SDK_INT >= 33) && (drawable is AdaptiveIconDrawable)) {
        val mono = try { drawable.monochrome } catch (_: Exception) { null }
        if (mono != null) {
            return renderMonochromeLayer(mono, palette, sizePx)
        }
    }

    // ============ Уровень 2: AdaptiveIconDrawable без <monochrome> ============
    if (drawable is AdaptiveIconDrawable) {
        val fg = try { drawable.foreground } catch (_: Exception) { null }
        if (fg != null) {
            return extractAndRenderMonochrome(fg, palette, sizePx, isAdaptiveFg = true)
        }
    }

    // ============ Уровень 3: Legacy PNG / Drawable ============
    return extractAndRenderMonochrome(drawable, palette, sizePx, isAdaptiveFg = false)
}

// ---------------------------------------------------------------------------
// УРОВЕНЬ 1: Рисуем оригинальный monochrome слой на фоновом контейнере
// ---------------------------------------------------------------------------
private fun renderMonochromeLayer(
    monochrome: Drawable,
    palette: EtherealPalette,
    sizePx: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1) Фоновый контейнер
    canvas.drawColor(palette.surfaceVariant.toArgb())

    // 2) Monochrome слой — масштаб 1.48f для сочной и выразительной подачи
    val scaleFactor = 1.48f

    canvas.save()
    canvas.translate(sizePx / 2f, sizePx / 2f)
    canvas.scale(scaleFactor, scaleFactor)
    canvas.translate(-sizePx / 2f, -sizePx / 2f)

    monochrome.colorFilter = PorterDuffColorFilter(palette.primary.toArgb(), PorterDuff.Mode.SRC_IN)
    monochrome.setBounds(0, 0, sizePx, sizePx)
    monochrome.draw(canvas)
    monochrome.colorFilter = null

    canvas.restore()

    return bitmap
}

// ---------------------------------------------------------------------------
// УРОВЕНЬ 2 и 3: Извлечение монохромного символа и отрисовка в теме
// ---------------------------------------------------------------------------
private fun extractAndRenderMonochrome(
    drawable: Drawable,
    palette: EtherealPalette,
    sizePx: Int,
    isAdaptiveFg: Boolean,
): Bitmap {
    // 1) Отрисовываем исходный слой
    val fgBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val fgCanvas = Canvas(fgBitmap)

    val scaleFactor = if (isAdaptiveFg) 1.48f else 1.0f
    fgCanvas.save()
    fgCanvas.translate(sizePx / 2f, sizePx / 2f)
    fgCanvas.scale(scaleFactor, scaleFactor)
    fgCanvas.translate(-sizePx / 2f, -sizePx / 2f)

    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(fgCanvas)
    fgCanvas.restore()

    // 2) Авто-извлечение контрастной монохромной маски символа
    val monoMask = generateMonochromeMask(fgBitmap)

    // 3) Итоговый результат: контейнер surfaceVariant + четкий силуэт primary
    val result = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(palette.surfaceVariant.toArgb())

    val paint = Paint().apply {
        isAntiAlias = true
        colorFilter = PorterDuffColorFilter(
            palette.primary.toArgb(),
            PorterDuff.Mode.SRC_IN,
        )
    }
    canvas.drawBitmap(monoMask, 0f, 0f, paint)

    return result
}

/**
 * Генерация яркой и контрастной монохромной маски с отделением фоновой плашки (Plate)
 * и усилением контрастности деталей.
 */
private fun generateMonochromeMask(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)

    val alphas = IntArray(w * h)
    val lums = FloatArray(w * h)
    var opaqueCount = 0

    for (i in pixels.indices) {
        val p = pixels[i]
        val a = Color.alpha(p)
        alphas[i] = a
        if (a > 30) {
            opaqueCount++
            val r = Color.red(p) / 255f
            val g = Color.green(p) / 255f
            val b = Color.blue(p) / 255f
            lums[i] = 0.2126f * r + 0.7152f * g + 0.0722f * b
        } else {
            lums[i] = 0f
        }
    }

    if (opaqueCount == 0) return src

    // Сканируем внешнюю границу для определения фоновой плашки (Plate Color)
    var plateRedSum = 0f
    var plateGreenSum = 0f
    var plateBlueSum = 0f
    var plateLumSum = 0f
    var borderSampleCount = 0

    val step = (h / 32).coerceAtLeast(1)
    for (y in 0 until h step step) {
        // Слева направо
        for (x in 0 until w) {
            val idx = y * w + x
            if (alphas[idx] > 180) {
                val p = pixels[idx]
                plateRedSum += Color.red(p)
                plateGreenSum += Color.green(p)
                plateBlueSum += Color.blue(p)
                plateLumSum += lums[idx]
                borderSampleCount++
                break
            }
        }
        // Справа налево
        for (x in w - 1 downTo 0) {
            val idx = y * w + x
            if (alphas[idx] > 180) {
                val p = pixels[idx]
                plateRedSum += Color.red(p)
                plateGreenSum += Color.green(p)
                plateBlueSum += Color.blue(p)
                plateLumSum += lums[idx]
                borderSampleCount++
                break
            }
        }
    }

    if (borderSampleCount == 0) borderSampleCount = 1
    val avgPlateR = plateRedSum / borderSampleCount / 255f
    val avgPlateG = plateGreenSum / borderSampleCount / 255f
    val avgPlateB = plateBlueSum / borderSampleCount / 255f
    val avgPlateLum = plateLumSum / borderSampleCount

    // Расчет цветового и яркостного отличия (delta) от фоновой плашки
    val deltas = FloatArray(w * h)
    var symbolCount = 0
    var plateCount = 0

    for (i in pixels.indices) {
        if (alphas[i] <= 30) continue

        val p = pixels[i]
        val r = Color.red(p) / 255f
        val g = Color.green(p) / 255f
        val b = Color.blue(p) / 255f

        val colorDiff = (abs(r - avgPlateR) + abs(g - avgPlateG) + abs(b - avgPlateB)) / 3f
        val lumDiff = abs(lums[i] - avgPlateLum)
        val delta = lumDiff * 0.6f + colorDiff * 0.4f
        deltas[i] = delta

        if (delta >= 0.15f) {
            symbolCount++
        } else {
            plateCount++
        }
    }

    val hasPlate = (plateCount > (w * h * 0.16f)) && (symbolCount > (w * h * 0.005f))

    val outPixels = IntArray(w * h)

    if (hasPlate) {
        // ТИП A: Есть подложка (Telegram, YouTube, Spotify, WhatsApp) -> четкий, плотный вывод символа
        for (i in pixels.indices) {
            val a = alphas[i]
            if (a <= 30) continue

            val delta = deltas[i]
            // Крутая и четкая крутизна контраста (0.06f .. 0.13f) для максимальной яркости и плотности символа
            val symbolAlphaFactor = ((delta - 0.06f) / 0.07f).coerceIn(0f, 1f)
            val finalAlpha = (a * symbolAlphaFactor).toInt()

            if (finalAlpha > 0) {
                outPixels[i] = Color.argb(finalAlpha, 255, 255, 255)
            }
        }
    } else {
        // ТИП B: Нет подложки (значки без подложки или полноцветные иконки) -> гамма-усиление средних тонов
        var maxLum = 0f
        var minLum = 1f
        for (i in pixels.indices) {
            if (alphas[i] > 30) {
                if (lums[i] > maxLum) maxLum = lums[i]
                if (lums[i] < minLum) minLum = lums[i]
            }
        }
        val lumRange = (maxLum - minLum).coerceAtLeast(0.2f)

        for (i in pixels.indices) {
            val a = alphas[i]
            if (a <= 30) continue

            val normLum = ((lums[i] - minLum) / lumRange).coerceIn(0f, 1f)
            // Гамма-коррекция (pow 0.5) для выделения и подсветки средних тонов и деталей
            val boostedLum = normLum.toDouble().pow(0.5).toFloat()
            val maskVal = (255 * (0.20f + 0.80f * boostedLum) * (a / 255f)).toInt().coerceIn(0, 255)

            outPixels[i] = Color.argb(maskVal, 255, 255, 255)
        }
    }

    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(outPixels, 0, w, 0, 0, w, h)
    return out
}
