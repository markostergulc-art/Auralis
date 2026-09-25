package com.marko.auralis.audio.scheduler

import com.marko.auralis.model.StereoMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StereoPulseSchedulerTest {
    @Test fun fiveMillionBalancedEventsNeverShareAFrameAtMaxRate() {
        val scheduler = StereoPulseScheduler(48_000, 6f, StereoMode.BALANCED_RANDOM, seed = 123)
        var previous: PulseEvent? = null
        repeat(5_000_000) {
            val event = scheduler.next()
            previous?.let { p -> assertTrue(event.frame > p.frame) }
            previous = event
        }
    }

    @Test fun balancedRandomNeverDriftsFarFromCenter() {
        val scheduler = StereoPulseScheduler(44_100, 5.7f, StereoMode.BALANCED_RANDOM, seed = 7)
        var left = 0
        var right = 0
        var lastFrame = -1L
        repeat(250_000) {
            val event = scheduler.next()
            assertTrue(event.frame > lastFrame)
            lastFrame = event.frame
            if (event.channel == PulseChannel.LEFT) left++ else right++
            assertTrue(abs(left - right) <= 2)
        }
    }

    @Test fun randomLrAllowsUnconstrainedChannelSelectionButFramesAlwaysAdvance() {
        val scheduler = StereoPulseScheduler(48_000, 6f, StereoMode.RANDOM_LR, seed = 11)
        var lastFrame = -1L
        var left = 0
        var right = 0
        repeat(100_000) {
            val event = scheduler.next()
            assertTrue(event.frame > lastFrame)
            lastFrame = event.frame
            if (event.channel == PulseChannel.LEFT) left++ else right++
        }
        assertTrue(left > 45_000)
        assertTrue(right > 45_000)
    }

    @Test fun randomTiming50PercentStillRespectsMinimumSeparation() {
        val scheduler = StereoPulseScheduler(
            sampleRate = 48_000,
            pulseRateHz = 6f,
            mode = StereoMode.RANDOM_LR,
            minimumSeparationFrames = 64,
            randomTimingPercent = 50,
            seed = 99
        )
        var prior = scheduler.next()
        repeat(100_000) {
            val current = scheduler.next()
            assertTrue(current.frame - prior.frame >= 64)
            prior = current
        }
    }

    @Test fun framesPerPulseMatchesRequestedRate() {
        assertEquals(48_000L, StereoPulseScheduler.framesPerPulse(48_000, 1f))
        assertEquals(8_000L, StereoPulseScheduler.framesPerPulse(48_000, 6f))
    }
}
