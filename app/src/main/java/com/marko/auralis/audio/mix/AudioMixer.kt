package com.marko.auralis.audio.mix

import kotlin.math.abs
import kotlin.math.max

/** Reusable stereo float mixer. No allocation occurs while mixing after construction. */
class AudioMixer(private val frames: Int) {
    val pcm = FloatArray(frames * 2)
    var peak: Float = 0f
        private set

    fun clear() {
        pcm.fill(0f)
        peak = 0f
    }

    fun addStereo(frame: Int, left: Float, right: Float, gain: Float = 1f) {
        if (frame !in 0 until frames) return
        val i = frame * 2
        pcm[i] += left * gain
        pcm[i + 1] += right * gain
        peak = max(peak, max(abs(pcm[i]), abs(pcm[i + 1])))
    }

    fun addMono(frame: Int, value: Float, gain: Float = 1f) = addStereo(frame, value, value, gain)

    /** Conservative final stage. Keeps phase/pulse math unchanged before mixdown.
     * Applies one block-wide protective gain rather than sample-by-sample clipping.
     */
    fun finalizeInto(target: FloatArray, ceiling: Float = 0.95f): Float {
        require(target.size >= pcm.size)
        val gain = if (peak > ceiling && peak > 0f) ceiling / peak else 1f
        for (i in pcm.indices) target[i] = pcm[i] * gain
        return gain
    }
}
