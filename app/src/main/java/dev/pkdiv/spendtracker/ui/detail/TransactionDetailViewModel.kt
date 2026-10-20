package dev.pkdiv.spendtracker.ui.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.repository.MessageUnavailableReason
import dev.pkdiv.spendtracker.repository.TransactionDetail
import dev.pkdiv.spendtracker.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: TransactionDetail) : DetailUiState
    data class Unavailable(val detail: TransactionDetail, val reason: MessageUnavailableReason) : DetailUiState
    data object NotFound : DetailUiState
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val transactionId: Long = checkNotNull(savedStateHandle["id"])

    val uiState: StateFlow<DetailUiState> = transactionRepository
        .observeTransactionDetail(transactionId)
        .map { detail ->
            when {
                detail == null -> DetailUiState.NotFound
                detail.body != null -> DetailUiState.Content(detail)
                else -> DetailUiState.Unavailable(detail, unavailableReason(detail.source))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState.Loading)

    fun setDuplicate(isDuplicate: Boolean) {
        viewModelScope.launch { transactionRepository.setDuplicate(transactionId, isDuplicate) }
    }

    /** Renames the merchant, also teaching the merchant->category mapping. */
    fun renameMerchant(merchant: String) {
        viewModelScope.launch { transactionRepository.renameMerchant(transactionId, merchant) }
    }

    /** Sets or clears the free-text note (null/blank clears it). */
    fun setNote(note: String?) {
        viewModelScope.launch { transactionRepository.setNote(transactionId, note) }
    }

    private fun unavailableReason(source: TransactionSource): MessageUnavailableReason {
        if (source == TransactionSource.SMS) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return MessageUnavailableReason.PERMISSION_REVOKED
        }
        return MessageUnavailableReason.DELETED
    }
}
