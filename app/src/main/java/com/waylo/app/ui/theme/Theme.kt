package com.waylo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WayloDarkColorScheme = darkColorScheme(
    primary = WayloColors.Primary,
    onPrimary = Color.White,
    primaryContainer = WayloColors.Primary,
    onPrimaryContainer = Color.White,
    secondary = WayloColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = WayloColors.SurfaceElevated,
    onSecondaryContainer = WayloColors.OnBackground,
    tertiary = WayloColors.Cyan,
    onTertiary = WayloColors.Background,
    background = WayloColors.Background,
    onBackground = WayloColors.OnBackground,
    surface = WayloColors.Surface,
    onSurface = WayloColors.OnBackground,
    surfaceVariant = WayloColors.SurfaceElevated,
    onSurfaceVariant = WayloColors.OnSecondaryText,
    surfaceContainerLowest = WayloColors.Background,
    surfaceContainerLow = WayloColors.Surface,
    surfaceContainer = WayloColors.Surface,
    surfaceContainerHigh = WayloColors.SurfaceElevated,
    surfaceContainerHighest = WayloColors.SurfaceElevated,
    outline = WayloColors.Outline,
    outlineVariant = WayloColors.Outline,
)

private val WayloLightColorScheme = lightColorScheme(
    primary = WayloColors.LightPrimary,
    onPrimary = Color.White,
    primaryContainer = WayloColors.LightPrimary,
    onPrimaryContainer = Color.White,
    secondary = WayloColors.LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = WayloColors.LightSurfaceElevated,
    onSecondaryContainer = WayloColors.LightOnBackground,
    tertiary = WayloColors.LightAccent,
    onTertiary = Color.White,
    background = WayloColors.LightBackground,
    onBackground = WayloColors.LightOnBackground,
    surface = WayloColors.LightSurface,
    onSurface = WayloColors.LightOnBackground,
    surfaceVariant = WayloColors.LightSurfaceElevated,
    onSurfaceVariant = WayloColors.LightOnSecondaryText,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WayloColors.LightSurface,
    surfaceContainer = WayloColors.LightSurface,
    surfaceContainerHigh = WayloColors.LightSurfaceElevated,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = WayloColors.LightOutline,
    outlineVariant = WayloColors.LightOutline,
)

@Composable
fun WayloTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) WayloDarkColorScheme else WayloLightColorScheme,
        typography = WayloTypography,
        shapes = WayloShapes,
        content = content,
    )
}
