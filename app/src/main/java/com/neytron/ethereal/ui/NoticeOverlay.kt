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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppNotice
import com.neytron.ethereal.ui.theme.EtherealPalette

/**
 * Разовое сообщение пользователю.
 *
 * Текст задаётся в [com.neytron.ethereal.data.Notices.CURRENT] и показывается один раз:
 * после закрытия сохраняется id сообщения, поэтому повторно оно не появится.
 */
@Composable
fun NoticeOverlay(
    visible: Boolean,
    notice: AppNotice?,
    palette: EtherealPalette,
    onDismiss: () -> Unit
) {
    if (notice == null) return

    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)) +
                slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(260)),
        exit = fadeOut(animationSpec = tween(150)) +
                slideOutVertically(targetOffsetY = { it / 14 }, animationSpec = tween(170))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                color = palette.background.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, palette.surfaceVariant)
            ) {
                var entered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { entered = true }

                val cardAlpha by animateFloatAsState(
                    targetValue = if (entered) 1f else 0f,
                    animationSpec = tween(260),
                    label = "noticeAlpha"
                )
                val cardOffset by animateFloatAsState(
                    targetValue = if (entered) 0f else 14f,
                    animationSpec = tween(280),
                    label = "noticeOffset"
                )

                Column(
                    modifier = Modifier
                        .alpha(cardAlpha)
                        .offset(y = cardOffset.dp)
                        .padding(20.dp)
                ) {
                    NoticeHeader(notice = notice, palette = palette, onDismiss = onDismiss)

                    Spacer(modifier = Modifier.height(14.dp))

                    val blocks = remember(notice.body) { ReleaseNotes.parse(notice.body) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ReleaseNotesView(blocks = blocks, palette = palette)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!notice.linkUrl.isNullOrBlank()) {
                        NoticeLinkButton(
                            notice = notice,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(palette.primary, palette.primary.copy(alpha = 0.82f))
                                )
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.notice_got_it),
                            color = palette.background,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeHeader(
    notice: AppNotice,
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

        Text(
            text = notice.title,
            color = palette.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(palette.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✕", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NoticeLinkButton(
    notice: AppNotice,
    palette: EtherealPalette,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.45f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val url = notice.linkUrl ?: return@clickable
                try {
                    uriHandler.openUri(url)
                } catch (_: Exception) {
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = notice.linkLabel ?: stringResource(R.string.notice_open_link),
            color = palette.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
