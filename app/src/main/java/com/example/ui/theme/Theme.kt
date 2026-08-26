package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HighContrastColorScheme =
  lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = OnSaffron,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = Color(0xFF000000),
    secondary = NavySecondary,
    onSecondary = OnNavy,
    secondaryContainer = NavyContainer,
    onSecondaryContainer = Color(0xFF000000),
    tertiary = GoldenTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Color(0xFF000000),
    background = BackgroundWarm,
    onBackground = Color(0xFF000000),
    surface = SurfaceWarm,
    onSurface = Color(0xFF000000),
    surfaceVariant = SurfaceVariantWarm,
    onSurfaceVariant = Color(0xFF000000),
    outline = CardBorderColor,
    error = BloodRed,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use contrasting light background and pure dark black text for maximum clarity and visibility
  MaterialTheme(colorScheme = HighContrastColorScheme, typography = Typography, content = content)
}
