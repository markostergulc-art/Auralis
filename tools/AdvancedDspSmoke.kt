import com.marko.auralis.audio.generators.*
import kotlin.math.*

fun rms(a:FloatArray)=sqrt(a.fold(0.0){s,v->s+v*v}/a.size)
fun crossings(a:FloatArray):Int { var c=0; for(i in 1 until a.size) if((a[i-1]<=0 && a[i]>0)||(a[i-1]>=0 && a[i]<0)) c++; return c }
fun lagPeak(a:FloatArray,b:FloatArray,maxLag:Int):Int { var best=0; var score=-1e99; for(lag in 0..maxLag){var s=0.0;var n=0;for(i in lag until minOf(a.size,b.size)){s+=a[i]*b[i-lag];n++}; if(n>0 && s>score){score=s;best=lag}};return best }
fun main(){
 val sr=48000; val n=sr*4
 val amb=AdvancedGenerators.ambient(sr,n); check(rms(amb.left)>0.001 && rms(amb.left)<.2)
 val bin=AdvancedGenerators.binaural(sr,sr,200.0,6.0); val zL=crossings(bin.left)/2.0;val zR=crossings(bin.right)/2.0;check(abs(zL-200)<3);check(abs(zR-206)<3)
 val mon=AdvancedGenerators.monaural(sr,sr,200.0,6.0);check(rms(mon.left)>0.02);check(mon.left.contentEquals(mon.right))
 val iso=AdvancedGenerators.isochronic(sr,sr*2,220.0,6.0);check(rms(iso.left)>0.01)
 val itd=AdvancedGenerators.spatialItd(sr,sr,400.0,true); val firstL=itd.left.indexOfFirst{abs(it)>1e-5}; val firstR=itd.right.indexOfFirst{abs(it)>1e-5}; check(firstR-firstL in 18..20)
 val mot=AdvancedGenerators.smoothStereo(sr,n,12.0,.8f);var dmin=1e9;var dmax=-1e9;for(i in mot.left.indices step 200){val d=mot.right[i]-mot.left[i];dmin=min(dmin,d.toDouble());dmax=max(dmax,d.toDouble())};check(dmin<0 && dmax>0)
 val bil=AdvancedGenerators.bilateral(sr,n,1.0,.8f);var leftDom=0;var rightDom=0;for(i in bil.left.indices step 200){if(abs(bil.left[i])>abs(bil.right[i]))leftDom++ else if(abs(bil.right[i])>abs(bil.left[i]))rightDom++};check(leftDom>10 && rightDom>10)
 for(hz in listOf(432.0,528.0,174.0,963.0)){val t=ExperimentalSoundGenerator.pure(sr,sr,hz);check(rms(t.left)>.01)}
 val m783=ExperimentalSoundGenerator.mod783(sr,sr*2);check(rms(m783.left)>.01)
 val bowl=ExperimentalSoundGenerator.bowl(sr,sr*2);check(rms(bowl.left)>.001)
 val g40=ResearchSoundGenerator.gamma40(sr,sr*2);check(rms(g40.left)>.005)
 for(sh in ResearchSoundGenerator.PulseShape.values()){val p=ResearchSoundGenerator.pulse(sh,64);check(p.all{it.isFinite()});check(abs(p.average())<.06)}
 println("ADVANCED_DSP_SMOKE_OK")
 println("BINAURAL_EST_HZ L=$zL R=$zR")
 println("ITD_400US_FRAMES=${firstR-firstL}")
}
