package com.neytron.ethereal.ui.theme

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.roundToInt

data class AppearanceState(
    val opacity: Float = 0.95f,
    val blur: Float = 0f,
    val fontScale: Float = 1.0f,
    val fontFamily: FontFamily? = null
)

val LocalAppearance = compositionLocalOf { AppearanceState() }
val LocalWallpaperPath = compositionLocalOf { null as String? }
val LocalWallpaperScaleMode = compositionLocalOf { "CROP" }

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
    val imageFile = remember(wallpaperPath) { wallpaperPath?.let { File(it) } }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

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
        if (blur > 0f && layoutCoordinates != null) {
            val rootCoordinates = layoutCoordinates?.findRootCoordinates()
            if (rootCoordinates != null && rootCoordinates.size.width > 0 && rootCoordinates.size.height > 0) {
                val rootSize = rootCoordinates.size
                val positionInRoot = layoutCoordinates!!.positionInRoot()

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                ) {
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints.fixed(rootSize.width, rootSize.height)
                                )
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.placeWithLayer(
                                        x = (-positionInRoot.x).roundToInt(),
                                        y = (-positionInRoot.y).roundToInt()
                                    )
                                }
                            }
                            .blur(blur.dp)
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
