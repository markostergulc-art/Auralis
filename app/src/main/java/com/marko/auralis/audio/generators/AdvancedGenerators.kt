package com.marko.auralis.audio.generators

import kotlin.math.*
import java.util.Random

object AdvancedGenerators {
    data class Stereo(val left: FloatArray, val right: FloatArray)

    fun ambient(sampleRate:Int, frames:Int, level:Float=.12f, brightness:Float=.5f, movement:Float=.35f, density:Float=.5f):Stereo {
        require(sampleRate>0 && frames>=0)
        val l=FloatArray(frames); val r=FloatArray(frames)
        val partials = doubleArrayOf(110.0, 164.81, 220.0, 277.18, 329.63)
        val amps = doubleArrayOf(.50,.28,.23,.16,.12)
        val phases=DoubleArray(partials.size)
        val bright=brightness.coerceIn(0f,1f); val dens=density.coerceIn(0f,1f); val mov=movement.coerceIn(0f,1f)
        for(i in 0 until frames){
            val t=i.toDouble()/sampleRate
            val pan=sin(2*PI*t/16.0)*mov
            var x=0.0
            for(k in partials.indices){
                val enabled = if(k==0) 1.0 else dens.toDouble()
                val tilt = 1.0 - k*(1.0-bright)*0.13
                phases[k]+=2*PI*(partials[k]*(1.0+0.0008*sin(2*PI*t/(7.0+k))))/sampleRate
                x += sin(phases[k])*amps[k]*enabled*tilt
            }
            val env=.72+.20*sin(2*PI*t/13.0)+.08*sin(2*PI*t/29.0)
            l[i]=(x*env*(1.0-pan)*.5*level).toFloat(); r[i]=(x*env*(1.0+pan)*.5*level).toFloat()
        }
        return Stereo(l,r)
    }

    fun binaural(sampleRate:Int, frames:Int, carrierHz:Double=200.0, beatHz:Double=6.0, level:Float=.10f):Stereo {
        require(carrierHz in 100.0..600.0 && beatHz in 1.0..12.0)
        val l=FloatArray(frames); val r=FloatArray(frames); var pL=0.0; var pR=0.0
        for(i in 0 until frames){ pL+=2*PI*carrierHz/sampleRate; pR+=2*PI*(carrierHz+beatHz)/sampleRate; l[i]=(sin(pL)*level).toFloat(); r[i]=(sin(pR)*level).toFloat() }
        return Stereo(l,r)
    }

    fun monaural(sampleRate:Int, frames:Int, carrierHz:Double=200.0, beatHz:Double=6.0, depth:Float=1f, level:Float=.10f):Stereo {
        require(carrierHz in 100.0..600.0 && beatHz in 1.0..12.0)
        val l=FloatArray(frames); val r=FloatArray(frames); var p1=0.0; var p2=0.0
        val d=depth.coerceIn(0f,1f)
        for(i in 0 until frames){ p1+=2*PI*carrierHz/sampleRate; p2+=2*PI*(carrierHz+beatHz)/sampleRate; val raw=(sin(p1)+sin(p2))*.5; val carrier=sin(p1); val v=((1-d)*carrier+d*raw)*level; l[i]=v.toFloat(); r[i]=v.toFloat() }
        return Stereo(l,r)
    }

    fun isochronic(sampleRate:Int, frames:Int, carrierHz:Double=220.0, rateHz:Double=6.0, depth:Float=.8f, softness:Float=.8f, level:Float=.10f):Stereo {
        require(rateHz in 1.0..80.0)
        val l=FloatArray(frames); val r=FloatArray(frames); var p=0.0
        val d=depth.coerceIn(0f,1f); val s=softness.coerceIn(0f,1f)
        for(i in 0 until frames){ val t=i.toDouble()/sampleRate; p+=2*PI*carrierHz/sampleRate; val sinus=.5+.5*sin(2*PI*rateHz*t); val hard=if(sinus>=.5)1.0 else 0.0; val env=(1-d)+d*(s*sinus+(1-s)*hard); val v=(sin(p)*env*level).toFloat(); l[i]=v;r[i]=v }
        return Stereo(l,r)
    }

    fun spatialItd(sampleRate:Int, frames:Int, itdUs:Double=400.0, leadLeft:Boolean=true, level:Float=.08f):Stereo {
        val delay=max(1, round(sampleRate*itdUs/1_000_000.0).toInt()); val l=FloatArray(frames); val r=FloatArray(frames); val mono=FloatArray(frames)
        var p=0.0; for(i in 0 until frames){ p+=2*PI*220/sampleRate; mono[i]=(sin(p)*(.65+.35*sin(2*PI*i/sampleRate/3.0))*level).toFloat() }
        for(i in 0 until frames){ if(leadLeft){l[i]=mono[i]; if(i>=delay)r[i]=mono[i-delay]} else {r[i]=mono[i]; if(i>=delay)l[i]=mono[i-delay]} }
        return Stereo(l,r)
    }

