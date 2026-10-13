package com.neytron.ethereal

import com.neytron.ethereal.tiles.calculateFrequencyLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSpectrumTest {

    @Test
    fun silenceProducesNoBars() {
        val levels = calculateFrequencyLevels(ByteArray(512), samplingRateMilliHz = 48_000_000)

        assertEquals(8, levels.size)
        assertTrue(levels.all { it == 0f })
    }

    @Test
    fun fftPeakOnlyRaisesItsFrequencyBand() {
        val fft = ByteArray(512)
        fft[2] = 100
        fft[3] = 100

        val levels = calculateFrequencyLevels(fft, samplingRateMilliHz = 48_000_000)

        assertTrue(levels[1] > 0f)
        assertEquals(1, levels.count { it > 0f })
    }
}
