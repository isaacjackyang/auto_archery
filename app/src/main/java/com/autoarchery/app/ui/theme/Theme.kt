package com.autoarchery.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SessionAccent,
    onPrimary = SessionAccentOn,
    primaryContainer = SessionAccentContainer,
    onPrimaryContainer = SessionAccentOnContainer,
    secondary = EditedBlue,
    onSecondary = EditedBlueOn,
    secondaryContainer = EditedBlueContainer,
    onSecondaryContainer = EditedBlueOnContainer,
    tertiary = PendingOrange,
    onTertiary = PendingOrangeOn,
    tertiaryContainer = PendingOrangeContainer,
    onTertiaryContainer = PendingOrangeOnContainer,
    background = LightSurface,
    onBackground = LightOnSurface,
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF0F5F8),
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSessionAccent,
    onPrimary = DarkSessionAccentOn,
    primaryContainer = DarkSessionAccentContainer,
    onPrimaryContainer = DarkSessionAccentOnContainer,
    secondary = DarkEditedBlue,
    onSecondary = DarkEditedBlueOn,
    secondaryContainer = DarkEditedBlueContainer,
    onSecondaryContainer = DarkEditedBlueOnContainer,
    tertiary = DarkPendingOrange,
    onTertiary = DarkPendingOrangeOn,
    tertiaryContainer = DarkPendingOrangeContainer,
    onTertiaryContainer = DarkPendingOrangeOnContainer,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF17232D),
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
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
