package com.cannatrace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

// ── Palette vert cannabis médical ─────────────────────────────────────

private val Green80 = Color(0xFF4CAF50)
private val Green60 = Color(0xFF2E7D32)
private val Green20 = Color(0xFFC8E6C9)
private val Blue80  = Color(0xFF1565C0)
private val Blue20  = Color(0xFFBBDEFB)

private val LightColorScheme = lightColorScheme(
    primary          = Green60,
    onPrimary        = Color.White,
    primaryContainer = Green20,
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary        = Blue80,
    onSecondary      = Color.White,
    secondaryContainer = Blue20,
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary         = Color(0xFF795548),
    background       = Color(0xFFFAFAFA),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFF5F5F5),
    error            = Color(0xFFB71C1C),
    errorContainer   = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF7F0000),
    outline          = Color(0xFF9E9E9E)
)

private val DarkColorScheme = darkColorScheme(
    primary          = Green80,
    onPrimary        = Color(0xFF1B5E20),
    primaryContainer = Green60,
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary        = Color(0xFF90CAF9),
    background       = Color(0xFF121212),
    surface          = Color(0xFF1E1E1E),
    error            = Color(0xFFEF9A9A)
)

@Composable
fun CannaTraceTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CannaTraceTypography,
        content     = content
    )
}
