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
    primary = HuaweiCrimson,
    onPrimary = Color.White,
    primaryContainer = HuaweiCrimsonContainer,
    onPrimaryContainer = HuaweiCrimsonLight,
    secondary = ChampagneGold,
    onSecondary = Color(0xFF23190B),
    secondaryContainer = ChampagneGoldContainer,
    onSecondaryContainer = ChampagneGold,
    tertiary = HarmonyTurquoise,
    onTertiary = Color(0xFF042026),
    tertiaryContainer = HarmonyTurquoiseContainer,
    onTertiaryContainer = HarmonyTurquoise,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = OutlineDark,
    outlineVariant = Color(0xFF222633)
)

private val LightColorScheme = lightColorScheme(
    primary = HuaweiCrimsonDark,
    onPrimary = Color.White,
    primaryContainer = HuaweiCrimsonContainerLight,
    onPrimaryContainer = Color(0xFF410006),
    secondary = ChampagneGoldDark,
    onSecondary = Color.White,
    secondaryContainer = ChampagneGoldContainerLight,
    onSecondaryContainer = Color(0xFF2B1800),
    tertiary = HarmonyTurquoiseDark,
    onTertiary = Color.White,
    tertiaryContainer = HarmonyTurquoiseContainerLight,
    onTertiaryContainer = Color(0xFF001F25),
    background = PorcelainBg,
    onBackground = TextPrimaryLight,
    surface = PorcelainSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PorcelainSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = PorcelainSurface,
    surfaceContainerLow = PorcelainBg,
    surfaceContainer = PorcelainSurfaceHigh,
    surfaceContainerHigh = PorcelainSurfaceHighest,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE5E2DC)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HuaweiThemeManagerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
