import com.marko.auralis.audio.dsp.PulseMath
import com.marko.auralis.audio.scheduler.StereoPulseScheduler
import com.marko.auralis.audio.generators.*
import com.marko.auralis.audio.mix.AudioMixer
import com.marko.auralis.model.SoundLayerConfig
import com.marko.auralis.model.StereoMode
import com.marko.auralis.audio.scheduler.PulseChannel
import kotlin.math.*

private fun rms(a: FloatArray): Double = sqrt(a.fold(0.0){s,v->s+v*v}/max(1,a.size))
private fun crossings(a:FloatArray):Int { var c=0; for(i in 1 until a.size) if((a[i-1]<=0f && a[i]>0f)||(a[i-1]>=0f && a[i]<0f)) c++; return c }
private fun estimateHz(a:FloatArray, seconds:Double)=crossings(a)/2.0/seconds

private fun goertzel(a:FloatArray, sr:Int, hz:Double):Double {
    val w=2.0*PI*hz/sr; val c=2.0*cos(w); var s0:Double; var s1=0.0; var s2=0.0
    for(v in a){ s0=v + c*s1 - s2; s2=s1; s1=s0 }
    return max(1e-20, s1*s1+s2*s2-c*s1*s2)
}
private fun spectralSlope(a:FloatArray,sr:Int):Double {
    val fs=doubleArrayOf(125.0,250.0,500.0,1000.0,2000.0,4000.0,8000.0,12000.0).filter{it<sr*.45}
    val xs=fs.map{ln(it)}; val ys=fs.map{ln(goertzel(a,sr,it))}; val xm=xs.average(); val ym=ys.average()
    return xs.zip(ys).sumOf{(x,y)->(x-xm)*(y-ym)} / xs.sumOf{(it-xm)*(it-xm)}
}

fun main(){
    val sampleRates=listOf(44100,48000,96000,192000)
    for(sr in sampleRates){
        for(us in listOf(40,55,70,90)){
            val n=PulseMath.samplesForDuration(sr,us)
            check(n>=2)
            val actual=PulseMath.actualDurationUs(sr,n)
            check(actual>0.0 && actual<200.0)
        }
        val sched=StereoPulseScheduler(sr,6.0f, StereoMode.BALANCED_RANDOM, minimumSeparationFrames=1, randomTimingPercent=50, seed=1234L)
        var last=-1L; var l=0; var r=0
        repeat(100000){
            val e=sched.next()
            check(e.frame>last); last=e.frame
            if(e.channel==PulseChannel.LEFT)l++ else r++
        }
        check(abs(l-r)<1000)
    }

    val sr=48000
    val seconds=4
    val n=sr*seconds
    val bin=AdvancedGenerators.binaural(sr,n,200.0,6.0)
    check(abs(estimateHz(bin.left,seconds.toDouble())-200.0)<1.0)
    check(abs(estimateHz(bin.right,seconds.toDouble())-206.0)<1.0)
    val mon=AdvancedGenerators.monaural(sr,n,200.0,6.0)
    check(mon.left.contentEquals(mon.right)); check(rms(mon.left)>.01)
    val iso=AdvancedGenerators.isochronic(sr,n,220.0,6.0)
    check(rms(iso.left)>.01)

    val itd=AdvancedGenerators.spatialItd(sr,sr,400.0,true)
    val iL=itd.left.indexOfFirst{abs(it)>1e-5}; val iR=itd.right.indexOfFirst{abs(it)>1e-5}
    check(iR-iL==19)

    val white=NoiseGenerator.render(NoiseGenerator.Type.WHITE, sr*8, 42L)
    val pink=NoiseGenerator.render(NoiseGenerator.Type.PINK, sr*8, 42L)
    val brown=NoiseGenerator.render(NoiseGenerator.Type.BROWN, sr*8, 42L)
    val sw=spectralSlope(white,sr); val sp=spectralSlope(pink,sr); val sb=spectralSlope(brown,sr)
    check(sw in -0.55..0.55) {"white slope $sw"}
    check(sp in -1.65..-0.35) {"pink slope $sp"}
    check(sb < -1.25) {"brown slope $sb"}
    check(sb < sp-.25) {"expected brown steeper than pink: p=$sp b=$sb"}

    val mx=AudioMixer(16); val out=FloatArray(32)
    repeat(16){mx.addStereo(it,1.2f,-1.2f)}
    val g=mx.finalizeInto(out,.95f)
    check(g<1f); check(out.maxOf{abs(it)}<=.95001f)
    check(out.all{it.isFinite()})

    val dirty=SoundLayerConfig(
        ambientLevel=99,binauralCarrierHz=999.0,binauralBeatHz=99.0,monauralDepth=-2,
        isochronicRateHz=40.0,itdUs=9999,motionCycleSeconds=1,bilateralRateHz=10.0,
        solfeggioHz=111,researchPulseDurationUs=99999,researchLevel=99
    ).sanitized()
    check(dirty.ambientLevel==25);check(dirty.binauralCarrierHz==600.0);check(dirty.binauralBeatHz==12.0)
    check(dirty.monauralDepth==0);check(dirty.isochronicRateHz==12.0);check(dirty.itdUs==800)
    check(dirty.motionCycleSeconds==4);check(dirty.bilateralRateHz==4.0);check(dirty.solfeggioHz==528)
    check(dirty.researchPulseDurationUs==5000);check(dirty.researchLevel==15)

    for(shape in ResearchSoundGenerator.PulseShape.values()){
        val p=ResearchSoundGenerator.pulse(shape,64)
        check(p.all{it.isFinite()}); check(abs(p.average())<.07)
    }
    println("FINAL_DSP_REGRESSION_OK")
    println("NOISE_SLOPES white=%.3f pink=%.3f brown=%.3f".format(sw,sp,sb))
    println("BINAURAL_HZ L=%.2f R=%.2f".format(estimateHz(bin.left,seconds.toDouble()),estimateHz(bin.right,seconds.toDouble())))
    println("ITD_400US_FRAMES=${iR-iL}")
    println("MIX_PROTECT_GAIN=$g")
}
