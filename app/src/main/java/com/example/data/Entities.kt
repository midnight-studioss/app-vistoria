package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val document: String, // CPF or CNPJ
    val phone: String,
    val email: String,
    val address: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ProjectStatus {
    LEAD, VISTORIA_PENDENTE, VISTORIA_CONCLUIDA, INSTALACAO, POS_VENDA
}

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(entity = Client::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("clientId")]
)
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val title: String,
    val status: String = ProjectStatus.LEAD.name,
    val createdAt: Long = System.currentTimeMillis()
)

object SyncState {
    const val DRAFT = "DRAFT"
    const val PENDING_SYNC = "PENDING_SYNC"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
    const val SYNC_FAILED = "SYNC_FAILED"
}

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("projectId")]
)
data class Inspection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    
    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val address: String = "",
    
    // Client Data
    val clientFirstName: String = "",
    val clientLastName: String = "",
    val clientIdString: String = "",
    
    // Technical Data
    val meterBoxType: String = "", // Tipo de Medidor
    val connectionType: String = "", // Monofásico, Bifásico, Trifásico
    val mainBreaker: String = "", // Disjuntor Geral
    val voltage: String = "", // Tensão
    
    // Checklist
    val hasGrounding: Boolean? = null,
    val needsScaffold: Boolean? = null,
    val needsPruning: Boolean? = null,
    val needsConstruction: Boolean? = null,
    val hasShadowArea: Boolean? = null,
    val hasWifi: Boolean? = null,
    val hasSpareTiles: Boolean? = null,
    val hasInfiltration: Boolean? = null,
    val hasShadow: Boolean? = null,
    
    // Roof
    val roofType: String = "",
    val roofInclination: String = "",
    val arrayArrangement: String = "",
    val inverterLocation: String = "",
    
    // Observations
    val observations: String = "",
    
    // Signatures
    val techName: String = "",
    val techSignatureUri: String? = null,
    val clientRepName: String = "",
    val clientSignatureUri: String? = null,
    
    // Photos
    val photoMeterUri: String? = null,
    val photoBreakerUri: String? = null,
    val photoPanelUri: String? = null,
    val photoRoofUri: String? = null,
    val photoGeneralUri: String? = null,
    
    // Status
    val currentStep: Int = 0,
    @get:com.google.firebase.firestore.PropertyName("isCompleted") @set:com.google.firebase.firestore.PropertyName("isCompleted") var isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.DRAFT
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(entity = Inspection::class, parentColumns = ["id"], childColumns = ["inspectionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("inspectionId")]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inspectionId: Long,
    val category: String, // e.g., "ROOF", "METER", "BREAKER", "INVERTER"
    val uri: String,
    val timestamp: Long = System.currentTimeMillis()
)
