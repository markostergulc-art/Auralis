package com.marko.auralis.model

data class SubjectiveScores(
    val relaxation: Int,
    val stress: Int,
    val mood: Int,
    val sleepiness: Int,
    val pleasantness: Int? = null,
    val wouldUseAgain: Boolean? = null
) {
    fun sanitized() = copy(
        relaxation = relaxation.coerceIn(0, 100), stress = stress.coerceIn(0, 100),
        mood = mood.coerceIn(0, 100), sleepiness = sleepiness.coerceIn(0, 100),
        pleasantness = pleasantness?.coerceIn(0, 100)
    )
}

data class ExperimentSession(
    val id: Long,
    val protocolId: String,
    val blinded: Boolean,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val pre: SubjectiveScores,
    val post: SubjectiveScores
)

data class PersonalProtocolSummary(
    val protocolId: String,
    val sessions: Int,
    val medianRelaxationChange: Double?,
    val medianStressChange: Double?,
    val medianMoodChange: Double?,
    val enoughData: Boolean
)

object PersonalExperimentAnalysis {
    const val MIN_SESSIONS = 5
    fun summarize(protocolId: String, sessions: List<ExperimentSession>): PersonalProtocolSummary {
        val x = sessions.filter { it.protocolId == protocolId }
        if (x.size < MIN_SESSIONS) return PersonalProtocolSummary(protocolId, x.size, null, null, null, false)
        fun med(v: List<Int>): Double { val a=v.sorted(); val m=a.size/2; return if(a.size%2==1)a[m].toDouble() else (a[m-1]+a[m])/2.0 }
        return PersonalProtocolSummary(
            protocolId, x.size,
            med(x.map { it.post.relaxation-it.pre.relaxation }),
            med(x.map { it.post.stress-it.pre.stress }),
            med(x.map { it.post.mood-it.pre.mood }), true
        )
    }
}
