import com.marko.auralis.audio.generators.BreathPacingGenerator
import com.marko.auralis.audio.generators.NoiseGenerator
import com.marko.auralis.audio.generators.NaturalSoundGenerator
import com.marko.auralis.audio.mix.AudioMixer

fun main(){
  val sr=48000
  val breath=BreathPacingGenerator.render(sr,sr*10,BreathPacingGenerator.Config())
  check(breath.size==sr*10)
  check(breath.maxOf{ kotlin.math.abs(it) } <= .1001f)
  for(t in NoiseGenerator.Type.values()){
    val a=NoiseGenerator.render(t,32768,123)
    check(a.any{it!=0f});check(a.all{it in -1f..1f})
  }
  for(t in NaturalSoundGenerator.Type.values()){
    val a=NaturalSoundGenerator.render(t,sr,32768,456)
    check(a.left.size==32768 && a.right.size==32768)
    check(a.left.any{it!=0f} || a.right.any{it!=0f})
    check(a.left.all{it in -1f..1f} && a.right.all{it in -1f..1f})
  }
  val m=AudioMixer(4);m.addStereo(0,.7f,.7f);m.addStereo(0,.5f,.5f);val out=FloatArray(8);m.finalizeInto(out);check(out[0]<=.95f)
  println("PHASE_0_TO_5_PURE_DSP_SMOKE_OK")
}
