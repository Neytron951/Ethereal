package com.neytron.ethereal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

@Composable
fun EtherealLauncherTheme(
    palette: EtherealPalette = EtherealGreenPalette,
    fontFamily: FontFamily? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.secondary,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.background,
            secondary = palette.secondary,
            onSecondary = palette.background,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline
        )
    }

    // Применяем кастомный шрифт ко ВСЕМ текстам через Typography
    val base = Typography()
    val typography = if (fontFamily != null) {
        Typography(
            displayLarge   = base.displayLarge.copy(fontFamily = fontFamily),
            displayMedium  = base.displayMedium.copy(fontFamily = fontFamily),
            displaySmall   = base.displaySmall.copy(fontFamily = fontFamily),
            headlineLarge  = base.headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall  = base.headlineSmall.copy(fontFamily = fontFamily),
            titleLarge     = base.titleLarge.copy(fontFamily = fontFamily),
            titleMedium    = base.titleMedium.copy(fontFamily = fontFamily),
            titleSmall     = base.titleSmall.copy(fontFamily = fontFamily),
            bodyLarge      = base.bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium     = base.bodyMedium.copy(fontFamily = fontFamily),
            bodySmall      = base.bodySmall.copy(fontFamily = fontFamily),
            labelLarge     = base.labelLarge.copy(fontFamily = fontFamily),
            labelMedium    = base.labelMedium.copy(fontFamily = fontFamily),
            labelSmall     = base.labelSmall.copy(fontFamily = fontFamily)
        )
    } else {
        base
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}