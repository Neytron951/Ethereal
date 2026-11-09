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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.roundToInt

/**
 * Общий характер интерфейса.
 *
 * GLASS — родной Ethereal: полупрозрачные панели, блюр обоев, тонкие стеклянные рамки.
 * FLAT  — Material You: плотные тональные поверхности, без блюра, текстур и стеклянных рамок.
 */
enum class UiStyle {
    GLASS,
    FLAT;

    companion object {
        fun fromName(raw: String?): UiStyle =
            values().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GLASS
    }
}

data class AppearanceState(
    val opacity: Float = 0.95f,
    val blur: Float = 0f,
    val fontScale: Float = 1.0f,
    val fontFamily: FontFamily? = null,
    val style: UiStyle = UiStyle.GLASS
) {
    val isFlat: Boolean get() = style == UiStyle.FLAT
}

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
    contentModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val wallpaperPath = LocalWallpaperPath.current
    val wallpaperScaleMode = LocalWallpaperScaleMode.current
    val wallpaperContainerCoords = LocalWallpaperContainerCoordinates.current
    val imageFile = remember(wallpaperPath) { wallpaperPath?.let { File(it) } }

    val uiStyle = LocalAppearance.current.style
    val isFlat = uiStyle == UiStyle.FLAT

    // В плоском стиле стекла нет: блюр отключаем, а прозрачность поднимаем до
    // плотной поверхности. Ноль остаётся нулём — «плитки-иконки» остаются прозрачными.
    val effectiveBlur = if (isFlat) 0f else blur
    val effectiveOpacity = if (isFlat) flatPanelOpacity(opacity) else opacity

    val blurAlpha by animateFloatAsState(
        targetValue = if (effectiveBlur > 0f) 1f else 0f,
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
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (effectiveBlur > 0f && wallpaperContainerCoords?.isAttached == true) {
            val containerSize = wallpaperContainerCoords.size
            if (containerSize.width > 0 && containerSize.height > 0) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .graphicsLayer(alpha = blurAlpha)
                ) {
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val wallpaperSize = wallpaperContainerCoords.size
                                val placeable = measurable.measure(
                                    Constraints.fixed(wallpaperSize.width, wallpaperSize.height)
                                )
                                // Keep the viewport panel-sized so Compose does not center
                                // an oversized child inside the panel's constraints.
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    val panelCoordinates = coordinates
                                    if (panelCoordinates != null && wallpaperContainerCoords.isAttached) {
                                        // Read coordinates during placement so moving the panel
                                        // (including ESearch's entrance) updates the sampled region.
                                        val offset = wallpaperContainerCoords.localPositionOf(
                                            panelCoordinates, Offset.Zero
                                        )
                                        placeable.place((-offset.x).roundToInt(), (-offset.y).roundToInt())
                                    }
                                }
                            }
                            .blur(effectiveBlur.dp)
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
                .background(backgroundColor.copy(alpha = effectiveOpacity))
        )
        Box(
            modifier = contentModifier,
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Float.scaled(): Float = this

/**
 * Плотность поверхности в плоском стиле: почти непрозрачная, но ноль остаётся нулём
 * (так «плитки-иконки» без фона продолжают жить без подложки).
 */
private fun flatPanelOpacity(opacity: Float): Float =
    if (opacity <= 0.01f) 0f else maxOf(opacity, 0.92f)
