package com.marko.auralis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.marko.auralis.MainActivity
import com.marko.auralis.R
import com.marko.auralis.audio.engine.ImpulseAudioEngine
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.AudioMode
import com.marko.auralis.model.PlaybackStatus
import com.marko.auralis.model.PlaybackUiState
import com.marko.auralis.model.StereoMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var audioManager: AudioManager
    private lateinit var engine: ImpulseAudioEngine
    private var focusRequest: AudioFocusRequest? = null
    private var currentConfig = AudioConfig()
    private var configuredTimerMinutes = 0
    private var timerDeadlineElapsed: Long? = null
    private var timerJob: Job? = null
    private var routeRestartJob: Job? = null
    private var callbacksRegistered = false
    private var routeSafetyGain = 1.0f

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                stopPlayback("Audio output disconnected")
            }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.any { isPrivateListeningDevice(it.type) }) {
                // Reduce first, then allow routing to settle. Never create a loud transition into headphones.
                routeSafetyGain = AudioRouteSafety.HEADPHONE_START_GAIN
                engine.setSafetyGain(routeSafetyGain)
            }
            scheduleRouteReconfigure()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = scheduleRouteReconfigure()
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> engine.setFocusGain(1.0f)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> engine.setFocusGain(0.20f)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> stopPlayback("Audio focus lost")
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        engine = ImpulseAudioEngine(applicationContext)
        createNotificationChannel()
        registerAudioCallbacks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startPlayback(intent.toAudioConfig())
            ACTION_UPDATE -> updatePlayback(intent.toAudioConfig(), intent.getBooleanExtra(EXTRA_RESET_TIMER, false))
            ACTION_STOP -> stopPlayback(null)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        routeRestartJob?.cancel()
        engine.stop()
        abandonAudioFocus()
        unregisterAudioCallbacks()
        scope.cancel()
        PlaybackController.updateState(PlaybackUiState(PlaybackStatus.IDLE))
        super.onDestroy()
    }

    private fun startPlayback(config: AudioConfig) {
        currentConfig = config.sanitized()
        PlaybackController.updateState(PlaybackUiState(PlaybackStatus.STARTING))
        startAsForeground()

        if (!requestAudioFocus()) {
            PlaybackController.updateState(
                PlaybackUiState(PlaybackStatus.ERROR, error = "Audio focus could not be acquired.")
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        configuredTimerMinutes = currentConfig.timerMinutes
        setTimerFromConfig(configuredTimerMinutes)
        routeSafetyGain = AudioRouteSafety.HEADPHONE_START_GAIN
        engine.setSafetyGain(routeSafetyGain)
        engine.start(
            currentConfig,
            onDiagnostics = PlaybackController::updateDiagnostics,
            onError = { throwable ->
                scope.launch {
                    PlaybackController.updateState(
                        PlaybackUiState(PlaybackStatus.ERROR, error = throwable.message ?: "Audio output failed.")
                    )
                    stopPlayback(null)
                }
            }
        )
        PlaybackController.updateState(
            PlaybackUiState(PlaybackStatus.PLAYING, remainingMs())
        )
        scope.launch { delay(300); applySafetyForResolvedRoute() }
        startTimerTicker()
    }

    private fun updatePlayback(config: AudioConfig, resetTimer: Boolean) {
        val previous = currentConfig
        currentConfig = config.sanitized()
        if (engine.isRunning()) {
            if (previous.audioMode != currentConfig.audioMode) {
                restartEngineForRoute()
            } else {
                engine.updateConfig(currentConfig)
            }
            if (resetTimer) {
                configuredTimerMinutes = currentConfig.timerMinutes
                setTimerFromConfig(configuredTimerMinutes)
            }
            updateNotification()
        }
    }

    private fun stopPlayback(reason: String?) {
        if (PlaybackController.state.value.status == PlaybackStatus.IDLE) return
        PlaybackController.updateState(PlaybackUiState(PlaybackStatus.STOPPING))
        timerJob?.cancel()
        timerJob = null
        timerDeadlineElapsed = null
        engine.stop()
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        PlaybackController.updateState(
            PlaybackUiState(
                status = PlaybackStatus.IDLE,
                error = reason?.takeIf { it != "Audio focus lost" && it != "Audio output disconnected" }
            )
        )
        stopSelf()
    }

    private fun restartEngineForRoute() {
        if (PlaybackController.state.value.status != PlaybackStatus.PLAYING || !engine.isRunning()) return
        engine.stop()
        engine = ImpulseAudioEngine(applicationContext)
        engine.setSafetyGain(AudioRouteSafety.HEADPHONE_START_GAIN)
        engine.start(
            currentConfig,
            onDiagnostics = PlaybackController::updateDiagnostics,
            onError = { t ->
                scope.launch {
                    PlaybackController.updateState(
                        PlaybackUiState(PlaybackStatus.ERROR, error = t.message ?: "Audio route change failed.")
                    )
                    stopPlayback(null)
                }
            }
        )
        scope.launch { delay(300); applySafetyForResolvedRoute() }
    }

    private fun applySafetyForResolvedRoute() {
        val type = engine.currentRoutedDeviceType()
        routeSafetyGain = if (isPrivateListeningDevice(type)) AudioRouteSafety.HEADPHONE_START_GAIN else 1.0f
        engine.setSafetyGain(routeSafetyGain)
    }

    private fun isPrivateListeningDevice(type: Int): Boolean = AudioRouteSafety.isPrivateListeningDevice(type)

    private fun scheduleRouteReconfigure() {
        routeRestartJob?.cancel()
        routeRestartJob = scope.launch {
            delay(450)
            restartEngineForRoute()
        }
    }

    private fun setTimerFromConfig(minutes: Int) {
        timerDeadlineElapsed = if (minutes > 0) {
            SystemClock.elapsedRealtime() + minutes * 60_000L
        } else null
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var lastNotificationSecond = Long.MIN_VALUE
            while (engine.isRunning()) {
                val remaining = remainingMs()
                if (remaining != null && remaining <= 0L) {
                    stopPlayback(null)
                    break
                }
                PlaybackController.updateState(
                    PlaybackUiState(PlaybackStatus.PLAYING, remaining)
                )
                val second = remaining?.div(1000L) ?: -1L
                if (second != lastNotificationSecond) {
                    updateNotification()
                    lastNotificationSecond = second
                }
                delay(250)
            }
        }
    }

    private fun remainingMs(): Long? = timerDeadlineElapsed?.let {
        (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
    }

    private fun requestAudioFocus(): Boolean {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(false)
            .build()
        focusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, PlaybackService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remaining = remainingMs()
        val content = if (remaining == null) "Playing" else "Playing • ${formatRemaining(remaining)} remaining"
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pulse)
            .setContentTitle("Auralis")
            .setContentText(content)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "Stop",
                    stopPending
                ).build()
            )
            .build()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active impulse playback and timer status"
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    private fun registerAudioCallbacks() {
        if (callbacksRegistered) return
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        callbacksRegistered = true
    }

    private fun unregisterAudioCallbacks() {
        if (!callbacksRegistered) return
        runCatching { unregisterReceiver(noisyReceiver) }
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        callbacksRegistered = false
    }

    private fun Intent.toAudioConfig(): AudioConfig = AudioConfig(
        pulseRateHz = getFloatExtra(EXTRA_RATE, 2f),
        requestedPulseUs = getIntExtra(EXTRA_DURATION, 55),
        volumePercent = getIntExtra(EXTRA_VOLUME, 20),
        randomDynamicsPercent = getIntExtra(EXTRA_DYNAMICS, 20),
        balancePercent = getIntExtra(EXTRA_BALANCE, 0),
        randomTimingPercent = getIntExtra(EXTRA_RANDOM_TIMING, 0),
        stereoMode = StereoMode.RANDOM_LR,
        randomLeftDelayMs = if (hasExtra(EXTRA_RANDOM_LEFT_DELAY)) getIntExtra(EXTRA_RANDOM_LEFT_DELAY, 30) else getIntExtra(EXTRA_STEREO_DELAY, 30),
        randomRightDelayMs = if (hasExtra(EXTRA_RANDOM_RIGHT_DELAY)) getIntExtra(EXTRA_RANDOM_RIGHT_DELAY, 30) else getIntExtra(EXTRA_STEREO_DELAY, 30),
        minimumStereoSeparationFrames = getIntExtra(EXTRA_MIN_SEPARATION, 1),
        audioMode = runCatching { AudioMode.valueOf(getStringExtra(EXTRA_AUDIO_MODE) ?: "LOW_LATENCY") }
            .getOrDefault(AudioMode.LOW_LATENCY),
        timerMinutes = getIntExtra(EXTRA_TIMER, 0)
    ).sanitized()

    companion object {
        private const val CHANNEL_ID = "impulse_playback"
        private const val NOTIFICATION_ID = 4101
                const val ACTION_START = "com.marko.auralis.START"
        const val ACTION_UPDATE = "com.marko.auralis.UPDATE"
        const val ACTION_STOP = "com.marko.auralis.STOP"
        private const val EXTRA_RATE = "rate"
        private const val EXTRA_DURATION = "duration"
        private const val EXTRA_VOLUME = "volume"
        private const val EXTRA_DYNAMICS = "dynamics"
        private const val EXTRA_BALANCE = "balance"
        private const val EXTRA_RANDOM_TIMING = "random_timing"
        private const val EXTRA_STEREO_DELAY = "stereo_delay_ms" // legacy migration
        private const val EXTRA_RANDOM_LEFT_DELAY = "random_left_delay_ms"
        private const val EXTRA_RANDOM_RIGHT_DELAY = "random_right_delay_ms"
        private const val EXTRA_STEREO = "stereo"
        private const val EXTRA_MIN_SEPARATION = "min_separation"
        private const val EXTRA_AUDIO_MODE = "audio_mode"
        private const val EXTRA_TIMER = "timer"
        private const val EXTRA_RESET_TIMER = "reset_timer"

        fun startIntent(context: Context, config: AudioConfig) = baseIntent(context, ACTION_START, config)
        fun updateIntent(context: Context, config: AudioConfig, resetTimer: Boolean = false) =
            baseIntent(context, ACTION_UPDATE, config).putExtra(EXTRA_RESET_TIMER, resetTimer)
        fun stopIntent(context: Context) = Intent(context, PlaybackService::class.java).setAction(ACTION_STOP)

        private fun baseIntent(context: Context, action: String, config: AudioConfig): Intent =
            Intent(context, PlaybackService::class.java).setAction(action).apply {
                val c = config.sanitized()
                putExtra(EXTRA_RATE, c.pulseRateHz)
                putExtra(EXTRA_DURATION, c.requestedPulseUs)
                putExtra(EXTRA_VOLUME, c.volumePercent)
                putExtra(EXTRA_DYNAMICS, c.randomDynamicsPercent)
                putExtra(EXTRA_BALANCE, c.balancePercent)
                putExtra(EXTRA_RANDOM_TIMING, c.randomTimingPercent)
                putExtra(EXTRA_RANDOM_LEFT_DELAY, c.randomLeftDelayMs)
                putExtra(EXTRA_RANDOM_RIGHT_DELAY, c.randomRightDelayMs)
                putExtra(EXTRA_STEREO, StereoMode.RANDOM_LR.name)
                putExtra(EXTRA_MIN_SEPARATION, c.minimumStereoSeparationFrames)
                putExtra(EXTRA_AUDIO_MODE, c.audioMode.name)
                putExtra(EXTRA_TIMER, c.timerMinutes)
            }

        private fun formatRemaining(ms: Long): String {
            val total = ms / 1000
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return "%02d:%02d:%02d".format(h, m, s)
        }
    }
}
