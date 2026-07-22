package com.example.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Link
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.Inspection
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    userName: String,
    companyName: String,
    userPreferences: com.example.data.UserPreferences,
    onSaveCompanyName: (String) -> Unit,
    onNavigateToWizard: (Long) -> Unit
) {
    val inspections by viewModel.inspections.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }
    
    var showAdminArea by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var showEmailSettings by remember { mutableStateOf(false) }
    var inspectionToEdit by remember { mutableStateOf<Inspection?>(null) }
    var pdfInspectionToDownload by remember { mutableStateOf<Inspection?>(null) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        viewModel.createNewInspection { newId ->
                            onNavigateToWizard(newId)
                        }
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Nova Vistoria") },
                text = { Text("Nova Vistoria", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern Hero Header with background image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_solar_banner),
                    contentDescription = "Solar Banner Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Bem-vindo novamente",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (syncStatus.contains("Sincronizado")) Color(0xFF4CAF50)
                                        else if (syncStatus.contains("Erro")) Color(0xFFF44336)
                                        else Color(0xFFFFC107)
                                    )
                            )
                            Text(
                                text = syncStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.syncFromCloud() },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sincronizar",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSyncing) "Sincronizando..." else "Sincronizar Nuvem",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { showEmailSettings = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Configurar E-mail",
                                    tint = Color.White
                                )
                            }
                        }

                        if (userName == "Administrador") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAdminAuthDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Área do Administrador",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SolarPower,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize()
                        )
                    }
                }
            }



            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vistorias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { showAdminAuthDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Access",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADMIN ACCESS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (inspections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.padding(24.dp).fillMaxSize(),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Nenhuma vistoria.", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toque em 'Nova Vistoria' para iniciar.", 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(inspections) { inspection ->
                        InspectionCard(inspection = inspection, onClick = { onNavigateToWizard(inspection.id) })
                    }
                }
            }
        }
    }

    if (showAdminAuthDialog) {
        AdminAuthDialog(
            onDismiss = { showAdminAuthDialog = false },
            onSuccess = {
                showAdminAuthDialog = false
                showAdminArea = true
            }
        )
    }

    if (showEmailSettings) {
        EmailSettingsDialog(
            userPreferences = userPreferences,
            onDismiss = { showEmailSettings = false }
        )
    }

    if (showAdminArea) {
        AdminAreaDialog(
            inspections = inspections,
            onDismiss = { showAdminArea = false },
            onDelete = { id -> viewModel.deleteInspection(id) },
            onUpdate = { inspection -> viewModel.updateInspection(inspection) },
            onEditClick = { inspection -> inspectionToEdit = inspection },
            onDownloadPdf = { inspection ->
                com.example.util.PdfGenerator.savePdfDirectly(context, inspection, companyName)
            }
        )
    }

    if (inspectionToEdit != null) {
        EditInspectionDialog(
            inspection = inspectionToEdit!!,
            onDismiss = { inspectionToEdit = null },
            onSave = { updated ->
                viewModel.updateInspection(updated)
                inspectionToEdit = null
            }
        )
    }
}

