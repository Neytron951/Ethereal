package com.neytron.ethereal

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.ui.DockBar
import com.neytron.ethereal.ui.theme.DockConfig
import com.neytron.ethereal.ui.theme.EtherealGreenPalette
import com.neytron.ethereal.ui.theme.LocalDockConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Док с пятью иконками не обязан влезать в экран: при большой иконке
 * (например, 70.6 dp на 360 dp экране) ряд шире доступного места.
 *
 * Регрессия: раньше в этом случае последняя иконка «съедала» свой паз и
 * расстояние 4→5 становилось заметно меньше остальных. Теперь ряд
 * масштабируется целиком, поэтому все промежутки обязаны быть равными.
 */
@RunWith(AndroidJUnit4::class)
class DockSpacingProbeTest {

    @get:Rule
    val compose = createComposeRule()

    private val iconSize = mutableStateOf(70.60465f)

    @Test
    fun dockGapsStayEqualWhenRowDoesNotFit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val screenWidthDp = context.resources.displayMetrics.widthPixels / density

        val apps = (1..5).map { index ->
            AppInfo(
                label = "App$index",
                packageName = "com.example.probe$index",
                icon = ColorDrawable(0xFF3366AA.toInt())
            )
        }

        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDockConfig provides DockConfig(iconSizeDp = iconSize.value)) {
                    Box(Modifier.fillMaxSize()) {
                        DockBar(
                            apps = apps,
                            palette = EtherealGreenPalette,
                            onAppClick = { _, _ -> },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        )
                    }
                }
            }
        }

        for (size in listOf(60f, 66f, 68.5f, 69f, 70f, 70.60465f, 71f, 72f)) {
            compose.runOnIdle { iconSize.value = size }

            val bounds = (1..5).map { index ->
                compose.onNodeWithContentDescription("App$index").getUnclippedBoundsInRoot()
            }
            val gaps = bounds.zipWithNext().map { (left, right) -> right.left - left.right }

            gaps.forEachIndexed { index, gap ->
                assertEquals(
                    "size=$size: паз ${index + 1} (${gap.value} dp) должен совпадать с первым (${gaps[0].value} dp)",
                    gaps[0].value,
                    gap.value,
                    0.51f
                )
            }
            assertTrue(
                "size=$size: ряд (${bounds.last().right.value} dp) вылез за экран ($screenWidthDp dp)",
                bounds.last().right.value <= screenWidthDp + 0.51f
            )
            assertTrue("size=$size: отступ слева отрицательный", bounds.first().left.value >= 0f)
            assertTrue("size=$size: иконка схлопнулась", (bounds[0].right - bounds[0].left).value > 24f)
        }
    }
}
