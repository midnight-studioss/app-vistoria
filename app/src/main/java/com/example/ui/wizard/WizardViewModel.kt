package com.example.ui.wizard

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Inspection
import com.example.data.SolarRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.example.BuildConfig
import com.example.api.*

class WizardViewModel(
    private val repository: SolarRepository
) : ViewModel() {

    private val _inspectionState = MutableStateFlow<Inspection?>(null)
    val inspectionState: StateFlow<Inspection?> = _inspectionState.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()
    
    private val _photoAnalysisMessage = MutableStateFlow<String?>(null)
    val photoAnalysisMessage: StateFlow<String?> = _photoAnalysisMessage.asStateFlow()

    private val _isAnalyzingPhoto = MutableStateFlow(false)
    val isAnalyzingPhoto: StateFlow<Boolean> = _isAnalyzingPhoto.asStateFlow()

    private val _currentPhotoType = MutableStateFlow<String?>(null)
    val currentPhotoType: StateFlow<String?> = _currentPhotoType.asStateFlow()
    
    private val _tempPhotoUriStr = MutableStateFlow<String?>(null)
    val tempPhotoUriStr: StateFlow<String?> = _tempPhotoUriStr.asStateFlow()

    fun setTempPhotoUriStr(uri: String?) {
        _tempPhotoUriStr.value = uri
    }

    fun setCurrentPhotoType(type: String?) {
        _currentPhotoType.value = type
    }

    fun clearPhotoMessage() {
        _photoAnalysisMessage.value = null
    }

    fun analyzePhoto(bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAnalyzingPhoto.value = true
            try {
                val (isClear, message) = com.example.util.AIVisionValidator.verifyImageLegibility(bitmap)
                
                if (!isClear) {
                    _photoAnalysisMessage.value = "Problema na foto: $message\nPor favor, tire a foto novamente."
                    onResult(false)
                } else {
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // If API fails, allow passing to not block the user
                onResult(true)
            } finally {
                _isAnalyzingPhoto.value = false
            }
        }
    }

    private var isLoaded = false
    private var saveJob: kotlinx.coroutines.Job? = null

    fun loadInspection(inspectionId: Long) {
        if (isLoaded || _inspectionState.value != null) return
        viewModelScope.launch {
            repository.getInspection(inspectionId).collect { inspection ->
                if (inspection != null && !isLoaded) {
                    _inspectionState.value = inspection
                    _currentStep.value = inspection.currentStep
                    isLoaded = true
                    android.util.Log.d("WizardViewModel", "Loaded inspection initially: ${inspection.id}")
                }
            }
        }
    }

    fun forceSave() {
        saveJob?.cancel()
        val current = _inspectionState.value ?: return
        viewModelScope.launch {
            android.util.Log.d("WizardViewModel", "Force saving inspection: ${current.id}")
            repository.updateInspection(current.copy(currentStep = _currentStep.value))
        }
    }

    private fun autoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Debounce typing by 1 second
            val current = _inspectionState.value ?: return@launch
            android.util.Log.d("WizardViewModel", "Debounced auto-saving inspection: ${current.id}")
            repository.updateInspection(current.copy(currentStep = _currentStep.value))
        }
    }

    fun updateField(force: Boolean = false, updater: (Inspection) -> Inspection) {
        _inspectionState.update { current ->
            if (current == null) return@update null
            val updated = updater(current)
            updated
        }
        if (force) {
            forceSave()
        } else {
            autoSave()
        }
    }

    fun nextStep() {
        if (_currentStep.value < 7) {
            _currentStep.value++
            forceSave()
        }
    }

    fun prevStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
            forceSave()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(context: Context) {
        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location: Location? = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    var addressText = ""
                    try {
                        val geocoder = Geocoder(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                if (addresses.isNotEmpty()) {
                                    val addr = addresses[0]
                                    val street = addr.thoroughfare ?: ""
                                    val number = addr.subThoroughfare ?: ""
                                    val city = addr.locality ?: addr.subAdminArea ?: ""
                                    addressText = listOf(street, number, city).filter { it.isNotBlank() }.joinToString(", ")
                                    if (addressText.isBlank()) addressText = addr.getAddressLine(0) ?: ""
                                    
                                    updateField { 
                                        it.copy(
                                            latitude = location.latitude,
                                            longitude = location.longitude,
                                            gpsAccuracy = location.accuracy,
                                            address = addressText
                                        )
                                    }
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                val street = addr.thoroughfare ?: ""
                                val number = addr.subThoroughfare ?: ""
                                val city = addr.locality ?: addr.subAdminArea ?: ""
                                addressText = listOf(street, number, city).filter { it.isNotBlank() }.joinToString(", ")
                                if (addressText.isBlank()) addressText = addr.getAddressLine(0) ?: ""
                            }
                            updateField { 
                                it.copy(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    gpsAccuracy = location.accuracy,
                                    address = addressText
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Update location even if geocoding fails
                        updateField { 
                            it.copy(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                gpsAccuracy = location.accuracy
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
