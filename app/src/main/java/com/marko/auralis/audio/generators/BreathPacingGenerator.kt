package com.marko.auralis.audio.generators

import kotlin.math.PI
import kotlin.math.sin

object BreathPacingGenerator {
    data class Config(
        val inhaleSeconds: Int = 4,
        val exhaleSeconds: Int = 6,
        val pauseSeconds: Int = 0,
        val level: Float = 0.10f
    ) {
        fun sanitized() = copy(
            inhaleSeconds = inhaleSeconds.coerceIn(3, 8),
            exhaleSeconds = exhaleSeconds.coerceIn(3, 10),
            pauseSeconds = pauseSeconds.coerceIn(0, 3),
            level = level.coerceIn(0f, 0.25f)
        )
    }

    /** Mono cue. Rising tone for inhale, falling tone for exhale, silence during optional pause. */
    fun render(sampleRate: Int, frames: Int, config: Config): FloatArray {
        require(sampleRate > 0 && frames >= 0)
        val c = config.sanitized()
        val cycle = c.inhaleSeconds + c.exhaleSeconds + c.pauseSeconds
        val out = FloatArray(frames)
        var phase = 0.0
        for (i in 0 until frames) {
            val sec = i.toDouble() / sampleRate
            val t = sec % cycle
            val (freq, env) = when {
                t < c.inhaleSeconds -> {
                    val p = t / c.inhaleSeconds
                    (180.0 + 90.0 * p) to sin(PI * p).coerceAtLeast(0.0)
                }
                t < c.inhaleSeconds + c.exhaleSeconds -> {
                    val p = (t - c.inhaleSeconds) / c.exhaleSeconds
                    (270.0 - 100.0 * p) to sin(PI * p).coerceAtLeast(0.0)
                }
                else -> 170.0 to 0.0
            }
            phase += 2.0 * PI * freq / sampleRate
            out[i] = (sin(phase) * env * c.level).toFloat()
        }
        return out
    }
}
