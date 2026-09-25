package com.marko.auralis.model

data class AudioConfig(
    val pulseRateHz: Float = 2.0f,
    val requestedPulseUs: Int = 55,
    val volumePercent: Int = 20,
    val balancePercent: Int = 0,
    val randomDynamicsPercent: Int = 20,
    val randomTimingPercent: Int = 0,
    val stereoMode: StereoMode = StereoMode.RANDOM_LR,
    val randomLeftDelayMs: Int = 30,
    val randomRightDelayMs: Int = 30,
    val minimumStereoSeparationFrames: Int = 1,
    val audioMode: AudioMode = AudioMode.LOW_LATENCY,
    val timerMinutes: Int = 0
) {
    fun sanitized(): AudioConfig = copy(
        pulseRateHz = pulseRateHz.coerceIn(1.0f, 6.0f),
        requestedPulseUs = requestedPulseUs.coerceIn(40, 90),
        volumePercent = volumePercent.coerceIn(0, 100),
        balancePercent = balancePercent.coerceIn(-100, 100),
        randomDynamicsPercent = randomDynamicsPercent.coerceIn(0, 50),
        randomTimingPercent = randomTimingPercent.coerceIn(0, 50),
        stereoMode = StereoMode.RANDOM_LR,
        randomLeftDelayMs = randomLeftDelayMs.coerceIn(1, 100),
        randomRightDelayMs = randomRightDelayMs.coerceIn(1, 100),
        minimumStereoSeparationFrames = minimumStereoSeparationFrames.coerceIn(1, 64),
        timerMinutes = timerMinutes.coerceIn(0, 12 * 60 + 59)
    )
}

enum class StereoMode { RANDOM_LR }

enum class AudioMode { LOW_LATENCY, COMPATIBLE }
