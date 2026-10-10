package dev.pkdiv.spendtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: TransactionCategory? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<TransactionCategory?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        transactionRepository.observeAll(),
        searchQuery,
        selectedCategory,
    ) { all, query, category ->
        val filtered = all.filter { transaction ->
            val matchesCategory = category == null || transaction.category == category
            val matchesQuery = query.isBlank() ||
                transaction.merchant.contains(query, ignoreCase = true) ||
                transaction.amount.toPlainString().contains(query)
            matchesCategory && matchesQuery
        }
        HistoryUiState(
            transactions = filtered,
            searchQuery = query,
            selectedCategory = category,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setCategory(category: TransactionCategory?) {
        selectedCategory.value = category
    }

    fun updateCategory(id: Long, category: TransactionCategory) {
        viewModelScope.launch { transactionRepository.updateCategory(id, category) }
    }
}
