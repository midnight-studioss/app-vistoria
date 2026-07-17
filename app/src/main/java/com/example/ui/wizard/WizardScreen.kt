package com.example.ui.wizard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.Inspection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(
    viewModel: WizardViewModel,
    inspectionId: Long,
    companyName: String = "BR SOLAR",
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(inspectionId) {
        viewModel.loadInspection(inspectionId)
    }

    val inspection by viewModel.inspectionState.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()

    if (inspection == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val stepTitles = listOf(
        "Localização",
        "Dados do Cliente",
        "Dados Técnicos",
        "Informações do Telhado",
        "Fotos Obrigatórias",
        "Observações",
        "Assinaturas",
        "Resumo"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stepTitles.getOrElse(currentStep) { "Vistoria" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) {
                            viewModel.prevStep()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(onClick = { viewModel.prevStep() }) {
                            Text("Voltar")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    val isNextEnabled = when (currentStep) {
                        0 -> inspection!!.latitude != null && inspection!!.longitude != null && inspection!!.address.isNotBlank()
                        1 -> inspection!!.clientFirstName.isNotBlank() && inspection!!.clientLastName.isNotBlank()
                        2 -> inspection!!.connectionType.isNotBlank() && inspection!!.mainBreaker.isNotBlank() && inspection!!.voltage.isNotBlank()
                        3 -> inspection!!.roofType.isNotBlank() && inspection!!.roofInclination.isNotBlank() && inspection!!.inverterLocation.isNotBlank()
                        4 -> inspection!!.photoMeterUri != null && inspection!!.photoBreakerUri != null && inspection!!.photoPanelUri != null && inspection!!.photoRoofUri != null && inspection!!.photoGeneralUri != null
                        5 -> true // Observations can be empty
                        6 -> inspection!!.techName.isNotBlank() // Only inspector name is needed
                        7 -> true // Summary
                        else -> true
                    }

                    Button(
                        onClick = {
                            if (currentStep < stepTitles.size - 1) {
                                viewModel.nextStep()
                            } else {
                                viewModel.updateField { it.copy(isCompleted = true) }
                                onNavigateBack()
                            }
                        },
                        enabled = isNextEnabled
                    ) {
                        Text(if (currentStep == stepTitles.size - 1) "Finalizar" else "Próximo")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / stepTitles.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "step_transition"
            ) { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (targetStep) {
                        0 -> LocationStep(inspection!!, viewModel)
                        1 -> ClientDataStep(inspection!!, viewModel)
                        2 -> TechnicalDataStep(inspection!!, viewModel)
                        3 -> RoofStep(inspection!!, viewModel)
                        4 -> PhotosStep(inspection!!, viewModel)
                        5 -> ObservationsStep(inspection!!, viewModel)
                        6 -> SignaturesStep(inspection!!, viewModel)
                        7 -> SummaryStep(inspection!!, companyName)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationStep(inspection: Inspection, viewModel: WizardViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.fetchLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Text("Captura de Localização", style = MaterialTheme.typography.titleMedium)

    if (inspection.latitude != null && inspection.longitude != null) {
        Text("Latitude: ${inspection.latitude}")
        Text("Longitude: ${inspection.longitude}")
        Text("Precisão: ${inspection.gpsAccuracy ?: "N/A"} metros")
        
        OutlinedTextField(
            value = inspection.address,
            onValueChange = { addr -> viewModel.updateField { it.copy(address = addr) } },
            label = { Text("Endereço (Editar se necessário)") },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text("Aguardando localização...")
        CircularProgressIndicator()
    }
}

@Composable
fun ClientDataStep(inspection: Inspection, viewModel: WizardViewModel) {
    OutlinedTextField(
        value = inspection.clientIdString,
        onValueChange = { },
        label = { Text("ID do Cliente (Gerado Automaticamente)") },
        modifier = Modifier.fillMaxWidth(),
        enabled = false
    )
    OutlinedTextField(
        value = inspection.clientFirstName,
        onValueChange = { v -> viewModel.updateField { it.copy(clientFirstName = v) } },
        label = { Text("Nome do Cliente *") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = inspection.clientLastName,
        onValueChange = { v -> viewModel.updateField { it.copy(clientLastName = v) } },
        label = { Text("Sobrenome do Cliente *") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TechnicalDataStep(inspection: Inspection, viewModel: WizardViewModel) {
    Text("Ligação *", style = MaterialTheme.typography.titleSmall)
    val connections = listOf("Monofásico", "Bifásico", "Trifásico")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        connections.forEach { conn ->
            FilterChip(
                selected = inspection.connectionType == conn,
                onClick = { viewModel.updateField { it.copy(connectionType = conn) } },
                label = { Text(conn) }
            )
        }
    }

    OutlinedTextField(
        value = inspection.mainBreaker,
        onValueChange = { v -> viewModel.updateField { it.copy(mainBreaker = v) } },
        label = { Text("Disjuntor Geral (ex: C50) *") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text("Tensão *", style = MaterialTheme.typography.titleSmall)
    val voltages = listOf("110V", "220V", "380V")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        voltages.forEach { v ->
            FilterChip(
                selected = inspection.voltage == v,
                onClick = { viewModel.updateField { it.copy(voltage = v) } },
                label = { Text(v) }
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    Text("Checklist", style = MaterialTheme.typography.titleMedium)
    
    ChecklistItem("Aterramento", inspection.hasGrounding) { v -> viewModel.updateField { it.copy(hasGrounding = v) } }
    ChecklistItem("Andaime", inspection.needsScaffold) { v -> viewModel.updateField { it.copy(needsScaffold = v) } }
    ChecklistItem("Poda", inspection.needsPruning) { v -> viewModel.updateField { it.copy(needsPruning = v) } }
    ChecklistItem("Obra", inspection.needsConstruction) { v -> viewModel.updateField { it.copy(needsConstruction = v) } }
    ChecklistItem("Área de Sombra", inspection.hasShadowArea) { v -> viewModel.updateField { it.copy(hasShadowArea = v) } }
    ChecklistItem("Wi-fi", inspection.hasWifi) { v -> viewModel.updateField { it.copy(hasWifi = v) } }
    ChecklistItem("Telha Reserva", inspection.hasSpareTiles) { v -> viewModel.updateField { it.copy(hasSpareTiles = v) } }
    ChecklistItem("Apresenta Infiltração", inspection.hasInfiltration) { v -> viewModel.updateField { it.copy(hasInfiltration = v) } }
    ChecklistItem("Apresenta Sombra", inspection.hasShadow) { v -> viewModel.updateField { it.copy(hasShadow = v) } }
}

@Composable
fun ChecklistItem(label: String, value: Boolean?, onValueChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = value == true, onClick = { onValueChange(true) })
            Text("Sim")
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(selected = value == false, onClick = { onValueChange(false) })
            Text("Não")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoofStep(inspection: Inspection, viewModel: WizardViewModel) {
    Text("Tipo do Telhado *", style = MaterialTheme.typography.titleSmall)
    val roofTypes = listOf("Colonial/Cerâmica", "Metálico", "Fibrocimento", "Laje", "Outro")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        roofTypes.forEach { type ->
            FilterChip(
                selected = inspection.roofType == type,
                onClick = { viewModel.updateField { it.copy(roofType = type) } },
                label = { Text(type) }
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    Text("Orientação (Face) *", style = MaterialTheme.typography.titleSmall)
    val orientations = listOf("Norte", "Sul", "Leste", "Oeste", "Nordeste", "Noroeste", "Sudeste", "Sudoeste")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        orientations.forEach { orientation ->
            FilterChip(
                selected = inspection.roofInclination == orientation,
                onClick = { viewModel.updateField { it.copy(roofInclination = orientation) } },
                label = { Text(orientation) }
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = inspection.arrayArrangement,
        onValueChange = { v -> viewModel.updateField { it.copy(arrayArrangement = v) } },
        label = { Text("Arranjo (Opcional)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = inspection.inverterLocation,
        onValueChange = { v -> viewModel.updateField { it.copy(inverterLocation = v) } },
        label = { Text("Local Inversor / String Box *") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PhotosStep(inspection: Inspection, viewModel: WizardViewModel) {
    val context = LocalContext.current
    var currentPhotoType by remember { mutableStateOf<String?>(null) }
    val photoAnalysisMessage by viewModel.photoAnalysisMessage.collectAsState()
    val isAnalyzingPhoto by viewModel.isAnalyzingPhoto.collectAsState()

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null && currentPhotoType != null) {
            viewModel.analyzePhoto(bitmap) { isClear ->
                if (isClear) {
                    val dummyUri = "content://photo_${System.currentTimeMillis()}"
                    when (currentPhotoType) {
                        "Medidor" -> viewModel.updateField { it.copy(photoMeterUri = dummyUri) }
                        "Disjuntor" -> viewModel.updateField { it.copy(photoBreakerUri = dummyUri) }
                        "Quadro Elétrico" -> viewModel.updateField { it.copy(photoPanelUri = dummyUri) }
                        "Telhado" -> viewModel.updateField { it.copy(photoRoofUri = dummyUri) }
                        "Onde vai ficar o inversor?" -> viewModel.updateField { it.copy(photoGeneralUri = dummyUri) }
                    }
                }
                currentPhotoType = null
            }
        } else {
            currentPhotoType = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        }
    }

    if (photoAnalysisMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPhotoMessage() },
            title = { Text("Atenção") },
            text = { Text(photoAnalysisMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPhotoMessage() }) {
                    Text("OK")
                }
            }
        )
    }

    if (isAnalyzingPhoto) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    Text("Fotos Obrigatórias *", style = MaterialTheme.typography.titleMedium)
    Text("Toque no ícone para capturar a foto correspondente.", style = MaterialTheme.typography.bodyMedium)
    
    val photoTypes = listOf(
        "Medidor" to inspection.photoMeterUri,
        "Disjuntor" to inspection.photoBreakerUri,
        "Quadro Elétrico" to inspection.photoPanelUri,
        "Telhado" to inspection.photoRoofUri,
        "Onde vai ficar o inversor?" to inspection.photoGeneralUri
    )
    
    photoTypes.forEach { (type, uri) ->
        val isCaptured = uri != null
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isAnalyzingPhoto) {
                    currentPhotoType = type
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isCaptured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isCaptured) Icons.Default.Check else Icons.Default.CameraAlt, 
                    contentDescription = null, 
                    modifier = Modifier.size(40.dp), 
                    tint = if (isCaptured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(type, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SignaturesStep(inspection: Inspection, viewModel: WizardViewModel) {
    Text("Assinaturas", style = MaterialTheme.typography.titleMedium)
    
    OutlinedTextField(
        value = inspection.techName,
        onValueChange = { v -> viewModel.updateField { it.copy(techName = v) } },
        label = { Text("Nome do Vistoriador *") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ObservationsStep(inspection: Inspection, viewModel: WizardViewModel) {
    OutlinedTextField(
        value = inspection.observations,
        onValueChange = { v -> viewModel.updateField { it.copy(observations = v) } },
        label = { Text("Observações") },
        modifier = Modifier.fillMaxWidth().height(150.dp),
        maxLines = 5
    )
}

@Composable
fun SummaryStep(inspection: Inspection, companyName: String = "BR SOLAR") {
    val context = LocalContext.current
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val bytes = com.example.util.PdfGenerator.generatePdfBytes(context, inspection, companyName)
                        outputStream.write(bytes)
                    }
                    android.widget.Toast.makeText(context, "Relatório PDF salvo com sucesso!", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Erro ao salvar PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Resumo da Vistoria", style = MaterialTheme.typography.titleLarge)
        
        Button(
            onClick = {
                val namePart = "${inspection.clientFirstName}_${inspection.clientLastName}".trim()
                val clientNameClean = if (namePart.isNotBlank()) namePart else "Cliente_Sem_Nome"
                val fileName = "Vistoria_${clientNameClean.replace("\\s+".toRegex(), "_")}.pdf"
                createDocumentLauncher.launch(fileName)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salvar PDF")
        }
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cliente: ${inspection.clientFirstName} ${inspection.clientLastName}".trim(), style = MaterialTheme.typography.titleMedium)
            Text("ID: ${inspection.clientIdString}")
            Text("Endereço: ${inspection.address}")
            
            if (inspection.latitude != null && inspection.longitude != null) {
                Button(onClick = {
                    val uri = "geo:${inspection.latitude},${inspection.longitude}?q=${inspection.latitude},${inspection.longitude}(Local da Vistoria)"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    context.startActivity(intent)
                }) {
                    Text("Abrir no Google Maps")
                }
            }
        }
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Dados Técnicos", style = MaterialTheme.typography.titleMedium)
            Text("Ligação: ${inspection.connectionType}")
            Text("Disjuntor: ${inspection.mainBreaker}")
            Text("Tensão: ${inspection.voltage}")
        }
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Telhado", style = MaterialTheme.typography.titleMedium)
            Text("Tipo: ${inspection.roofType}")
            Text("Orientação: ${inspection.roofInclination}")
            Text("Arranjo: ${inspection.arrayArrangement}")
            Text("Local Inversor: ${inspection.inverterLocation}")
        }
    }
}
