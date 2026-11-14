package com.neytron.ethereal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.R
import com.neytron.ethereal.data.UpdateInfo
import com.neytron.ethereal.ui.theme.EtherealPalette
import androidx.compose.ui.res.stringResource

/**
 * Центр обновлений.
 *
 * Показывает бренд, переход версий, метаданные релиза (дата, размер APK) и заметки
 * к релизу, отрисованные собственным мини-рендерером [ReleaseNotesView] — поэтому они
 * видны всегда, в отличие от прежнего markdown-рендера, который падал на HTML-разметке.
 */
@Composable
fun UpdateOverlay(
    visible: Boolean,
    info: UpdateInfo?,
    palette: EtherealPalette,
    onDismiss: () -> Unit
) {
    if (info == null) return

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = tween(280)
                ),
        exit = fadeOut(animationSpec = tween(160)) +
                slideOutVertically(
                    targetOffsetY = { it / 12 },
                    animationSpec = tween(180)
                )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(28.dp),
                color = palette.background.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, palette.surfaceVariant)
            ) {
                var entered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    entered = true
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    UpdateHeader(palette = palette, onDismiss = onDismiss)

                    Spacer(modifier = Modifier.height(16.dp))

                    VersionHero(
                        info = info,
                        palette = palette,
                        modifier = Modifier.reveal(entered, 60)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NotesCard(
                        info = info,
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .reveal(entered, 120)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    UpdateButtons(
                        info = info,
                        palette = palette,
                        onDismiss = onDismiss,
                        modifier = Modifier.reveal(entered, 180)
                    )
                }
            }
        }
    }
}

// ============================================================
// Появление с задержкой
// ============================================================

@Composable
private fun Modifier.reveal(entered: Boolean, delayMs: Int): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = delayMs),
        label = "revealAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 16f,
        animationSpec = tween(durationMillis = 320, delayMillis = delayMs),
        label = "revealOffset"
    )
    return this.alpha(alpha).offset(y = offsetY.dp)
}

// ============================================================
// Шапка: марка + название + закрыть
// ============================================================

@Composable
private fun UpdateHeader(
    palette: EtherealPalette,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = com.neytron.ethereal.R.drawable.ic_logo),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(palette.primary),
            modifier = Modifier.size(34.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ethereal",
                color = palette.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.update_center),
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================
// Версии + метаданные
// ============================================================

@Composable
private fun VersionHero(
    info: UpdateInfo,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val hasUpdate = info.hasUpdate
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VersionBadge(
                text = info.currentVersion.display(),
                palette = palette,
                highlighted = false
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (hasUpdate) "→" else "·",
                color = palette.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(10.dp))
            VersionBadge(
                text = info.latestVersion.display(),
                palette = palette,
                highlighted = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (hasUpdate) {
                stringResource(R.string.update_available_title)
            } else {
                stringResource(R.string.update_up_to_date)
            },
            color = if (hasUpdate) palette.primary else palette.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        val meta = buildList {
            val date = formatDate(info.publishedAt)
            if (date.isNotBlank()) add(stringResource(R.string.update_released_on, date))
            val size = formatSize(info.apkSizeBytes)
            if (size.isNotBlank()) add(size)
        }
        if (meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = meta.joinToString("  ·  "),
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VersionBadge(
    text: String,
    palette: EtherealPalette,
    highlighted: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlighted) palette.primary.copy(alpha = 0.16f)
                else palette.surfaceVariant.copy(alpha = 0.4f)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (highlighted) palette.primary else palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================
// Заметки к релизу
// ============================================================

@Composable
private fun NotesCard(
    info: UpdateInfo,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val blocks = remember(info.changelog) { ReleaseNotes.parse(info.changelog) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.22f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.update_changelog_title).uppercase(),
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f)
            )
            if (blocks.isNotEmpty()) {
                Text(
                    text = "${blocks.size}",
                    color = palette.textSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (blocks.isEmpty()) {
            Text(
                text = stringResource(R.string.update_empty_notes),
                color = palette.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                ReleaseNotesView(blocks = blocks, palette = palette)
            }
        }
    }
}

// ============================================================
// Кнопки
// ============================================================

@Composable
private fun UpdateButtons(
    info: UpdateInfo,
    palette: EtherealPalette,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    val target = if (info.hasUpdate) {
        info.apkUrl.ifBlank { info.htmlUrl }
    } else {
        info.htmlUrl
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.update_later),
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .weight(1.5f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(palette.primary, palette.primary.copy(alpha = 0.82f))
                    )
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (target.isNotBlank()) {
                        try {
                            uriHandler.openUri(target)
                        } catch (_: Exception) {
                        }
                    }
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (info.hasUpdate) {
                    stringResource(R.string.update_download)
                } else {
                    stringResource(R.string.update_open_github)
                },
                color = palette.background,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// ============================================================
// Форматирование
// ============================================================

private fun formatDate(iso: String): String {
    // 2026-11-09T01:23:59Z -> 09.11.2026
    if (iso.length < 10) return ""
    val parts = iso.take(10).split("-")
    if (parts.size != 3) return ""
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("%.1f MB", mb)
}
