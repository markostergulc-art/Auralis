package com.marko.auralis.content

data class LearnArticle(val id:String,val titleEn:String,val mechanism:String,val limitation:String)
object LearnCatalog {
    val articles=listOf(
        LearnArticle("hz","What does Hz mean?","Hertz means events per second; acoustic rate and EEG band labels are different measurements.","Matching numerical frequencies do not prove matching brain states."),
        LearnArticle("localization","How hearing localizes sound","ITD, interaural level difference and head-related filtering contribute to spatial hearing.","A phone speaker does not preserve L/R separation like headphones."),
        LearnArticle("binaural","Binaural beats","Two different ear-specific tones can create a perceived difference-rate fluctuation.","EEG entrainment and psychological outcomes are not guaranteed."),
        LearnArticle("monaural","Monaural beats","Two nearby tones physically interfere and create amplitude modulation in the waveform.","A measurable beat is not automatically a relaxation effect."),
        LearnArticle("noise","White / Pink / Brown noise","These names specify different spectral power slopes.","Spectrum is defined; individual relaxation response is not universal."),
        LearnArticle("itd","ITD vs Lead/Lag","ITD is microsecond-scale localization timing; millisecond lead/lag crosses into precedence/echo perception.","They must not be described as the same mechanism."),
        LearnArticle("432","432 Hz","Pure 432 Hz and A4=432-tuned music are different stimuli.","Universal/cosmic-frequency claims are not established."),
        LearnArticle("528","528 Hz","528 Hz can be a pure tone or harmonic center.","Ordinary audio has not established DNA repair claims."),
        LearnArticle("schumann","Schumann resonance","The ~7.83 Hz Schumann fundamental is electromagnetic; audio can only share the numeric modulation rate.","Audio modulation is not the Earth-ionosphere electromagnetic field."),
        LearnArticle("safe","Safe listening","Risk depends on real SPL and duration; app gain is not calibrated SPL.","Start low and stop on discomfort or ringing.")
    )
}
