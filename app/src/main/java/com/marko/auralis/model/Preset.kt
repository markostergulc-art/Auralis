package com.marko.auralis.model

/**
 * Preset schema v2 stores the complete sound session while keeping old v1
 * pulse-only presets readable. Missing layer data always falls back to
 * SoundLayerConfig defaults rather than mutating the legacy pulse values.
 */
data class Preset(
    val id: Long,
    val name: String,
    val config: AudioConfig,
    val layers: SoundLayerConfig = SoundLayerConfig(),
    val schemaVersion: Int = CURRENT_SCHEMA
) {
    companion object {
        const val CURRENT_SCHEMA = 2
    }
}
