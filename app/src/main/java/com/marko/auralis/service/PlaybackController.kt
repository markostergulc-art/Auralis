package com.marko.auralis.service

import com.marko.auralis.model.AudioDiagnostics
import com.marko.auralis.model.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackController {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _diagnostics = MutableStateFlow(AudioDiagnostics())
    val diagnostics: StateFlow<AudioDiagnostics> = _diagnostics.asStateFlow()

    fun updateState(value: PlaybackUiState) { _state.value = value }
    fun updateDiagnostics(value: AudioDiagnostics) { _diagnostics.value = value }
}
