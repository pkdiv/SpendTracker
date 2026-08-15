package dev.pkdiv.spendtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.ingestion.sms.SmsReader
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val smsReader: SmsReader,
) : ViewModel() {

    fun backfill() {
        viewModelScope.launch {
            runCatching { smsReader.backfill() }
        }
    }
}
