package com.neytron.ethereal

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import coil.compose.AsyncImage
import com.neytron.ethereal.ui.theme.FrostedGlassBox
import com.neytron.ethereal.ui.theme.LocalWallpaperContainerCoordinates
import com.neytron.ethereal.ui.theme.LocalWallpaperPath
import com.neytron.ethereal.ui.theme.LocalWallpaperScaleMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.roundToInt

/** Compare the visible wallpaper itself, rather than duplicating the layout calculation. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class FrostedGlassBoxTest {
    @get:Rule
    val compose = createComposeRule()

    private val blur = mutableStateOf(0f)
    private val mode = mutableStateOf("CROP")
    private val direction = mutableStateOf(LayoutDirection.Ltr)
    private val movement = mutableStateOf(IntOffset.Zero)
    private val panelWidth = mutableStateOf(120)
    private val wallpaperCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    private val wallpaperLoaded = mutableStateOf(false)

    @Test
    fun enablingOnePercentBlurPreservesWallpaperInEveryScaleModeAndDirection() {
        showWallpaper()
        for (scaleMode in listOf("CROP", "FIT", "STRETCH", "CENTER")) {
            for (layoutDirection in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                compose.runOnIdle {
                    blur.value = 0f
                    mode.value = scaleMode
                    direction.value = layoutDirection
                }
                val reference = captureWallpaper()
                compose.runOnIdle { blur.value = 0.25f }
                assertWallpaperAligned(reference, "$scaleMode / $layoutDirection")
            }
        }
    }

    @Test
    fun wallpaperTracksPlacementOnlyMovementAndPanelResizing() {
        showWallpaper()
        val reference = captureWallpaper()
        compose.runOnIdle { blur.value = 0.25f }
        assertWallpaperAligned(reference, "initial placement")

        // This state is read only by the placement lambda, as in a slide animation.
        compose.runOnIdle { movement.value = IntOffset(17, 53) }
        assertWallpaperAligned(reference, "after moving without recomposition")

        compose.runOnIdle { panelWidth.value = 190 }
        assertWallpaperAligned(reference, "after changing ESearch width")
    }

    private fun showWallpaper() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wallpaper = File(context.cacheDir, "frosted-glass-regression.png")
        // Smooth two-dimensional colour ramps reveal both shifts and unintended scaling.
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                bitmap.setPixel(x, y, android.graphics.Color.rgb(
                    x * 255 / (bitmap.width - 1),
                    y * 255 / (bitmap.height - 1),
                    (x + y) * 255 / (bitmap.width + bitmap.height - 2)
                ))
            }
        }
        wallpaper.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        compose.setContent {
            val origin = with(LocalDensity.current) {
                IntOffset(31.dp.roundToPx(), 100.dp.roundToPx())
            }
            CompositionLocalProvider(
                LocalWallpaperPath provides wallpaper.absolutePath,
                LocalWallpaperScaleMode provides mode.value,
                LocalWallpaperContainerCoordinates provides wallpaperCoordinates.value,
                LocalLayoutDirection provides direction.value
            ) {
                Box(Modifier.size(240.dp, 320.dp).testTag("wallpaper")
                    .background(Color(0xFF112233))
                    .onGloballyPositioned { wallpaperCoordinates.value = it },
                    contentAlignment = AbsoluteAlignment.TopLeft) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = null,
                        contentScale = when (mode.value) {
                            "FIT" -> ContentScale.Fit
                            "STRETCH" -> ContentScale.FillBounds
                            "CENTER" -> ContentScale.Inside
                            else -> ContentScale.Crop
                        },
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { wallpaperLoaded.value = true }
                    )
                    // Hide the original while testing blur: an absent or unloaded
                    // frosted layer must not pass by showing the wallpaper underneath.
                    if (blur.value > 0f) {
                        Box(Modifier.fillMaxSize().background(Color.Magenta))
                    }
                    FrostedGlassBox(
                        blur = blur.value,
                        opacity = 0f,
                        backgroundColor = Color(0xFF112233),
                        shape = RectangleShape,
                        modifier = Modifier
                            .absoluteOffset { origin + movement.value }
                            .size(panelWidth.value.dp, 100.dp)
                            .testTag("panel")
                    ) { Spacer(Modifier.fillMaxSize()) }
                }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) { wallpaperLoaded.value }
    }

    private fun captureWallpaper(): ImageBitmap {
        compose.waitForIdle()
        return compose.onNodeWithTag("wallpaper").captureToImage()
    }

    private fun assertWallpaperAligned(reference: ImageBitmap, scenario: String) {
        val actual = captureWallpaper().toPixelMap()
        val expected = reference.toPixelMap()
        val root = compose.onNodeWithTag("wallpaper").fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot
        val localPanel = Rect(
            panel.left - root.left, panel.top - root.top,
            panel.right - root.left, panel.bottom - root.top
        )
        for (fx in listOf(0.25f, 0.5f, 0.75f)) {
            for (fy in listOf(0.25f, 0.5f, 0.75f)) {
                val x = (localPanel.left + localPanel.width * fx).roundToInt()
                val y = (localPanel.top + localPanel.height * fy).roundToInt()
                val a = actual[x, y]
                val e = expected[x, y]
                // One percent blur can smooth a colour ramp, but must not move it.
                assertEquals("$scenario red at ($x, $y)", e.red, a.red, 0.025f)
                assertEquals("$scenario green at ($x, $y)", e.green, a.green, 0.025f)
                assertEquals("$scenario blue at ($x, $y)", e.blue, a.blue, 0.025f)
            }
        }
    }
}
