package com.neytron.ethereal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.data.AppSorter
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.LocalAppearance
import com.neytron.ethereal.ui.theme.LocalESearchConfig
import com.neytron.ethereal.ui.theme.FrostedGlassBox
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.toImageBitmap
import kotlinx.coroutines.delay

@Composable
fun ESearchOverlay(
    visible: Boolean,
    apps: List<AppInfo>,
    palette: EtherealPalette,
    onAppClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    rouletteMode: Boolean = false,
    onRouletteComplete: () -> Unit = {}
) {
    val appearance = LocalAppearance.current
    val config = LocalESearchConfig.current
    val haptic = LocalHapticFeedback.current

    val effectiveOpacity = config.opacityOverride ?: appearance.opacity
    val effectiveBlur = config.blurOverride ?: appearance.blur

    LaunchedEffect(visible) {
        if (visible) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                ) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) +
                scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            ESearchPanel(
                apps = apps,
                palette = palette,
                opacity = effectiveOpacity,
                blur = effectiveBlur,
                widthFraction = config.widthFraction,
                visibleItems = config.visibleItems,
                placeholder = config.placeholder,
                themedIcons = config.themedIcons,
                rouletteMode = rouletteMode,
                onRouletteComplete = onRouletteComplete,
                onAppClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAppClick(app)
                }
            )
        }
    }
}

@Composable
private fun ESearchPanel(
    apps: List<AppInfo>,
    palette: EtherealPalette,
    opacity: Float,
    blur: Float,
    widthFraction: Float,
    visibleItems: Int,
    placeholder: String,
    themedIcons: Boolean,
    rouletteMode: Boolean = false,
    onRouletteComplete: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Множество пакетов, которые уже хотя бы раз отобразились в рамках текущей сессии ESearch
    val shownPackages = remember { mutableStateSetOf<String>() }

    val filtered = remember(query, apps) {
        AppSorter.sortByScore(context, apps, query)
    }

    LaunchedEffect(rouletteMode) {
        if (rouletteMode) {
            val startTime = System.currentTimeMillis()
            val durationMs = 1200L
            try {
                listState.scroll {
                    while (System.currentTimeMillis() - startTime < durationMs) {
                        val progress = (System.currentTimeMillis() - startTime).toFloat() / durationMs
                        val speed = (1f - progress) * 35f + 3f
                        scrollBy(speed)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(16)
                    }
                }
            } catch (_: Exception) {}
            delay(150L)
            onRouletteComplete()
        }
    }

    LaunchedEffect(filtered) { selectedIndex = 0 }
    LaunchedEffect(Unit) { if (!rouletteMode) focusRequester.requestFocus() }

    var isFirstSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (!isFirstSelection && filtered.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        isFirstSelection = false
    }

    val itemHeight = 52.dp
    val listMaxHeight = itemHeight * visibleItems

    Surface(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .wrapContentHeight()
            .imePadding(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        FrostedGlassBox(
            blur = blur,
            opacity = opacity,
            backgroundColor = palette.background,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "❯",
                            color = palette.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = palette.textSecondary,
                                    fontSize = 18.sp
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                textStyle = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 18.sp
                                ),
                                cursorBrush = SolidColor(palette.primary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    filtered.getOrNull(selectedIndex)?.let(onAppClick)
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .focusable()
                            )
                        }
                    }
                }

                ESearchDivider(color = palette.surfaceVariant, thickness = 1.dp)

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.esearch_nothing_found),
                            color = palette.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filtered.size, key = { filtered[it].packageName }) { index ->
                            val app = filtered[index]
                            ESearchItem(
                                modifier = Modifier.animateItem(),
                                app = app,
                                index = index,
                                selected = index == selectedIndex,
                                palette = palette,
                                themedIcons = themedIcons,
                                alreadyShown = app.packageName in shownPackages,
                                onShown = { pkg -> shownPackages.add(pkg) },
                                onClick = { onAppClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ESearchItem(
    modifier: Modifier = Modifier,
    app: AppInfo,
    index: Int,
    selected: Boolean,
    palette: EtherealPalette,
    themedIcons: Boolean,
    alreadyShown: Boolean,
    onShown: (String) -> Unit,
    onClick: () -> Unit
) {
    // === Каскадная анимация появления ===
    var appeared by remember(app.packageName) { mutableStateOf(alreadyShown) }

    LaunchedEffect(app.packageName, alreadyShown) {
        if (!alreadyShown && !appeared) {
            val delayMs = (index.coerceAtMost(10)) * 35L
            if (delayMs > 0) delay(delayMs)
            appeared = true
            onShown(app.packageName)
        } else if (alreadyShown && !appeared) {
            appeared = true
        }
    }

    val animationSpec = spring<Float>(
        stiffness = Spring.StiffnessLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val alpha by animateFloatAsState(
        targetValue = if (appeared || alreadyShown) 1f else 0f,
        animationSpec = animationSpec,
        label = "alpha"
    )

    val translationY by animateFloatAsState(
        targetValue = if (appeared || alreadyShown) 0f else 16f,
        animationSpec = animationSpec,
        label = "translationY"
    )

    val scale by animateFloatAsState(
        targetValue = if (appeared || alreadyShown) 1f else 0.95f,
        animationSpec = animationSpec,
        label = "scale"
    )

    val targetBg = if (selected) palette.primary.copy(alpha = 0.15f) else Color.Transparent
    val targetTextColor = if (selected) palette.primary else palette.textPrimary

    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 150),
        label = "bg"
    )

    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 150),
        label = "textColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = alpha,
                translationY = translationY,
                scaleX = scale,
                scaleY = scale
            )
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bmp = remember(app.packageName, themedIcons, palette.id) {
            if (themedIcons) {
                renderThemedIcon(app.icon, palette).asImageBitmap()
            } else {
                app.icon.toImageBitmap()
            }
        }
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = app.label,
            color = textColor,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ESearchDivider(color: Color, thickness: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}