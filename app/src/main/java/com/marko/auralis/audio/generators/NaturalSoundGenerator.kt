package com.marko.auralis.audio.generators

import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural, offline natural-sound textures used by the Relax layer.
 *
 * These are deliberately synthetic approximations, not recordings. The output is
 * deterministic for a given seed and is rendered as a stereo buffer so the same
 * engine can be unit-tested without Android dependencies.
 */
object NaturalSoundGenerator {
    enum class Type { RAIN, OCEAN, STREAM, WIND, FOREST }

    data class Stereo(val left: FloatArray, val right: FloatArray)

    fun render(
        type: Type,
        sampleRate: Int,
        frames: Int,
        seed: Long = 0x4E41545552454CL,
        level: Float = 0.14f
    ): Stereo {
        require(sampleRate > 0)
        require(frames >= 0)
        val gain = level.coerceIn(0f, 0.25f)
        val left = FloatArray(frames)
        val right = FloatArray(frames)
        val random = Random(seed)

        when (type) {
            Type.RAIN -> renderRain(left, right, sampleRate, random)
            Type.OCEAN -> renderOcean(left, right, sampleRate, random)
            Type.STREAM -> renderStream(left, right, sampleRate, random)
            Type.WIND -> renderWind(left, right, sampleRate, random)
            Type.FOREST -> renderForest(left, right, sampleRate, random)
        }

        // Keep a conservative layer peak before the global mixer and soften loop seams.
        var peak = 1e-9f
        for (i in 0 until frames) peak = maxOf(peak, kotlin.math.abs(left[i]), kotlin.math.abs(right[i]))
        val norm = if (peak > 0.95f) 0.95f / peak else 1f
        for (i in 0 until frames) {
            left[i] *= gain * norm
            right[i] *= gain * norm
        }
        seamCrossfade(left, right, sampleRate)
        return Stereo(left, right)
    }

    private fun renderRain(l: FloatArray, r: FloatArray, sr: Int, random: Random) {
        var lpL = 0.0; var lpR = 0.0
        var hpL = 0.0; var hpR = 0.0
        var prevLpL = 0.0; var prevLpR = 0.0
        val dropProb = 18.0 / sr // sparse bright drops per sample
        var dropL = 0.0; var dropR = 0.0
        for (i in l.indices) {
            val shared = random.nextDouble() * 2.0 - 1.0
            val wL = shared * 0.72 + (random.nextDouble() * 2.0 - 1.0) * 0.28
            val wR = shared * 0.72 + (random.nextDouble() * 2.0 - 1.0) * 0.28
            lpL += 0.035 * (wL - lpL); lpR += 0.035 * (wR - lpR)
            hpL = lpL - prevLpL; hpR = lpR - prevLpR
            prevLpL = lpL; prevLpR = lpR
            if (random.nextDouble() < dropProb) dropL += 0.55 + random.nextDouble() * 0.35
            if (random.nextDouble() < dropProb) dropR += 0.55 + random.nextDouble() * 0.35
            dropL *= 0.992; dropR *= 0.992
            l[i] = (0.60 * lpL + 6.0 * hpL + dropL).toFloat()
            r[i] = (0.60 * lpR + 6.0 * hpR + dropR).toFloat()
        }
    }

    private fun renderOcean(l: FloatArray, r: FloatArray, sr: Int, random: Random) {
        var lowL = 0.0; var lowR = 0.0
        var foamL = 0.0; var foamR = 0.0
        for (i in l.indices) {
            val t = i.toDouble() / sr
            val swell = 0.28 + 0.72 * (0.5 + 0.5 * sin(2.0 * PI * 0.075 * t + 0.6 * sin(2.0 * PI * 0.017 * t)))
            val shared = random.nextDouble() * 2.0 - 1.0
            val wL = shared * 0.80 + (random.nextDouble() * 2.0 - 1.0) * 0.20
            val wR = shared * 0.80 + (random.nextDouble() * 2.0 - 1.0) * 0.20
            lowL += 0.0035 * (wL - lowL); lowR += 0.0035 * (wR - lowR)
            foamL += 0.06 * (wL - foamL); foamR += 0.06 * (wR - foamR)
            l[i] = (1.25 * lowL + 0.45 * foamL * swell).toFloat()
            r[i] = (1.25 * lowR + 0.45 * foamR * swell).toFloat()
        }
    }

