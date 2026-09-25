import com.marko.auralis.audio.dsp.PulseMath
import com.marko.auralis.audio.scheduler.PulseChannel
import com.marko.auralis.audio.scheduler.StereoPulseScheduler
import com.marko.auralis.model.StereoMode
import java.util.Random
import kotlin.math.abs

fun main() {
    check(PulseMath.samplesForDuration(48_000, 40) == 2)
    check(PulseMath.samplesForDuration(48_000, 50) == 2)
    check(PulseMath.samplesForDuration(48_000, 55) == 3)
    check(abs(PulseMath.actualDurationUs(48_000, 3) - 62.5) < 1e-9)
    check(PulseMath.samplesForDuration(44_100, 55) == 2)
    check(PulseMath.samplesForDuration(48_000, 90) == 4)

    val balanced = StereoPulseScheduler(48_000, 6f, StereoMode.BALANCED_RANDOM, seed = 7)
    var previousFrame = -1L
    var left = 0
    var right = 0
    repeat(5_000_000) {
        val event = balanced.next()
        check(event.frame > previousFrame)
        previousFrame = event.frame
        when(event.channel) {
            PulseChannel.LEFT -> left++
            PulseChannel.RIGHT -> right++
        }
        check(abs(left - right) <= 2)
    }

    val randomTiming = StereoPulseScheduler(
        sampleRate = 48_000,
        pulseRateHz = 6f,
        mode = StereoMode.RANDOM_LR,
        minimumSeparationFrames = 64,
        randomTimingPercent = 50,
        seed = 99
    )
    var prior = randomTiming.next()
    repeat(500_000) {
        val current = randomTiming.next()
        check(current.frame - prior.frame >= 64)
        prior = current
    }

    val rng = Random(120)
    for (sampleRate in listOf(44_100, 48_000, 96_000)) {
        repeat(1_000_000) {
            val requestedMax = 1 + rng.nextInt(100)
            val requested = 1.0 + rng.nextDouble() * (requestedMax - 1).toDouble()
            val delayFrames = PulseMath.samplesForDelayMs(sampleRate, requested)
            check(delayFrames >= 1)
            val leadFrame = 1000L + it * 100L
            val delayedFrame = leadFrame + delayFrames
            check(delayedFrame > leadFrame)
            check(PulseMath.actualDelayMs(sampleRate, delayFrames) > 0.0)
        }
    }

    check(PulseMath.balanceGain(-100, PulseChannel.RIGHT) == 0f)
    check(PulseMath.balanceGain(100, PulseChannel.LEFT) == 0f)
    check(PulseMath.balanceGain(0, PulseChannel.LEFT) == 1f)
    check(PulseMath.balanceGain(0, PulseChannel.RIGHT) == 1f)

    println("PURE_DSP_TESTS_OK")
    println("48k/55us: ${PulseMath.samplesForDuration(48_000, 55)} samples = ${PulseMath.actualDurationUs(48_000, 3)} us")
    println("48k max 100ms delay: ${PulseMath.samplesForDelayMs(48_000, 100.0)} frames = ${PulseMath.actualDelayMs(48_000, PulseMath.samplesForDelayMs(48_000, 100.0))} ms")
    println("balanced random counts: L=$left R=$right")
}
