package com.cannatrace.presentation.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.data.repository.LocalTraceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TraceViewModel @Inject constructor(val store: LocalTraceStore) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun inform(message: String) { _message.value = message }
    fun dismiss() { _message.value = null }
    fun submit(success: () -> Unit = {}, work: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { work(); success() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _message.value = e.message ?: "Opération impossible" }
            finally { _busy.value = false }
        }
    }
}
