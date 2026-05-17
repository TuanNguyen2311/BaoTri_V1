package com.example.baotri.ui.shared.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand Colors ─────────────────────────────────────────────
val GreenPrimary    = Color(0xFF0F6E56)
val GreenLight      = Color(0xFFE1F5EE)
val GreenOnPrimary  = Color.White

val PurplePrimary   = Color(0xFF6D28D9)
val PurpleLight     = Color(0xFFEDE9FE)

val AmberColor      = Color(0xFFE8930A)
val AmberLight      = Color(0xFFFAEEDA)

val RedColor        = Color(0xFFC93535)
val RedLight        = Color(0xFFFCEBEB)

val SurfaceColor    = Color(0xFFF9FAFB)
val BorderColor     = Color(0xFFD1D5DB)
val TextSecondary   = Color(0xFF6B7280)
val TextTertiary    = Color(0xFF9CA3AF)

// ── Light Color Scheme ────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = GreenOnPrimary,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenPrimary,
    secondary        = PurplePrimary,
    onSecondary      = Color.White,
    secondaryContainer = PurpleLight,
    onSecondaryContainer = PurplePrimary,
    background       = Color.White,
    onBackground     = Color(0xFF111827),
    surface          = Color.White,
    onSurface        = Color(0xFF111827),
    surfaceVariant   = SurfaceColor,
    onSurfaceVariant = TextSecondary,
    outline          = BorderColor,
    error            = RedColor,
    onError          = Color.White,
    errorContainer   = RedLight,
)

// ── Dark Color Scheme ─────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF34D399),
    onPrimary        = Color(0xFF003823),
    primaryContainer = Color(0xFF0A4F3C),
    secondary        = Color(0xFFA78BFA),
    onSecondary      = Color(0xFF1A0050),
    background       = Color(0xFF111827),
    onBackground     = Color(0xFFF9FAFB),
    surface          = Color(0xFF1F2937),
    onSurface        = Color(0xFFF9FAFB),
    surfaceVariant   = Color(0xFF374151),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline          = Color(0xFF4B5563),
    error            = Color(0xFFF87171),
    errorContainer   = Color(0xFF7F1D1D),
)

// ── Typography ────────────────────────────────────────────────
val BaoTriTypography = Typography(
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    headlineSmall  = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleLarge     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleSmall     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    bodyLarge      = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 17.sp),
    labelLarge     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelMedium    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp),
)

// ── Theme Composable ──────────────────────────────────────────
@Composable
fun BaoTriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = BaoTriTypography,
        content     = content
    )
}
