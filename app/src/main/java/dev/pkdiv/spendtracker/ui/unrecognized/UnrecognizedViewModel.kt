package dev.pkdiv.spendtracker.ui.unrecognized

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UnrecognizedUiState(
    val messages: List<UnrecognizedMessageEntity> = emptyList(),
)

@HiltViewModel
class UnrecognizedViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<UnrecognizedUiState> = transactionRepository
        .observeUnrecognized()
        .map { UnrecognizedUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnrecognizedUiState())

    fun dismiss(id: Long) {
        viewModelScope.launch { transactionRepository.deleteUnrecognized(id) }
    }
}
