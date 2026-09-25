package com.marko.auralis.model

data class SessionProtocol(val id:String,val evidence:String,val description:String)
object SessionProtocols {
    val all=listOf(
        SessionProtocol("calm","supported","Natural sound plus subtle slow stereo motion."),
        SessionProtocol("deep_relax","supported","4 s inhale / 6 s exhale with low-level natural sound."),
        SessionProtocol("focused_calm","emerging","Ambient layer with mild monaural modulation."),
        SessionProtocol("spatial_calm","experimental","Ambient layer with slow left-right movement."),
        SessionProtocol("pulse_exploration","experimental","Existing pulse engine with low timing and amplitude randomness."),
        SessionProtocol("binaural_6","emerging","6 Hz binaural difference; headphones required for intended separation."),
        SessionProtocol("432_experimental","experimental","Ambient system tuned around A4=432 Hz; not a universal-frequency claim."),
        SessionProtocol("528_experimental","experimental","528 Hz centered harmonic exploration; no DNA/cellular claims.")
    )
}
