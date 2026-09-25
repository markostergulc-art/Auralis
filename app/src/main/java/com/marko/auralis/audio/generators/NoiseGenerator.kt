package com.marko.auralis.audio.generators

import java.util.Random

object NoiseGenerator {
    enum class Type { WHITE, PINK, BROWN }

    fun render(type: Type, frames: Int, seed: Long = 0x4E4F495345L): FloatArray {
        require(frames >= 0)
        val out = FloatArray(frames)
        val random = Random(seed)
        var b0=0.0; var b1=0.0; var b2=0.0; var b3=0.0; var b4=0.0; var b5=0.0; var b6=0.0
        var brown = 0.0
        for (i in 0 until frames) {
            val white = random.nextDouble() * 2.0 - 1.0
            val v = when(type) {
                Type.WHITE -> white
                Type.PINK -> {
                    b0 = 0.99886*b0 + white*0.0555179
                    b1 = 0.99332*b1 + white*0.0750759
                    b2 = 0.96900*b2 + white*0.1538520
                    b3 = 0.86650*b3 + white*0.3104856
                    b4 = 0.55000*b4 + white*0.5329522
                    b5 = -0.7616*b5 - white*0.0168980
                    val pink = b0+b1+b2+b3+b4+b5+b6+white*0.5362
                    b6 = white*0.115926
                    pink*0.11
                }
                Type.BROWN -> {
                    brown = (brown + 0.02*white) / 1.02
                    brown*3.5
                }
            }
            out[i] = v.coerceIn(-1.0, 1.0).toFloat()
        }
        return out
    }
}
