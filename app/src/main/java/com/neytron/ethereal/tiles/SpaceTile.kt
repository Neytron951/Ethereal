package com.neytron.ethereal.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.neytron.ethereal.ui.theme.EtherealPalette
import java.io.File

/**
 * Внутренний контент плитки-пространства.
 * Рисуется поверх фона плитки (фон даёт родительский TileCell).
 *
 * Приоритет иконки: iconUri (картинка) > iconText (эмодзи/символ) > первая буква name.
 * Под иконкой — имя пространства (если showLabel == true).
 */
@Composable
fun SpaceTileContent(
    config: SpaceConfig,
    tileSize: Dp,
    palette: EtherealPalette,
    contentScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val iconSize = if (config.showLabel) {
        (tileSize * 0.45f * contentScale).coerceAtLeast(20.dp)
    } else {
        (tileSize * 0.65f * contentScale).coerceAtLeast(24.dp)
    }
    val labelSize = (tileSize.value * 0.16f).coerceIn(9f, 14f).sp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (config.showLabel) 6.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ИКОНКА
        Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center
        ) {
            when {
                !config.iconUri.isNullOrEmpty() -> {
                    val file = remember(config.iconUri) { File(config.iconUri) }
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = config.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        FallbackIcon(config = config, palette = palette)
                    }
                }
                !config.iconText.isNullOrEmpty() -> {
                    Text(
                        text = config.iconText,
                        fontSize = (tileSize.value * (if (config.showLabel) 0.34f else 0.45f)).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                else -> {
                    FallbackIcon(config = config, palette = palette)
                }
            }
        }

        if (config.showLabel) {
            Spacer(modifier = Modifier.height(4.dp))

            // ИМЯ
            Text(
                text = config.name,
                color = palette.textPrimary,
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FallbackIcon(
    config: SpaceConfig,
    palette: EtherealPalette
) {
    val letter = config.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Text(
        text = letter,
        color = palette.primary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}
