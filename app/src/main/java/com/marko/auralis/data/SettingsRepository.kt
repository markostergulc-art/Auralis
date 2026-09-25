package com.marko.auralis.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.AudioMode
import com.marko.auralis.model.Preset
import com.marko.auralis.model.StereoMode
import com.marko.auralis.model.SoundLayerConfig
import com.marko.auralis.model.AlternativeMode
import com.marko.auralis.model.ResearchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "brain_relax_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class LanguageMode { ENGLISH, GERMAN, CROATIAN }

data class AppSettings(
    val config: AudioConfig = AudioConfig(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepScreenAwake: Boolean = false,
    val resumeLastSettings: Boolean = true,
    val languageMode: LanguageMode = LanguageMode.ENGLISH,
    val languageSelected: Boolean = false,
    val firstLaunchAccepted: Boolean = false,
    val layers: SoundLayerConfig = SoundLayerConfig()
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val pulseRate = floatPreferencesKey("pulse_rate")
        val pulseDuration = intPreferencesKey("pulse_duration_us")
        val volume = intPreferencesKey("volume_percent")
        val dynamics = intPreferencesKey("dynamics_percent")
        val balance = intPreferencesKey("balance_percent")
        val randomTiming = intPreferencesKey("random_timing_percent")
        val randomLeftDelayMs = intPreferencesKey("random_left_delay_ms")
        val randomRightDelayMs = intPreferencesKey("random_right_delay_ms")
        val legacyStereoDelayMs = intPreferencesKey("stereo_delay_ms")
        val legacyStereoDelayUs = intPreferencesKey("stereo_delay_us")
        val stereoMode = stringPreferencesKey("stereo_mode")
        val minSeparation = intPreferencesKey("min_separation_frames")
        val audioMode = stringPreferencesKey("audio_mode")
        val timerMinutes = intPreferencesKey("timer_minutes")
        val theme = stringPreferencesKey("theme")
        val keepAwake = booleanPreferencesKey("keep_awake")
        val resume = booleanPreferencesKey("resume_last")
        val language = stringPreferencesKey("language")
        val languageSelected = booleanPreferencesKey("language_selected_v12")
        val firstLaunch = booleanPreferencesKey("first_launch_accepted_v12")
        val presets = stringPreferencesKey("presets_json")
        val layers = stringPreferencesKey("sound_layers_v2")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val config = AudioConfig(
            pulseRateHz = p[Keys.pulseRate] ?: 2.0f,
            requestedPulseUs = p[Keys.pulseDuration] ?: 55,
            volumePercent = p[Keys.volume] ?: 20,
            randomDynamicsPercent = p[Keys.dynamics] ?: 20,
            balancePercent = p[Keys.balance] ?: 0,
            randomTimingPercent = p[Keys.randomTiming] ?: 0,
            stereoMode = StereoMode.RANDOM_LR,
            randomLeftDelayMs = p[Keys.randomLeftDelayMs] ?: p[Keys.legacyStereoDelayMs] ?: p[Keys.legacyStereoDelayUs] ?: 30,
            randomRightDelayMs = p[Keys.randomRightDelayMs] ?: p[Keys.legacyStereoDelayMs] ?: p[Keys.legacyStereoDelayUs] ?: 30,
            minimumStereoSeparationFrames = p[Keys.minSeparation] ?: 1,
            audioMode = runCatching { AudioMode.valueOf(p[Keys.audioMode] ?: "LOW_LATENCY") }.getOrDefault(AudioMode.LOW_LATENCY),
            timerMinutes = p[Keys.timerMinutes] ?: 0
        ).sanitized()
        AppSettings(
            config = config,
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            keepScreenAwake = p[Keys.keepAwake] ?: false,
            resumeLastSettings = p[Keys.resume] ?: true,
            languageMode = runCatching { LanguageMode.valueOf(p[Keys.language] ?: "ENGLISH") }.getOrDefault(LanguageMode.ENGLISH),
            languageSelected = p[Keys.languageSelected] ?: false,
            firstLaunchAccepted = p[Keys.firstLaunch] ?: false,
            layers = decodeLayers(p[Keys.layers] ?: "{}")
        )
    }

    val presets: Flow<List<Preset>> = context.dataStore.data.map { p ->
        decodePresets(p[Keys.presets] ?: "[]")
    }

    suspend fun saveConfig(config: AudioConfig) {
        val c = config.sanitized()
        context.dataStore.edit { p ->
            p[Keys.pulseRate] = c.pulseRateHz
            p[Keys.pulseDuration] = c.requestedPulseUs
            p[Keys.volume] = c.volumePercent
            p[Keys.dynamics] = c.randomDynamicsPercent
            p[Keys.balance] = c.balancePercent
            p[Keys.randomTiming] = c.randomTimingPercent
            p[Keys.stereoMode] = StereoMode.RANDOM_LR.name
            p[Keys.randomLeftDelayMs] = c.randomLeftDelayMs
            p[Keys.randomRightDelayMs] = c.randomRightDelayMs
            p[Keys.minSeparation] = c.minimumStereoSeparationFrames
            p[Keys.audioMode] = c.audioMode.name
            p[Keys.timerMinutes] = c.timerMinutes
        }
    }

    suspend fun saveLayers(layers: SoundLayerConfig) = context.dataStore.edit { it[Keys.layers] = encodeLayers(layers.sanitized()).toString() }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setKeepAwake(value: Boolean) = context.dataStore.edit { it[Keys.keepAwake] = value }
    suspend fun setResumeLast(value: Boolean) = context.dataStore.edit { it[Keys.resume] = value }
    suspend fun setLanguage(mode: LanguageMode, selected: Boolean = true) = context.dataStore.edit { p ->
        p[Keys.language] = mode.name
        p[Keys.languageSelected] = selected
    }
    suspend fun acceptFirstLaunch() = context.dataStore.edit { it[Keys.firstLaunch] = true }

    suspend fun savePreset(name: String, config: AudioConfig, layers: SoundLayerConfig = SoundLayerConfig()) {
        context.dataStore.edit { p ->
            val current = decodePresets(p[Keys.presets] ?: "[]").toMutableList()
            val now = System.currentTimeMillis()
            current += Preset(now, name.trim().ifBlank { "Preset" }, config.sanitized(), layers.sanitized())
            p[Keys.presets] = encodePresets(current)
        }
    }


    suspend fun updatePreset(id: Long, config: AudioConfig, layers: SoundLayerConfig = SoundLayerConfig()) = context.dataStore.edit { p ->
        val current = decodePresets(p[Keys.presets] ?: "[]").map {
            if (it.id == id) it.copy(config = config.sanitized(), layers = layers.sanitized(), schemaVersion = Preset.CURRENT_SCHEMA) else it
        }
        p[Keys.presets] = encodePresets(current)
    }

    suspend fun deletePreset(id: Long) = context.dataStore.edit { p ->
        val current = decodePresets(p[Keys.presets] ?: "[]").filterNot { it.id == id }
        p[Keys.presets] = encodePresets(current)
    }

    suspend fun renamePreset(id: Long, newName: String) = context.dataStore.edit { p ->
        val current = decodePresets(p[Keys.presets] ?: "[]").map {
            if (it.id == id) it.copy(name = newName.trim().ifBlank { it.name }) else it
        }
        p[Keys.presets] = encodePresets(current)
    }

    suspend fun duplicatePreset(id: Long) = context.dataStore.edit { p ->
        val current = decodePresets(p[Keys.presets] ?: "[]").toMutableList()
        current.firstOrNull { it.id == id }?.let {
            current += it.copy(id = System.currentTimeMillis(), name = "${it.name} Copy")
        }
        p[Keys.presets] = encodePresets(current)
    }

    private fun encodePresets(items: List<Preset>): String = JSONArray().apply {
        items.forEach { preset ->
            put(JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("schemaVersion", preset.schemaVersion)
                put("layers", encodeLayers(preset.layers))
                put("rate", preset.config.pulseRateHz.toDouble())
                put("duration", preset.config.requestedPulseUs)
                put("volume", preset.config.volumePercent)
                put("dynamics", preset.config.randomDynamicsPercent)
                put("balance", preset.config.balancePercent)
                put("randomTiming", preset.config.randomTimingPercent)
                put("stereo", StereoMode.RANDOM_LR.name)
                put("randomLeftDelayMs", preset.config.randomLeftDelayMs)
                put("randomRightDelayMs", preset.config.randomRightDelayMs)
                put("minSeparation", preset.config.minimumStereoSeparationFrames)
                put("audioMode", preset.config.audioMode.name)
                put("timer", preset.config.timerMinutes)
            })
        }
    }.toString()

    private fun decodePresets(raw: String): List<Preset> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Preset(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        config = AudioConfig(
                            pulseRateHz = o.optDouble("rate", 2.0).toFloat(),
                            requestedPulseUs = o.optInt("duration", 55),
                            volumePercent = o.optInt("volume", 20),
                            randomDynamicsPercent = o.optInt("dynamics", 20),
                            balancePercent = o.optInt("balance", 0),
                            randomTimingPercent = o.optInt("randomTiming", 0),
                            stereoMode = StereoMode.RANDOM_LR,
                            randomLeftDelayMs = if (o.has("randomLeftDelayMs")) o.optInt("randomLeftDelayMs", 30) else if (o.has("stereoDelayMs")) o.optInt("stereoDelayMs", 30) else o.optInt("stereoDelayUs", 30),
                            randomRightDelayMs = if (o.has("randomRightDelayMs")) o.optInt("randomRightDelayMs", 30) else if (o.has("stereoDelayMs")) o.optInt("stereoDelayMs", 30) else o.optInt("stereoDelayUs", 30),
                            minimumStereoSeparationFrames = o.optInt("minSeparation", 1),
                            audioMode = runCatching { AudioMode.valueOf(o.optString("audioMode", "LOW_LATENCY")) }.getOrDefault(AudioMode.LOW_LATENCY),
                            timerMinutes = o.optInt("timer", 0)
                        ).sanitized(),
                        layers = if (o.has("layers")) decodeLayers(o.optJSONObject("layers")?.toString() ?: "{}") else SoundLayerConfig(),
                        schemaVersion = o.optInt("schemaVersion", 1)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun encodeLayers(l: SoundLayerConfig): JSONObject = JSONObject().apply {
        val x = l.sanitized()
        put("ambientEnabled", x.ambientEnabled); put("ambientBrightness", x.ambientBrightness); put("ambientMovement", x.ambientMovement); put("ambientDensity", x.ambientDensity); put("ambientLevel", x.ambientLevel)
        put("binauralEnabled", x.binauralEnabled); put("binauralCarrierHz", x.binauralCarrierHz); put("binauralBeatHz", x.binauralBeatHz); put("binauralLevel", x.binauralLevel)
        put("monauralEnabled", x.monauralEnabled); put("monauralCarrierHz", x.monauralCarrierHz); put("monauralBeatHz", x.monauralBeatHz); put("monauralDepth", x.monauralDepth); put("monauralLevel", x.monauralLevel)
        put("isochronicEnabled", x.isochronicEnabled); put("isochronicCarrierHz", x.isochronicCarrierHz); put("isochronicRateHz", x.isochronicRateHz); put("isochronicDepth", x.isochronicDepth); put("isochronicSoftness", x.isochronicSoftness); put("isochronicLevel", x.isochronicLevel)
        put("itdEnabled", x.itdEnabled); put("itdUs", x.itdUs); put("itdLeadLeft", x.itdLeadLeft); put("itdLevel", x.itdLevel)
        put("smoothMotionEnabled", x.smoothMotionEnabled); put("motionCycleSeconds", x.motionCycleSeconds); put("motionDepth", x.motionDepth); put("motionLevel", x.motionLevel)
        put("bilateralEnabled", x.bilateralEnabled); put("bilateralRateHz", x.bilateralRateHz); put("bilateralSoftness", x.bilateralSoftness); put("bilateralLevel", x.bilateralLevel)
        put("alternativeMode", x.alternativeMode.name); put("solfeggioHz", x.solfeggioHz); put("alternativeLevel", x.alternativeLevel)
        put("researchMode", x.researchMode.name); put("researchPulseShape", x.researchPulseShape); put("researchPulseDurationUs", x.researchPulseDurationUs); put("researchLevel", x.researchLevel)
    }

    private fun decodeLayers(raw: String): SoundLayerConfig = runCatching {
        val o = JSONObject(raw)
        SoundLayerConfig(
            ambientEnabled=o.optBoolean("ambientEnabled",false), ambientBrightness=o.optInt("ambientBrightness",50), ambientMovement=o.optInt("ambientMovement",35), ambientDensity=o.optInt("ambientDensity",50), ambientLevel=o.optInt("ambientLevel",12),
            binauralEnabled=o.optBoolean("binauralEnabled",false), binauralCarrierHz=o.optDouble("binauralCarrierHz",200.0), binauralBeatHz=o.optDouble("binauralBeatHz",6.0), binauralLevel=o.optInt("binauralLevel",10),
            monauralEnabled=o.optBoolean("monauralEnabled",false), monauralCarrierHz=o.optDouble("monauralCarrierHz",200.0), monauralBeatHz=o.optDouble("monauralBeatHz",6.0), monauralDepth=o.optInt("monauralDepth",100), monauralLevel=o.optInt("monauralLevel",10),
            isochronicEnabled=o.optBoolean("isochronicEnabled",false), isochronicCarrierHz=o.optDouble("isochronicCarrierHz",220.0), isochronicRateHz=o.optDouble("isochronicRateHz",6.0), isochronicDepth=o.optInt("isochronicDepth",80), isochronicSoftness=o.optInt("isochronicSoftness",80), isochronicLevel=o.optInt("isochronicLevel",10),
            itdEnabled=o.optBoolean("itdEnabled",false), itdUs=o.optInt("itdUs",400), itdLeadLeft=o.optBoolean("itdLeadLeft",true), itdLevel=o.optInt("itdLevel",8),
            smoothMotionEnabled=o.optBoolean("smoothMotionEnabled",false), motionCycleSeconds=o.optInt("motionCycleSeconds",12), motionDepth=o.optInt("motionDepth",70), motionLevel=o.optInt("motionLevel",8),
            bilateralEnabled=o.optBoolean("bilateralEnabled",false), bilateralRateHz=o.optDouble("bilateralRateHz",1.0), bilateralSoftness=o.optInt("bilateralSoftness",75), bilateralLevel=o.optInt("bilateralLevel",8),
            alternativeMode=runCatching { AlternativeMode.valueOf(o.optString("alternativeMode","OFF")) }.getOrDefault(AlternativeMode.OFF), solfeggioHz=o.optInt("solfeggioHz",528), alternativeLevel=o.optInt("alternativeLevel",8),
            researchMode=runCatching { ResearchMode.valueOf(o.optString("researchMode","OFF")) }.getOrDefault(ResearchMode.OFF), researchPulseShape=o.optString("researchPulseShape","huawei"), researchPulseDurationUs=o.optInt("researchPulseDurationUs",50), researchLevel=o.optInt("researchLevel",6)
        ).sanitized()
    }.getOrDefault(SoundLayerConfig())

}
