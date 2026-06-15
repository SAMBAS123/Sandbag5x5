package com.sandbagstrong.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brutally simple high-contrast palette
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8C42),       // burnt orange
    onPrimary = Color(0xFF121212),
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCCCCCC),
    error = Color(0xFFE53935),
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFCC5500),
    onPrimary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212)
)

@Composable
fun SandbagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography, // keep system for minimal
        content = content
    )
}

// Convenience colors for this app (can be used without Material if wanted)
object AppColors {
    val accent = Color(0xFFFF8C42)
    val bg = Color(0xFF121212)
    val surface = Color(0xFF1E1E1E)
    val text = Color.White
    val muted = Color(0xFF888888)
    val completeGreen = Color(0xFF4CAF50)
}
