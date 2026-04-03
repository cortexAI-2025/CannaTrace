package com.cannatrace.presentation.culture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.data.local.database.dao.CultureEntryDao
import com.cannatrace.data.local.database.entities.CultureEntryEntity
import com.cannatrace.domain.model.CultureEntry
import com.cannatrace.domain.model.CultureEntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CultureState(
    val entries: List<CultureEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CultureViewModel @Inject constructor(
    private val cultureEntryDao: CultureEntryDao
) : ViewModel() {

    private val _state = MutableStateFlow(CultureState())
    val state: StateFlow<CultureState> = _state.asStateFlow()

    fun loadForBatch(batchId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            cultureEntryDao.getCultureEntriesByBatch(batchId).collect { entities ->
                _state.update {
                    it.copy(
                        entries = entities.map { e -> e.toDomain() },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addEntry(
        batchId: String,
        type: CultureEntryType,
        value: Double,
        unit: String,
        notes: String,
        operatorId: String = "user-001"
    ) {
        viewModelScope.launch {
            val entity = CultureEntryEntity(
                id = UUID.randomUUID().toString(),
                batchId = batchId,
                type = type.name,
                value = value,
                unit = unit,
                recordedAt = System.currentTimeMillis(),
                operatorId = operatorId,
                notes = notes
            )
            cultureEntryDao.insertCultureEntry(entity)
        }
    }

    private fun CultureEntryEntity.toDomain() = CultureEntry(
        id = id,
        batchId = batchId,
        type = CultureEntryType.valueOf(type),
        value = value,
        unit = unit,
        recordedAt = recordedAt,
        operatorId = operatorId,
        notes = notes
    )
}
