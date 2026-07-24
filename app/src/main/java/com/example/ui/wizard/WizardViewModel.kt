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

    private val _isFinalizing = MutableStateFlow(false)
    val isFinalizing: StateFlow<Boolean> = _isFinalizing.asStateFlow()

    private val _finalizeProgress = MutableStateFlow(0f)
    val finalizeProgress: StateFlow<Float> = _finalizeProgress.asStateFlow()

    private val _finalizeStatus = MutableStateFlow("")
    val finalizeStatus: StateFlow<String> = _finalizeStatus.asStateFlow()

    private val _finalizeError = MutableStateFlow<String?>(null)
    val finalizeError: StateFlow<String?> = _finalizeError.asStateFlow()

    private val _generatedPdfBytes = MutableStateFlow<ByteArray?>(null)
    val generatedPdfBytes: StateFlow<ByteArray?> = _generatedPdfBytes.asStateFlow()

    private val _offlineSuccess = MutableStateFlow(false)
    val offlineSuccess: StateFlow<Boolean> = _offlineSuccess.asStateFlow()

    fun resetFinalizeState() {
        _isFinalizing.value = false
        _finalizeError.value = null
        _generatedPdfBytes.value = null
        _finalizeProgress.value = 0f
        _offlineSuccess.value = false
    }

    fun startFinalization(context: android.content.Context, companyName: String) {
        val inspection = _inspectionState.value ?: return
        
        viewModelScope.launch {
            _isFinalizing.value = true
            _finalizeError.value = null
            _generatedPdfBytes.value = null
            _offlineSuccess.value = false
            
            try {
                // Etapa 1: Validar Formulário
                _finalizeStatus.value = "Validando formulário..."
                _finalizeProgress.value = 0.1f
                kotlinx.coroutines.delay(500)
                validateForm(inspection)
                
                // Etapa 2: Validar Fotos
                _finalizeStatus.value = "Validando fotos..."
                _finalizeProgress.value = 0.3f
                kotlinx.coroutines.delay(500)
                validatePhotos(context, inspection)
                
                // Etapa 3: Salvar localmente
                _finalizeStatus.value = "Salvando vistoria localmente..."
                _finalizeProgress.value = 0.4f
                val pendingInspection = inspection.copy(syncState = com.example.data.SyncState.PENDING_SYNC, isCompleted = true)
                repository.updateInspection(pendingInspection)
                
                // Etapa 4: Verificar conexão e enviar
                if (isOnline(context)) {
                    _finalizeStatus.value = "Enviando informações para o Firebase..."
                    _finalizeProgress.value = 0.5f
                    try {
                        kotlinx.coroutines.withTimeout(30000) {
                            repository.syncInspectionToCloud(pendingInspection)
                        }
                        
                        _finalizeStatus.value = "Aguardando confirmação do Firebase..."
                        _finalizeProgress.value = 0.7f
                        val syncedInspection = pendingInspection.copy(syncState = com.example.data.SyncState.SYNCED)
                        repository.updateInspection(syncedInspection)
                        kotlinx.coroutines.delay(500) // Extra tempo para garantir leitura se necessário
                        
                        // Etapa 5 e 6: Gerar e Salvar PDF
                        _finalizeStatus.value = "Gerando e salvando PDF..."
                        _finalizeProgress.value = 0.85f
                        val pdfBytes = com.example.util.PdfGenerator.createAndSavePdf(context, syncedInspection, companyName)
                        
                        // Concluído (Etapa 7 fica na View para liberar o botão de Compartilhar)
                        _finalizeStatus.value = "Concluído!"
                        _finalizeProgress.value = 1.0f
                        _generatedPdfBytes.value = pdfBytes
                    } catch (e: Exception) {
                        // Salvo localmente, mas falhou na nuvem
                        val failedInspection = pendingInspection.copy(syncState = com.example.data.SyncState.SYNC_FAILED)
                        repository.updateInspection(failedInspection)
                        _offlineSuccess.value = true
                        com.example.worker.SyncManager.scheduleSync(context)
                    }
                } else {
                    _offlineSuccess.value = true
                    com.example.worker.SyncManager.scheduleSync(context)
                }
                
            } catch (e: Exception) {
                _finalizeError.value = e.message ?: "Erro desconhecido durante a finalização."
            }
        }
    }

    private fun isOnline(context: android.content.Context): Boolean {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun validateForm(inspection: Inspection) {
        if (inspection.clientFirstName.isBlank() || inspection.clientLastName.isBlank()) {
            throw Exception("O nome e sobrenome do cliente são obrigatórios.")
        }
        if (inspection.connectionType.isBlank() || inspection.mainBreaker.isBlank() || inspection.voltage.isBlank()) {
            throw Exception("Preencha todos os dados técnicos obrigatórios (Ligação, Disjuntor, Tensão).")
        }
        if (inspection.roofType.isBlank() || inspection.roofInclination.isBlank() || inspection.inverterLocation.isBlank()) {
            throw Exception("Preencha os dados do telhado obrigatórios.")
        }
    }

    private fun validatePhotos(context: android.content.Context, inspection: Inspection) {
        val requiredPhotos = mapOf(
            "Medidor" to inspection.photoMeterUri,
            "Disjuntor" to inspection.photoBreakerUri,
            "Quadro Elétrico" to inspection.photoPanelUri,
            "Telhado" to inspection.photoRoofUri,
            "Onde vai ficar o inversor?" to inspection.photoGeneralUri
        )
        
        for ((name, uriStr) in requiredPhotos) {
            if (uriStr.isNullOrBlank()) {
                throw Exception("A foto '$name' é obrigatória e não foi tirada.")
            }
            
            try {
                val uri = android.net.Uri.parse(uriStr)
                var valid = false
                
                if (uri.scheme == "file" || uriStr.startsWith("/")) {
                    val file = java.io.File(uri.path ?: uriStr)
                    if (file.exists() && file.length() > 0) {
                        valid = true
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        if (stream.available() > 0) {
                            valid = true
                        }
                    }
                }
                
                if (!valid) {
                    throw Exception("A foto '$name' parece estar corrompida ou vazia. Tire a foto novamente.")
                }
            } catch (e: Exception) {
                throw Exception("Erro ao ler a foto '$name': ${e.message}. Tire a foto novamente.")
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
