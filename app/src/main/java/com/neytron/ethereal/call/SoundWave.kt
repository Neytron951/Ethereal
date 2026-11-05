package com.neytron.ethereal.call

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SoundWave(
    palette: EtherealPalette,
    active: Boolean,
    ringing: Boolean = false,
    holding: Boolean = false,
    mirrored: Boolean = false,
    seed: Int = 0,
    audioSpectrum: List<Float>? = null,
    audioGain: Float = 1f,
    modifier: Modifier = Modifier
) {
    // === Настройки физики ===
    val pointCount = if (audioSpectrum != null) 72 else 26
    val smoothness = 0.22f
    val headSmooth = 0.28f
    val noiseAmplitude = 0.10f

    val rng = remember(seed) { kotlin.random.Random(seed * 31 + 7) }

    val phases = remember(seed, pointCount) {
        List(pointCount) { rng.nextFloat() * (2f * Math.PI.toFloat()) }
    }
    val speeds = remember(seed, pointCount) {
        List(pointCount) { 0.6f + rng.nextFloat() * 0.8f }
    }

    // positions = отклонение ВНУТРЬ, 0..1
    val positions = remember(seed, pointCount) {
        MutableList(pointCount) { 0f }
    }
    val currentAudioSpectrum by rememberUpdatedState(audioSpectrum)
    val currentAudioGain by rememberUpdatedState(audioGain)

    // === Скорость анимации ===
    val tickMs = when {
        holding -> 90L
        ringing -> 40L
        active -> 55L
        else -> 80L
    }

    // === Драйвер анимации ===
    LaunchedEffect(active, ringing, holding, pointCount) {
        val headRange = when {
            holding -> 0.10f
            ringing -> 0.95f
            active -> 0.80f
            else -> 0.40f
        }

        var time = 0f

        while (true) {
            val spectrum = currentAudioSpectrum
            val frameDelayMs = if (spectrum != null) 32L else tickMs
            time += frameDelayMs / 1000f
            if (spectrum != null && spectrum.isNotEmpty()) {
                for (i in 0 until pointCount) {
                    val bandPosition = i.toFloat() / (pointCount - 1) * (spectrum.size - 1)
                    val lowerBand = bandPosition.toInt()
                    val upperBand = (lowerBand + 1).coerceAtMost(spectrum.lastIndex)
                    val fraction = bandPosition - lowerBand
                    val localLevel = (
                        spectrum[lowerBand] * (1f - fraction) +
                            spectrum[upperBand] * fraction
                    )
                    val broadbandLevel = kotlin.math.sqrt(
                        spectrum.sumOf { (it * it).toDouble() }.toFloat() / spectrum.size
                    )
                    val target = (
                        localLevel * 0.85f + broadbandLevel * 0.15f
                    ) * currentAudioGain
                    val smoothing = if (holding) 0.76f else 0.48f
                    positions[i] += (target.coerceIn(0f, 1f) - positions[i]) * smoothing
                }
            } else {
                // Preserve the original free-floating animation outside audio-reactive scenes.
                val head = positions[0]
                val headTarget = rng.nextFloat() * headRange
                positions[0] = head + (headTarget - head) * headSmooth

                for (i in 1 until pointCount) {
                    val prev = positions[i - 1]
                    val cur = positions[i]
                    val follow = cur + (prev - cur) * smoothness
                    val noise = sin(time * speeds[i] + phases[i]) * noiseAmplitude
                    positions[i] = (follow + noise).coerceIn(0f, 1f)
                }
            }

            delay(frameDelayMs)
        }
    }

    // === Отрисовка ===
    val primary = palette.primary
    val idleColor = palette.primary.copy(alpha = 0.25f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = pointCount

        val edgeOffsetPx = 4.dp.toPx()
        val maxDeviationPx = (w - edgeOffsetPx).coerceAtLeast(0f)

        // Точки волны
        val points = positions.mapIndexed { i, p ->
            val y = (i.toFloat() / (n - 1)) * h
            val inward = p * maxDeviationPx
            val x = if (mirrored) {
                w - edgeOffsetPx - inward
            } else {
                edgeOffsetPx + inward
            }
            Offset(x, y)
        }

        val edgeX = if (mirrored) w - edgeOffsetPx else edgeOffsetPx

        // === Градиенты ===
        val fillBrush = Brush.verticalGradient(
            colors = if (active || ringing) {
                listOf(
                    primary.copy(alpha = 0.10f),
                    primary.copy(alpha = 0.55f)
                )
            } else {
                listOf(
                    idleColor.copy(alpha = 0.05f),
                    idleColor.copy(alpha = 0.30f)
                )
            }
        )

        val strokeBrush = Brush.verticalGradient(
            colors = if (active || ringing) {
                listOf(
                    primary.copy(alpha = 0.20f),
                    primary.copy(alpha = 1.0f)
                )
            } else {
                listOf(
                    idleColor.copy(alpha = 0.15f),
                    idleColor.copy(alpha = 0.9f)
                )
            }
        )

        val glowBrush = Brush.verticalGradient(
            colors = if (active || ringing) {
                listOf(
                    primary.copy(alpha = 0.05f),
                    primary.copy(alpha = 0.30f)
                )
            } else {
                listOf(
                    idleColor.copy(alpha = 0.03f),
                    idleColor.copy(alpha = 0.15f)
                )
            }
        )

        // === Path для заливки: волна + замыкание по краю ===
        val fillPath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                val midY = (p0.y + p1.y) / 2f
                quadraticBezierTo(p0.x, p0.y, midX, midY)
            }
            lineTo(points.last().x, points.last().y)
            // Замыкаем по краю экрана
            lineTo(edgeX, h)
            lineTo(edgeX, 0f)
            close()
        }

        drawPath(path = fillPath, brush = fillBrush)

        // === Path только для контура (без замыкания) ===
        val strokePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                val midY = (p0.y + p1.y) / 2f
                quadraticBezierTo(p0.x, p0.y, midX, midY)
            }
            lineTo(points.last().x, points.last().y)
        }

        // Свечение
        drawPath(
            path = strokePath,
            brush = glowBrush,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Основная линия
        drawPath(
            path = strokePath,
            brush = strokeBrush,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoundWavePair(
    palette: EtherealPalette,
    active: Boolean,
    ringing: Boolean,
    holding: Boolean,
    audioSpectrum: List<Float>? = null,
    audioGain: Float = 1f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        SoundWave(
            palette = palette,
            active = active,
            ringing = ringing,
            holding = holding,
            mirrored = false,
            seed = 1,
            audioSpectrum = audioSpectrum,
            audioGain = audioGain,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(56.dp)
        )

        SoundWave(
            palette = palette,
            active = active,
            ringing = ringing,
            holding = holding,
            mirrored = true,
            seed = 2,
            audioSpectrum = audioSpectrum,
            audioGain = audioGain,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(56.dp)
        )
    }
}