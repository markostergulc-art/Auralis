package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.generators.NaturalSoundGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NaturalSoundGeneratorTest {
    @Test fun allNaturalTypesRenderFiniteStereoWithinLayerBound() {
        NaturalSoundGenerator.Type.entries.forEach { type ->
            val x = NaturalSoundGenerator.render(type, 48_000, 48_000, seed = 7L, level = 0.14f)
            assertEquals(48_000, x.left.size)
            assertEquals(48_000, x.right.size)
            assertTrue(x.left.all { it.isFinite() })
            assertTrue(x.right.all { it.isFinite() })
            assertTrue(x.left.maxOf { abs(it) } <= 0.251f)
            assertTrue(x.right.maxOf { abs(it) } <= 0.251f)
        }
    }

    @Test fun sameSeedIsDeterministic() {
        val a = NaturalSoundGenerator.render(NaturalSoundGenerator.Type.OCEAN, 48_000, 4096, 99L)
        val b = NaturalSoundGenerator.render(NaturalSoundGenerator.Type.OCEAN, 48_000, 4096, 99L)
        assertTrue(a.left.contentEquals(b.left))
        assertTrue(a.right.contentEquals(b.right))
    }
}
