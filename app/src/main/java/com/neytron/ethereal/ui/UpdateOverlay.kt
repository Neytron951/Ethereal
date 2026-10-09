package com.neytron.ethereal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.neytron.ethereal.R
import com.neytron.ethereal.data.UpdateInfo
import com.neytron.ethereal.ui.theme.EtherealPalette

/**
 * Оверлей с информацией об обновлении.
 *
 * Показывает:
 *  1. Переход версий: vCURRENT → vLATEST
 *  2. Changelog (markdown, из GitHub release body)
 *     — свёрнут по умолчанию (~6 строк), кнопка "Развернуть"
 *     — раскрытый скроллится внутри, максимум ~60% высоты экрана
 *  3. Кнопки: [Обновить] → открывает html_url в браузере, [Закрыть]
 *
 * Рисуется как Surface поверх контента, с затемнением фона.
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
        enter = fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = tween(280)
                ),
        exit = fadeOut(animationSpec = tween(180)) +
                slideOutVertically(
                    targetOffsetY = { it / 8 },
                    animationSpec = tween(200)
                )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
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
                    .fillMaxHeight(0.82f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                color = palette.background.copy(alpha = 0.98f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    palette.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    UpdateHeader(
                        info = info,
                        palette = palette,
                        onDismiss = onDismiss
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ChangelogSection(
                        changelog = info.changelog,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    UpdateButtons(
                        palette = palette,
                        htmlUrl = info.htmlUrl,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

// ============================================================
// Header: заголовок + переход версий
// ============================================================

@Composable
private fun UpdateHeader(
    info: UpdateInfo,
    palette: EtherealPalette,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.update_title),
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
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

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VersionBadge(
            text = info.currentVersion.display(),
            palette = palette,
            isLatest = false
        )

        Text(
            text = "→",
            color = palette.textSecondary,
            fontSize = 14.sp
        )

        VersionBadge(
            text = info.latestVersion.display(),
            palette = palette,
            isLatest = true
        )
    }
}

@Composable
private fun VersionBadge(
    text: String,
    palette: EtherealPalette,
    isLatest: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isLatest) palette.primary.copy(alpha = 0.15f)
                else palette.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (isLatest) 1.dp else 0.dp,
                color = if (isLatest) palette.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (isLatest) palette.primary else palette.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ============================================================
// Changelog: markdown + свёрнутость
// ============================================================

@Composable
private fun ChangelogSection(
    changelog: String,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val trimmed = changelog.trim()

    if (trimmed.isBlank()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.update_no_changelog),
                color = palette.textSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = palette.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.update_changelog_title),
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        val maxCollapsedHeight = 140.dp
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (expanded) {
                        Modifier
                            .fillMaxHeight(0.9f)
                            .verticalScroll(scrollState)
                    } else {
                        Modifier
                            .heightIn(max = maxCollapsedHeight)
                            .verticalScroll(scrollState, enabled = false)
                    }
                )
        ) {
            Markdown(
                content = trimmed,
                colors = markdownColor(
                    text = palette.textPrimary,
                    codeText = palette.primary,
                    codeBackground = palette.surface,
                    inlineCodeBackground = palette.surface,
                    dividerColor = palette.outline
                ),
                typography = markdownTypography(
                    text = androidx.compose.ui.text.TextStyle(
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    h1 = androidx.compose.ui.text.TextStyle(
                        color = palette.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    h2 = androidx.compose.ui.text.TextStyle(
                        color = palette.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    h3 = androidx.compose.ui.text.TextStyle(
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    paragraph = androidx.compose.ui.text.TextStyle(
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    bullet = androidx.compose.ui.text.TextStyle(
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ExpandToggle(
            expanded = expanded,
            palette = palette,
            onClick = { expanded = !expanded }
        )
    }
}

@Composable
private fun ExpandToggle(
    expanded: Boolean,
    palette: EtherealPalette,
    onClick: () -> Unit
) {
    val arrowAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(200),
        label = "arrow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (expanded)
                stringResource(R.string.update_collapse)
            else
                stringResource(R.string.update_expand),
            color = palette.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (expanded) "▲" else "▼",
            color = palette.primary,
            fontSize = 11.sp,
            modifier = Modifier.alpha(arrowAlpha)
        )
    }
}

// ============================================================
// Кнопки
// ============================================================

@Composable
private fun UpdateButtons(
    palette: EtherealPalette,
    htmlUrl: String,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.close),
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .weight(1.4f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.primary)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (htmlUrl.isNotBlank()) {
                        try {
                            uriHandler.openUri(htmlUrl)
                        } catch (_: Exception) {}
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.update_button),
                color = palette.background,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}