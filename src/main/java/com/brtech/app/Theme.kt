package com.brtech.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Orange = Color(0xFFF58220)

private val DarkScheme = darkColorScheme(
    primary = Orange, onPrimary = Color.Black,
    secondary = Color(0xFF8E9196),
    background = Color(0xFF0D0D0E), onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF18191B), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF26282B), onSurfaceVariant = Color(0xFF9A9DA3),
    outline = Color(0xFF3A3C40),
)
private val LightScheme = lightColorScheme(
    primary = Orange, onPrimary = Color.Black,
    secondary = Color(0xFF5F636B),
    background = Color(0xFFF3F4F6), onBackground = Color(0xFF111111),
    surface = Color.White, onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE6E7EA), onSurfaceVariant = Color(0xFF5F636B),
    outline = Color(0xFFB9BBC0),
)

@Composable
fun BrTechTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val w = (view.context as Activity).window
            w.statusBarColor = scheme.background.toArgb()
            w.navigationBarColor = scheme.surface.toArgb()
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
