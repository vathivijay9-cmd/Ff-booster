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

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityAccentBlue,
    secondary = HighDensityAccentPurple,
    tertiary = HighDensityAccentGreen,
    background = HighDensityDarkBg,
    surface = HighDensityCard,
    onBackground = Slate100,
    onSurface = Slate100
  )

private val LightColorScheme =
  darkColorScheme( // Free Fire is a high performance gaming app, support premium slate darker colors across light & dark themes!
    primary = HighDensityAccentBlue,
    secondary = HighDensityAccentPurple,
    tertiary = HighDensityAccentGreen,
    background = HighDensityDarkBg,
    surface = HighDensityCard,
    onBackground = Slate100,
    onSurface = Slate100
  )


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
