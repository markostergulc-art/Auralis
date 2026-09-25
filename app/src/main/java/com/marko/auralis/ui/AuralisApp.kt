package com.marko.auralis.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marko.auralis.model.PlaybackStatus
import com.marko.auralis.model.BuiltInPresetCatalog
import com.marko.auralis.data.LanguageMode
import com.marko.auralis.ui.screens.HomeScreen
import com.marko.auralis.ui.screens.PresetsScreen
import com.marko.auralis.ui.screens.SettingsScreen
import com.marko.auralis.ui.theme.AuralisTheme

enum class AppScreen { HOME, PRESETS, SETTINGS }

@Composable
fun AuralisApp(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val playback by vm.playback.collectAsState()
    val diagnostics by vm.diagnostics.collectAsState()
    val presets by vm.presets.collectAsState()
    val currentPresetName by vm.currentPresetName.collectAsState()
    val context = LocalContext.current
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var lastBackAt by remember { mutableStateOf(0L) }
    var presetCategory by remember { mutableStateOf("all") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Playback is still allowed when notification permission is declined;
        // Android keeps foreground-service disclosure in system UI.
        vm.startPlayback()
    }

    fun requestPlayOrToggle() {
        if (playback.status != PlaybackStatus.IDLE && playback.status != PlaybackStatus.ERROR) {
            vm.stopPlayback()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startPlayback()
        }
    }

    DisposableEffect(settings.keepScreenAwake) {
        val activity = context as? Activity
        if (settings.keepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (!settings.keepScreenAwake) activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = true) {
        val now = SystemClock.elapsedRealtime()
        if (screen != AppScreen.HOME) {
            screen = AppScreen.HOME
            lastBackAt = now
        } else if (now - lastBackAt <= 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackAt = now
        }
    }

    AuralisTheme(settings.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    config = settings.config,
                    playback = playback,
                    currentPresetName = currentPresetName,
                    onConfigChange = vm::setConfig,
                    onPersistConfig = vm::persistCurrentConfig,
                    onPlayStop = ::requestPlayOrToggle,
                    onGoal = { presetCategory = it; screen = AppScreen.PRESETS },
                    onOpenPresets = { presetCategory = "all"; screen = AppScreen.PRESETS },
                    onOpenSettings = { screen = AppScreen.SETTINGS }
                )
                AppScreen.PRESETS -> PresetsScreen(
                    config = settings.config,
                    presets = presets,
                    builtIns = BuiltInPresetCatalog.all,
                    language = settings.languageMode,
                    initialCategory = presetCategory,
                    onCategoryChange = { presetCategory = it },
                    onLoadBuiltIn = { vm.loadBuiltInPreset(it); screen = AppScreen.HOME },
                    onCopyBuiltIn = vm::copyBuiltInPreset,
                    onBack = { screen = AppScreen.HOME },
                    onSave = vm::savePreset,
                    onLoad = { vm.loadPreset(it); screen = AppScreen.HOME },
                    onRename = vm::renamePreset,
                    onDuplicate = vm::duplicatePreset,
                    onUpdate = vm::updatePreset,
                    onDelete = vm::deletePreset
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    diagnostics = diagnostics,
                    onBack = { screen = AppScreen.HOME },
                    onConfigChange = vm::setConfig,
                    onPersistConfig = vm::persistCurrentConfig,
                    onTheme = vm::setTheme,
                    onLanguage = vm::setLanguage,
                    onKeepAwake = vm::setKeepAwake,
                    onResumeLast = vm::setResumeLast
                )
            }
        }

        if (!settings.languageSelected) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Select language / Sprache wählen / Odaberite jezik") },
                text = { Text("English · Deutsch · Hrvatski") },
                confirmButton = {
                    TextButton(onClick = { vm.setLanguage(LanguageMode.ENGLISH) }) { Text("English") }
                },
                dismissButton = {
                    androidx.compose.foundation.layout.Row {
                        TextButton(onClick = { vm.setLanguage(LanguageMode.GERMAN) }) { Text("Deutsch") }
                        TextButton(onClick = { vm.setLanguage(LanguageMode.CROATIAN) }) { Text("Hrvatski") }
                    }
                }
            )
        } else if (!settings.firstLaunchAccepted) {
            val text = when (settings.languageMode) {
                LanguageMode.ENGLISH -> "Use a comfortable listening level and start low, especially with headphones. Stop if the sound is uncomfortable. This is an experimental sound, focus and psychoacoustic exploration tool and is not a medical device."
                LanguageMode.GERMAN -> "Verwenden Sie eine angenehme Lautstärke und beginnen Sie besonders bei Kopfhörern leise. Beenden Sie die Nutzung, wenn der Klang unangenehm ist. Dies ist ein experimentelles Werkzeug zur Entspannung / Hörwahrnehmung und kein Medizinprodukt."
                LanguageMode.CROATIAN -> "Koristite ugodnu razinu glasnoće i počnite tiho, osobito sa slušalicama. Prekinite upotrebu ako je zvuk neugodan. Ovo je eksperimentalni alat za zvuk, fokus i psihoakustičko istraživanje i nije medicinski uređaj."
            }
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Auralis") },
                text = { Text(text) },
                confirmButton = { TextButton(onClick = vm::acceptFirstLaunch) { Text("Yes, I Accept") } },
                dismissButton = { TextButton(onClick = { (context as? Activity)?.finish() }) { Text("No") } }
            )
        }
    }
}
