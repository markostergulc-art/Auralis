import com.marko.auralis.audio.dsp.PulseMath
import com.marko.auralis.audio.generators.AdvancedGenerators
import com.marko.auralis.audio.generators.ResearchSoundGenerator
import kotlin.math.*

fun main(){
    val rates=listOf(44100,48000,96000,192000)
    for(sr in rates){
        for(us in listOf(40,55,70,90)){
            val n=PulseMath.samplesForDuration(sr,us)
            val p=PulseMath.bipolarPulse(n)
            check(p.size==n);check(p.all{it.isFinite()});check(abs(p.sum())<1e-5f)
            check(p.maxOf{abs(it)}<=1.00001f)
            val actual=PulseMath.actualDurationUs(sr,n)
            check(actual>0 && actual<150)
        }
        for(ms in listOf(1.0,30.0,100.0)){
            val n=PulseMath.samplesForDelayMs(sr,ms)
            val actual=PulseMath.actualDelayMs(sr,n)
            check(abs(actual-ms)<=1000.0/sr+.0001)
        }
        for(us in listOf(20.0,400.0,800.0)){
            val n=sr/5
            val s=AdvancedGenerators.spatialItd(sr,n,us,true)
            val l=s.left.indexOfFirst{abs(it)>1e-5}; val r=s.right.indexOfFirst{abs(it)>1e-5}
            check(l>=0 && r>l)
            val actual=(r-l)*1_000_000.0/sr
            check(abs(actual-us)<=1_000_000.0/sr+0.01)
        }
        val b=AdvancedGenerators.binaural(sr,sr,200.0,6.0)
        check(b.left.all{it.isFinite()} && b.right.all{it.isFinite()})
        val g=ResearchSoundGenerator.gamma40(sr,sr)
        check(g.left.all{it.isFinite()} && g.left.maxOf{abs(it)}<.2f)
        println("RATE $sr OK pulse55=${PulseMath.samplesForDuration(sr,55)} samples delay100=${PulseMath.samplesForDelayMs(sr,100.0)} frames")
    }
    println("DEVICE_COMPAT_DSP_OK")
}
