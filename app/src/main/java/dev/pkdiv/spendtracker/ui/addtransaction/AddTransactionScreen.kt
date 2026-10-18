package dev.pkdiv.spendtracker.ui.addtransaction

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.categoryIcon
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved = state is AddTransactionUiState.Saved
    LaunchedEffect(saved) { if (saved) onBack() }

    AddTransactionContent(
        state = state,
        onAmountChange = viewModel::setAmount,
        onDirectionChange = viewModel::setDirection,
        onMerchantChange = viewModel::setMerchant,
        onCategoryChange = viewModel::setCategory,
        onAccountChange = viewModel::setAccount,
        onDateChange = viewModel::setDate,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    state: AddTransactionUiState,
    onAmountChange: (String) -> Unit,
    onDirectionChange: (TransactionDirection) -> Unit,
    onMerchantChange: (String) -> Unit,
    onCategoryChange: (TransactionCategory) -> Unit,
    onAccountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state as? AddTransactionUiState.Editing
    val form = editing?.form ?: ManualTransactionForm()
    val errors = editing?.errors ?: ManualTransactionErrors()
    val saving = editing?.saving == true
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_transaction_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_transaction_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = onSave,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 48.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        stringResource(
                            if (saving) R.string.add_transaction_saving
                            else R.string.add_transaction_save,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DirectionSelector(
                selected = form.direction,
                enabled = !saving,
                onSelect = onDirectionChange,
            )

            OutlinedTextField(
                value = form.amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                label = { Text(stringResource(R.string.add_transaction_amount)) },
                prefix = { Text("₹") },
                singleLine = true,
                isError = errors.amount,
                supportingText = if (errors.amount) {
                    { Text(stringResource(R.string.add_transaction_amount_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            OutlinedTextField(
                value = form.merchant,
                onValueChange = onMerchantChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                label = { Text(stringResource(R.string.add_transaction_merchant)) },
                placeholder = { Text(stringResource(R.string.add_transaction_merchant_hint)) },
                singleLine = true,
                isError = errors.merchant,
                supportingText = if (errors.merchant) {
                    { Text(stringResource(R.string.add_transaction_merchant_error)) }
                } else {
                    null
                },
            )

            CategorySelector(
                selected = form.category,
                enabled = !saving,
                onSelect = onCategoryChange,
            )

            OutlinedTextField(
                value = form.account,
                onValueChange = onAccountChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                label = { Text(stringResource(R.string.add_transaction_account)) },
                placeholder = { Text(stringResource(R.string.add_transaction_account_hint)) },
                singleLine = true,
            )

            DateField(
                date = form.date,
                enabled = !saving,
                onClick = { showDatePicker = true },
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateChange(it.toLocalDateUtc()) }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.add_transaction_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.add_transaction_date_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSelector(
    selected: TransactionDirection,
    enabled: Boolean,
    onSelect: (TransactionDirection) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TransactionDirection.entries.forEachIndexed { index, direction ->
            SegmentedButton(
                selected = direction == selected,
                onClick = { onSelect(direction) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TransactionDirection.entries.size,
                ),
                label = {
                    Text(
                        stringResource(
                            if (direction == TransactionDirection.DEBIT) {
                                R.string.add_transaction_direction_out
                            } else {
                                R.string.add_transaction_direction_in
                            },
                        ),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    selected: TransactionCategory,
    enabled: Boolean,
    onSelect: (TransactionCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.add_transaction_category),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransactionCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onSelect(category) },
                    enabled = enabled,
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
    }
}

@Composable
private fun DateField(
    date: LocalDate,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.add_transaction_date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Formatters.dateLabel(date),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = stringResource(R.string.add_transaction_date_pick),
            )
        }
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(name = "Light empty", showBackground = true)
@Preview(name = "Dark empty", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddTransactionEmptyPreview() {
    SpendTrackerTheme {
        AddTransactionContent(
            state = AddTransactionUiState.Editing(ManualTransactionForm()),
            onAmountChange = {},
            onDirectionChange = {},
            onMerchantChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onDateChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(name = "Light filled", showBackground = true)
@Preview(name = "Dark filled", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddTransactionFilledPreview() {
    SpendTrackerTheme {
        AddTransactionContent(
            state = AddTransactionUiState.Editing(
                form = ManualTransactionForm(
                    amount = "450",
                    merchant = "Chai stall",
                    category = TransactionCategory.FOOD,
                    account = "1234",
                ),
            ),
            onAmountChange = {},
            onDirectionChange = {},
            onMerchantChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onDateChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(name = "Light errors", showBackground = true)
@Preview(name = "Dark errors", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddTransactionErrorsPreview() {
    SpendTrackerTheme {
        AddTransactionContent(
            state = AddTransactionUiState.Editing(
                form = ManualTransactionForm(),
                errors = ManualTransactionErrors(amount = true, merchant = true),
            ),
            onAmountChange = {},
            onDirectionChange = {},
            onMerchantChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onDateChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
