package dev.pkdiv.spendtracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.ui.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Bottom sheet hosting the Activity filter controls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheet(
    filters: HistoryFilters,
    onDirectionChange: (TransactionDirection?) -> Unit,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit,
    onExcludeDuplicatesChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column {
            HistoryFilterContent(
                filters = filters,
                onDirectionChange = onDirectionChange,
                onDateRangeChange = onDateRangeChange,
                onMinAmountChange = onMinAmountChange,
                onMaxAmountChange = onMaxAmountChange,
                onExcludeDuplicatesChange = onExcludeDuplicatesChange,
                onClearFilters = onClearFilters,
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(stringResource(R.string.history_filter_done))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterContent(
    filters: HistoryFilters,
    onDirectionChange: (TransactionDirection?) -> Unit,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit,
    onExcludeDuplicatesChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_filters),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.history_clear_filters))
            }
        }

        FilterSection(title = stringResource(R.string.history_filter_direction)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filters.direction == null,
                    onClick = { onDirectionChange(null) },
                    label = { Text(stringResource(R.string.history_direction_all)) },
                )
                FilterChip(
                    selected = filters.direction == TransactionDirection.DEBIT,
                    onClick = { onDirectionChange(TransactionDirection.DEBIT) },
                    label = { Text(stringResource(R.string.history_direction_out)) },
                )
                FilterChip(
                    selected = filters.direction == TransactionDirection.CREDIT,
                    onClick = { onDirectionChange(TransactionDirection.CREDIT) },
                    label = { Text(stringResource(R.string.history_direction_in)) },
                )
            }
        }

        FilterSection(title = stringResource(R.string.history_filter_date_range)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateRangeLabel(filters))
                }
                if (filters.fromDate != null || filters.toDate != null) {
                    IconButton(onClick = { onDateRangeChange(null, null) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.history_filter_date_clear),
                        )
                    }
                }
            }
        }

        FilterSection(title = stringResource(R.string.history_filter_amount)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filters.minAmountInput,
                    onValueChange = onMinAmountChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.history_filter_amount_min)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = filters.maxAmountInput,
                    onValueChange = onMaxAmountChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.history_filter_amount_max)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_filter_exclude_duplicates),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = filters.excludeDuplicates,
                onCheckedChange = onExcludeDuplicatesChange,
            )
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            initialFrom = filters.fromDate,
            initialTo = filters.toDate,
            onConfirm = { from, to ->
                showDatePicker = false
                onDateRangeChange(from, to)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialFrom: LocalDate?,
    initialTo: LocalDate?,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.toUtcMillis(),
        initialSelectedEndDateMillis = initialTo?.toUtcMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        state.selectedStartDateMillis?.toUtcLocalDate(),
                        state.selectedEndDateMillis?.toUtcLocalDate(),
                    )
                },
            ) {
                Text(stringResource(R.string.history_filter_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.history_filter_cancel))
            }
        },
    ) {
        DateRangePicker(
            state = state,
            showModeToggle = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun dateRangeLabel(filters: HistoryFilters): String = when {
    filters.fromDate != null && filters.toDate != null -> stringResource(
        R.string.history_filter_date_range_label,
        Formatters.dateLabel(filters.fromDate),
        Formatters.dateLabel(filters.toDate),
    )

    filters.fromDate != null ->
        stringResource(R.string.history_filter_date_from, Formatters.dateLabel(filters.fromDate))

    filters.toDate != null ->
        stringResource(R.string.history_filter_date_until, Formatters.dateLabel(filters.toDate))

    else -> stringResource(R.string.history_filter_date_any)
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
