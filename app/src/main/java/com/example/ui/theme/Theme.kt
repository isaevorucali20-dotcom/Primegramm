package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicSlateColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = ElectricPurple,
    tertiary = TechGreen,
    background = BackgroundDark,
    surface = SlateGray,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardOverlayDark,
    onSurfaceVariant = TextSecondary
)

private val NeonNebulaColorScheme = darkColorScheme(
    primary = Color(0xFFFF007F), // Electric Pink / Neon Magenta
    secondary = Color(0xFF9D00FF), // Vibrant purple
    tertiary = Color(0xFF00FFFF), // Cyan
    background = Color(0xFF0F051D), // Deep dark galaxy purple
    surface = Color(0xFF221133), // Dark violet card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFFBE4FF),
    onSurface = Color(0xFFFBE4FF),
    surfaceVariant = Color(0xFF2D1B44),
    onSurfaceVariant = Color(0xFFC0AECB)
)

private val MonochromeMatrixColorScheme = darkColorScheme(
    primary = Color(0xFF00FF3C), // Terminal Lime Green
    secondary = Color(0xFF00AA2C), // Darker matrix green
    tertiary = Color(0xFFFFFFFF), // Pure code white
    background = Color(0xFF000000), // AMOLED Black
    surface = Color(0xFF121212), // Very dark grey
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFCCFFDD),
    onSurface = Color(0xFFCCFFDD),
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFF7D9C86)
)

private val ClassicCyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFFCEE09), // Cyberpunk Neon Yellow
    secondary = Color(0xFF00F0FF), // Cyber Neon Cyan
    tertiary = Color(0xFFFF003C), // Cyberpunk Red
    background = Color(0xFF1A191D),
    surface = Color(0xFF2A2830),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF383540),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

private val LightAlabasterColorScheme = lightColorScheme(
    primary = Color(0xFF2979FF), // Royal Indigo Blue
    secondary = Color(0xFF651FFF), // Vibrant Violet Accent
    tertiary = Color(0xFF00B0FF), // Ice light blue
    background = Color(0xFFF5F7FA), // Clean soft light grey
    surface = Color(0xFFFFFFFF), // Crisp white card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B), // Deep slate text
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B)
)

private val MintFreshColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32), // Forest Green
    secondary = Color(0xFFFF6D00), // Zesty orange
    tertiary = Color(0xFF2979FF), // Indigo
    background = Color(0xFFE8F5E9), // Soft mint
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20),
    surfaceVariant = Color(0xFFC8E6C9),
    onSurfaceVariant = Color(0xFF43A047)
)

@Composable
fun PrimegramTheme(
    themeName: String = "Dark Cosmic Slate",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Neon Nebula" -> NeonNebulaColorScheme
        "Monochrome Matrix" -> MonochromeMatrixColorScheme
        "Classic Cyberpunk" -> ClassicCyberpunkColorScheme
        "Light Alabaster" -> LightAlabasterColorScheme
        "Mint Fresh" -> MintFreshColorScheme
        else -> CosmicSlateColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

