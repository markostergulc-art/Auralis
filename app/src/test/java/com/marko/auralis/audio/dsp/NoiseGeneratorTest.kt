package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.generators.NoiseGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseGeneratorTest {
    @Test fun deterministicAndBounded() {
        for (type in NoiseGenerator.Type.values()) {
            val a=NoiseGenerator.render(type,4096,7)
            val b=NoiseGenerator.render(type,4096,7)
            assertEquals(a.toList(),b.toList())
            assertTrue(a.all { it in -1f..1f })
        }
    }
}
