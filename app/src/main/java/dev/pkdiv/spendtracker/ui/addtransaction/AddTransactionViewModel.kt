package dev.pkdiv.spendtracker.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.util.AmountInput
import dev.pkdiv.spendtracker.widget.WidgetRefresher
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AddTransactionUiState {
    data class Editing(
        val form: ManualTransactionForm,
        val errors: ManualTransactionErrors = ManualTransactionErrors(),
        val saving: Boolean = false,
    ) : AddTransactionUiState

    /** The transaction was stored; the screen pops back. */
    data object Saved : AddTransactionUiState
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddTransactionUiState>(
        AddTransactionUiState.Editing(ManualTransactionForm()),
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    fun setAmount(input: String) = edit { it.copy(amount = AmountInput.sanitize(input)) }

    fun setDirection(direction: TransactionDirection) = edit { it.copy(direction = direction) }

    fun setMerchant(merchant: String) = edit { it.copy(merchant = merchant) }

    fun setCategory(category: TransactionCategory) = edit { it.copy(category = category) }

    fun setAccount(account: String) = edit { it.copy(account = account) }

    fun setDate(date: LocalDate) = edit { it.copy(date = date) }

    /**
     * Validates and stores the entry. The category is stored exactly as chosen —
     * unlike ingested messages it is not re-derived from the merchant, so the user's
     * explicit pick always wins.
     */
    fun save() {
        val current = _uiState.value as? AddTransactionUiState.Editing ?: return
        if (current.saving) return

        val errors = current.form.errors()
        if (errors.amount || errors.merchant) {
            _uiState.value = current.copy(errors = errors)
            return
        }

        _uiState.value = current.copy(errors = errors, saving = true)
        viewModelScope.launch {
            transactionRepository.insertManual(
                amount = current.form.amountValue ?: return@launch,
                direction = current.form.direction,
                merchant = current.form.merchantValue,
                category = current.form.category,
                account = current.form.account.trim().ifEmpty { null },
                timestamp = current.form.date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            )
            widgetRefresher.refresh()
            _uiState.value = AddTransactionUiState.Saved
        }
    }

    private fun edit(transform: (ManualTransactionForm) -> ManualTransactionForm) {
        val current = _uiState.value as? AddTransactionUiState.Editing ?: return
        _uiState.value = current.copy(form = transform(current.form))
    }
}
