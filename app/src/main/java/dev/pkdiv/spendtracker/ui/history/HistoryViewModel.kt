package dev.pkdiv.spendtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filters: HistoryFilters = HistoryFilters(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(HistoryFilters())
    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<HistoryUiState> = combine(
        transactionRepository.observeAll(),
        filters,
    ) { all, current ->
        HistoryUiState(
            transactions = filterTransactions(all, current, zone),
            filters = current,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setSearchQuery(query: String) = update { it.copy(query = query) }

    fun setCategory(category: TransactionCategory?) = update { it.copy(category = category) }

    fun setDirection(direction: TransactionDirection?) = update { it.copy(direction = direction) }

    fun setDateRange(from: LocalDate?, to: LocalDate?) = update { it.copy(fromDate = from, toDate = to) }

    fun setMinAmount(input: String) = update { it.copy(minAmountInput = sanitizeAmount(input)) }

    fun setMaxAmount(input: String) = update { it.copy(maxAmountInput = sanitizeAmount(input)) }

    fun setExcludeDuplicates(exclude: Boolean) = update { it.copy(excludeDuplicates = exclude) }

    fun clearFilters() = update { HistoryFilters() }

    fun updateCategory(id: Long, category: TransactionCategory) {
        viewModelScope.launch { transactionRepository.updateCategory(id, category) }
    }

    fun setDuplicate(id: Long, isDuplicate: Boolean) {
        viewModelScope.launch { transactionRepository.setDuplicate(id, isDuplicate) }
    }

    private fun update(transform: (HistoryFilters) -> HistoryFilters) {
        filters.value = transform(filters.value)
    }

    /** Keeps only digits and a single decimal point so the amount parses reliably. */
    private fun sanitizeAmount(input: String): String {
        var seenDot = false
        return buildString {
            input.forEach { char ->
                when {
                    char.isDigit() -> append(char)
                    char == '.' && !seenDot -> {
                        append(char)
                        seenDot = true
                    }
                }
            }
        }
    }
}
