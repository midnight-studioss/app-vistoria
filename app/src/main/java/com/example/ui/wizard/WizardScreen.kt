package com.example.ui.wizard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.Inspection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

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
                            viewModel.forceSave()
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
                        1 -> inspection!!.clientFirstName.isNotBlank()
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
                                viewModel.updateField(force = true) { it.copy(isCompleted = true) }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Text("Aguardando localização GPS...", style = MaterialTheme.typography.bodyMedium)
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.updateField { 
                        it.copy(
                            latitude = -23.55052,
                            longitude = -46.633308,
                            gpsAccuracy = 10f,
                            address = "Av. Paulista, 1000 - São Paulo"
                        )
                    }
                }
            ) {
                Text("Preencher Manualmente")
            }
        }
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
        label = { Text("Sobrenome do Cliente") },
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
    var isOutros by remember { mutableStateOf(
        inspection.roofType.isNotBlank() && inspection.roofType !in listOf("Colonial/Cerâmica", "Metálico", "Fibrocimento", "Laje")
    ) }

    val roofTypes = listOf("Colonial/Cerâmica", "Metálico", "Fibrocimento", "Laje", "Outros")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        roofTypes.forEach { type ->
            FilterChip(
                selected = if (type == "Outros") isOutros else inspection.roofType == type,
                onClick = { 
                    if (type == "Outros") {
                        isOutros = true
                        if (inspection.roofType in listOf("Colonial/Cerâmica", "Metálico", "Fibrocimento", "Laje")) {
                            viewModel.updateField { it.copy(roofType = "") }
                        }
                    } else {
                        isOutros = false
                        viewModel.updateField { it.copy(roofType = type) }
                    }
                },
                label = { Text(type) }
            )
        }
    }
    
    if (isOutros) {
        OutlinedTextField(
            value = if (inspection.roofType in listOf("Colonial/Cerâmica", "Metálico", "Fibrocimento", "Laje")) "" else inspection.roofType,
            onValueChange = { v -> viewModel.updateField { it.copy(roofType = v) } },
            label = { Text("Especifique o tipo do telhado *") },
            modifier = Modifier.fillMaxWidth()
        )
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
        label = { Text("Arranjo") },
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
    val currentPhotoType by viewModel.currentPhotoType.collectAsState()
    val photoAnalysisMessage by viewModel.photoAnalysisMessage.collectAsState()
    val isAnalyzingPhoto by viewModel.isAnalyzingPhoto.collectAsState()
    
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && tempPhotoUri != null && currentPhotoType != null) {
            try {
                var bitmap: Bitmap? = null
                context.contentResolver.openInputStream(tempPhotoUri!!)?.use {
                    bitmap = BitmapFactory.decodeStream(it)
                }
                
                if (bitmap != null) {
                    val maxDim = 1024
                    val scale = Math.min(maxDim.toFloat() / bitmap!!.width, maxDim.toFloat() / bitmap!!.height)
                    val resizedBitmap = if(scale < 1) Bitmap.createScaledBitmap(bitmap!!, (bitmap!!.width * scale).toInt(), (bitmap!!.height * scale).toInt(), true) else bitmap!!
                    
                    viewModel.analyzePhoto(resizedBitmap) { isClear ->
                        if (isClear) {
                            val prefix = currentPhotoType?.replace("?", "")?.replace(" ", "_") ?: "photo"
                            val fileUri = saveBitmapToCache(context, resizedBitmap, prefix) ?: tempPhotoUri.toString()
                            when (currentPhotoType) {
                                "Medidor" -> viewModel.updateField(force = true) { it.copy(photoMeterUri = fileUri) }
                                "Disjuntor" -> viewModel.updateField(force = true) { it.copy(photoBreakerUri = fileUri) }
                                "Quadro Elétrico" -> viewModel.updateField(force = true) { it.copy(photoPanelUri = fileUri) }
                                "Telhado" -> viewModel.updateField(force = true) { it.copy(photoRoofUri = fileUri) }
                                "Onde vai ficar o inversor?" -> viewModel.updateField(force = true) { it.copy(photoGeneralUri = fileUri) }
                            }
                        }
                        viewModel.setCurrentPhotoType(null)
                        tempPhotoUri = null
                    }
                } else {
                    viewModel.setCurrentPhotoType(null)
                    tempPhotoUri = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.setCurrentPhotoType(null)
                tempPhotoUri = null
            }
        } else {
            viewModel.setCurrentPhotoType(null)
            tempPhotoUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val dir = java.io.File(context.cacheDir, "images")
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, "temp_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempPhotoUri = uri
            takePictureLauncher.launch(uri)
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
    Text("Toque no item para capturar a foto correspondente.", style = MaterialTheme.typography.bodyMedium)
    
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
                    viewModel.setCurrentPhotoType(type)
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isCaptured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
            ),
            border = if (isCaptured) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uri != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Foto $type",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Capturada",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Tirar Foto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = type,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCaptured) "Toque para substituir a foto" else "Toque para capturar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    
    SignaturePad(
        label = "Assinatura do Vistoriador",
        currentSignatureUri = inspection.techSignatureUri,
        onSaveSignature = { uri -> viewModel.updateField { it.copy(techSignatureUri = uri) } },
        onClearSignature = { viewModel.updateField { it.copy(techSignatureUri = null) } }
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = inspection.clientRepName,
        onValueChange = { v -> viewModel.updateField { it.copy(clientRepName = v) } },
        label = { Text("Nome do Responsável / Cliente") },
        modifier = Modifier.fillMaxWidth()
    )
    
    SignaturePad(
        label = "Assinatura do Responsável (Cliente)",
        currentSignatureUri = inspection.clientSignatureUri,
        onSaveSignature = { uri -> viewModel.updateField { it.copy(clientSignatureUri = uri) } },
        onClearSignature = { viewModel.updateField { it.copy(clientSignatureUri = null) } }
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
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Resumo da Vistoria", style = MaterialTheme.typography.titleLarge)
        
        Button(
            onClick = {
                com.example.util.PdfGenerator.savePdfDirectly(context, inspection, companyName)
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

class LinePath(val points: MutableList<android.graphics.PointF> = mutableStateListOf())

@Composable
fun SignaturePad(
    label: String,
    currentSignatureUri: String?,
    onSaveSignature: (String) -> Unit,
    onClearSignature: () -> Unit
) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<LinePath>() }
    var hasSigned by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        
        if (currentSignatureUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Assinatura Salva", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { onClearSignature() },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Limpar")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                hasSigned = true
                                val newPath = LinePath(mutableStateListOf(android.graphics.PointF(offset.x, offset.y)))
                                paths.add(newPath)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                paths.lastOrNull()?.points?.add(
                                    android.graphics.PointF(change.position.x, change.position.y)
                                )
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { path ->
                        val composePath = Path()
                        path.points.forEachIndexed { idx, pt ->
                            if (idx == 0) composePath.moveTo(pt.x, pt.y)
                            else composePath.lineTo(pt.x, pt.y)
                        }
                        drawPath(
                            composePath,
                            color = Color.Black,
                            style = Stroke(
                                width = 5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
                
                if (!hasSigned) {
                    Text(
                        "Assine aqui com o dedo",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.LightGray
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        paths.clear()
                        hasSigned = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpar")
                }
                
                Button(
                    onClick = {
                        val savedUri = saveSignatureToCache(context, paths, 400, 120)
                        if (savedUri != null) {
                            onSaveSignature(savedUri)
                        }
                    },
                    enabled = hasSigned,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar")
                }
            }
        }
    }
}

fun saveSignatureToCache(context: Context, paths: List<LinePath>, width: Int, height: Int): String? {
    if (paths.isEmpty()) return null
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 6f
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        
        paths.forEach { path ->
            val androidPath = android.graphics.Path()
            path.points.forEachIndexed { idx, pt ->
                if (idx == 0) androidPath.moveTo(pt.x, pt.y)
                else androidPath.lineTo(pt.x, pt.y)
            }
            canvas.drawPath(androidPath, paint)
        }
        
        val directory = java.io.File(context.filesDir, "signatures")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = java.io.File(directory, "sig_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String): String? {
    return try {
        val directory = java.io.File(context.filesDir, "photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = java.io.File(directory, "${prefix}_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