@Composable
fun InspectionCard(inspection: Inspection, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(inspection.date))
    val clientFullName = "${inspection.clientFirstName} ${inspection.clientLastName}".trim()
    val title = if (clientFullName.isNotBlank()) clientFullName else "Novo Cliente (Não identificado)"
    val statusColor = if (inspection.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.tertiary
    val statusText = if (inspection.isCompleted) "Concluída" else "Em andamento"
    val containerColor = if (inspection.isCompleted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f),
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                if (inspection.address.isNotBlank()) {
                    Text(
                        text = inspection.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminAreaDialog(
    inspections: List<Inspection>,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (Inspection) -> Unit,
    onEditClick: (Inspection) -> Unit,
    onDownloadPdf: (Inspection) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Área do Administrador",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Gerenciar Vistorias (${inspections.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (inspections.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhuma vistoria encontrada.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(inspections) { inspection ->
                            val clientFullName = "${inspection.clientFirstName} ${inspection.clientLastName}".trim()
                            val name = if (clientFullName.isNotBlank()) clientFullName else "Sem nome (ID: ${inspection.clientIdString})"
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (inspection.isCompleted) "Status: Concluída" else "Status: Em andamento",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (inspection.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                            )
                                            if (inspection.address.isNotBlank()) {
                                                Text(
                                                    text = inspection.address,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        
                                        IconButton(onClick = { onDelete(inspection.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Quick Correction button
                                        Button(
                                            onClick = { onEditClick(inspection) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Corrigir", style = MaterialTheme.typography.labelSmall)
                                        }
                                        
                                        // Reopen or Complete Toggle
                                        OutlinedButton(
                                            onClick = { onUpdate(inspection.copy(isCompleted = !inspection.isCompleted)) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = if (inspection.isCompleted) "Reabrir" else "Concluir",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        
                                        // Download PDF button
                                        Button(
                                            onClick = { onDownloadPdf(inspection) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("PDF", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditInspectionDialog(
    inspection: Inspection,
    onDismiss: () -> Unit,
    onSave: (Inspection) -> Unit
) {
    var firstName by remember { mutableStateOf(inspection.clientFirstName) }
    var lastName by remember { mutableStateOf(inspection.clientLastName) }
    var address by remember { mutableStateOf(inspection.address) }
    var clientId by remember { mutableStateOf(inspection.clientIdString) }
    
    var connectionType by remember { mutableStateOf(inspection.connectionType) }
    var mainBreaker by remember { mutableStateOf(inspection.mainBreaker) }
    var voltage by remember { mutableStateOf(inspection.voltage) }
    
    var roofType by remember { mutableStateOf(inspection.roofType) }
    var roofInclination by remember { mutableStateOf(inspection.roofInclination) }
    var arrayArrangement by remember { mutableStateOf(inspection.arrayArrangement) }
    var inverterLocation by remember { mutableStateOf(inspection.inverterLocation) }
    
    var observations by remember { mutableStateOf(inspection.observations) }
    var techName by remember { mutableStateOf(inspection.techName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Corrigir Dados da Vistoria",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Dados do Cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Nome do Cliente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Sobrenome do Cliente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("ID do Cliente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Endereço") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("2. Dados Técnicos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = connectionType,
                        onValueChange = { connectionType = it },
                        label = { Text("Tipo de Conexão") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = mainBreaker,
                        onValueChange = { mainBreaker = it },
                        label = { Text("Disjuntor Geral") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = voltage,
                        onValueChange = { voltage = it },
                        label = { Text("Tensão") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("3. Informações do Telhado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = roofType,
                        onValueChange = { roofType = it },
                        label = { Text("Tipo de Telhado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = roofInclination,
                        onValueChange = { roofInclination = it },
                        label = { Text("Inclinação") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = arrayArrangement,
                        onValueChange = { arrayArrangement = it },
                        label = { Text("Arranjo dos Módulos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = inverterLocation,
                        onValueChange = { inverterLocation = it },
                        label = { Text("Localização do Inversor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("4. Observações e Outros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    
                    OutlinedTextField(
                        value = techName,
                        onValueChange = { techName = it },
                        label = { Text("Nome do Vistoriador") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            onSave(
                                inspection.copy(
                                    clientFirstName = firstName,
                                    clientLastName = lastName,
                                    address = address,
                                    clientIdString = clientId,
                                    meterBoxType = inspection.meterBoxType,
                                    connectionType = connectionType,
                                    mainBreaker = mainBreaker,
                                    voltage = voltage,
                                    roofType = roofType,
                                    roofInclination = roofInclination,
                                    arrayArrangement = arrayArrangement,
                                    inverterLocation = inverterLocation,
                                    observations = observations,
                                    techName = techName
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuthDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Acesso Restrito",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Digite as credenciais de administrador para prosseguir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        errorMessage = null
                    },
                    label = { Text("Usuário") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Senha") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (username.trim().equals("admin", ignoreCase = true) && password == "Miguel123") {
                                onSuccess()
                            } else {
                                errorMessage = "Usuário ou senha incorretos."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSettingsDialog(
    userPreferences: com.example.data.UserPreferences,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentRecipient by userPreferences.emailRecipient.collectAsState(initial = "")
    val currentSender by userPreferences.emailSender.collectAsState(initial = "")
    val currentResendKey by userPreferences.resendApiKey.collectAsState(initial = "")
    val currentSendMethod by userPreferences.sendMethod.collectAsState(initial = "manual")
    val currentWebhookUrl by userPreferences.webhookUrl.collectAsState(initial = "")
    val currentSmtpHost by userPreferences.smtpHost.collectAsState(initial = "smtp.gmail.com")
    val currentSmtpPort by userPreferences.smtpPort.collectAsState(initial = "587")
    val currentSmtpUsername by userPreferences.smtpUsername.collectAsState(initial = "")
    val currentSmtpPassword by userPreferences.smtpPassword.collectAsState(initial = "")

    var recipient by remember(currentRecipient) { mutableStateOf(currentRecipient ?: "") }
    var sender by remember(currentSender) { mutableStateOf(currentSender ?: "") }
    var resendKey by remember(currentResendKey) { mutableStateOf(currentResendKey ?: "") }
    var sendMethod by remember(currentSendMethod) { mutableStateOf(currentSendMethod ?: "manual") }
    var webhookUrl by remember(currentWebhookUrl) { mutableStateOf(currentWebhookUrl ?: "") }
    var smtpHost by remember(currentSmtpHost) { mutableStateOf(currentSmtpHost ?: "smtp.gmail.com") }
    var smtpPort by remember(currentSmtpPort) { mutableStateOf(currentSmtpPort ?: "587") }
    var smtpUsername by remember(currentSmtpUsername) { mutableStateOf(currentSmtpUsername ?: "") }
    var smtpPassword by remember(currentSmtpPassword) { mutableStateOf(currentSmtpPassword ?: "") }

    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Configuração de E-mail",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Configure o envio automático dos relatórios de vistoria em formato PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selection of Send Method
                Text(
                    text = "Modo de Envio:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("manual" to "Manual", "smtp" to "E-mail Direto (SMTP)").forEach { (methodId, label) ->
                            FilterChip(
                                selected = sendMethod == methodId,
                                onClick = { sendMethod = methodId },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("resend" to "Resend API", "webhook" to "Webhook").forEach { (methodId, label) ->
                            FilterChip(
                                selected = sendMethod == methodId,
                                onClick = { sendMethod = methodId },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (sendMethod == "manual") {
                    Text(
                        text = "No modo Manual, ao clicar em salvar, você poderá escolher o aplicativo de e-mail (Gmail, Outlook, etc.) para enviar o relatório.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recipient Email field
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("E-mail Destinatário") },
                    placeholder = { Text("exemplo@brsolar.com.br") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (sendMethod == "smtp") {
                    Text(
                        text = "Insira as credenciais do seu servidor de e-mail (Gmail, Outlook, Hostgator, Locaweb, etc.) para realizar o envio direto em segundo plano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = { smtpHost = it },
                        label = { Text("Servidor SMTP (Host)") },
                        placeholder = { Text("smtp.brsolarengenharia.com.br ou smtp.gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = { smtpPort = it },
                        label = { Text("Porta SMTP") },
                        placeholder = { Text("587 ou 465") },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpUsername,
                        onValueChange = { smtpUsername = it },
                        label = { Text("Usuário SMTP (Seu E-mail)") },
                        placeholder = { Text("projeto@brsolarengenharia.com.br") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpPassword,
                        onValueChange = { smtpPassword = it },
                        label = { Text("Senha SMTP (ou Senha de App)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (sendMethod == "resend") {
                    Text(
                        text = "O Resend envia e-mails automaticamente em segundo plano. Cadastre-se grátis em resend.com para obter sua chave.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = sender,
                        onValueChange = { sender = it },
                        label = { Text("E-mail Remetente (Opcional)") },
                        placeholder = { Text("onboarding@resend.dev") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = resendKey,
                        onValueChange = { resendKey = it },
                        label = { Text("Chave da API do Resend (re_...)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (sendMethod == "webhook") {
                    Text(
                        text = "Envia os dados da vistoria e o arquivo PDF codificado em Base64 para a URL especificada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("URL do Webhook") },
                        placeholder = { Text("https://hook.us1.make.com/...") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                userPreferences.saveEmailRecipient(recipient.trim())
                                userPreferences.saveEmailSender(sender.trim())
                                userPreferences.saveResendApiKey(resendKey.trim())
                                userPreferences.saveSendMethod(sendMethod)
                                userPreferences.saveWebhookUrl(webhookUrl.trim())
                                userPreferences.saveSmtpHost(smtpHost.trim())
                                userPreferences.saveSmtpPort(smtpPort.trim())
                                userPreferences.saveSmtpUsername(smtpUsername.trim())
                                userPreferences.saveSmtpPassword(smtpPassword) // don't trim password in case it contains deliberate trailing spaces
                                Toast.makeText(context, "Configurações salvas!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

