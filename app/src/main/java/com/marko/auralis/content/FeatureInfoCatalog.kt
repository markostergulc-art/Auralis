package com.marko.auralis.content

import com.marko.auralis.model.EvidenceLevel
import com.marko.auralis.model.FeatureInfo
import com.marko.auralis.model.LocalizedFeatureText

object FeatureInfoCatalog {
    val pulse = FeatureInfo(
        id = "pulse_engine",
        evidenceLevel = EvidenceLevel.EXPERIMENTAL,
        en = LocalizedFeatureText(
            title = "Pulse Engine",
            whatIsThis = "A sample-timed stereo generator of very short broadband transients.",
            whatHappens = "Pulse Rate sets the average number of events per second. Pulse Duration sets the requested digital transient length; at 48 kHz one audio frame is about 20.83 microseconds, so the actual duration is quantized. Random Pulse Timing changes event spacing, Random Dynamics changes pulse amplitude, and Stereo Lead/Lag delays the opposite channel after the leading channel.",
            howToUse = "Start around 2 pulses/s, low Random Pulse Timing and low volume. Change one control at a time so you can hear which parameter caused the difference.",
            whatYouMayNotice = "Individual clicks, changing spatial position, or a more diffuse stereo image as timing and lead/lag are increased.",
            playbackRecommendation = "Headphones give clearer left/right separation. A phone speaker can strongly reshape very short transients.",
            startingPoint = "2.0 pulses/s, 55 microseconds requested duration, 0-10% timing randomization, low volume.",
            evidenceExplanation = "Short clicks and rhythmic auditory stimulation are established psychoacoustic and neuroscience stimuli. Direct evidence that this exact pulse pattern reliably causes relaxation is limited.",
            limitation = "A 4 Hz pulse rate does not by itself mean that the brain enters a 4 Hz or theta state.",
            safety = "Keep the level comfortable, especially with headphones. App volume percentage is not a calibrated sound-pressure measurement."
        ),
        de = LocalizedFeatureText(
            title = "Impuls-Engine",
            whatIsThis = "Ein samplegenau getakteter Stereo-Generator für sehr kurze breitbandige Transienten.",
            whatHappens = "Die Impulsrate bestimmt die durchschnittliche Zahl der Ereignisse pro Sekunde. Die Impulsdauer ist die gewünschte digitale Transientenlänge; bei 48 kHz dauert ein Audio-Frame etwa 20,83 Mikrosekunden, daher wird die reale Dauer quantisiert. Zufälliges Impuls-Timing verändert die Abstände, Zufällige Dynamik die Amplitude und Stereo Lead/Lag verzögert den Gegenkanal nach dem führenden Kanal.",
            howToUse = "Beginnen Sie bei etwa 2 Impulsen/s, wenig Timing-Zufall und niedriger Lautstärke. Ändern Sie jeweils nur einen Parameter, damit die Wirkung hörbar zugeordnet werden kann.",
            whatYouMayNotice = "Einzelne Klicks, wechselnde räumliche Position oder ein diffuseres Stereobild bei stärkerer Timing- und Lead/Lag-Variation.",
            playbackRecommendation = "Kopfhörer liefern eine klarere Links/Rechts-Trennung. Smartphone-Lautsprecher können sehr kurze Transienten stark verändern.",
            startingPoint = "2,0 Impulse/s, 55 Mikrosekunden gewünschte Dauer, 0-10% Timing-Zufall, niedrige Lautstärke.",
            evidenceExplanation = "Kurze Klicks und rhythmische Hörstimulation sind etablierte psychoakustische und neurowissenschaftliche Reize. Direkte Belege dafür, dass genau dieses Impulsmuster zuverlässig entspannt, sind begrenzt.",
            limitation = "Eine Impulsrate von 4 Hz bedeutet nicht automatisch, dass das Gehirn in einen 4-Hz- oder Theta-Zustand übergeht.",
            safety = "Lautstärke angenehm halten, besonders mit Kopfhörern. Die App-Prozentanzeige ist keine kalibrierte Schalldruckmessung."
        ),
        hr = LocalizedFeatureText(
            title = "Generator impulsa",
            whatIsThis = "Stereo generator vrlo kratkih širokopojasnih tranzijenata, vremenski raspoređenih na razini audio sampleova.",
            whatHappens = "Brzina impulsa određuje prosječan broj događaja u sekundi. Trajanje impulsa je tražena digitalna duljina tranzijenta; pri 48 kHz jedan audio frame traje oko 20,83 mikrosekunde pa se stvarno trajanje kvantizira. Nasumično vrijeme impulsa mijenja razmake, Nasumična dinamika amplitudu, a Stereo Lead/Lag odgađa suprotni kanal nakon vodećeg kanala.",
            howToUse = "Počnite oko 2 impulsa/s, s malo Random Pulse Timing-a i niskom glasnoćom. Mijenjajte jedan parametar odjednom kako biste mogli čuti što je napravilo razliku.",
            whatYouMayNotice = "Pojedinačne klikove, promjenu prostornog položaja ili difuzniju stereo sliku kada povećate vremensku slučajnost i Lead/Lag.",
            playbackRecommendation = "Slušalice daju jasnije razdvajanje lijevog i desnog kanala. Zvučnik telefona može snažno promijeniti vrlo kratke tranzijente.",
            startingPoint = "2,0 impulsa/s, 55 mikrosekundi traženog trajanja, 0-10% vremenske slučajnosti i niska glasnoća.",
            evidenceExplanation = "Kratki klikovi i ritmička slušna stimulacija etablirani su psihoakustički i neuroznanstveni podražaji. Izravni dokazi da baš ovaj uzorak pouzdano izaziva relaksaciju još su ograničeni.",
            limitation = "Brzina od 4 Hz sama po sebi ne znači da mozak prelazi u 4-Hz ili theta stanje.",
            safety = "Koristite ugodnu razinu, osobito sa slušalicama. Postotak glasnoće u aplikaciji nije kalibrirano mjerenje zvučnog tlaka."
        )
    )
}
