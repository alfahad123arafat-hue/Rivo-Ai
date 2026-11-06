package com.rivo.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SoftWhite = Color(0xFFFCFBFF)
val LightLavender = Color(0xFFF1EFFF)
val LavenderSurface = Color(0xFFEDE8FF)
val PurplePrimary = Color(0xFF7C6CF6)
val PurpleLight = Color(0xFF9B8CF8)
val PinkAccent = Color(0xFFE87CA8)
val DeepText = Color(0xFF1E1B2E)
val MutedText = Color(0xFF7A748E)
val GlassSurface = Color(0xFFF7F5FF)
val BorderLight = Color(0xFFE8E0FF)
val ErrorRed = Color(0xFFE5486A)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderSurface,
    onPrimaryContainer = DeepText,
    secondary = PurpleLight,
    onSecondary = Color.White,
    background = SoftWhite,
    onBackground = DeepText,
    surface = SoftWhite,
    onSurface = DeepText,
    surfaceVariant = LightLavender,
    onSurfaceVariant = MutedText,
    error = ErrorRed,
    outline = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    background = Color(0xFF12111A),
    surface = Color(0xFF1A1924)
)

@Composable
fun RivoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RivoTypography,
        content = content
    )
}

val RivoTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, color = DeepText),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = DeepText),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = DeepText),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = DeepText),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = DeepText),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = DeepText),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = DeepText)
)
