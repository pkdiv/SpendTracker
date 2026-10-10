package dev.pkdiv.spendtracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Deep teal seed — confident, calm, fintech. */
private val Teal10 = Color(0xFF002022)
private val Teal20 = Color(0xFF00363A)
private val Teal30 = Color(0xFF004F53)
private val Teal40 = Color(0xFF00696E)
private val Teal80 = Color(0xFF80D4DA)
private val Teal90 = Color(0xFF9CF1F7)

private val TealSecondary40 = Color(0xFF4A6365)
private val TealSecondary80 = Color(0xFFB0CBCD)
private val TealSecondary90 = Color(0xFFCCE8EA)
private val TealSecondary20 = Color(0xFF051F21)
private val TealSecondary30 = Color(0xFF1B3436)

private val Indigo40 = Color(0xFF4F5F7E)
private val Indigo80 = Color(0xFFB7C6E8)
private val Indigo90 = Color(0xFFD7E3FF)
private val Indigo10 = Color(0xFF071B36)
private val Indigo20 = Color(0xFF20314D)
private val Indigo30 = Color(0xFF374765)

val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = TealSecondary40,
    onSecondary = Color.White,
    secondaryContainer = TealSecondary90,
    onSecondaryContainer = TealSecondary20,
    tertiary = Indigo40,
    onTertiary = Color.White,
    tertiaryContainer = Indigo90,
    onTertiaryContainer = Indigo10,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFA),
    onBackground = Color(0xFF171D1D),
    surface = Color(0xFFF5FAFA),
    onSurface = Color(0xFF171D1D),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4849),
    outline = Color(0xFF6F797A),
    outlineVariant = Color(0xFFBEC8C9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F5),
    surfaceContainer = Color(0xFFE9EFEF),
    surfaceContainerHigh = Color(0xFFE3E9E9),
    surfaceContainerHighest = Color(0xFFDDE4E4),
    scrim = Color(0xFF000000),
)

val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = TealSecondary80,
    onSecondary = TealSecondary30,
    secondaryContainer = Color(0xFF324B4D),
    onSecondaryContainer = TealSecondary90,
    tertiary = Indigo80,
    onTertiary = Indigo20,
    tertiaryContainer = Indigo30,
    onTertiaryContainer = Indigo90,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDDE4E4),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDDE4E4),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8C9),
    outline = Color(0xFF899393),
    outlineVariant = Color(0xFF3F4849),
    surfaceContainerLowest = Color(0xFF090F10),
    surfaceContainerLow = Color(0xFF171D1E),
    surfaceContainer = Color(0xFF1B2122),
    surfaceContainerHigh = Color(0xFF252B2C),
    surfaceContainerHighest = Color(0xFF303637),
    scrim = Color(0xFF000000),
)

/**
 * Semantic money colors, kept separate from the Material role palette.
 * Each variant is chosen for AA contrast against its surface container.
 */
@Immutable
data class SpendColors(
    val expense: Color,
    val income: Color,
    val warning: Color,
    val neutral: Color,
)

val LightSpendColors = SpendColors(
    expense = Color(0xFFB3261E),
    income = Color(0xFF1B6E3B),
    warning = Color(0xFF7A5900),
    neutral = Color(0xFF3F4849),
)

val DarkSpendColors = SpendColors(
    expense = Color(0xFFFFB4AB),
    income = Color(0xFF7FDD9A),
    warning = Color(0xFFF2C14E),
    neutral = Color(0xFFBEC8C9),
)

val LocalSpendColors = staticCompositionLocalOf { LightSpendColors }
