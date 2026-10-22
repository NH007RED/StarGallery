package com.stargallery.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Palette: deep black / midnight navy, electric violet, soft magenta, subtle cyan, white + cool grey.
object SGColors {
    val DeepBlack = Color(0xFF05060F)
    val MidnightNavy = Color(0xFF0B1030)
    val NavySurface = Color(0xFF121838)
    val NavySurfaceHigh = Color(0xFF1A2146)
    val ElectricViolet = Color(0xFF8B5CF6)
    val VioletDeep = Color(0xFF4C1D95)
    val SoftMagenta = Color(0xFFF472B6)
    val SubtleCyan = Color(0xFF22D3EE)
    val CoolGrey = Color(0xFF9AA4BF)
    val White = Color(0xFFFFFFFF)

    val AccentGradient = Brush.linearGradient(listOf(ElectricViolet, SoftMagenta))
}

private val DarkScheme = darkColorScheme(
    primary = SGColors.ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = SGColors.VioletDeep,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = SGColors.SoftMagenta,
    onSecondary = Color(0xFF3B0A24),
    tertiary = SGColors.SubtleCyan,
    onTertiary = Color(0xFF00363D),
    background = SGColors.DeepBlack,
    onBackground = Color.White,
    surface = SGColors.MidnightNavy,
    onSurface = Color.White,
    surfaceVariant = SGColors.NavySurface,
    onSurfaceVariant = SGColors.CoolGrey,
    surfaceContainer = SGColors.NavySurface,
    surfaceContainerHigh = SGColors.NavySurfaceHigh,
    outline = Color(0xFF3A4366),
    outlineVariant = Color(0xFF262E52),
    error = Color(0xFFFF6B8A)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFFBE185D),
    tertiary = Color(0xFF0E7490),
    background = Color(0xFFF7F6FF),
    onBackground = Color(0xFF0B1030),
    surface = Color.White,
    onSurface = Color(0xFF0B1030),
    surfaceVariant = Color(0xFFECEAFB),
    onSurfaceVariant = Color(0xFF50597A),
    surfaceContainer = Color(0xFFEFEDFC),
    surfaceContainerHigh = Color(0xFFE6E3F9),
    outline = Color(0xFF8C93B0),
    outlineVariant = Color(0xFFD3D6EA),
    error = Color(0xFFB3261E)
)

private val SGTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

@Composable
fun StarGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = SGTypography, content = content)
}
