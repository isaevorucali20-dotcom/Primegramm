package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme

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
    val context = LocalContext.current
    val colorScheme = when {
        themeName == "Monet Dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        themeName == "Monet Dynamic" -> {
            // High-end pastel Monet style palette fallback
            darkColorScheme(
                primary = Color(0xFFD0BCFF),
                secondary = Color(0xFFCCC2DC),
                background = Color(0xFF131217),
                surface = Color(0xFF1E1C24),
                onPrimary = Color(0xFF381E72),
                onSecondary = Color(0xFF332D41),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = Color(0xFF24222A),
                onSurfaceVariant = Color(0xFFCAC4D0)
            )
        }
        themeName == "Midnight Cherry" -> MidnightCherryScheme
        themeName == "AMOLED Gold" -> AmoledGoldScheme
        themeName == "Mint Ghost" -> MintGhostScheme
        themeName == "Sapphire Prime" -> SapphirePrimeScheme
        themeName == "Classic Telegram" -> ClassicTelegramScheme
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
