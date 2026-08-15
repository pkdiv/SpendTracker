package dev.pkdiv.spendtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = transactionRepository
        .observeAll()
        .map { HistoryUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
