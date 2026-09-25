package com.marko.auralis.model

enum class EvidenceLevel {
    EVIDENCE_SUPPORTED,
    EMERGING_EVIDENCE,
    EXPERIMENTAL,
    TRADITIONAL_EXPLORATORY,
    RESEARCH_ONLY
}

data class LocalizedFeatureText(
    val title: String,
    val whatIsThis: String,
    val whatHappens: String,
    val howToUse: String,
    val whatYouMayNotice: String,
    val playbackRecommendation: String,
    val startingPoint: String,
    val evidenceExplanation: String,
    val limitation: String,
    val safety: String
)

data class FeatureInfo(
    val id: String,
    val evidenceLevel: EvidenceLevel,
    val en: LocalizedFeatureText,
    val de: LocalizedFeatureText,
    val hr: LocalizedFeatureText
)
