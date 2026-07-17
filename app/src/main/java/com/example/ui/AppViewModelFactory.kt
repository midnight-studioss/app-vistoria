package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.AppDatabase
import com.example.data.SolarRepository
import com.example.ui.wizard.WizardViewModel
import com.example.ui.dashboard.DashboardViewModel

class AppViewModelFactory(
    private val repository: SolarRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(WizardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WizardViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
