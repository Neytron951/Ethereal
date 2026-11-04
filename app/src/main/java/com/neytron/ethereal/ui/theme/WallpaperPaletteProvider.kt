package com.neytron.ethereal.ui.theme

import android.app.WallpaperManager
import android.content.Context
import androidx.compose.ui.graphics.Color

object WallpaperPaletteProvider {
    fun load(context: Context): EtherealPalette {
        val wallpaperColors = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?: return WallpaperPalette

        val primaryHsv = FloatArray(3)
        val primaryColor = wallpaperColors.primaryColor.toArgb()
        android.graphics.Color.colorToHSV(primaryColor, primaryHsv)

        val secondarySource = wallpaperColors.secondaryColor
            ?: wallpaperColors.tertiaryColor
            ?: wallpaperColors.primaryColor
        val secondaryHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(secondarySource.toArgb(), secondaryHsv)

        if (primaryHsv[1] < 0.12f && secondaryHsv[1] < 0.12f) {
            return WallpaperPalette.copy(
                background = Color(0xFF090909),
                surface = Color(0xFF141414),
                surfaceVariant = Color(0xFF202020),
                primary = Color(0xFFD0D0D0),
                secondary = Color(0xFF969696),
                outline = Color(0xFF383838),
                textPrimary = Color(0xFFF2F2F2),
                textSecondary = Color(0xFFB0B0B0)
            )
        }

        val primary = Color.hsv(
            hue = primaryHsv[0],
            saturation = primaryHsv[1],
            value = maxOf(primaryHsv[2], 0.82f)
        )
        val secondary = Color.hsv(
            hue = secondaryHsv[0],
            saturation = secondaryHsv[1],
            value = maxOf(secondaryHsv[2], 0.72f)
        )

        return WallpaperPalette.copy(
            background = Color.hsv(primaryHsv[0], primaryHsv[1] * 0.28f, 0.055f),
            surface = Color.hsv(primaryHsv[0], primaryHsv[1] * 0.34f, 0.09f),
            surfaceVariant = Color.hsv(primaryHsv[0], primaryHsv[1] * 0.42f, 0.15f),
            primary = primary,
            secondary = secondary,
            outline = Color.hsv(primaryHsv[0], primaryHsv[1] * 0.55f, 0.27f),
            textPrimary = Color(0xFFF2F2F2),
            textSecondary = Color(0xFFB0B0B0)
        )
    }
}
