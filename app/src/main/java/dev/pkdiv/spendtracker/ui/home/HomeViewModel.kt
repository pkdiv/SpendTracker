package dev.pkdiv.spendtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val transactions: List<TransactionEntity> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = transactionRepository
        .observeBetween(
            today.atStartOfDay(ZoneOffset.systemDefault()).toInstant(),
            today.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant(),
        )
        .map { txns ->
            HomeUiState(
                totalSpend = txns
                    .filter { it.direction == TransactionDirection.DEBIT }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount },
                transactions = txns,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
