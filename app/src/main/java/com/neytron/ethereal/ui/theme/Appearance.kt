package com.neytron.ethereal.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

data class AppearanceState(
    val opacity: Float = 0.95f,
    val blur: Float = 0f,
    val fontScale: Float = 1.0f,
    val fontFamily: FontFamily? = null
)

val LocalAppearance = compositionLocalOf { AppearanceState() }
val LocalWallpaperPath = compositionLocalOf { null as String? }
val LocalWallpaperScaleMode = compositionLocalOf { "CROP" }
val LocalWallpaperContainerCoordinates = compositionLocalOf { null as LayoutCoordinates? }

@Composable
fun FrostedGlassBox(
    blur: Float,
    opacity: Float,
    backgroundColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val wallpaperPath = LocalWallpaperPath.current
    val wallpaperScaleMode = LocalWallpaperScaleMode.current
    val wallpaperContainerCoords = LocalWallpaperContainerCoordinates.current
    val imageFile = remember(wallpaperPath) { wallpaperPath?.let { File(it) } }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val blurAlpha by animateFloatAsState(
        targetValue = if (blur > 0f) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "blurFadeIn"
    )

    val contentScaleMode = when (wallpaperScaleMode) {
        "FIT" -> ContentScale.Fit
        "STRETCH" -> ContentScale.FillBounds
        "CENTER" -> ContentScale.Inside
        else -> ContentScale.Crop
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                layoutCoordinates = coordinates
            }
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (blur > 0f && layoutCoordinates != null && wallpaperContainerCoords != null) {
            val containerSize = wallpaperContainerCoords.size
            if (containerSize.width > 0 && containerSize.height > 0) {
                val localOffset = try {
                    wallpaperContainerCoords.localPositionOf(layoutCoordinates!!, Offset.Zero)
                } catch (_: Exception) {
                    Offset.Zero
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .graphicsLayer(alpha = blurAlpha)
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                translationX = -localOffset.x,
                                translationY = -localOffset.y
                            )
                            .blur(blur.dp)
                            .layout { measurable, _ ->
                                val placeable = measurable.measure(
                                    Constraints.fixed(containerSize.width, containerSize.height)
                                )
                                layout(containerSize.width, containerSize.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                    ) {
                        if (imageFile != null && imageFile.exists()) {
                            AsyncImage(
                                model = imageFile,
                                contentDescription = null,
                                contentScale = contentScaleMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(backgroundColor.copy(alpha = opacity))
        )
        Box(
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Float.scaled(): Float = this
