package com.marko.auralis.model

data class SoundLayerConfig(
    val ambientEnabled:Boolean=false, val ambientBrightness:Int=50, val ambientMovement:Int=35, val ambientDensity:Int=50, val ambientLevel:Int=12,
    val binauralEnabled:Boolean=false, val binauralCarrierHz:Double=200.0, val binauralBeatHz:Double=6.0, val binauralLevel:Int=10,
    val monauralEnabled:Boolean=false, val monauralCarrierHz:Double=200.0, val monauralBeatHz:Double=6.0, val monauralDepth:Int=100, val monauralLevel:Int=10,
    val isochronicEnabled:Boolean=false, val isochronicCarrierHz:Double=220.0, val isochronicRateHz:Double=6.0, val isochronicDepth:Int=80, val isochronicSoftness:Int=80, val isochronicLevel:Int=10,
    val itdEnabled:Boolean=false, val itdUs:Int=400, val itdLeadLeft:Boolean=true, val itdLevel:Int=8,
    val smoothMotionEnabled:Boolean=false, val motionCycleSeconds:Int=12, val motionDepth:Int=70, val motionLevel:Int=8,
    val bilateralEnabled:Boolean=false, val bilateralRateHz:Double=1.0, val bilateralSoftness:Int=75, val bilateralLevel:Int=8,
    val alternativeMode:AlternativeMode=AlternativeMode.OFF, val solfeggioHz:Int=528, val alternativeLevel:Int=8,
    val researchMode:ResearchMode=ResearchMode.OFF, val researchPulseShape:String="huawei", val researchPulseDurationUs:Int=50, val researchLevel:Int=6
) {
    fun sanitized()=copy(
        ambientBrightness=ambientBrightness.coerceIn(0,100),ambientMovement=ambientMovement.coerceIn(0,100),ambientDensity=ambientDensity.coerceIn(0,100),ambientLevel=ambientLevel.coerceIn(0,25),
        binauralCarrierHz=binauralCarrierHz.coerceIn(100.0,600.0),binauralBeatHz=binauralBeatHz.coerceIn(1.0,12.0),binauralLevel=binauralLevel.coerceIn(0,20),
        monauralCarrierHz=monauralCarrierHz.coerceIn(100.0,600.0),monauralBeatHz=monauralBeatHz.coerceIn(1.0,12.0),monauralDepth=monauralDepth.coerceIn(0,100),monauralLevel=monauralLevel.coerceIn(0,20),
        isochronicCarrierHz=isochronicCarrierHz.coerceIn(100.0,600.0),isochronicRateHz=isochronicRateHz.coerceIn(1.0,12.0),isochronicDepth=isochronicDepth.coerceIn(0,100),isochronicSoftness=isochronicSoftness.coerceIn(0,100),isochronicLevel=isochronicLevel.coerceIn(0,20),
        itdUs=itdUs.coerceIn(20,800),itdLevel=itdLevel.coerceIn(0,20),motionCycleSeconds=motionCycleSeconds.coerceIn(4,30),motionDepth=motionDepth.coerceIn(0,100),motionLevel=motionLevel.coerceIn(0,20),
        bilateralRateHz=bilateralRateHz.coerceIn(.5,4.0),bilateralSoftness=bilateralSoftness.coerceIn(0,100),bilateralLevel=bilateralLevel.coerceIn(0,20),
        solfeggioHz=if(solfeggioHz in setOf(174,285,396,417,528,639,741,852,963)) solfeggioHz else 528,alternativeLevel=alternativeLevel.coerceIn(0,20),researchPulseDurationUs=researchPulseDurationUs.coerceIn(50,5000),researchLevel=researchLevel.coerceIn(0,15)
    )
}
enum class AlternativeMode { OFF, PURE_432, AMBIENT_432, PURE_528, HARMONIC_528, SOLFEGGIO, MOD_783, BOWL, SOUND_BATH, HUMMING }
enum class ResearchMode { OFF, GAMMA_40, ADVANCED_PULSE }
