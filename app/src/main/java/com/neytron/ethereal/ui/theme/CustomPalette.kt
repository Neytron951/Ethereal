package com.neytron.ethereal.ui.theme

import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import kotlin.math.max
import kotlin.math.min

/**
 * Своя тема: три цвета и один тумблер.
 *
 *   Color 1 — акцент (кнопки, выделения, свечение)
 *   Color 2 — фон
 *   Color 3 — вторичный акцент
 *
 * «Подстроить тему на основе Color 1»: Color 2 и Color 3 считаются из первого,
 * так что достаточно выбрать один цвет. Выключишь тумблер — все три цвета
 * задаются руками.
 *
 * Внутри у палитры 8 токенов, но они выводятся из этих трёх: поверхности —
 * сдвиг фона к белому/чёрному, обводка — ещё сдвиг, текст — по контрасту.
 */
enum class ThemeMode(val label: String, val isLight: Boolean) {
    DARK("Dark", false),
    LIGHT("Light", true)
}

data class SimpleTheme(
    val mode: ThemeMode,
    val color1: Color,
    val color2: Color,
    val color3: Color,
    val auto: Boolean,
    val pureBlack: Boolean
)

/** id: custom:dark:FF3355-0A0A0A-7DDF64:a0  (режим, три цвета, флаги auto/black) */
fun simpleThemeId(theme: SimpleTheme): String {
    val flags = buildString {
        if (theme.auto) append('a')
        if (theme.pureBlack) append('b')
        if (isEmpty()) append('n')
    }
    return "custom:${theme.mode.name.lowercase()}:" +
        "${hexOf(theme.color1)}-${hexOf(theme.color2)}-${hexOf(theme.color3)}:$flags"
}

fun parseSimpleTheme(id: String): SimpleTheme? {
    if (!id.startsWith("custom:")) return null
    val parts = id.split(":")
    if (parts.size != 4) return null

    // Новый формат: custom:dark:RRGGBB-RRGGBB-RRGGBB:a0
    // Старые сборки писали custom:dark:red:normal и custom:dark:h27:c180:l62 —
    // их тоже понимаем, чтобы у людей не пропала своя тема после обновления.
    if (!parts[2].contains("-")) return parseLegacyTheme(parts)

    val mode = when (parts[1]) {
        "dark" -> ThemeMode.DARK
        "light" -> ThemeMode.LIGHT
        else -> return null
    }
    val colors = parts[2].split("-")
    if (colors.size != 3) return null
    val c1 = colorOf(colors[0]) ?: return null
    val c2 = colorOf(colors[1]) ?: return null
    val c3 = colorOf(colors[2]) ?: return null
    val flags = parts[3]
    return SimpleTheme(
        mode = mode,
        color1 = c1,
        color2 = c2,
        color3 = c3,
        auto = flags.contains('a'),
        pureBlack = flags.contains('b')
    )
}

/** Значения по умолчанию из текущей палитры — чтобы вкладка открывалась на ней. */
fun themeFromPalette(palette: EtherealPalette): SimpleTheme = SimpleTheme(
    mode = if (palette.isLight) ThemeMode.LIGHT else ThemeMode.DARK,
    color1 = palette.primary,
    color2 = palette.background,
    color3 = palette.secondary,
    auto = false,
    pureBlack = !palette.isLight && luminance(palette.background) < 0.001
)

/**
 * Пересчитывает всё из трёх цветов.
 *
 * В авто-режиме Color 2/3 игнорируются и выводятся из Color 1:
 * фон — тот же оттенок почти без насыщенности, вторичный — тот же оттенок посветлее.
 */
fun buildSimpleTheme(theme: SimpleTheme): EtherealPalette {
    val light = theme.mode.isLight
    val black = !light && theme.pureBlack

    val accent = theme.color1
    val background = when {
        theme.auto -> autoBackground(accent, light, black)
        else -> if (black) Color.Black else theme.color2
    }
    val secondary = if (theme.auto) autoSecondary(accent, light) else theme.color3

    val baseLight = luminance(background)
    val lightSurface = baseLight > 0.5

    val surface = mix(background, if (lightSurface) Color.Black else Color.White, 0.05f)
    val surfaceVariant = mix(background, if (lightSurface) Color.Black else Color.White, 0.10f)
    val outline = mix(background, if (lightSurface) Color.Black else Color.White, 0.22f)

    val textPrimary = if (lightSurface) Color(0xFF141414) else Color(0xFFF2F4F2)
    val textSecondary = if (lightSurface) Color(0xFF585858) else Color(0xFF9BA3A0)

    val primary = readable(accent, background, lightSurface)
    val secondaryReadable = readable(secondary, background, lightSurface)

    return EtherealPalette(
        id = simpleThemeId(theme),
        name = when {
            theme.auto -> "Custom Auto"
            lightSurface -> "Custom Light"
            black -> "Custom AMOLED"
            else -> "Custom Dark"
        },
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        primary = primary,
        secondary = secondaryReadable,
        outline = outline,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        isLight = lightSurface
    )
}

