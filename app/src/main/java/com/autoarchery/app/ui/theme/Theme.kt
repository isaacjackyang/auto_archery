package com.autoarchery.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SessionAccent,
    secondary = EditedBlue,
    tertiary = PendingOrange,
)

private val DarkColorScheme = darkColorScheme(
    primary = SessionAccent,
    secondary = EditedBlue,
    tertiary = PendingOrange,
)

@Composable
fun AutoArcheryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

