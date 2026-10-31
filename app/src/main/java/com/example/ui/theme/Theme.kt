package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FxColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberObsidian,
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = NeonCyan,
    secondary = NeonViolet,
    onSecondary = CyberObsidian,
    secondaryContainer = NeonVioletContainer,
    onSecondaryContainer = NeonViolet,
    tertiary = NeonLime,
    onTertiary = CyberObsidian,
    background = CyberObsidian,
    onBackground = TextPrimary,
    surface = CyberDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = NeonHotPink,
    onError = CyberObsidian
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark cyber aesthetic
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CyberObsidian.toArgb()
                window.navigationBarColor = CyberObsidian.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = FxColorScheme,
        typography = Typography,
        content = content
    )
}
