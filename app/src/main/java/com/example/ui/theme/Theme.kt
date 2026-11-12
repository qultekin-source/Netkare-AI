package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AmberCinema,
    onPrimary = OnAmberCinema,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberCinemaBright,
    secondary = CyanAi,
    onSecondary = OnCyanAi,
    secondaryContainer = CyanAiContainer,
    onSecondaryContainer = CyanAi,
    tertiary = EmeraldRestore,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldRestore,
    background = ObsidianCanvas,
    onBackground = TextPrimaryDark,
    surface = StudioSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = StudioSlate,
    surfaceContainer = StudioCard,
    surfaceContainerHigh = StudioCardElevated,
    outline = StudioOutline,
    outlineVariant = Color(0xFF1F293D),
    error = CoralError,
    onError = Color(0xFF3B080D),
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralError
)

private val LightColorScheme = lightColorScheme(
    primary = AmberDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = CyanDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = EmeraldRestore,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = DaylightCanvas,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val NetKareShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NetKareTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = NetKareShapes,
        content = content
    )
}
