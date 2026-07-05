package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ElectricBlue, // Color(0xFFD0BCFF)
    onPrimary = Color(0xFF381E72),
    secondary = DarkSurfaceElevated, // Color(0xFF4F378B)
    onSecondary = Color(0xFFEADDFF),
    tertiary = CoralRed, // Color(0xFFF2B8B5)
    onTertiary = Color(0xFF601410),
    background = DarkBg, // Color(0xFF1C1B1F)
    onBackground = TextPrimary, // Color(0xFFE6E1E5)
    surface = DarkSurface, // Color(0xFF2B2930)
    onSurface = TextPrimary, // Color(0xFFE6E1E5)
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFEADDFF),
    outline = BorderColor // Color(0xFF49454F)
  )

private val LightColorScheme = DarkColorScheme // Standardize on beautiful dark theme for premium brand feel

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force premium dark theme for that stunning Axio feel
  dynamicColor: Boolean = false, // Set to false to preserve our gorgeous neon branding colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
