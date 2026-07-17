package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Inspection
import com.example.data.SolarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SolarRepository
) : ViewModel() {

    // Simple flow for testing; normally we'd list projects or inspections
    // Let's list inspections for now. Actually, we don't have a direct query in Daos for all inspections.
    // I will add a query for all inspections to Dao.

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
}
