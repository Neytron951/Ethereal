package com.neytron.ethereal.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
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
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val config = LocalESearchConfig.current
    val haptic = LocalHapticFeedback.current
    var contextApp by remember { mutableStateOf<AppInfo?>(null) }

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
                onAppLongClick = { contextApp = it },
                onAppClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAppClick(app)
                }
            )
        }
    }

    contextApp?.let { app ->
        BackHandler { contextApp = null }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { contextApp = null },
                contentAlignment = Alignment.Center
            ) {
                FrostedGlassBox(
                    blur = effectiveBlur,
                    opacity = effectiveOpacity,
                    backgroundColor = palette.background,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .border(1.dp, palette.outline.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .clickable(enabled = false, onClick = {})
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.esearch_context_title),
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = app.label,
                                color = palette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = app.packageName,
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    contextApp = null
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${app.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.esearch_app_info),
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary.copy(alpha = 0.15f))
                                .clickable {
                                    contextApp = null
                                    context.startActivity(
                                        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                                    )
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.esearch_uninstall),
                                color = palette.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { contextApp = null }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = palette.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
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
    onAppLongClick: (AppInfo) -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val config = LocalESearchConfig.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
    LaunchedEffect(Unit) {
        if (!rouletteMode) {
            delay(250L)
            if (config.autoShowKeyboard) {
                focusRequester.requestFocus()
                keyboardController?.show()
                try {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                } catch (_: Exception) {}
            }
        }
    }

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
                                onLongClick = { onAppLongClick(app) },
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
    onLongClick: () -> Unit,
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bmp = remember(app.packageName, app.icon, themedIcons, palette.id) {
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