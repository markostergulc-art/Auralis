package com.marko.auralis.audio.scheduler

import com.marko.auralis.model.StereoMode
import java.util.Random
import kotlin.math.max
import kotlin.math.roundToLong

enum class PulseChannel { LEFT, RIGHT }

data class PulseEvent(val frame: Long, val channel: PulseChannel)

class StereoPulseScheduler(
    private val sampleRate: Int,
    pulseRateHz: Float,
    private val mode: StereoMode = StereoMode.RANDOM_LR,
    private val minimumSeparationFrames: Int = 1,
    private var randomTimingPercent: Int = 0,
    seed: Long = 0x425241494E4CL
) {
    private val random = Random(seed)
    private var nextFrame = 0L
    private var framesPerPulse = framesPerPulse(sampleRate, pulseRateHz)

    fun updateRate(rateHz: Float) {
        framesPerPulse = framesPerPulse(sampleRate, rateHz)
    }

    fun updateRandomTiming(percent: Int) {
        randomTimingPercent = percent.coerceIn(0, 50)
    }

    fun next(): PulseEvent {
        val channel = if (random.nextBoolean()) PulseChannel.LEFT else PulseChannel.RIGHT

        val event = PulseEvent(nextFrame, channel)

        val randomDepth = randomTimingPercent.coerceIn(0, 50) / 100.0
        val randomFactor = if (randomDepth == 0.0) 1.0 else {
            1.0 + ((random.nextDouble() * 2.0) - 1.0) * randomDepth
        }
        val randomizedInterval = (framesPerPulse * randomFactor).roundToLong()
        nextFrame += max(minimumSeparationFrames.toLong(), randomizedInterval)
        return event
    }

    fun reset(startFrame: Long = 0L) {
        nextFrame = startFrame
    }

    companion object {
        fun framesPerPulse(sampleRate: Int, rateHz: Float): Long {
            val safeRate = rateHz.coerceIn(1f, 6f)
            return max(1L, (sampleRate / safeRate).roundToLong())
        }
    }
}
