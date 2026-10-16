package dev.pkdiv.spendtracker.ui.merchantcategory

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.repository.MerchantCategoryMapping
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.EmptyState
import dev.pkdiv.spendtracker.ui.components.MerchantCategoryRow
import dev.pkdiv.spendtracker.ui.components.PreviewData
import dev.pkdiv.spendtracker.ui.components.categoryIcon
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme

@Composable
fun MerchantCategoryScreen(
    onBack: () -> Unit,
    viewModel: MerchantCategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MerchantCategoryContent(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantCategoryContent(
    state: MerchantCategoryUiState,
    onBack: () -> Unit,
    onSave: (original: String?, merchant: String, category: TransactionCategory) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<MerchantCategoryMapping?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun openEditor(mapping: MerchantCategoryMapping?) {
        editing = mapping
        showEditor = true
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.merchant_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.merchant_categories_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { openEditor(null) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.merchant_categories_add)) },
            )
        },
    ) { padding ->
        when (state) {
            MerchantCategoryUiState.Loading -> MerchantCategorySkeleton(Modifier.padding(padding))

            is MerchantCategoryUiState.Content -> if (state.mappings.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.merchant_categories_empty_title),
                    message = stringResource(R.string.merchant_categories_empty_message),
                    modifier = Modifier.padding(padding),
                    icon = Icons.Filled.Category,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.mappings, key = { it.merchant }) { mapping ->
                        MerchantCategoryRow(
                            merchant = mapping.merchant,
                            category = mapping.category,
                            onClick = { openEditor(mapping) },
                            onDelete = { onDelete(mapping.merchant) },
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        val initial = editing
        MerchantCategoryEditorDialog(
            initial = initial,
            onDismiss = { showEditor = false },
            onSave = { merchant, category ->
                onSave(initial?.merchant, merchant, category)
                showEditor = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantCategoryEditorDialog(
    initial: MerchantCategoryMapping?,
    onDismiss: () -> Unit,
    onSave: (merchant: String, category: TransactionCategory) -> Unit,
) {
    var merchant by remember { mutableStateOf(initial?.merchant.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: TransactionCategory.FOOD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.merchant_categories_add_title
                    else R.string.merchant_categories_edit_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.merchant_categories_merchant_label)) },
                    placeholder = { Text(stringResource(R.string.merchant_categories_merchant_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.merchant_categories_category_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TransactionCategory.entries.forEach { entry ->
                            FilterChip(
                                selected = entry == category,
                                onClick = { category = entry },
                                label = { Text(stringResource(Formatters.categoryLabelRes(entry))) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(entry),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(merchant, category) },
                enabled = merchant.isNotBlank(),
            ) {
                Text(stringResource(R.string.merchant_categories_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.merchant_categories_cancel))
            }
        },
    )
}

@Composable
private fun MerchantCategorySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(4) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(64.dp))
            }
        }
    }
}

@Preview(name = "Light mappings", showBackground = true)
@Preview(name = "Dark mappings", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCategoryContentPreview() {
    SpendTrackerTheme {
        MerchantCategoryContent(
            state = MerchantCategoryUiState.Content(PreviewData.merchantMappings),
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}

@Preview(name = "Light empty", showBackground = true)
@Preview(name = "Dark empty", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCategoryEmptyPreview() {
    SpendTrackerTheme {
        MerchantCategoryContent(
            state = MerchantCategoryUiState.Content(emptyList()),
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}

@Preview(name = "Light loading", showBackground = true)
@Preview(name = "Dark loading", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantCategoryLoadingPreview() {
    SpendTrackerTheme {
        MerchantCategoryContent(
            state = MerchantCategoryUiState.Loading,
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}
