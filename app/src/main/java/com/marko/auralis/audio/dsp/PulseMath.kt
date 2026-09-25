package com.marko.auralis.audio.dsp

import com.marko.auralis.audio.scheduler.PulseChannel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

object PulseMath {
    const val MAX_DIGITAL_AMPLITUDE = 0.75f

    fun samplesForDuration(sampleRate: Int, requestedUs: Int): Int {
        require(sampleRate > 0)
        val nearest = (sampleRate * requestedUs.coerceIn(40, 90) / 1_000_000.0).roundToInt()
        return max(2, nearest)
    }

    fun actualDurationUs(sampleRate: Int, sampleCount: Int): Double =
        sampleCount * 1_000_000.0 / sampleRate

    fun samplesForDelayMs(sampleRate: Int, requestedMs: Double): Int {
        require(sampleRate > 0)
        return max(1, (sampleRate * requestedMs.coerceIn(1.0, 100.0) / 1_000.0).roundToInt())
    }

    fun actualDelayMs(sampleRate: Int, sampleCount: Int): Double =
        sampleCount * 1_000.0 / sampleRate

    fun bipolarPulse(sampleCount: Int): FloatArray {
        require(sampleCount >= 2)
        if (sampleCount == 2) return floatArrayOf(1f, -1f)
        // Huawei/handset-friendly asymmetric DC-balanced biphasic transient.
        // Keeps the exact requested sample count while moving more energy into
        // the audible band than the old symmetric second-difference pulse.
        val out = FloatArray(sampleCount)
        out[0] = 1f
        var sum = 0.0
        val weights = DoubleArray(sampleCount - 1) { i -> kotlin.math.exp(-0.6 * i) }
        for (w in weights) sum += w
        for (i in 1 until sampleCount) out[i] = (-weights[i - 1] / sum).toFloat()
        return out
    }

    fun amplitude(volumePercent: Int, randomDynamicsPercent: Int, randomUnit: Float): Float {
        val volume = volumePercent.coerceIn(0, 100) / 100f
        val dynamics = randomDynamicsPercent.coerceIn(0, 50) / 100f
        val r = randomUnit.coerceIn(0f, 1f)
        val dynamicFactor = 1f - (0.75f * dynamics * r)
        return MAX_DIGITAL_AMPLITUDE * volume * dynamicFactor
    }

    fun balanceGain(balancePercent: Int, channel: PulseChannel): Float {
        val b = balancePercent.coerceIn(-100, 100)
        return when {
            b > 0 && channel == PulseChannel.LEFT -> 1f - b / 100f
            b < 0 && channel == PulseChannel.RIGHT -> 1f - (-b) / 100f
            else -> 1f
        }
    }
}
