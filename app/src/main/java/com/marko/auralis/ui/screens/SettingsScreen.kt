package com.marko.auralis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marko.auralis.BuildConfig
import com.marko.auralis.data.AppSettings
import com.marko.auralis.data.ThemeMode
import com.marko.auralis.data.LanguageMode
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.AudioMode
import com.marko.auralis.model.AudioDiagnostics
import com.marko.auralis.model.StereoMode
import com.marko.auralis.ui.components.AdjustableControlCard
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    diagnostics: AudioDiagnostics,
    onBack: () -> Unit,
    onConfigChange: (AudioConfig, Boolean) -> Unit,
    onPersistConfig: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (LanguageMode) -> Unit,
    onKeepAwake: (Boolean) -> Unit,
    onResumeLast: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        SectionCard("General") {
            Text("Language", fontWeight = FontWeight.SemiBold)
            LanguageMode.entries.forEach { mode ->
                val label = when (mode) {
                    LanguageMode.ENGLISH -> "English"
                    LanguageMode.GERMAN -> "Deutsch"
                    LanguageMode.CROATIAN -> "Hrvatski"
                }
                Row(Modifier.fillMaxWidth().clickable { onLanguage(mode) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.languageMode == mode, onClick = { onLanguage(mode) })
                    Text(label)
                }
            }
            HorizontalDivider()
            Text("Theme", fontWeight = FontWeight.SemiBold)
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTheme(mode) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { onTheme(mode) })
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
            HorizontalDivider()
            SwitchRow("Keep screen awake", settings.keepScreenAwake, onKeepAwake)
            SwitchRow("Resume last settings", settings.resumeLastSettings, onResumeLast)
        }

        SectionCard("Preferred Audio Mode") {
            AudioMode.entries.forEach { mode ->
                val label = if (mode == AudioMode.LOW_LATENCY) "Low latency" else "Compatible"
                Row(
                    Modifier.fillMaxWidth().clickable { onConfigChange(settings.config.copy(audioMode = mode), true) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.config.audioMode == mode,
                        onClick = { onConfigChange(settings.config.copy(audioMode = mode), true) }
                    )
                    Text(label)
                }
            }
            Text(
                "Low latency requests Android's low-latency AudioTrack performance mode. Compatible uses the normal playback path.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Stereo Behavior") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = true, onClick = null)
                Text("Random Left / Right channel")
            }
            Text(
                "Left and right pulses never start on the same audio frame.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AdjustableControlCard(
            title = "Random Left",
            valueLabel = "${settings.config.randomLeftDelayMs} ms",
            value = settings.config.randomLeftDelayMs.toFloat(),
            range = 1f..100f,
            steps = 98,
            onValueChange = { onConfigChange(settings.config.copy(randomLeftDelayMs = it.roundToInt()), false) },
            onValueChangeFinished = onPersistConfig,
            onMinus = { onConfigChange(settings.config.copy(randomLeftDelayMs = (settings.config.randomLeftDelayMs - 1).coerceAtLeast(1)), true) },
            onPlus = { onConfigChange(settings.config.copy(randomLeftDelayMs = (settings.config.randomLeftDelayMs + 1).coerceAtMost(100)), true) },
            description = "Maximum random delay used when the Left channel is the delayed/follower pulse. Range: 1–100 ms."
        )

        AdjustableControlCard(
            title = "Random Right",
            valueLabel = "${settings.config.randomRightDelayMs} ms",
            value = settings.config.randomRightDelayMs.toFloat(),
            range = 1f..100f,
            steps = 98,
            onValueChange = { onConfigChange(settings.config.copy(randomRightDelayMs = it.roundToInt()), false) },
            onValueChangeFinished = onPersistConfig,
            onMinus = { onConfigChange(settings.config.copy(randomRightDelayMs = (settings.config.randomRightDelayMs - 1).coerceAtLeast(1)), true) },
            onPlus = { onConfigChange(settings.config.copy(randomRightDelayMs = (settings.config.randomRightDelayMs + 1).coerceAtMost(100)), true) },
            description = "Maximum random delay used when the Right channel is the delayed/follower pulse. Range: 1–100 ms."
        )

        SectionCard("Audio Diagnostics") {
            DiagnosticRow("Output", diagnostics.outputDevice)
            DiagnosticRow("Selected sample rate", if (diagnostics.selectedSampleRate > 0) "${diagnostics.selectedSampleRate} Hz" else "Not active")
            DiagnosticRow("Native rate hint", if (diagnostics.nativeRateHint > 0) "${diagnostics.nativeRateHint} Hz" else "Unknown")
            DiagnosticRow("Channels", diagnostics.channelCount.toString())
            DiagnosticRow("PCM format", diagnostics.pcmFormat)
            DiagnosticRow("Requested pulse", "${settings.config.requestedPulseUs} µs")
            DiagnosticRow("Actual pulse", if (diagnostics.actualPulseUs > 0) "%.2f µs".format(diagnostics.actualPulseUs) else "Start playback to measure")
            DiagnosticRow("Samples / pulse", diagnostics.samplesPerPulse.toString())
            DiagnosticRow("Random Left max", "${settings.config.randomLeftDelayMs} ms")
            DiagnosticRow("Random Right max", "${settings.config.randomRightDelayMs} ms")
            DiagnosticRow("Actual Left max", if (diagnostics.actualRandomLeftDelayMs > 0) "%.2f ms".format(diagnostics.actualRandomLeftDelayMs) else "Start playback to measure")
            DiagnosticRow("Actual Right max", if (diagnostics.actualRandomRightDelayMs > 0) "%.2f ms".format(diagnostics.actualRandomRightDelayMs) else "Start playback to measure")
            DiagnosticRow("Buffer", "${diagnostics.bufferFrames} frames")
            DiagnosticRow("Underruns", diagnostics.underrunCount.toString())
            DiagnosticRow("Low latency requested", if (diagnostics.lowLatencyRequested) "Yes" else "No / unavailable")
            DiagnosticRow("CPU usage", "%.1f%%".format(diagnostics.cpuUsagePercent))
            DiagnosticRow("Memory usage", "%.1f MB PSS".format(diagnostics.memoryUsageMb))
        }

        SectionCard("About") {
            Text("Auralis", fontWeight = FontWeight.Bold)
            Text("Sound, Focus & Psychoacoustics", color = MaterialTheme.colorScheme.primary)
            Text("Version ${BuildConfig.VERSION_NAME}")
            Text(
                "Experimental stereo impulse generator for relaxation and auditory-perception exercises. It is not a medical device and makes no therapeutic claims.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Use a comfortable listening level and start low, especially with headphones.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onValue: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = value, onCheckedChange = onValue)
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DurationEntryDialog(current: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pulse Duration") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("40 – 90 µs") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = parsed == null || parsed !in 40..90
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.takeIf { it in 40..90 }?.let(onApply) },
                enabled = parsed != null && parsed in 40..90
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
