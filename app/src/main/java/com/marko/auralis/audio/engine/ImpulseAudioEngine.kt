package com.marko.auralis.audio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import com.marko.auralis.audio.dsp.PulseMath
import com.marko.auralis.audio.scheduler.PulseChannel
import com.marko.auralis.audio.scheduler.PulseEvent
import com.marko.auralis.audio.scheduler.StereoPulseScheduler
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.AudioDiagnostics
import com.marko.auralis.model.AudioMode
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

class ImpulseAudioEngine(private val context: Context) {
    private val running = AtomicBoolean(false)
    private val configRef = AtomicReference(AudioConfig())
    private val safetyGainRef = AtomicReference(1.0f)
    private val focusGainRef = AtomicReference(1.0f)
    private val random = Random(0x425241494E5F4453L)

    @Volatile private var worker: Thread? = null
    @Volatile private var track: AudioTrack? = null

    fun start(initialConfig: AudioConfig, onDiagnostics: (AudioDiagnostics) -> Unit, onError: (Throwable) -> Unit) {
        if (!running.compareAndSet(false, true)) return
        configRef.set(initialConfig.sanitized())
        worker = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            try {
                runLoop(onDiagnostics)
            } catch (t: Throwable) {
                onError(t)
            } finally {
                releaseTrack()
                running.set(false)
            }
        }, "Auralis-Audio").also { it.start() }
    }

    fun updateConfig(config: AudioConfig) { configRef.set(config.sanitized()) }

    /** Route safety does not rewrite the user's stored volume; it is an output-only gain. */
    fun setSafetyGain(value: Float) { safetyGainRef.set(value.coerceIn(0f, 1f)) }
    fun setFocusGain(value: Float) { focusGainRef.set(value.coerceIn(0f, 1f)) }
    fun currentRoutedDeviceType(): Int = runCatching { track?.routedDevice?.type ?: 0 }.getOrDefault(0)

    fun stop() {
        running.set(false)
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        worker?.interrupt()
        worker = null
    }

    fun isRunning(): Boolean = running.get()

    private fun runLoop(onDiagnostics: (AudioDiagnostics) -> Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nativeRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
            ?.takeIf { it in 8_000..384_000 } ?: 48_000
        val sampleRate = nativeRate
        val channelMask = AudioFormat.CHANNEL_OUT_STEREO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
        check(minBytes > 0) { "AudioTrack returned invalid minimum buffer size: $minBytes" }

        val bytesPerFrame = 2 * 4
        val preferredFrames = max(128, sampleRate / 100)
        val bufferBytes = max(minBytes, preferredFrames * bytesPerFrame)
        val bufferFrames = bufferBytes / bytesPerFrame

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()
        val trackBuilder = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
        if (configRef.get().audioMode == AudioMode.LOW_LATENCY) {
            trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        } else {
            trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_NONE)
        }
        val audioTrack = trackBuilder.build()
        check(audioTrack.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack failed to initialize" }
        track = audioTrack

        val initial = configRef.get()
        var pulseSamples = PulseMath.samplesForDuration(sampleRate, initial.requestedPulseUs)
        val chunkFrames = min(512, max(128, bufferFrames / 2))
        val pcm = FloatArray(chunkFrames * 2)
        var frameCursor = 0L
        var scheduler = createScheduler(sampleRate, initial)
        val startRampFrames = max(1L, sampleRate / 20L)
        scheduler.reset(startRampFrames)
        var nextEvent = scheduler.next()
        var activeEvent: PulseEvent? = null
        var activePulse = PulseMath.bipolarPulse(pulseSamples)
        var activeBaseAmplitude = 0f
        var activePulseStart = Long.MIN_VALUE
        var activeDelayFrames = 1
        var lastRate = initial.pulseRateHz
        var lastDuration = initial.requestedPulseUs
        var lastStereo = initial.stereoMode
        var lastMinSeparation = initial.minimumStereoSeparationFrames
        var lastRandomTiming = initial.randomTimingPercent

        var lastDiagWall = SystemClock.elapsedRealtime()
        var lastDiagCpu = Process.getElapsedCpuTime()
        var cpuPercent = 0.0
        var memoryMb = Debug.getPss() / 1024.0

        audioTrack.play()
        onDiagnostics(buildDiagnostics(audioTrack, initial, sampleRate, nativeRate, pulseSamples, bufferFrames, cpuPercent, memoryMb))

        while (running.get() && !Thread.currentThread().isInterrupted) {
            val config = configRef.get()

            if (config.stereoMode != lastStereo ||
                config.minimumStereoSeparationFrames != lastMinSeparation ||
                config.randomTimingPercent != lastRandomTiming
            ) {
                scheduler = createScheduler(sampleRate, config)
                scheduler.reset(frameCursor + StereoPulseScheduler.framesPerPulse(sampleRate, config.pulseRateHz))
                nextEvent = scheduler.next()
                lastStereo = config.stereoMode
                lastMinSeparation = config.minimumStereoSeparationFrames
                lastRandomTiming = config.randomTimingPercent
                lastRate = config.pulseRateHz
            } else if (config.pulseRateHz != lastRate) {
                scheduler.updateRate(config.pulseRateHz)
                scheduler.reset(frameCursor + StereoPulseScheduler.framesPerPulse(sampleRate, config.pulseRateHz))
                nextEvent = scheduler.next()
                lastRate = config.pulseRateHz
            }

            if (config.requestedPulseUs != lastDuration) {
                pulseSamples = PulseMath.samplesForDuration(sampleRate, config.requestedPulseUs)
                activePulse = PulseMath.bipolarPulse(pulseSamples)
                lastDuration = config.requestedPulseUs
            }

            pcm.fill(0f)
            for (localFrame in 0 until chunkFrames) {
                val absoluteFrame = frameCursor + localFrame
                if (absoluteFrame == nextEvent.frame) {
                    activeEvent = nextEvent
                    activePulseStart = absoluteFrame
                    activeBaseAmplitude = PulseMath.amplitude(
                        config.volumePercent,
                        config.randomDynamicsPercent,
                        random.nextFloat()
                    )
                    val delayedChannel = if (nextEvent.channel == PulseChannel.LEFT) PulseChannel.RIGHT else PulseChannel.LEFT
                    val maxDelayMs = if (delayedChannel == PulseChannel.LEFT) config.randomLeftDelayMs else config.randomRightDelayMs
                    val requestedDelayMs = 1.0 + random.nextDouble() * (maxDelayMs.coerceIn(1, 100) - 1).toDouble()
                    activeDelayFrames = PulseMath.samplesForDelayMs(sampleRate, requestedDelayMs)
                    nextEvent = scheduler.next()
                }

                val event = activeEvent
                if (event != null) {
                    val leadIndex = (absoluteFrame - activePulseStart).toInt()
                    val delayedIndex = (absoluteFrame - (activePulseStart + activeDelayFrames)).toInt()
                    val base = localFrame * 2
                    if (leadIndex in activePulse.indices) {
                        val leadGain = PulseMath.balanceGain(config.balancePercent, event.channel)
                        val value = (activePulse[leadIndex] * activeBaseAmplitude * leadGain).coerceIn(-1f, 1f)
                        if (event.channel == PulseChannel.LEFT) pcm[base] += value else pcm[base + 1] += value
                    }
                    if (delayedIndex in activePulse.indices) {
                        val delayedChannel = if (event.channel == PulseChannel.LEFT) PulseChannel.RIGHT else PulseChannel.LEFT
                        val delayedGain = PulseMath.balanceGain(config.balancePercent, delayedChannel)
                        val value = (activePulse[delayedIndex] * activeBaseAmplitude * delayedGain).coerceIn(-1f, 1f)
                        if (delayedChannel == PulseChannel.LEFT) pcm[base] += value else pcm[base + 1] += value
                    }
                    if (leadIndex >= activePulse.size && delayedIndex >= activePulse.size) {
                        activeEvent = null
                    }
                }
            }

            val outputGain = (safetyGainRef.get() * focusGainRef.get()).coerceIn(0f, 1f)
            if (outputGain < 0.9999f) {
                for (i in pcm.indices) pcm[i] *= outputGain
            }

            var written = 0
            while (written < pcm.size && running.get()) {
                val count = audioTrack.write(pcm, written, pcm.size - written, AudioTrack.WRITE_BLOCKING)
                if (count < 0) error("AudioTrack write failed: $count")
                if (count == 0) continue
                written += count
            }
            frameCursor += chunkFrames

            val nowWall = SystemClock.elapsedRealtime()
            if (nowWall - lastDiagWall >= 1_000L) {
                val nowCpu = Process.getElapsedCpuTime()
                val wallDelta = (nowWall - lastDiagWall).coerceAtLeast(1L)
                val cpuDelta = (nowCpu - lastDiagCpu).coerceAtLeast(0L)
                val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                cpuPercent = ((cpuDelta.toDouble() / wallDelta) * 100.0 / cores).coerceIn(0.0, 100.0)
                memoryMb = Debug.getPss() / 1024.0
                onDiagnostics(buildDiagnostics(audioTrack, config, sampleRate, nativeRate, pulseSamples, bufferFrames, cpuPercent, memoryMb))
                lastDiagWall = nowWall
                lastDiagCpu = nowCpu
            }
        }
    }

    private fun createScheduler(sampleRate: Int, config: AudioConfig) = StereoPulseScheduler(
        sampleRate = sampleRate,
        pulseRateHz = config.pulseRateHz,
        mode = config.stereoMode,
        minimumSeparationFrames = config.minimumStereoSeparationFrames,
        randomTimingPercent = config.randomTimingPercent
    )

    private fun buildDiagnostics(
        audioTrack: AudioTrack,
        config: AudioConfig,
        sampleRate: Int,
        nativeRate: Int,
        pulseSamples: Int,
        bufferFrames: Int,
        cpuPercent: Double,
        memoryMb: Double
    ): AudioDiagnostics {
        val routed = runCatching { audioTrack.routedDevice }.getOrNull()
        val deviceName = routed?.let { "${it.productName} (type ${it.type})" } ?: "System audio output"
        return AudioDiagnostics(
            outputDevice = deviceName,
            selectedSampleRate = sampleRate,
            channelCount = 2,
            pcmFormat = "PCM_FLOAT",
            requestedPulseUs = config.requestedPulseUs,
            actualPulseUs = PulseMath.actualDurationUs(sampleRate, pulseSamples),
            samplesPerPulse = pulseSamples,
            requestedRandomLeftDelayMs = config.randomLeftDelayMs,
            requestedRandomRightDelayMs = config.randomRightDelayMs,
            actualRandomLeftDelayMs = PulseMath.actualDelayMs(sampleRate, PulseMath.samplesForDelayMs(sampleRate, config.randomLeftDelayMs.toDouble())),
            actualRandomRightDelayMs = PulseMath.actualDelayMs(sampleRate, PulseMath.samplesForDelayMs(sampleRate, config.randomRightDelayMs.toDouble())),
            bufferFrames = bufferFrames,
            underrunCount = runCatching { audioTrack.underrunCount }.getOrDefault(0),
            lowLatencyRequested = runCatching { audioTrack.performanceMode == AudioTrack.PERFORMANCE_MODE_LOW_LATENCY }.getOrDefault(false),
            nativeRateHint = nativeRate,
            cpuUsagePercent = cpuPercent,
            memoryUsageMb = memoryMb,
            audioRouteType = routed?.type ?: 0,
            safetyOutputGain = safetyGainRef.get(),
            focusOutputGain = focusGainRef.get(),
            activeLayers = listOf("Pulse")
        )
    }

    private fun releaseTrack() {
        val t = track ?: return
        runCatching { t.pause() }
        runCatching { t.flush() }
        runCatching { t.stop() }
        runCatching { t.release() }
        track = null
    }
}
