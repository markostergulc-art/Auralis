package com.marko.auralis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.PlaybackStatus
import com.marko.auralis.model.PlaybackUiState
import com.marko.auralis.ui.components.AdjustableControlCard
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * v1.9 goal-first home. The DSP/configuration model is unchanged; this screen only
 * changes information hierarchy so a beginner can start from a goal and expand
 * technical controls only when needed.
 */
@Composable
fun HomeScreen(
    config: AudioConfig,
    playback: PlaybackUiState,
    currentPresetName: String,
    onConfigChange: (AudioConfig, Boolean) -> Unit,
    onPersistConfig: () -> Unit,
    onPlayStop: () -> Unit,
    onGoal: (String) -> Unit,
    onOpenPresets: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showTimerDialog by remember { mutableStateOf(false) }
    var showVolumeWindow by remember { mutableStateOf(false) }
    var showPulseWindow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auralis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Sound, Focus & Psychoacoustics", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.height(48.dp)) { Text("⚙") }
        }

        Text("What do you need right now?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalButton("Relax", "Calmer soundscapes", Modifier.weight(1f)) { onGoal("relax") }
            GoalButton("Focus", "Stable concentration sound", Modifier.weight(1f)) { onGoal("focus") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalButton("Breathe", "Guided slow breathing", Modifier.weight(1f)) { onGoal("breathing") }
            GoalButton("Sleep Prep", "Low-information evening sound", Modifier.weight(1f)) { onGoal("sleep") }
        }

        Text("Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(currentPresetName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                playback.timerRemainingMs?.let { Text("Timer ${formatRemaining(it)}", color = MaterialTheme.colorScheme.primary) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPlayStop, modifier = Modifier.weight(1.35f).height(52.dp)) {
                        Text(if (playback.status == PlaybackStatus.PLAYING || playback.status == PlaybackStatus.STARTING) "■ Stop" else "▶ Start")
                    }
                    OutlinedButton(onClick = onOpenPresets, modifier = Modifier.weight(1f).height(52.dp)) { Text("Presets") }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = { showVolumeWindow = true }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Volume") }
            OutlinedButton(onClick = { showPulseWindow = true }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Pulse") }
            OutlinedButton(onClick = { showTimerDialog = true }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Timer") }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showVolumeWindow) {
        AlertDialog(
            onDismissRequest = { showVolumeWindow = false },
            title = { Text("Volume", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AdjustableControlCard("Volume", "${config.volumePercent}%", config.volumePercent.toFloat(), 0f..100f, 99,
                        { onConfigChange(config.copy(volumePercent = it.roundToInt()), false) }, onPersistConfig,
                        { onConfigChange(config.copy(volumePercent = (config.volumePercent - 1).coerceAtLeast(0)), true) },
                        { onConfigChange(config.copy(volumePercent = (config.volumePercent + 1).coerceAtMost(100)), true) })
                    AdjustableControlCard("Balance Left / Right", balanceLabel(config.balancePercent), config.balancePercent.toFloat(), -100f..100f, 199,
                        { onConfigChange(config.copy(balancePercent = it.roundToInt()), false) }, onPersistConfig,
                        { onConfigChange(config.copy(balancePercent = (config.balancePercent - 1).coerceAtLeast(-100)), true) },
                        { onConfigChange(config.copy(balancePercent = (config.balancePercent + 1).coerceAtMost(100)), true) })
                    AdjustableControlCard("Random Dynamics", "${config.randomDynamicsPercent}%", config.randomDynamicsPercent.toFloat(), 0f..50f, 49,
                        { onConfigChange(config.copy(randomDynamicsPercent = it.roundToInt()), false) }, onPersistConfig,
                        { onConfigChange(config.copy(randomDynamicsPercent = (config.randomDynamicsPercent - 1).coerceAtLeast(0)), true) },
                        { onConfigChange(config.copy(randomDynamicsPercent = (config.randomDynamicsPercent + 1).coerceAtMost(50)), true) })
                }
            }, confirmButton = { TextButton(onClick = { showVolumeWindow = false }) { Text("Close") } }
        )
    }

    if (showPulseWindow) {
        AlertDialog(
            onDismissRequest = { showPulseWindow = false },
            title = { Text("Pulse", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AdjustableControlCard("Pulse Rate", "%.1f pulses/s".format(config.pulseRateHz), config.pulseRateHz, 1f..6f, 49,
                        { onConfigChange(config.copy(pulseRateHz = (it * 10f).roundToInt() / 10f), false) }, onPersistConfig,
                        { onConfigChange(config.copy(pulseRateHz = (config.pulseRateHz - .1f).coerceAtLeast(1f)), true) },
                        { onConfigChange(config.copy(pulseRateHz = (config.pulseRateHz + .1f).coerceAtMost(6f)), true) })
                    AdjustableControlCard("Random Pulse Timing", "${config.randomTimingPercent}%", config.randomTimingPercent.toFloat(), 0f..50f, 49,
                        { onConfigChange(config.copy(randomTimingPercent = it.roundToInt()), false) }, onPersistConfig,
                        { onConfigChange(config.copy(randomTimingPercent = (config.randomTimingPercent - 1).coerceAtLeast(0)), true) },
                        { onConfigChange(config.copy(randomTimingPercent = (config.randomTimingPercent + 1).coerceAtMost(50)), true) })
                    AdjustableControlCard("Pulse Duration", "${config.requestedPulseUs} µs", config.requestedPulseUs.toFloat(), 40f..90f, 49,
                        { onConfigChange(config.copy(requestedPulseUs = it.roundToInt()), false) }, onPersistConfig,
                        { onConfigChange(config.copy(requestedPulseUs = (config.requestedPulseUs - 1).coerceAtLeast(40)), true) },
                        { onConfigChange(config.copy(requestedPulseUs = (config.requestedPulseUs + 1).coerceAtMost(90)), true) })
                }
            }, confirmButton = { TextButton(onClick = { showPulseWindow = false }) { Text("Close") } }
        )
    }

    if (showTimerDialog) {
        TimerDialog(config.timerMinutes, { showTimerDialog = false }) {
            onConfigChange(config.copy(timerMinutes = it), true); showTimerDialog = false
        }
    }
}

@Composable
private fun GoalButton(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(72.dp)) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun balanceLabel(value: Int): String = when { value < 0 -> "L ${abs(value)}%"; value > 0 -> "R ${value}%"; else -> "0%" }

@Composable
private fun TimerDialog(currentMinutes: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    var hours by remember(currentMinutes) { mutableIntStateOf((currentMinutes / 60).coerceIn(0, 12)) }
    var minutes by remember(currentMinutes) { mutableIntStateOf((currentMinutes % 60).coerceIn(0, 59)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Playback Timer") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                NumberStepper("Hours", hours, 0, 12, { hours = it }, Modifier.weight(1f))
                NumberStepper("Minutes", minutes, 0, 59, { minutes = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(5,10,15,20,30,45,60).take(4).forEach { q -> FilledTonalButton(onClick = { hours=q/60;minutes=q%60 }, modifier=Modifier.weight(1f)) { Text("${q}m") } }
            }
        }
    }, confirmButton = { TextButton(onClick = { onApply((hours * 60 + minutes).coerceIn(0, 12*60+59)) }) { Text("Set Timer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun NumberStepper(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onValue((value - 1).coerceAtLeast(min)) }) { Text("−") }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onValue((value + 1).coerceAtMost(max)) }) { Text("+") }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val total=(ms/1000).coerceAtLeast(0); val h=total/3600; val m=(total%3600)/60; val s=total%60
    return "%02d:%02d:%02d".format(h,m,s)
}
