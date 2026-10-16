package com.neytron.ethereal.tiles

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.*

enum class AudioSpectrumStyle {
    BARS,        // 1) Столбики (Стандартный)
    WAVEFORM,    // 2) Осциллограф (Две плавающие линии левого и правого канала)
    VECTORSCOPE, // 3) Векторскоп (Лиссажу / Облако точек со шлейфом)
    CURVE        // 4) Спектральная кривая (Плавная сглаженная волна с заливкой)
}

data class AudioSpectrumConfig(
    val style: AudioSpectrumStyle = AudioSpectrumStyle.BARS
) {
    fun toJson(): String = "{\"style\":\"${style.name}\"}"

    companion object {
        fun fromJson(json: String?): AudioSpectrumConfig {
            if (json.isNullOrBlank()) return AudioSpectrumConfig()
            return try {
                val obj = JSONObject(json)
                AudioSpectrumConfig(
                    style = AudioSpectrumStyle.valueOf(obj.optString("style", AudioSpectrumStyle.BARS.name))
                )
            } catch (_: Exception) {
                AudioSpectrumConfig()
            }
        }
    }
}

@Composable
fun AudioSpectrumTileContent(
    palette: EtherealPalette,
    contentScale: Float,
    config: AudioSpectrumConfig = AudioSpectrumConfig()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleStarted by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var levels by remember { mutableStateOf(List(8) { 0.1f }) }
    var tick by remember { mutableFloatStateOf(0f) }

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
        while (true) {
            delay(40)
            if (isLifecycleStarted) {
                val active = audioManager?.isMusicActive == true
                if (active) {
                    tick += 0.35f
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
        val w = size.width
        val h = size.height

        when (config.style) {
            AudioSpectrumStyle.BARS -> {
                // === 1) СТОЛБИКИ ===
                val gap = (w * 0.04f * contentScale).coerceAtLeast(2f)
                val barWidth = ((w - gap * (animatedLevels.size - 1)) / animatedLevels.size).coerceAtLeast(3f)
                animatedLevels.forEachIndexed { index, level ->
                    val barHeight = (h * level).coerceIn(4f, h)
                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = palette.primary,
                            topLeft = Offset(index * (barWidth + gap), h - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f)
                        )
                    }
                }
            }

            AudioSpectrumStyle.WAVEFORM -> {
                // === 2) ОСЦИЛЛОГРАФ (две плавные линии L и R) ===
                val leftPath = Path()
                val rightPath = Path()
                val pointsCount = 32
                val midY1 = h * 0.3f
                val midY2 = h * 0.7f
                val avgLevel = animatedLevels.average().toFloat()

                leftPath.moveTo(0f, midY1)
                rightPath.moveTo(0f, midY2)

                for (i in 0..pointsCount) {
                    val x = (w / pointsCount) * i
                    val stepNorm = i.toFloat() / pointsCount
                    val ampL = sin(tick * 2.2f + stepNorm * 9f) * cos(tick * 1.1f + stepNorm * 4f) * (h * 0.22f * avgLevel)
                    val ampR = cos(tick * 2.6f - stepNorm * 8f) * sin(tick * 1.4f - stepNorm * 5f) * (h * 0.22f * avgLevel)

                    leftPath.lineTo(x, midY1 + ampL)
                    rightPath.lineTo(x, midY2 + ampR)
                }

                drawPath(
                    path = leftPath,
                    color = palette.primary,
                    style = Stroke(width = (2.5f * contentScale).dp.toPx())
                )
                drawPath(
                    path = rightPath,
                    color = palette.primary.copy(alpha = 0.65f),
                    style = Stroke(width = (2f * contentScale).dp.toPx())
                )
            }

            AudioSpectrumStyle.VECTORSCOPE -> {
                // === 3) ВЕКТОРСКОП (Лиссажу / Облако точек со шлейфом) ===
                val centerX = w / 2f
                val centerY = h / 2f
                val radius = min(centerX, centerY) * 0.85f
                val avgLevel = animatedLevels.average().toFloat()

                val trailCount = 14
                for (i in trailCount downTo 0) {
                    val tOffset = tick - (i * 0.12f)
                    val alpha = (1f - (i.toFloat() / trailCount)).coerceIn(0.1f, 1f)
                    val scaleFactor = (0.7f + 0.3f * sin(tOffset * 2.5f)) * avgLevel

                    val posX = centerX + sin(tOffset * 3.2f) * cos(tOffset * 1.8f) * radius * scaleFactor
                    val posY = centerY + cos(tOffset * 2.8f) * sin(tOffset * 1.4f) * radius * scaleFactor

                    val dotRadius = if (i == 0) (4.5f * contentScale).dp.toPx() else (2.5f * contentScale).dp.toPx()
                    drawCircle(
                        color = palette.primary.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(posX, posY)
                    )
                }
            }

            AudioSpectrumStyle.CURVE -> {
                // === 4) СПЕКТРАЛЬНАЯ ВОЛНА (Плавная сглаженная кривая с заливкой) ===
                val curvePath = Path()
                val stepX = w / (animatedLevels.size - 1)

                curvePath.moveTo(0f, h - (animatedLevels.first() * h * 0.88f))

                for (i in 0 until animatedLevels.size - 1) {
                    val x1 = i * stepX
                    val y1 = h - (animatedLevels[i] * h * 0.88f)
                    val x2 = (i + 1) * stepX
                    val y2 = h - (animatedLevels[i + 1] * h * 0.88f)

                    val cx = (x1 + x2) / 2f
                    curvePath.cubicTo(cx, y1, cx, y2, x2, y2)
                }

                val fillPath = Path().apply {
                    addPath(curvePath)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = 0.45f),
                            palette.primary.copy(alpha = 0.05f)
                        )
                    )
                )

                drawPath(
                    path = curvePath,
                    color = palette.primary,
                    style = Stroke(width = (2.5f * contentScale).dp.toPx())
                )
            }
        }
    }
}