    private fun renderStream(l: FloatArray, r: FloatArray, sr: Int, random: Random) {
        var lowL = 0.0; var lowR = 0.0
        var midL = 0.0; var midR = 0.0
        var bubblePhaseL = 0.0; var bubblePhaseR = 0.0
        var bubbleEnvL = 0.0; var bubbleEnvR = 0.0
        var bubbleFreqL = 900.0; var bubbleFreqR = 1050.0
        for (i in l.indices) {
            val shared = random.nextDouble() * 2.0 - 1.0
            val wL = shared * 0.65 + (random.nextDouble() * 2.0 - 1.0) * 0.35
            val wR = shared * 0.65 + (random.nextDouble() * 2.0 - 1.0) * 0.35
            lowL += 0.012 * (wL - lowL); lowR += 0.012 * (wR - lowR)
            midL += 0.11 * (wL - midL); midR += 0.11 * (wR - midR)
            if (random.nextDouble() < 5.0 / sr) { bubbleEnvL = 1.0; bubbleFreqL = 650.0 + random.nextDouble() * 850.0 }
            if (random.nextDouble() < 5.0 / sr) { bubbleEnvR = 1.0; bubbleFreqR = 650.0 + random.nextDouble() * 850.0 }
            bubblePhaseL += 2.0 * PI * bubbleFreqL / sr; bubblePhaseR += 2.0 * PI * bubbleFreqR / sr
            bubbleEnvL *= 0.996; bubbleEnvR *= 0.996
            val bL = sin(bubblePhaseL) * bubbleEnvL * 0.22
            val bR = sin(bubblePhaseR) * bubbleEnvR * 0.22
            l[i] = (0.75 * lowL + 0.55 * midL + bL).toFloat()
            r[i] = (0.75 * lowR + 0.55 * midR + bR).toFloat()
        }
    }

    private fun renderWind(l: FloatArray, r: FloatArray, sr: Int, random: Random) {
        var lowL = 0.0; var lowR = 0.0
        var gustL = 0.0; var gustR = 0.0
        for (i in l.indices) {
            val t = i.toDouble() / sr
            val macro = 0.35 + 0.65 * (0.5 + 0.5 * sin(2.0 * PI * 0.045 * t + 0.8 * sin(2.0 * PI * 0.011 * t)))
            val shared = random.nextDouble() * 2.0 - 1.0
            val wL = shared * 0.88 + (random.nextDouble() * 2.0 - 1.0) * 0.12
            val wR = shared * 0.88 + (random.nextDouble() * 2.0 - 1.0) * 0.12
            lowL += 0.004 * (wL - lowL); lowR += 0.004 * (wR - lowR)
            gustL += 0.0007 * ((random.nextDouble() * 2.0 - 1.0) - gustL)
            gustR += 0.0007 * ((random.nextDouble() * 2.0 - 1.0) - gustR)
            l[i] = (lowL * macro * (0.9 + 0.25 * gustL)).toFloat()
            r[i] = (lowR * macro * (0.9 + 0.25 * gustR)).toFloat()
        }
    }

    private fun renderForest(l: FloatArray, r: FloatArray, sr: Int, random: Random) {
        var bedL = 0.0; var bedR = 0.0
        var chirpEnvL = 0.0; var chirpEnvR = 0.0
        var chirpPhaseL = 0.0; var chirpPhaseR = 0.0
        var chirpStartL = 1800.0; var chirpStartR = 2100.0
        var chirpAgeL = 0; var chirpAgeR = 0
        val chirpLen = maxOf(1, (0.10 * sr).toInt())
        for (i in l.indices) {
            val shared = random.nextDouble() * 2.0 - 1.0
            val wL = shared * 0.78 + (random.nextDouble() * 2.0 - 1.0) * 0.22
            val wR = shared * 0.78 + (random.nextDouble() * 2.0 - 1.0) * 0.22
            bedL += 0.006 * (wL - bedL); bedR += 0.006 * (wR - bedR)
            if (chirpEnvL < 1e-4 && random.nextDouble() < 0.35 / sr) { chirpEnvL = 1.0; chirpAgeL = 0; chirpStartL = 1500.0 + random.nextDouble() * 1200.0 }
            if (chirpEnvR < 1e-4 && random.nextDouble() < 0.35 / sr) { chirpEnvR = 1.0; chirpAgeR = 0; chirpStartR = 1500.0 + random.nextDouble() * 1200.0 }
            var cL = 0.0; var cR = 0.0
            if (chirpEnvL >= 1e-4) {
                val p = (chirpAgeL.toDouble() / chirpLen).coerceIn(0.0, 1.0)
                val f = chirpStartL + 900.0 * p
                chirpPhaseL += 2.0 * PI * f / sr
                chirpEnvL = exp(-5.5 * p)
                cL = sin(chirpPhaseL) * chirpEnvL * 0.25
                chirpAgeL++
                if (chirpAgeL >= chirpLen) chirpEnvL = 0.0
            }
            if (chirpEnvR >= 1e-4) {
                val p = (chirpAgeR.toDouble() / chirpLen).coerceIn(0.0, 1.0)
                val f = chirpStartR + 900.0 * p
                chirpPhaseR += 2.0 * PI * f / sr
                chirpEnvR = exp(-5.5 * p)
                cR = sin(chirpPhaseR) * chirpEnvR * 0.25
                chirpAgeR++
                if (chirpAgeR >= chirpLen) chirpEnvR = 0.0
            }
            l[i] = (0.82 * bedL + cL).toFloat()
            r[i] = (0.82 * bedR + cR).toFloat()
        }
    }

    private fun seamCrossfade(l: FloatArray, r: FloatArray, sr: Int) {
        val n = minOf((sr * 0.5).toInt(), l.size / 4)
        if (n < 2) return
        val start = l.size - n
        for (i in 0 until n) {
            val t = i.toFloat() / (n - 1)
            l[start + i] = l[start + i] * (1f - t) + l[i] * t
            r[start + i] = r[start + i] * (1f - t) + r[i] * t
        }
    }
}
