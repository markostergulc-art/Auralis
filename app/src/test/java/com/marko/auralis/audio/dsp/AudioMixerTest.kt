package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.mix.AudioMixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMixerTest {
    @Test fun sumsLayersAndAppliesBlockGainOnlyAtFinalStage() {
        val m = AudioMixer(2)
        m.addStereo(0, .6f, -.4f)
        m.addStereo(0, .6f, -.7f)
        assertTrue(m.peak > 1f)
        val out = FloatArray(4)
        m.finalizeInto(out)
        assertEquals(.95f, out[0], 0.0001f)
        assertEquals(-.8708333f, out[1], 0.0001f)
    }
}
