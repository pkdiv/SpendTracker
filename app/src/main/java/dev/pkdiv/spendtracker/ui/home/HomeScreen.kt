package dev.pkdiv.spendtracker.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.reports.CategorySpend
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.CategoryRow
import dev.pkdiv.spendtracker.ui.components.DateHeader
import dev.pkdiv.spendtracker.ui.components.EmptyState
import dev.pkdiv.spendtracker.ui.components.HeroSpendCard
import dev.pkdiv.spendtracker.ui.components.MonthSelector
import dev.pkdiv.spendtracker.ui.components.SectionHeader
import dev.pkdiv.spendtracker.ui.components.SyncFab
import dev.pkdiv.spendtracker.ui.components.TransactionRow
import dev.pkdiv.spendtracker.ui.components.categoryColors
import dev.pkdiv.spendtracker.ui.components.categoryIcon
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToInsights: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rescanState by viewModel.rescanState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 48
        }
    }

    LaunchedEffect(rescanState) {
        when (val current = rescanState) {
            is RescanState.Done -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.home_sync_done, current.processed),
                )
                viewModel.consumeRescan()
            }

            is RescanState.NoPermission -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.home_no_permission),
                )
                viewModel.consumeRescan()
            }

            is RescanState.Error -> {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.home_sync_error, current.message),
                    actionLabel = context.getString(R.string.home_sync_retry),
                )
                viewModel.consumeRescan()
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.rescan()
                }
            }

            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SyncFab(
                syncing = rescanState is RescanState.Running,
                expanded = fabExpanded,
                onClick = { viewModel.rescan() },
                syncLabel = stringResource(R.string.home_sync),
                syncingLabel = stringResource(R.string.home_syncing),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "month") {
                MonthSelector(
                    label = state.monthLabel,
                    selectedMonth = state.selectedMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    onMonthSelected = viewModel::selectMonth,
                    nextEnabled = state.selectedMonth < YearMonth.now(),
                )
            }

            item(key = "hero") {
                HeroSpendCard(
                    label = stringResource(R.string.home_total_spend),
                    amount = Formatters.amountParts(state.totalSpend),
                    deltaPercent = state.deltaPercent,
                    transactionCountLabel = stringResource(
                        R.string.home_transactions_today,
                        state.transactionCount,
                    ),
                    dailySpend = state.dailySpend,
                    dimmed = state.transactionCount == 0,
                    sparklineDescription = stringResource(R.string.home_sparkline_desc),
                )
            }

            item(key = "chips") {
                HomeSummaryChips(
                    today = state.todaySpend,
                    week = state.weekSpend,
                    average = state.dailyAverage,
                )
            }

            if (state.topCategories.isNotEmpty()) {
                item(key = "categories_header") {
                    SectionHeader(
                        title = stringResource(R.string.home_top_categories),
                        actionLabel = stringResource(R.string.home_see_all),
                        onAction = onNavigateToInsights,
                    )
                }
                items(state.topCategories, key = { "category_${it.category}" }) { spend ->
                    CategorySummaryRow(spend = spend, total = state.totalSpend)
                }
            }

            item(key = "recent_header") {
                SectionHeader(title = stringResource(R.string.home_recent))
            }

            if (state.sections.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        actionLabel = stringResource(R.string.home_sync),
                        onAction = { viewModel.rescan() },
                    )
                }
            } else {
                state.sections.forEach { section ->
                    stickyHeader(key = "header_${section.date}") {
                        DateHeader(date = section.date)
                    }

                    items(section.transactions, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            onTransactionClick = onTransactionClick,
                            onCategoryChange = { category ->
                                viewModel.updateCategory(transaction.id, category)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryRow(spend: CategorySpend, total: BigDecimal) {
    val colors = categoryColors(spend.category)
    val share = if (total.signum() > 0) {
        spend.total.divide(total, 4, RoundingMode.HALF_UP).toFloat()
    } else {
        0f
    }
    CategoryRow(
        name = stringResource(Formatters.categoryLabelRes(spend.category)),
        amount = Formatters.formatInr(spend.total),
        share = share,
        icon = categoryIcon(spend.category),
        containerColor = colors.container,
        contentColor = colors.content,
    )
}

@Composable
private fun HomeSummaryChips(today: BigDecimal, week: BigDecimal, average: BigDecimal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip(
            label = stringResource(R.string.home_chip_today),
            value = Formatters.amountParts(today).whole,
            modifier = Modifier.weight(1f),
        )
        StatChip(
            label = stringResource(R.string.home_chip_week),
            value = Formatters.amountParts(week).whole,
            modifier = Modifier.weight(1f),
        )
        StatChip(
            label = stringResource(R.string.home_chip_avg),
            value = Formatters.amountParts(average).whole,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
