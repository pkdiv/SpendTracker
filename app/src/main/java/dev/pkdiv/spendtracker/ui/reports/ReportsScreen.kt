package dev.pkdiv.spendtracker.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.reports.MonthlyReport
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.BarChart
import dev.pkdiv.spendtracker.ui.components.BarDatum
import dev.pkdiv.spendtracker.ui.components.CategoryRow
import dev.pkdiv.spendtracker.ui.components.DonutChart
import dev.pkdiv.spendtracker.ui.components.DonutSlice
import dev.pkdiv.spendtracker.ui.components.EmptyState
import dev.pkdiv.spendtracker.ui.components.SectionHeader
import dev.pkdiv.spendtracker.ui.components.categoryColors
import dev.pkdiv.spendtracker.ui.components.categoryIcon
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.reports_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        val report = state.report
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))

            report == null || report.transactionCount == 0 -> EmptyState(
                title = stringResource(R.string.reports_empty),
                message = stringResource(R.string.home_empty_message),
                modifier = Modifier.padding(16.dp),
            )

            else -> ReportsContent(
                monthLabel = Formatters.monthLabel(state.month),
                report = report,
            )
        }
    }
}

@Composable
private fun ReportsContent(monthLabel: String, report: MonthlyReport) {
    val categories = report.categories
    val labels = categories.map { stringResource(Formatters.categoryLabelRes(it.category)) }
    val slices = categories.mapIndexed { index, spend ->
        DonutSlice(label = labels[index], value = spend.total.toFloat(), color = sliceColor(index))
    }
    val donutDescription = categories
        .mapIndexed { index, spend -> "${labels[index]} ${percent(spend.total, report.totalSpend)}%" }
        .joinToString(", ")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "month") {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item(key = "total") {
            MonthTotalCard(report)
        }

        item(key = "donut_header") {
            SectionHeader(title = stringResource(R.string.reports_by_category))
        }

        item(key = "donut") {
            DonutChart(
                slices = slices,
                contentDescription = stringResource(R.string.reports_donut_desc) + ": " + donutDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 24.dp),
            )
        }

        items(categories, key = { "cat_${it.category}" }) { spend ->
            val colors = categoryColors(spend.category)
            CategoryRow(
                name = stringResource(Formatters.categoryLabelRes(spend.category)),
                amount = Formatters.formatInr(spend.total),
                share = share(spend.total, report.totalSpend),
                icon = categoryIcon(spend.category),
                containerColor = colors.container,
                contentColor = colors.content,
            )
        }

        item(key = "mom_header") {
            SectionHeader(title = stringResource(R.string.reports_previous_month))
        }

        item(key = "mom_chart") {
            BarChart(
                bars = listOf(
                    BarDatum(
                        label = stringResource(R.string.reports_previous_month),
                        valueText = Formatters.formatInr(report.previousMonthTotal),
                        value = report.previousMonthTotal.toFloat(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    BarDatum(
                        label = stringResource(R.string.reports_month_total),
                        valueText = Formatters.formatInr(report.totalSpend),
                        value = report.totalSpend.toFloat(),
                        color = MaterialTheme.colorScheme.primary,
                    ),
                ),
                contentDescription = stringResource(R.string.reports_mom_desc),
            )
        }

        if (report.topMerchants.isNotEmpty()) {
            item(key = "merchants_header") {
                SectionHeader(title = stringResource(R.string.reports_top_merchants))
            }
            items(report.topMerchants, key = { "merchant_${it.merchant}" }) { merchant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = merchant.merchant, modifier = Modifier.weight(1f))
                    Text(
                        text = Formatters.formatInr(merchant.total),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthTotalCard(report: MonthlyReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.reports_month_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Formatters.formatInr(report.totalSpend),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reports_previous_month) + ": " +
                    Formatters.formatInr(report.previousMonthTotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        scheme.surfaceVariant,
    )
    return palette[index % palette.size]
}

private fun share(part: BigDecimal, total: BigDecimal): Float =
    if (total.signum() > 0) part.divide(total, 4, RoundingMode.HALF_UP).toFloat() else 0f

private fun percent(part: BigDecimal, total: BigDecimal): String =
    if (total.signum() > 0) {
        part.divide(total, 4, RoundingMode.HALF_UP)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toPlainString()
    } else {
        "0"
    }
