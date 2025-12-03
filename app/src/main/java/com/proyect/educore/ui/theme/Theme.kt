package com.proyect.educore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryLight,
    onPrimary = White,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = White,
    secondary = TealSecondaryLight,
    onSecondary = White,
    secondaryContainer = TealSecondaryDark,
    onSecondaryContainer = White,
    tertiary = OrangeTertiaryLight,
    onTertiary = OrangeTertiaryDark,
    tertiaryContainer = OrangeTertiaryDark,
    onTertiaryContainer = OrangeTertiaryLight,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnSurfaceVariantDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceVariantDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BluePrimaryLight,
    onPrimaryContainer = BluePrimaryDark,
    secondary = TealSecondary,
    onSecondary = White,
    secondaryContainer = TealSecondaryLight,
    onSecondaryContainer = TealSecondaryDark,
    tertiary = OrangeTertiary,
    onTertiary = OrangeTertiaryDark,
    tertiaryContainer = OrangeTertiaryLight,
    onTertiaryContainer = OrangeTertiaryDark,
    background = NeutralBackgroundLight,
    onBackground = Color(0xFF101828),
    surface = NeutralSurfaceLight,
    onSurface = Color(0xFF101828),
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralOnSurfaceVariantLight,
    outline = NeutralOutlineLight,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun EduCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