    fun smoothStereo(sampleRate:Int, frames:Int, cycleSeconds:Double=12.0, depth:Float=.7f, level:Float=.08f):Stereo {
        val l=FloatArray(frames); val r=FloatArray(frames); var p=0.0; val d=depth.coerceIn(0f,1f)
        for(i in 0 until frames){ val t=i.toDouble()/sampleRate; p+=2*PI*220/sampleRate; val pan=sin(2*PI*t/cycleSeconds)*d; val v=sin(p)*level; l[i]=(v*(1-pan)*.5).toFloat(); r[i]=(v*(1+pan)*.5).toFloat() }
        return Stereo(l,r)
    }

    fun bilateral(sampleRate:Int, frames:Int, rateHz:Double=1.0, softness:Float=.75f, level:Float=.08f):Stereo {
        val l=FloatArray(frames); val r=FloatArray(frames); var p=0.0
        for(i in 0 until frames){ val t=i.toDouble()/sampleRate; p+=2*PI*220/sampleRate; val x=sin(2*PI*rateHz*t); val smooth=tanh(x*(1.0+7.0*(1-softness.coerceIn(0f,1f)))); val v=sin(p)*level; l[i]=(v*(1-smooth)*.5).toFloat();r[i]=(v*(1+smooth)*.5).toFloat() }
        return Stereo(l,r)
    }
}

object ExperimentalSoundGenerator {
    data class Stereo(val left: FloatArray, val right: FloatArray)
    private fun tone(sr:Int, frames:Int, hz:Double, level:Float, harmonic:Boolean=false):Stereo { val l=FloatArray(frames); val r=FloatArray(frames); var p=0.0; for(i in 0 until frames){p+=2*PI*hz/sr; var v=sin(p); if(harmonic)v=.72*v+.20*sin(2*p)+.08*sin(3*p); val x=(v*level).toFloat();l[i]=x;r[i]=x};return Stereo(l,r)}
    fun pure(sr:Int,frames:Int,hz:Double,level:Float=.08f)=tone(sr,frames,hz,level,false)
    fun harmonic(sr:Int,frames:Int,hz:Double,level:Float=.08f)=tone(sr,frames,hz,level,true)
    fun mod783(sr:Int,frames:Int,carrier:Double=220.0,level:Float=.08f):Stereo { val l=FloatArray(frames);val r=FloatArray(frames);var p=0.0;for(i in 0 until frames){val t=i.toDouble()/sr;p+=2*PI*carrier/sr;val env=.35+.65*(.5+.5*sin(2*PI*7.83*t));val v=(sin(p)*env*level).toFloat();l[i]=v;r[i]=v};return Stereo(l,r)}
    fun bowl(sr:Int,frames:Int,level:Float=.10f):Stereo { val l=FloatArray(frames);val r=FloatArray(frames);val fs=doubleArrayOf(220.0,331.7,512.3,734.1,1012.5);val a=doubleArrayOf(1.0,.55,.32,.20,.12);val ph=DoubleArray(fs.size);for(i in 0 until frames){val t=i.toDouble()/sr;var x=0.0;for(k in fs.indices){ph[k]+=2*PI*fs[k]/sr;x+=sin(ph[k])*a[k]*exp(-t/(4.0+1.8*k))};val v=(x*.45*level).toFloat();l[i]=v;r[i]=v};return Stereo(l,r)}
    fun soundBath(sr:Int,frames:Int,level:Float=.08f,seed:Long=528L):Stereo { val b=bowl(sr,frames,level);val rnd=Random(seed);var lowL=0.0;var lowR=0.0;for(i in 0 until frames){val w1=rnd.nextDouble()*2-1;val w2=rnd.nextDouble()*2-1;lowL+=.001*(w1-lowL);lowR+=.001*(w2-lowR);b.left[i]+=(lowL*level*.35).toFloat();b.right[i]+=(lowR*level*.35).toFloat()};return b }
}

object ResearchSoundGenerator {
    enum class PulseShape { HUAWEI_BIPHASIC, SOFT_BIPHASIC, GAUSSIAN, SINE_BURST }
    fun gamma40(sr:Int,frames:Int,carrier:Double=220.0,level:Float=.06f):AdvancedGenerators.Stereo = AdvancedGenerators.isochronic(sr,frames,carrier,40.0,.8f,1f,level)
    fun pulse(shape:PulseShape,samples:Int):FloatArray { require(samples>=2); return when(shape){
        PulseShape.HUAWEI_BIPHASIC -> { val a=FloatArray(samples); a[0]=1f; var sum=0.0; for(i in 1 until samples){val v=exp(-.6*(i-1));a[i]=-v.toFloat();sum+=v};for(i in 1 until samples)a[i]=(a[i]/sum).toFloat();a }
        PulseShape.SOFT_BIPHASIC -> FloatArray(samples){i-> sin(2*PI*i/(samples-1)).toFloat() }
        PulseShape.GAUSSIAN -> { val a=FloatArray(samples); val c=(samples-1)/2.0; for(i in a.indices){val x=(i-c)/(samples*.18);a[i]=((1-x*x)*exp(-x*x/2)).toFloat()}; val mean=a.average().toFloat();for(i in a.indices)a[i]-=mean;a }
        PulseShape.SINE_BURST -> FloatArray(samples){i-> sin(2*PI*i/(samples-1)).toFloat() }
    }}
}
