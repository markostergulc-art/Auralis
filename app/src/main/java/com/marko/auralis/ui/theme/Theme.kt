package com.marko.auralis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.marko.auralis.data.ThemeMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Color(0xFF38C7C9),
    onPrimary = Color(0xFF042627),
    secondary = Color(0xFF38C7C9),
    background = Color(0xFF0E141B),
    surface = Color(0xFF151E27),
    surfaceVariant = Color(0xFF1C2835),
    onBackground = Color(0xFFF1F5FA),
    onSurface = Color(0xFFF1F5FA),
    onSurfaceVariant = Color(0xFFAFBAC7),
    error = Color(0xFFFFB4AB)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF159B9E),
    onPrimary = Color.White,
    secondary = Color(0xFF159B9E),
    background = Color(0xFFF6F8FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EEF5),
    onBackground = Color(0xFF151A20),
    onSurface = Color(0xFF151A20),
    onSurfaceVariant = Color(0xFF53606D),
    error = Color(0xFFB3261E)
)


private val AppTypography = Typography().let { base ->
    base.copy(
        bodySmall = base.bodySmall.copy(fontSize = 15.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 17.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 18.sp),
        labelLarge = base.labelLarge.copy(fontSize = 15.sp),
        titleSmall = base.titleSmall.copy(fontSize = 17.sp),
        titleMedium = base.titleMedium.copy(fontSize = 19.sp),
        titleLarge = base.titleLarge.copy(fontSize = 23.sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 27.sp),
        headlineMedium = base.headlineMedium.copy(fontSize = 30.sp)
    )
}
@Composable
fun AuralisTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}
