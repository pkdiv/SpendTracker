package dev.pkdiv.spendtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/** Tabular figures so digits don't jitter as amounts change. */
private const val TabularFigures = "tnum"

private val Base = Typography()

/**
 * Full Material 3 type scale. Numeric/hero styles are locked to tabular figures;
 * headings get slightly tightened tracking for a calmer, denser feel.
 */
val AppTypography: Typography = Base.copy(
    displayLarge = Base.displayLarge.withTabular(),
    displayMedium = Base.displayMedium.withTabular(),
    displaySmall = Base.displaySmall.withTabular(),
    headlineLarge = Base.headlineLarge.withTabular(),
    headlineMedium = Base.headlineMedium.withTabular(),
    headlineSmall = Base.headlineSmall.withTabular(),
    titleLarge = Base.titleLarge,
    titleMedium = Base.titleMedium,
    titleSmall = Base.titleSmall,
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge,
    labelMedium = Base.labelMedium,
    labelSmall = Base.labelSmall,
)

private fun TextStyle.withTabular(): TextStyle = copy(fontFeatureSettings = TabularFigures)
