package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.generators.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class AdvancedGeneratorsTest {
 @Test fun generatorsAreFiniteAndBounded(){ val sr=48000; val n=sr*2; val xs=listOf(
   AdvancedGenerators.ambient(sr,n),AdvancedGenerators.binaural(sr,n),AdvancedGenerators.monaural(sr,n),AdvancedGenerators.isochronic(sr,n),AdvancedGenerators.spatialItd(sr,n),AdvancedGenerators.smoothStereo(sr,n),AdvancedGenerators.bilateral(sr,n),ExperimentalSoundGenerator.mod783(sr,n),ExperimentalSoundGenerator.bowl(sr,n),ResearchSoundGenerator.gamma40(sr,n))
   xs.forEach{ s-> assertEquals(n,s.left.size); assertEquals(n,s.right.size); (s.left+s.right).forEach{assertTrue(it.isFinite());assertTrue(abs(it)<1f)} }
 }
 @Test fun itdQuantizesToFrames(){ val sr=48000; val us=400.0; val expected=kotlin.math.round(sr*us/1_000_000.0).toInt(); assertEquals(19,expected) }
 @Test fun researchPulseIsZeroMeanEnough(){ for(shape in ResearchSoundGenerator.PulseShape.values()){ val p=ResearchSoundGenerator.pulse(shape,64); assertTrue(abs(p.average())<0.05) } }
}
