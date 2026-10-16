package dev.pkdiv.spendtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.pkdiv.spendtracker.parsing.TransactionCategory

/** Icon for each category, shared by rows, chips and charts. */
fun categoryIcon(category: TransactionCategory): ImageVector = when (category) {
    TransactionCategory.FOOD -> Icons.Filled.Restaurant
    TransactionCategory.TRANSPORT -> Icons.Filled.DirectionsCar
    TransactionCategory.SHOPPING -> Icons.Filled.ShoppingBag
    TransactionCategory.BILLS -> Icons.AutoMirrored.Filled.ReceiptLong
    TransactionCategory.ENTERTAINMENT -> Icons.Filled.Movie
    TransactionCategory.HEALTH -> Icons.Filled.MedicalServices
    TransactionCategory.OTHER -> Icons.Filled.Category
    TransactionCategory.SELF_TRANSFER -> Icons.Filled.SwapHoriz
}

@Immutable
data class CategoryColors(val container: Color, val content: Color)

/**
 * Rows without a meaningful income/expense signal — duplicates and internal (self)
 * transfers — are rendered muted instead of using the income/expense colors.
 */
fun isMutedRow(category: TransactionCategory, isDuplicate: Boolean): Boolean =
    isDuplicate || category == TransactionCategory.SELF_TRANSFER

/** Restrained, M3-role-based tint for a category's icon circle. */
@Composable
fun categoryColors(category: TransactionCategory): CategoryColors {
    val scheme = MaterialTheme.colorScheme
    return when (category) {
        TransactionCategory.FOOD,
        TransactionCategory.ENTERTAINMENT,
        -> CategoryColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)

        TransactionCategory.TRANSPORT,
        TransactionCategory.BILLS,
        -> CategoryColors(scheme.secondaryContainer, scheme.onSecondaryContainer)

        TransactionCategory.SHOPPING,
        TransactionCategory.HEALTH,
        -> CategoryColors(scheme.primaryContainer, scheme.onPrimaryContainer)

        TransactionCategory.OTHER,
        TransactionCategory.SELF_TRANSFER,
        -> CategoryColors(scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
    }
}
