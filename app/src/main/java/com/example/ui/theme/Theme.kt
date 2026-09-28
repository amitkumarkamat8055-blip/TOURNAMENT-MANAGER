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

private val DarkColorScheme = darkColorScheme(
  primary = PolishPrimaryDark,
  onPrimary = Color(0xFF121F0E),
  primaryContainer = PolishPrimaryContainerDark,
  onPrimaryContainer = PolishOnPrimaryContainerDark,
  secondary = PolishTextMutedDark,
  onSecondary = Color.White,
  secondaryContainer = PolishSurfaceVariantDark,
  onSecondaryContainer = PolishTextPrimaryDark,
  tertiary = TrophyGold,
  onTertiary = Color.White,
  tertiaryContainer = TrophyGoldDark.copy(alpha = 0.3f),
  onTertiaryContainer = TrophyGold,
  background = PolishBackgroundDark,
  onBackground = PolishTextPrimaryDark,
  surface = PolishSurfaceDark,
  onSurface = PolishTextPrimaryDark,
  surfaceVariant = PolishSurfaceVariantDark,
  onSurfaceVariant = PolishTextSecondaryDark,
  outline = PolishCardBorderDark,
  error = AlertRed,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = ForestGreenPrimary,
  onPrimary = Color.White,
  primaryContainer = ForestGreenContainer,
  onPrimaryContainer = ForestGreenOnContainer,
  secondary = SageSecondary,
  onSecondary = Color.White,
  secondaryContainer = SageSecondaryContainer,
  onSecondaryContainer = SageOnSecondaryContainer,
  tertiary = TrophyGold,
  onTertiary = Color.White,
  tertiaryContainer = TrophyGoldContainer,
  onTertiaryContainer = TrophyGoldDark,
  background = PolishBackgroundLight,
  onBackground = PolishTextPrimaryLight,
  surface = PolishSurfaceLight,
  onSurface = PolishTextPrimaryLight,
  surfaceVariant = PolishSurfaceVariantLight,
  onSurfaceVariant = PolishTextSecondaryLight,
  outline = PolishCardBorderLight,
  error = AlertRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = TournamentTypography,
    content = content
  )
}
