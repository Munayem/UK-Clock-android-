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

private val DarkColorScheme =
  darkColorScheme(
    primary = SkyBlue,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = SkyBlue,
    secondary = Slate400,
    onSecondary = Slate950,
    surface = Slate900,
    onSurface = Slate50,
    background = Slate950,
    onBackground = Slate50,
    tertiary = RoseAccent
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SkyBlueDark,
    onPrimary = Color.White,
    primaryContainer = Slate200,
    onPrimaryContainer = Slate900,
    secondary = Slate700,
    onSecondary = Color.White,
    surface = Slate50,
    onSurface = Slate900,
    background = Color(0xFFF1F5F9),
    onBackground = Slate900,
    tertiary = RoseAccent
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
