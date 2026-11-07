package com.neytron.ethereal.ui

import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.tiles.TileStyle
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.IconShape
import com.neytron.ethereal.ui.theme.iconShapeFor
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.toForegroundOnlyBitmap
import com.neytron.ethereal.utils.tileBorder
import com.neytron.ethereal.utils.toImageBitmap

data class AppLaunchAnimationRequest(
    val app: AppInfo,
    val sourceBounds: Rect,
    val icon: ImageBitmap,
    val accent: Color,
    val surface: Color,
    val palette: EtherealPalette,
    val iconShape: IconShape,
    val showIconBackground: Boolean,
    val tileStyle: TileStyle?,
    val tileRadiusDp: Float,
    val iconContentScale: Float,
    val iconInsetDp: Float,
    val showAccentHalo: Boolean,
    val showSourceIcon: Boolean,
    val closeSearchOnFinish: Boolean
)

@Composable
fun AppLaunchAnimation(
    request: AppLaunchAnimationRequest?,
    onFinished: (AppLaunchAnimationRequest) -> Unit
) {
    val progress = remember { Animatable(0f) }
    val view = LocalView.current
    val density = LocalDensity.current

    LaunchedEffect(request) {
        val launchRequest = request ?: return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
        withFrameNanos { }
        onFinished(launchRequest)
    }

    if (request != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            val viewLocation = remember(view) {
                IntArray(2).also(view::getLocationOnScreen)
            }
            val bounds = request.sourceBounds
            val iconWidthPx = bounds.width().toFloat().coerceAtLeast(1f)
            val iconHeightPx = bounds.height().toFloat().coerceAtLeast(1f)
            val centerX = bounds.exactCenterX() - viewLocation[0]
            val centerY = bounds.exactCenterY() - viewLocation[1]
            val iconSizeDp = with(density) { minOf(iconWidthPx, iconHeightPx).toDp().value }
            val iconShape = iconShapeFor(request.iconShape, iconSizeDp)
            val frameShape = RoundedCornerShape(request.tileRadiusDp.dp)
            val iconFraction = request.iconContentScale

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val origin = Offset(centerX, centerY)
                        val radius = maxDistanceTo(origin) * progress.value
                        if (radius > 0f) {
                            drawCircle(
                                color = request.surface,
                                radius = radius,
                                center = origin
                            )
                            if (request.showAccentHalo) {
                                drawCircle(
                                    color = request.accent.copy(alpha = 0.16f * (1f - progress.value * 0.5f)),
                                    radius = radius,
                                    center = origin,
                                    style = Stroke(width = 28.dp.toPx())
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            request.accent.copy(alpha = 0.95f),
                                            request.accent.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        center = origin,
                                        radius = radius + 16.dp.toPx()
                                    ),
                                    radius = radius + 2.dp.toPx(),
                                    center = origin,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        }
                    }
            )

            if (request.showSourceIcon) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (centerX - iconWidthPx / 2f).toDp() },
                            y = with(density) { (centerY - iconHeightPx / 2f).toDp() }
                        )
                        .size(
                            width = with(density) { iconWidthPx.toDp() },
                            height = with(density) { iconHeightPx.toDp() }
                        )
                        .graphicsLayer {
                            val scale = 1f + 0.08f * progress.value
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - ((progress.value - 0.88f) / 0.12f).coerceIn(0f, 1f)
                        }
                        .clip(if (request.tileStyle != null) frameShape else iconShape)
                        .background(
                            if (request.tileStyle != null || request.showIconBackground) {
                                request.surface
                            } else {
                                Color.Transparent
                            }
                        )
                        .then(
                            if (request.tileStyle != null) {
                                Modifier.tileBorder(
                                    request.tileStyle,
                                    palette = request.palette,
                                    shape = frameShape
                                )
                            } else if (request.showIconBackground) {
                                Modifier.border(1.dp, request.accent.copy(alpha = 0.35f), iconShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Image(
                        bitmap = request.icon,
                        contentDescription = request.app.label,
                        modifier = Modifier
                            .fillMaxSize(iconFraction.coerceIn(0f, 1f))
                            .graphicsLayer {
                                val overflowScale = iconFraction.coerceAtLeast(1f)
                                scaleX = overflowScale
                                scaleY = overflowScale
                            }
                            .clip(iconShape)
                            .padding(request.iconInsetDp.dp)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.maxDistanceTo(origin: Offset): Float {
    val horizontal = maxOf(origin.x, size.width - origin.x)
    val vertical = maxOf(origin.y, size.height - origin.y)
    return kotlin.math.sqrt(horizontal * horizontal + vertical * vertical) + 32.dp.toPx()
}

fun createAppLaunchAnimationRequest(
    app: AppInfo,
    sourceBounds: Rect,
    palette: EtherealPalette,
    themedIcons: Boolean,
    iconShape: IconShape,
    showIconBackground: Boolean,
    tileStyle: TileStyle?,
    tileRadiusDp: Float,
    iconContentScale: Float,
    iconInsetDp: Float,
    showAccentHalo: Boolean,
    showSourceIcon: Boolean,
    closeSearchOnFinish: Boolean,
    foregroundOnlyIcon: Boolean = false
): AppLaunchAnimationRequest? =
    when {
        themedIcons -> renderThemedIcon(
            app.icon,
            palette,
            drawBackground = !foregroundOnlyIcon
        ).asImageBitmap()
        foregroundOnlyIcon -> app.icon.toForegroundOnlyBitmap()
        else -> app.icon.toImageBitmap()
    }
        ?.let { bitmap ->
        AppLaunchAnimationRequest(
            app,
            sourceBounds,
            bitmap,
            palette.primary,
            palette.surface,
            palette,
            iconShape,
            showIconBackground,
            tileStyle,
            tileRadiusDp,
            iconContentScale,
            iconInsetDp,
            showAccentHalo,
            showSourceIcon,
            closeSearchOnFinish
        )
    }
