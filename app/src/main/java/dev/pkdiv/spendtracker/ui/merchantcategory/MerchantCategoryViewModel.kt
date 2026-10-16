package dev.pkdiv.spendtracker.ui.merchantcategory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.repository.MerchantCategoryMapping
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.util.MerchantKey
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MerchantCategoryUiState {
    data object Loading : MerchantCategoryUiState
    data class Content(val mappings: List<MerchantCategoryMapping>) : MerchantCategoryUiState
}

@HiltViewModel
class MerchantCategoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<MerchantCategoryUiState> = transactionRepository
        .observeMerchantCategories()
        .map { MerchantCategoryUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MerchantCategoryUiState.Loading)

    /**
     * Saves a mapping. When [original] is non-null the key was renamed, so the old
     * key is removed first (the merchant is the primary key).
     */
    fun save(original: String?, merchant: String, category: TransactionCategory) {
        viewModelScope.launch {
            if (original != null && MerchantKey.normalize(original) != MerchantKey.normalize(merchant)) {
                transactionRepository.deleteMerchantCategory(original)
            }
            transactionRepository.setMerchantCategory(merchant, category)
        }
    }

    fun delete(merchant: String) {
        viewModelScope.launch { transactionRepository.deleteMerchantCategory(merchant) }
    }
}
