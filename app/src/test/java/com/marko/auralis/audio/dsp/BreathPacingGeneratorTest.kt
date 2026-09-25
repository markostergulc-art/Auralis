package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.generators.BreathPacingGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathPacingGeneratorTest {
    @Test fun defaultCycleIsTenSecondsAndBounded() {
        val sr = 48000
        val out = BreathPacingGenerator.render(sr, sr * 10, BreathPacingGenerator.Config())
        assertEquals(sr * 10, out.size)
        assertTrue(out.maxOf { kotlin.math.abs(it) } <= .10f + 1e-4f)
    }
}
