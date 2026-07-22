package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Inspection
import com.example.data.SolarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    private val repository: SolarRepository
) : ViewModel() {

    val syncStatus: StateFlow<String> = repository.syncStatus

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    val inspections: StateFlow<List<Inspection>> = repository.getAllInspections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun createNewInspection(onCreated: (Long) -> Unit) {
        val randomId = (100000..999999).random().toString()
        val id = repository.createInitialInspection(randomId)
        onCreated(id)
    }

    fun deleteInspection(id: Long) {
        viewModelScope.launch {
            repository.deleteInspection(id)
        }
    }

    fun updateInspection(inspection: Inspection) {
        viewModelScope.launch {
            repository.updateInspection(inspection)
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncFromCloud()
                _syncMessage.value = "Sincronização concluída com sucesso!"
            } catch (e: Exception) {
                _syncMessage.value = "Erro na sincronização: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
