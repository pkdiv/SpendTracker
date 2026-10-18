package dev.pkdiv.spendtracker.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.DateHeader
import dev.pkdiv.spendtracker.ui.components.EmptyState
import dev.pkdiv.spendtracker.ui.components.TransactionRow
import dev.pkdiv.spendtracker.ui.components.categoryIcon
import java.time.ZoneId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onTransactionClick: (Long) -> Unit = {},
    onAddTransaction: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = state.filters
    var showFilters by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val sections = remember(state.transactions) {
        state.transactions
            .groupBy { it.timestamp.atZone(zone).toLocalDate() }
            .toList()
            .sortedByDescending { (date, _) -> date }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.history_add_transaction)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.history_clear_filters))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filters.activeFilterCount > 0,
                    onClick = { showFilters = true },
                    label = {
                        Text(
                            text = if (filters.activeFilterCount > 0) {
                                stringResource(R.string.history_filters_count, filters.activeFilterCount)
                            } else {
                                stringResource(R.string.history_filters)
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                if (filters.hasAnyCriteria) {
                    TextButton(onClick = viewModel::clearFilters) {
                        Text(stringResource(R.string.history_clear_filters))
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = filters.category == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.history_filter_all)) },
                    )
                }
                items(TransactionCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = filters.category == category,
                        onClick = {
                            viewModel.setCategory(if (filters.category == category) null else category)
                        },
                        label = { Text(stringResource(Formatters.categoryLabelRes(category))) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                if (filters.hasAnyCriteria) {
                    EmptyState(
                        title = stringResource(R.string.history_no_matches),
                        message = stringResource(R.string.history_no_matches_message),
                        icon = Icons.Filled.Tune,
                    )
                } else {
                    EmptyState(
                        title = stringResource(R.string.history_empty),
                        message = stringResource(R.string.history_empty_message),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sections.forEach { (date, transactions) ->
                        stickyHeader(key = "header_$date") {
                            DateHeader(date = date)
                        }
                        items(transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                onTransactionClick = onTransactionClick,
                                onCategoryChange = { category ->
                                    viewModel.updateCategory(transaction.id, category)
                                },
                                onToggleDuplicate = { id, isDuplicate ->
                                    viewModel.setDuplicate(id, isDuplicate)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        HistoryFilterSheet(
            filters = filters,
            onDirectionChange = viewModel::setDirection,
            onDateRangeChange = viewModel::setDateRange,
            onMinAmountChange = viewModel::setMinAmount,
            onMaxAmountChange = viewModel::setMaxAmount,
            onExcludeDuplicatesChange = viewModel::setExcludeDuplicates,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilters = false },
        )
    }
}
