package dev.pkdiv.spendtracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.ingestion.sms.SmsReader
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackfillState {
    data object Idle : BackfillState
    data object Running : BackfillState
    data class Done(val processed: Int) : BackfillState
    data class Error(val message: String) : BackfillState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val smsReader: SmsReader,
) : ViewModel() {

    private val _backfillState = MutableStateFlow<BackfillState>(BackfillState.Idle)
    val backfillState: StateFlow<BackfillState> = _backfillState.asStateFlow()

    private var completedSuccessfully = false

    fun backfill() {
        if (completedSuccessfully) return
        rescan()
    }

    fun rescan() {
        if (_backfillState.value is BackfillState.Running) return
        viewModelScope.launch {
            _backfillState.value = BackfillState.Running
            runCatching { smsReader.backfill() }
                .onSuccess { count ->
                    completedSuccessfully = true
                    _backfillState.value = BackfillState.Done(count)
                }
                .onFailure { e ->
                    Log.e("MainViewModel", "Backfill failed", e)
                    _backfillState.value = BackfillState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
