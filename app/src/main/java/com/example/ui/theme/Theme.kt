package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MidnightCherryScheme = darkColorScheme(
    primary = CherryPrimary,
    secondary = CherrySecondary,
    background = CherryBackground,
    surface = CherrySurface,
    onPrimary = CherryOnPrimary,
    onSecondary = CherryOnPrimary,
    onBackground = CherryOnText,
    onSurface = CherryOnText,
    surfaceVariant = Color(0xFF261216),
    onSurfaceVariant = Color(0xFFFFCDD2)
)

private val AmoledGoldScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    background = GoldBackground,
    surface = GoldSurface,
    onPrimary = GoldOnPrimary,
    onSecondary = GoldOnPrimary,
    onBackground = GoldOnText,
    onSurface = GoldOnText,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE0E0E0)
)

private val MintGhostScheme = darkColorScheme(
    primary = MintPrimary,
    secondary = MintSecondary,
    background = MintBackground,
    surface = MintSurface,
    onPrimary = MintOnPrimary,
    onSecondary = MintOnPrimary,
    onBackground = MintOnText,
    onSurface = MintOnText,
    surfaceVariant = Color(0xFF1B2328),
    onSurfaceVariant = Color(0xFFB2EBF2)
)

private val SapphirePrimeScheme = darkColorScheme(
    primary = SapphirePrimary,
    secondary = SapphireSecondary,
    background = SapphireBackground,
    surface = SapphireSurface,
    onPrimary = SapphireOnPrimary,
    onSecondary = SapphireOnPrimary,
    onBackground = SapphireOnText,
    onSurface = SapphireOnText,
    surfaceVariant = Color(0xFF162035),
    onSurfaceVariant = Color(0xFFBBDEFB)
)

private val ClassicTelegramScheme = darkColorScheme(
    primary = TelegramPrimary,
    secondary = TelegramSecondary,
    background = TelegramBackground,
    surface = TelegramSurface,
    onPrimary = TelegramOnPrimary,
    onSecondary = TelegramOnPrimary,
    onBackground = TelegramOnText,
    onSurface = TelegramOnText,
    surfaceVariant = Color(0xFF202B36),
    onSurfaceVariant = Color(0xFF82B1FF)
)

private val ElegantDarkScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    secondary = ElegantDarkSecondary,
    background = ElegantDarkBackground,
    surface = ElegantDarkSurface,
    onPrimary = ElegantDarkOnPrimary,
    onSecondary = ElegantDarkOnPrimary,
    onBackground = ElegantDarkOnText,
    onSurface = ElegantDarkOnText,
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = Color(0xFFD0BCFF)
)

@Composable
fun PrimegramTheme(
    themeName: String,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Midnight Cherry" -> MidnightCherryScheme
        "AMOLED Gold" -> AmoledGoldScheme
        "Mint Ghost" -> MintGhostScheme
        "Sapphire Prime" -> SapphirePrimeScheme
        "Classic Telegram" -> ClassicTelegramScheme
        else -> ElegantDarkScheme // "Elegant Dark"
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep the old MyApplicationTheme standard to avoid broken dependencies, 
// just delegate to Elegant Dark.
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PrimegramTheme(themeName = "Elegant Dark", content = content)
}
