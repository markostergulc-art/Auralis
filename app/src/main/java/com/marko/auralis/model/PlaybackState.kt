package com.marko.auralis.model

enum class PlaybackStatus { IDLE, STARTING, PLAYING, STOPPING, ERROR }

data class PlaybackUiState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val timerRemainingMs: Long? = null,
    val error: String? = null
)
