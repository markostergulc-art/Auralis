package com.marko.auralis.ui

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marko.auralis.data.AppSettings
import com.marko.auralis.data.SettingsRepository
import com.marko.auralis.data.LanguageMode
import com.marko.auralis.data.ThemeMode
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.PlaybackStatus
import com.marko.auralis.model.Preset
import com.marko.auralis.model.SoundLayerConfig
import com.marko.auralis.model.BuiltInPresetDefinition
import com.marko.auralis.service.PlaybackController
import com.marko.auralis.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application.applicationContext)
    private val _settings = MutableStateFlow(AppSettings())
    private val _currentPresetName = MutableStateFlow("Custom")
    val currentPresetName: StateFlow<String> = _currentPresetName.asStateFlow()
    private var firstSettingsEmission = true
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _layers = MutableStateFlow(SoundLayerConfig())
    val layers: StateFlow<SoundLayerConfig> = _layers.asStateFlow()

    val playback = PlaybackController.state
    val diagnostics = PlaybackController.diagnostics
    val presets: StateFlow<List<Preset>> = repository.presets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            repository.settings.collect {
                if (firstSettingsEmission) {
                    _settings.value = if (it.resumeLastSettings) it else it.copy(config = AudioConfig(), layers = SoundLayerConfig())
                    _layers.value = _settings.value.layers
                    firstSettingsEmission = false
                } else {
                    _settings.value = it
                    _layers.value = it.layers
                }
            }
        }
    }

    fun setConfig(config: AudioConfig, persist: Boolean = true) {
        _currentPresetName.value = "Custom"
        val sanitized = config.sanitized()
        val timerChanged = sanitized.timerMinutes != _settings.value.config.timerMinutes
        _settings.value = _settings.value.copy(config = sanitized)
        if (persist) viewModelScope.launch { repository.saveConfig(sanitized) }
        if (playback.value.status == PlaybackStatus.PLAYING || playback.value.status == PlaybackStatus.STARTING) {
            getApplication<Application>().startService(
                PlaybackService.updateIntent(getApplication(), sanitized, resetTimer = timerChanged)
            )
        }
    }


    fun setLayers(value: SoundLayerConfig, persist: Boolean = true) {
        val clean = value.sanitized()
        _layers.value = clean
        _settings.value = _settings.value.copy(layers = clean)
        if (persist) viewModelScope.launch { repository.saveLayers(clean) }
    }

    fun persistCurrentConfig() {
        viewModelScope.launch { repository.saveConfig(_settings.value.config) }
    }

    fun startPlayback() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, PlaybackService.startIntent(context, _settings.value.config))
    }

    fun stopPlayback() {
        val context = getApplication<Application>()
        context.startService(PlaybackService.stopIntent(context))
    }

    fun togglePlayback() {
        when (playback.value.status) {
            PlaybackStatus.IDLE, PlaybackStatus.ERROR -> startPlayback()
            else -> stopPlayback()
        }
    }

    fun setTheme(mode: ThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
        viewModelScope.launch { repository.setTheme(mode) }
    }

    fun setLanguage(mode: LanguageMode) {
        _settings.value = _settings.value.copy(languageMode = mode, languageSelected = true)
        viewModelScope.launch { repository.setLanguage(mode, true) }
    }

    fun setKeepAwake(value: Boolean) {
        _settings.value = _settings.value.copy(keepScreenAwake = value)
        viewModelScope.launch { repository.setKeepAwake(value) }
    }

    fun setResumeLast(value: Boolean) {
        _settings.value = _settings.value.copy(resumeLastSettings = value)
        viewModelScope.launch { repository.setResumeLast(value) }
    }

    fun acceptFirstLaunch() {
        _settings.value = _settings.value.copy(firstLaunchAccepted = true)
        viewModelScope.launch { repository.acceptFirstLaunch() }
    }


    private fun builtInName(def: BuiltInPresetDefinition): String = when (_settings.value.languageMode) {
        LanguageMode.GERMAN -> def.nameDe
        LanguageMode.CROATIAN -> def.nameHr
        else -> def.nameEn
    }

    fun loadBuiltInPreset(def: BuiltInPresetDefinition) {
        setConfig(def.config)
        setLayers(def.layers)
        _currentPresetName.value = builtInName(def)
    }

    fun copyBuiltInPreset(def: BuiltInPresetDefinition) {
        val name = builtInName(def)
        viewModelScope.launch { repository.savePreset(name, def.config, def.layers) }
    }

    fun savePreset(name: String) {
        val clean = name.trim().ifBlank { "Preset" }
        _currentPresetName.value = clean
        viewModelScope.launch { repository.savePreset(clean, _settings.value.config, _layers.value) }
    }

    fun loadPreset(preset: Preset) {
        setConfig(preset.config)
        setLayers(preset.layers)
        _currentPresetName.value = preset.name
    }
    fun updatePreset(id: Long) { viewModelScope.launch { repository.updatePreset(id, _settings.value.config, _layers.value) } }
    fun deletePreset(id: Long) { viewModelScope.launch { repository.deletePreset(id) } }
    fun renamePreset(id: Long, name: String) { viewModelScope.launch { repository.renamePreset(id, name) } }
    fun duplicatePreset(id: Long) { viewModelScope.launch { repository.duplicatePreset(id) } }
}
