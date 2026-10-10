package dev.pkdiv.spendtracker.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme

private const val LightName = "Light"
private const val DarkName = "Dark"
private const val NightMode = Configuration.UI_MODE_NIGHT_YES

@Composable
private fun PreviewScaffold(content: @Composable () -> Unit) {
    SpendTrackerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

@Preview(name = "$LightName hero", showBackground = true)
@Preview(name = "$DarkName hero", showBackground = true, uiMode = NightMode)
@Composable
private fun HeroSpendCardPreview() {
    PreviewScaffold {
        HeroSpendCard(
            label = "Spent this month",
            amount = Formatters.amountParts(java.math.BigDecimal("24567.50")),
            deltaPercent = java.math.BigDecimal("12"),
            transactionCountLabel = "28 transactions",
            dailySpend = PreviewData.dailySpend,
        )
        HeroSpendCard(
            label = "Spent this month",
            amount = Formatters.amountParts(java.math.BigDecimal("0.00")),
            deltaPercent = null,
            transactionCountLabel = "0 transactions",
            dailySpend = List(24) { java.math.BigDecimal.ZERO },
            dimmed = true,
        )
    }
}

@Preview(name = "$LightName category", showBackground = true)
@Preview(name = "$DarkName category", showBackground = true, uiMode = NightMode)
@Composable
private fun CategoryRowPreview() {
    PreviewScaffold {
        CategoryRow(
            name = "Food",
            amount = Formatters.formatInr(java.math.BigDecimal("4200.00")),
            share = 0.45f,
            icon = categoryIcon(TransactionCategory.FOOD),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        CategoryRow(
            name = "Transport",
            amount = Formatters.formatInr(java.math.BigDecimal("1850.00")),
            share = 0.2f,
            icon = categoryIcon(TransactionCategory.TRANSPORT),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Preview(name = "$LightName transaction", showBackground = true)
@Preview(name = "$DarkName transaction", showBackground = true, uiMode = NightMode)
@Composable
private fun TransactionRowPreview() {
    PreviewScaffold {
        PreviewData.transactions.forEach { transaction ->
            TransactionRow(transaction = transaction, onCategoryChange = {})
        }
    }
}

@Preview(name = "$LightName sparkline", showBackground = true)
@Preview(name = "$DarkName sparkline", showBackground = true, uiMode = NightMode)
@Composable
private fun SparklinePreview() {
    PreviewScaffold {
        Sparkline(values = PreviewData.dailySpend.map { it.toFloat() })
    }
}

@Preview(name = "$LightName sync fab", showBackground = true)
@Preview(name = "$DarkName sync fab", showBackground = true, uiMode = NightMode)
@Composable
private fun SyncFabPreview() {
    PreviewScaffold {
        SyncFab(
            syncing = false,
            expanded = true,
            onClick = {},
            syncLabel = "Sync",
            syncingLabel = "Syncing…",
        )
        SyncFab(
            syncing = true,
            expanded = true,
            onClick = {},
            syncLabel = "Sync",
            syncingLabel = "Syncing…",
        )
    }
}

@Preview(name = "$LightName empty", showBackground = true)
@Preview(name = "$DarkName empty", showBackground = true, uiMode = NightMode)
@Composable
private fun EmptyStatePreview() {
    PreviewScaffold {
        EmptyState(
            title = "No transactions yet",
            message = "Sync your messages to see this month's spending.",
            actionLabel = "Sync messages",
            onAction = {},
        )
    }
}

@Preview(name = "$LightName charts", showBackground = true)
@Preview(name = "$DarkName charts", showBackground = true, uiMode = NightMode)
@Composable
private fun ChartsPreview() {
    PreviewScaffold {
        DonutChart(
            slices = PreviewData.categories.mapIndexed { index, spend ->
                DonutSlice(
                    label = spend.category.name,
                    value = spend.total.toFloat(),
                    color = sliceColor(index),
                )
            },
        )
        BarChart(
            bars = listOf(
                BarDatum("Sep", Formatters.formatInr(java.math.BigDecimal("21000")), 21000f, MaterialTheme.colorScheme.surfaceVariant),
                BarDatum("Oct", Formatters.formatInr(java.math.BigDecimal("24567")), 24567f, MaterialTheme.colorScheme.primary),
            ),
        )
    }
}

@Composable
private fun sliceColor(index: Int): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primary,
        scheme.tertiary,
        scheme.secondary,
        scheme.primaryContainer,
        scheme.surfaceVariant,
    )
    return palette[index % palette.size]
}
