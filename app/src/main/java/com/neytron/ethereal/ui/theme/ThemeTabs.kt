package com.neytron.ethereal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.R
import com.neytron.ethereal.data.SettingsRepository

private enum class ThemeTab { PRESETS, CUSTOM, WALLPAPER }

/**
 * Три способа выбрать тему: готовые палитры, своя (цвет + режим), цвета обоев.
 */
@Composable
fun ThemeTabs(
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit,
    wallpaperPalette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tab by remember {
        mutableStateOf(
            ThemeTab.values().getOrElse(SettingsRepository.getThemeTab(context)) { ThemeTab.PRESETS }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeTab.values().forEach { option ->
                val selected = option == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) currentPalette.primary.copy(alpha = 0.18f)
                            else currentPalette.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) currentPalette.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            tab = option
                            SettingsRepository.setThemeTab(context, option.ordinal)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            when (option) {
                                ThemeTab.PRESETS -> R.string.settings_theme_tab_presets
                                ThemeTab.CUSTOM -> R.string.settings_theme_tab_custom
                                ThemeTab.WALLPAPER -> R.string.settings_theme_tab_wallpaper
                            }
                        ),
                        color = if (selected) currentPalette.primary else currentPalette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (tab) {
            ThemeTab.PRESETS -> PresetsTab(currentPalette, onPaletteChange)
            ThemeTab.CUSTOM -> CustomTab(currentPalette, onPaletteChange)
            ThemeTab.WALLPAPER -> WallpaperTab(currentPalette, onPaletteChange, wallpaperPalette)
        }
    }
}

@Composable
private fun PresetsTab(
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit
) {
    // Wallpaper живёт на отдельной вкладке
    val presets = AllPalettes.filter { it.id != "wallpaper" }
    val dark = presets.filter { !it.isLight }
    val light = presets.filter { it.isLight }

    Column {
        Text(
            text = stringResource(R.string.settings_theme_dark_group),
            color = currentPalette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dark) { palette -> PaletteChip(palette, currentPalette, onPaletteChange) }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_theme_light_group),
            color = currentPalette.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(light) { palette -> PaletteChip(palette, currentPalette, onPaletteChange) }
        }
    }
}

@Composable
private fun PaletteChip(
    palette: EtherealPalette,
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit
) {
    val selected = palette.id == currentPalette.id
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) palette.primary.copy(alpha = 0.22f)
                else palette.surface.copy(alpha = 0.85f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.primary else palette.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onPaletteChange(palette) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Dot(palette.background)
            Dot(palette.primary)
            Dot(palette.secondary)
        }
        Text(
            text = palette.name,
            color = if (selected) palette.primary else palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Своя тема: три цвета + тумблер авто-подстройки.
 *
 * Color 1 — акцент, Color 2 — фон, Color 3 — вторичный акцент.
 * «Подстроить на основе Color 1» считает второй и третий цвет из первого.
 * Любой цвет можно выбрать пикером или вписать HEX.
 */
@Composable
private fun CustomTab(
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit
) {
    val initial = remember { themeFromPalette(currentPalette) }
    var theme by remember { mutableStateOf(initial) }
    var slot by remember { mutableStateOf(0) }

    fun apply(newTheme: SimpleTheme) {
        theme = newTheme
        onPaletteChange(buildSimpleTheme(newTheme))
    }

    val preview = remember(theme) { buildSimpleTheme(theme) }
    val colors = listOf(theme.color1, theme.color2, theme.color3)
    val editable = listOf(true, !theme.auto, !theme.auto)

    Column {
        // Режим
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { option ->
                val selected = option == theme.mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) currentPalette.primary.copy(alpha = 0.18f)
                            else currentPalette.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) currentPalette.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            apply(theme.copy(mode = option, pureBlack = option == ThemeMode.DARK && theme.pureBlack))
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) currentPalette.primary else currentPalette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Чистый чёрный — только для тёмной
        if (theme.mode == ThemeMode.DARK) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentPalette.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { apply(theme.copy(pureBlack = !theme.pureBlack)) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_pure_black),
                    color = currentPalette.textPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                ToggleDot(on = theme.pureBlack, palette = currentPalette)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Тумблер авто-подстройки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(currentPalette.surfaceVariant.copy(alpha = 0.3f))
                .clickable { apply(theme.copy(auto = !theme.auto)) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_theme_auto_from_color1),
                    color = currentPalette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.settings_theme_auto_hint),
                    color = currentPalette.textSecondary,
                    fontSize = 10.sp
                )
            }
            ToggleDot(on = theme.auto, palette = currentPalette)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Три цвета
        colors.forEachIndexed { index, color ->
            val isSlot = index == slot
            val canEdit = editable[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSlot && canEdit) currentPalette.primary.copy(alpha = 0.12f)
                        else currentPalette.surfaceVariant.copy(alpha = 0.25f)
                    )
                    .clickable(enabled = canEdit) { slot = index }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_theme_color_n, index + 1),
                    color = if (canEdit) currentPalette.textPrimary else currentPalette.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "#" + colorToHex(color),
                    color = if (canEdit) currentPalette.textSecondary else currentPalette.textSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Пикер для выбранного слота + HEX
        ColorPicker(
            selected = colors[slot.coerceIn(0, 2)],
            palette = currentPalette,
            onPick = { color ->
                val updated = when (slot) {
                    0 -> theme.copy(color1 = color)
                    1 -> theme.copy(color2 = color, auto = false)
                    else -> theme.copy(color3 = color, auto = false)
                }
                apply(updated)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        HexField(
            value = colorToHex(colors[slot.coerceIn(0, 2)]),
            palette = currentPalette,
            onValueChange = { hex ->
                colorFromHex(hex)?.let { color ->
                    val updated = when (slot) {
                        0 -> theme.copy(color1 = color)
                        1 -> theme.copy(color2 = color, auto = false)
                        else -> theme.copy(color3 = color, auto = false)
                    }
                    apply(updated)
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                preview.background,
                preview.surface,
                preview.primary,
                preview.secondary,
                preview.textPrimary
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun ToggleDot(on: Boolean, palette: EtherealPalette) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (on) palette.primary else palette.outline),
        contentAlignment = Alignment.Center
    ) {
        if (on) {
            Text(
                text = "✓",
                color = palette.background,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Поле ввода HEX: тапнул — правишь, ввёл валидный цвет — применилось. */
@Composable
private fun HexField(
    value: String,
    palette: EtherealPalette,
    onValueChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            color = palette.textSecondary,
            fontSize = 13.sp
        )
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val filtered = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
                text = filtered
                if (filtered.length == 6) onValueChange(filtered)
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = palette.textPrimary,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.primary),
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        )
    }
}


@Composable
private fun WallpaperTab(
    currentPalette: EtherealPalette,
    onPaletteChange: (EtherealPalette) -> Unit,
    wallpaperPalette: EtherealPalette
) {
    Column {
        Text(
            text = stringResource(R.string.settings_theme_wallpaper_hint),
            color = currentPalette.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                wallpaperPalette.background,
                wallpaperPalette.surface,
                wallpaperPalette.primary,
                wallpaperPalette.secondary,
                wallpaperPalette.textPrimary
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(currentPalette.primary)
                .clickable { onPaletteChange(wallpaperPalette.copy(id = "wallpaper")) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_theme_wallpaper_apply),
                color = currentPalette.background,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
    )
}