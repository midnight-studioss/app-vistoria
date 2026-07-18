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

    fun clearPhotoMessage() {
        _photoAnalysisMessage.value = null
    }

    fun analyzePhoto(bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAnalyzingPhoto.value = true
            try {
                val (base64Image, _) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    encoded to true
                }
                
                val prompt = "Analise esta imagem com extremo rigor. Verifique se ela está nítida ou borrada. Além disso, verifique se cada letra ou texto presente na imagem está legível. Se a foto estiver borrada, desfocada, ou se qualquer texto estiver ilegível, responda exatamente com a palavra: YES. Se a foto estiver perfeitamente nítida e qualquer texto estiver legível, responda: NO."
                
                val request = GenerateContentRequest(
                    contents = listOf(Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    ))
                )
                
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                if (text.trim().uppercase().contains("YES")) {
                    _photoAnalysisMessage.value = "Não conseguimos reconhecer sua foto. A imagem pode estar borrada ou com textos ilegíveis. Por favor, tire a foto novamente."
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

    fun loadInspection(inspectionId: Long) {
        viewModelScope.launch {
            repository.getInspection(inspectionId).collect { inspection ->
                if (inspection != null) {
                    _inspectionState.value = inspection
                    _currentStep.value = inspection.currentStep
                }
            }
        }
    }

    private fun autoSave() {
        val current = _inspectionState.value ?: return
        viewModelScope.launch {
            repository.updateInspection(current.copy(currentStep = _currentStep.value))
        }
    }

    fun updateField(updater: (Inspection) -> Inspection) {
        _inspectionState.update { current ->
            if (current == null) return@update null
            val updated = updater(current)
            updated
        }
        autoSave()
    }

    fun nextStep() {
        if (_currentStep.value < 7) {
            _currentStep.value++
            autoSave()
        }
    }

    fun prevStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
            autoSave()
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
