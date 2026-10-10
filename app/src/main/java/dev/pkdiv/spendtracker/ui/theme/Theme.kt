package dev.pkdiv.spendtracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Android 12+ dynamic color is available but off by default so the brand palette
 * stays consistent across devices.
 */
const val UseDynamicColor = false

@Composable
fun SpendTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = UseDynamicColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val spendColors = if (darkTheme) DarkSpendColors else LightSpendColors

    CompositionLocalProvider(LocalSpendColors provides spendColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Accessor for the semantic money colors. */
object SpendTheme {
    val colors: SpendColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSpendColors.current
}