// ============================================================
// Производные цвета
// ============================================================

private fun autoBackground(accent: Color, light: Boolean, black: Boolean): Color {
    if (black) return Color.Black
    val hsv = hsvOf(accent)
    return Color.hsv(
        hsv[0],
        (hsv[1] * 0.16f).coerceIn(0f, 1f),
        if (light) 0.97f else 0.06f
    )
}

private fun autoSecondary(accent: Color, light: Boolean): Color {
    val hsv = hsvOf(accent)
    return Color.hsv(
        hsv[0],
        (hsv[1] * 0.72f).coerceIn(0f, 1f),
        if (light) (hsv[2] * 0.82f).coerceIn(0f, 1f) else (hsv[2] * 0.92f).coerceIn(0f, 1f)
    )
}

private fun mix(a: Color, b: Color, amount: Float): Color = Color(
    red = a.red + (b.red - a.red) * amount,
    green = a.green + (b.green - a.green) * amount,
    blue = a.blue + (b.blue - a.blue) * amount,
    alpha = 1f
)

private fun hsvOf(color: Color): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(argbOf(color), hsv)
    return hsv
}

private fun argbOf(color: Color): Int = AndroidColor.rgb(
    (color.red * 255).toInt().coerceIn(0, 255),
    (color.green * 255).toInt().coerceIn(0, 255),
    (color.blue * 255).toInt().coerceIn(0, 255)
)

private fun hexOf(color: Color): String =
    String.format("%02X%02X%02X", (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())

private fun colorOf(hex: String): Color? {
    if (hex.length != 6) return null
    val rgb = hex.toLongOrNull(16) ?: return null
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f
    )
}

fun colorToHex(color: Color): String = hexOf(color)

fun colorFromHex(hex: String): Color? {
    val cleaned = hex.trim().removePrefix("#")
    return colorOf(cleaned.uppercase())
}

fun luminance(color: Color): Double {
    fun channel(v: Float): Double {
        val x = v.toDouble()
        return if (x <= 0.03928) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

fun contrastRatio(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/** Подтягивает цвет по яркости, пока он не станет читаемым на фоне. Оттенок не меняем. */
private fun readable(color: Color, background: Color, lightBackground: Boolean): Color {
    if (contrastRatio(color, background) >= 4.5) return color
    val hsv = hsvOf(color)
    var value = hsv[2]
    var result = color
    repeat(20) {
        value = if (lightBackground) (value - 0.04f).coerceAtLeast(0f) else (value + 0.04f).coerceAtMost(1f)
        result = Color.hsv(hsv[0], hsv[1], value)
        if (contrastRatio(result, background) >= 4.5) return result
    }
    return result
}
/**
 * Миграция старых id кастомных палитр.
 *
 *   custom:<base>:<accent>:<intensity>   — база/акцент/насыщенность
 *   custom:<base>:h<hue>:c<chroma>[:l<n>] — свободный цвет
 *
 * Возвращаем эквивалент в новой модели: один цвет + режим, остальное авто.
 */
private fun parseLegacyTheme(parts: List<String>): SimpleTheme? {
    val mode = when (parts[1]) {
        "amoled" -> ThemeMode.DARK
        "paper" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        "light" -> ThemeMode.LIGHT
        else -> return null
    }
    val pureBlack = parts[1] == "amoled"

    val accentPart = parts.getOrNull(2) ?: return null
    val color = if (accentPart.startsWith("h")) {
        // Свободный цвет: угол оттенка и хрома
        val hue = accentPart.removePrefix("h").toFloatOrNull() ?: return null
        val chroma = (parts.getOrNull(3)?.removePrefix("c")?.toFloatOrNull() ?: 150f) / 1000f
        val saturation = (chroma / 0.28f).coerceIn(0.25f, 1f)
        Color.hsv(((hue % 360f) + 360f) % 360f, saturation, 0.95f)
    } else {
        val hue = LEGACY_ACCENT_HUES[accentPart] ?: return null
        val saturation = LEGACY_INTENSITY_SATURATION[parts.getOrNull(3) ?: "normal"] ?: 0.80f
        Color.hsv(hue, saturation, 0.95f)
    }

    return SimpleTheme(
        mode = mode,
        color1 = color,
        color2 = color,
        color3 = color,
        auto = true,
        pureBlack = pureBlack
    )
}

private val LEGACY_ACCENT_HUES = mapOf(
    "green" to 120f,
    "lime" to 85f,
    "yellow" to 55f,
    "orange" to 30f,
    "red" to 0f,
    "pink" to 335f,
    "magenta" to 310f,
    "violet" to 280f,
    "indigo" to 250f,
    "blue" to 220f,
    "cyan" to 190f,
    "teal" to 175f
)

private val LEGACY_INTENSITY_SATURATION = mapOf(
    "muted" to 0.55f,
    "normal" to 0.80f,
    "vivid" to 1.0f
)