package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.scheduler.PulseChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PulseMathTest {
    @Test fun commonSampleRatesProduceExpectedDurations() {
        assertEquals(2, PulseMath.samplesForDuration(48_000, 40))
        assertEquals(2, PulseMath.samplesForDuration(48_000, 50))
        assertEquals(3, PulseMath.samplesForDuration(48_000, 55))
        assertEquals(3, PulseMath.samplesForDuration(48_000, 70))
        assertEquals(4, PulseMath.samplesForDuration(48_000, 90))
        assertEquals(2, PulseMath.samplesForDuration(44_100, 50))
        assertEquals(2, PulseMath.samplesForDuration(44_100, 55))
        assertEquals(3, PulseMath.samplesForDuration(44_100, 70))
        assertEquals(5, PulseMath.samplesForDuration(96_000, 55))
    }

    @Test fun bipolarPulseHasNearZeroDcAndNeverExceedsOne() {
        for (n in 2..16) {
            val p = PulseMath.bipolarPulse(n)
            assertEquals(n, p.size)
            assertTrue(abs(p.sum()) < 1e-5f)
            assertTrue(p.all { abs(it) <= 1.00001f })
        }
    }

    @Test fun amplitudeStaysWithinMasterAndNeverNegative() {
        for (volume in 0..100 step 5) {
            for (dynamics in 0..50 step 5) {
                for (r in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                    val a = PulseMath.amplitude(volume, dynamics, r)
                    val max = PulseMath.MAX_DIGITAL_AMPLITUDE * volume / 100f
                    assertTrue(a >= 0f)
                    assertTrue(a <= max + 1e-6f)
                }
            }
        }
    }

    @Test fun balanceCenterAndExtremesAreCorrect() {
        assertEquals(1f, PulseMath.balanceGain(0, PulseChannel.LEFT), 0f)
        assertEquals(1f, PulseMath.balanceGain(0, PulseChannel.RIGHT), 0f)
        assertEquals(1f, PulseMath.balanceGain(-100, PulseChannel.LEFT), 0f)
        assertEquals(0f, PulseMath.balanceGain(-100, PulseChannel.RIGHT), 0f)
        assertEquals(0f, PulseMath.balanceGain(100, PulseChannel.LEFT), 0f)
        assertEquals(1f, PulseMath.balanceGain(100, PulseChannel.RIGHT), 0f)
    }
}
