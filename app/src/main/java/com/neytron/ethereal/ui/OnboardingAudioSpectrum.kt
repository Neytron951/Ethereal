package com.neytron.ethereal.ui

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

internal data class OnboardingAudioSpectrum(
    val frames: List<FloatArray>,
    val frameDurationMs: Double,
    val durationMs: Double
) {
    fun frameAt(elapsedMs: Long): FloatArray {
        val sampleTimeMs = elapsedMs % durationMs.toLong().coerceAtLeast(1L)
        val index = (sampleTimeMs / frameDurationMs).toInt().coerceAtMost(frames.lastIndex)
        return frames[index]
    }
}

internal fun decodeOnboardingAudioSpectrum(context: Context, resourceId: Int): OnboardingAudioSpectrum? {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var decoderStarted = false
    try {
        context.resources.openRawResourceFd(resourceId).use { descriptor ->
            extractor.setDataSource(
                descriptor.fileDescriptor,
                descriptor.startOffset,
                descriptor.length
            )
        }

        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(trackIndex)

        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        decoder = MediaCodec.createDecoderByType(mime).apply {
            configure(format, null, null, 0)
            start()
            decoderStarted = true
        }

        val samples = ArrayList<Float>(format.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 4)
        val info = MediaCodec.BufferInfo()
        var inputEnded = false
        var outputEnded = false
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

        while (!outputEnded) {
            if (!inputEnded) {
                val inputIndex = decoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputEnded = true
                    } else {
                        decoder.queueInputBuffer(
                            inputIndex, 0, sampleSize, extractor.sampleTime, 0
                        )
                        extractor.advance()
                    }
                }
            }

            when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outputFormat = decoder.outputFormat
                    sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmEncoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                else -> if (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && info.size > 0) {
                        outputBuffer.position(info.offset)
                        outputBuffer.limit(info.offset + info.size)
                        val pcm = outputBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)
                        if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            while (pcm.remaining() >= 4 * channelCount) {
                                var mono = 0f
                                repeat(channelCount) { mono += pcm.float }
                                samples += mono / channelCount
                            }
                        } else {
                            while (pcm.remaining() >= 2 * channelCount) {
                                var mono = 0f
                                repeat(channelCount) { mono += pcm.short / 32768f }
                                samples += mono / channelCount
                            }
                        }
                    }
                    outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    decoder.releaseOutputBuffer(outputIndex, false)
                }
            }
        }

        return buildSpectrum(samples, sampleRate)
    } catch (error: Exception) {
        Log.w("OnboardingAudioSpectrum", "Could not analyze onboarding audio", error)
        return null
    } finally {
        decoder?.let { codec ->
            try {
                if (decoderStarted) codec.stop()
            } catch (error: IllegalStateException) {
                Log.w("OnboardingAudioSpectrum", "Could not stop audio decoder cleanly", error)
            } finally {
                codec.release()
            }
        }
        extractor.release()
    }
}

private fun buildSpectrum(samples: List<Float>, sampleRate: Int): OnboardingAudioSpectrum? {
    val fftSize = 4096
    val hopSize = 512
    if (samples.size < fftSize || sampleRate <= 0) return null

    val bandCount = 32
    val minFrequency = 30.0
    val maxFrequency = minOf(8_000.0, sampleRate / 2.0)
    val logFrequencyRange = ln(maxFrequency / minFrequency)
    val binRanges = (0 until bandCount).map { band ->
        val startFrequency = minFrequency * kotlin.math.exp(logFrequencyRange * band / bandCount)
        val endFrequency = minFrequency * kotlin.math.exp(logFrequencyRange * (band + 1) / bandCount)
        val start = (startFrequency * fftSize / sampleRate).toInt().coerceAtLeast(1)
        val end = (endFrequency * fftSize / sampleRate).toInt().coerceAtLeast(start + 1)
        start until end.coerceAtMost(fftSize / 2)
    }
    val window = FloatArray(fftSize) { index ->
        (0.5 - 0.5 * cos(2.0 * PI * index / (fftSize - 1))).toFloat()
    }
    val rawFrames = ArrayList<FloatArray>()
    val bandPeaks = FloatArray(binRanges.size)

    var offset = 0
    while (offset + fftSize <= samples.size) {
        val real = FloatArray(fftSize) { samples[offset + it] * window[it] }
        val imaginary = FloatArray(fftSize)
        fft(real, imaginary)

        val bands = FloatArray(binRanges.size)
        for (band in binRanges.indices) {
            val bins = binRanges[band]
            var energy = 0f
            for (bin in bins) {
                energy += real[bin] * real[bin] + imaginary[bin] * imaginary[bin]
            }
            bands[band] = sqrt(energy / bins.count().coerceAtLeast(1))
            bandPeaks[band] = maxOf(bandPeaks[band], bands[band])
        }
        rawFrames += bands
        offset += hopSize
    }

    if (rawFrames.isEmpty() || bandPeaks.maxOrNull() == 0f) return null
    val normalizedFrames = rawFrames.map { frame ->
        FloatArray(frame.size) { index ->
            val peak = bandPeaks[index]
            if (peak <= 0f) 0f else (frame[index] / peak).coerceIn(0f, 1f).pow(0.42f)
        }
    }
    val frameDurationMs = (hopSize * 1000.0 / sampleRate).coerceAtLeast(1.0)
    val durationMs = samples.size * 1000.0 / sampleRate
    return OnboardingAudioSpectrum(normalizedFrames, frameDurationMs, durationMs)
}

private fun fft(real: FloatArray, imaginary: FloatArray) {
    val size = real.size
    var j = 0
    for (i in 1 until size) {
        var bit = size shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val realValue = real[i]
            real[i] = real[j]
            real[j] = realValue
            val imaginaryValue = imaginary[i]
            imaginary[i] = imaginary[j]
            imaginary[j] = imaginaryValue
        }
    }

    var length = 2
    while (length <= size) {
        val angle = -2.0 * PI / length
        val stepReal = cos(angle).toFloat()
        val stepImaginary = sin(angle).toFloat()
        for (start in 0 until size step length) {
            var factorReal = 1f
            var factorImaginary = 0f
            for (i in 0 until length / 2) {
                val even = start + i
                val odd = even + length / 2
                val oddReal = real[odd] * factorReal - imaginary[odd] * factorImaginary
                val oddImaginary = real[odd] * factorImaginary + imaginary[odd] * factorReal
                real[odd] = real[even] - oddReal
                imaginary[odd] = imaginary[even] - oddImaginary
                real[even] += oddReal
                imaginary[even] += oddImaginary

                val nextReal = factorReal * stepReal - factorImaginary * stepImaginary
                factorImaginary = factorReal * stepImaginary + factorImaginary * stepReal
                factorReal = nextReal
            }
        }
        length = length shl 1
    }
}

private fun Float.pow(exponent: Float): Float = kotlin.math.exp(ln(this) * exponent)
