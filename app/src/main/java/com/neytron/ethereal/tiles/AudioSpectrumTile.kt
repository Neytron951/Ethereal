package com.neytron.ethereal.tiles

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.hypot

fun calculateFrequencyLevels(fft: ByteArray, samplingRateMilliHz: Int): List<Float> {
    samplingRateMilliHz.let {}
    if (fft.isEmpty()) return List(8) { 0f }
    val n = fft.size / 2
    if (n <= 0) return List(8) { 0f }

    val magnitudes = FloatArray(n)
    magnitudes[0] = abs(fft[0].toFloat())
    if (n > 1) {
        magnitudes[n - 1] = abs(fft[1].toFloat())
    }
    for (k in 1 until n - 1) {
        val r = fft[2 * k].toFloat()
        val i = if (2 * k + 1 < fft.size) fft[2 * k + 1].toFloat() else 0f
        magnitudes[k] = hypot(r, i)
    }

    val levels = MutableList(8) { 0f }
    val boundaries = intArrayOf(0, 1, 2, 4, 8, 16, 32, 64, n)
    for (b in 0 until 8) {
        val start = boundaries[b].coerceIn(0, n)
        val end = boundaries[b + 1].coerceIn(0, n)
        var sum = 0f
        var count = 0
        for (i in start until end) {
            sum += magnitudes[i]
            count++
        }
        val avg = if (count > 0) sum / count else 0f
        levels[b] = (avg / 128f).coerceIn(0f, 1f)
    }
    return levels
}

@Composable
fun AudioSpectrumTileContent(
    palette: EtherealPalette,
    contentScale: Float
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleStarted by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var levels by remember { mutableStateOf(List(8) { 0.1f }) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> isLifecycleStarted = true
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> isLifecycleStarted = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isLifecycleStarted) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        var tick = 0f
        while (true) {
            delay(40)
            if (isLifecycleStarted) {
                val active = audioManager?.isMusicActive == true
                if (active) {
                    tick += 0.45f
                    levels = List(8) { index ->
                        val bassBoost = if (index <= 1) (abs(sin(tick * 1.6f)) * 0.7f + 0.3f) else 1f
                        val wave1 = sin(tick * 1.3f + index * 0.75f)
                        val wave2 = cos(tick * 2.4f - index * 0.45f)
                        val wave3 = sin(tick * 0.9f + index * 1.2f)
                        val raw = ((wave1 + wave2 + wave3 + 3f) / 6f) * bassBoost
                        (raw.coerceIn(0.15f, 1f))
                    }
                } else {
                    levels = List(8) { 0.1f }
                }
            } else {
                levels = List(8) { 0.1f }
            }
        }
    }

    val animatedLevels = levels.map { level ->
        animateFloatAsState(
            targetValue = level,
            animationSpec = tween(durationMillis = 40),
            label = "spectrum_level"
        ).value
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = (8 * contentScale).dp,
                vertical = (8 * contentScale).dp
            )
    ) {
        val gap = (size.width * 0.04f * contentScale).coerceAtLeast(2f)
        val barWidth = ((size.width - gap * (animatedLevels.size - 1)) / animatedLevels.size).coerceAtLeast(3f)
        animatedLevels.forEachIndexed { index, level ->
            val barHeight = (size.height * level).coerceIn(4f, size.height)
            if (barHeight > 0f) {
                drawRoundRect(
                    color = palette.primary,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
        }
    }
}
