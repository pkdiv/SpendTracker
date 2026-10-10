package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.theme.SpendTheme
import java.math.BigDecimal

/**
 * The month hero: label, tabular amount (whole + smaller paise), delta chip, sparkline,
 * and a transaction-count caption.
 */
@Composable
fun HeroSpendCard(
    label: String,
    amount: Formatters.AmountParts,
    deltaPercent: BigDecimal?,
    transactionCountLabel: String,
    dailySpend: List<BigDecimal>,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    sparklineDescription: String? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (dimmed) 0.6f else 1f },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = amount.whole,
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = amount.paise,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DeltaChip(deltaPercent)

            Spacer(modifier = Modifier.height(16.dp))

            Sparkline(
                values = dailySpend.map { it.toFloat() },
                contentDescription = sparklineDescription,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = transactionCountLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeltaChip(deltaPercent: BigDecimal?) {
    val colors = SpendTheme.colors
    val (text, color, icon) = when {
        deltaPercent == null ->
            Triple("—", colors.neutral, Icons.AutoMirrored.Filled.TrendingFlat)

        deltaPercent.signum() > 0 ->
            Triple("+${magnitude(deltaPercent)}%", colors.expense, Icons.AutoMirrored.Filled.TrendingUp)

        deltaPercent.signum() < 0 ->
            Triple("−${magnitude(deltaPercent)}%", colors.income, Icons.AutoMirrored.Filled.TrendingDown)

        else ->
            Triple("0%", colors.neutral, Icons.AutoMirrored.Filled.TrendingFlat)
    }

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

private fun magnitude(value: BigDecimal): String =
    value.abs().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
