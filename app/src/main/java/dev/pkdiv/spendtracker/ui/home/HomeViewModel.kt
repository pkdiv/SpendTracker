package dev.pkdiv.spendtracker.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.ingestion.sms.SmsReader
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RescanState {
    data object Idle : RescanState
    data object Running : RescanState
    data class Done(val processed: Int) : RescanState
    data object NoPermission : RescanState
    data class Error(val message: String) : RescanState
}

data class HomeUiState(
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val transactions: List<TransactionEntity> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    transactionRepository: TransactionRepository,
    private val smsReader: SmsReader,
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthStart = today.withDayOfMonth(1)

    val uiState: StateFlow<HomeUiState> = transactionRepository
        .observeBetween(
            monthStart.atStartOfDay(ZoneOffset.systemDefault()).toInstant(),
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

    private val _rescanState = MutableStateFlow<RescanState>(RescanState.Idle)
    val rescanState: StateFlow<RescanState> = _rescanState.asStateFlow()

    fun rescan() {
        if (_rescanState.value is RescanState.Running) return

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _rescanState.value = RescanState.NoPermission
            return
        }

        viewModelScope.launch {
            _rescanState.value = RescanState.Running
            runCatching { smsReader.backfill() }
                .onSuccess { count ->
                    _rescanState.value = RescanState.Done(count)
                }
                .onFailure { e ->
                    _rescanState.value = RescanState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
