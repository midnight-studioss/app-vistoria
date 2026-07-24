package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SolarRepository(private val db: AppDatabase) {
    val clients: Flow<List<Client>> = db.clientDao().getAllClients()
    
    private val _syncStatus = MutableStateFlow<String>("Inicializando sincronização...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()
    
    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w("SolarRepository", "Firebase not initialized: ${e.message}")
            _syncStatus.value = "Erro: Firebase não inicializado (${e.message})"
            null
        }
    }
    
    suspend fun addClient(client: Client): Long = db.clientDao().insertClient(client)
    
    suspend fun addProject(project: Project): Long = db.projectDao().insertProject(project)
    
    suspend fun createInitialInspection(clientIdString: String): Long {
        val uniqueId = System.currentTimeMillis() + (0..1000).random()
        val inspection = Inspection(id = uniqueId, projectId = null, techName = "", clientIdString = clientIdString)
        db.inspectionDao().insertInspection(inspection)
        
        val firestore = getFirestore()
        if (firestore == null) {
            _syncStatus.value = "Salvo localmente (Sem Firebase)"
        } else {
            firestore.collection("inspections").document(uniqueId.toString()).set(inspection)
                .addOnSuccessListener {
                    Log.d("SolarRepository", "Firestore: Inspection $uniqueId created.")
                    _syncStatus.value = "Sincronizado (Tempo Real Ativo)"
                }
                .addOnFailureListener { e ->
                    Log.w("SolarRepository", "Firestore error creating inspection $uniqueId", e)
                    _syncStatus.value = "Erro ao enviar: ${e.message}"
                }
        }
        return uniqueId
    }
    
    fun getAllInspections(): Flow<List<Inspection>> = db.inspectionDao().getAllInspections()
    
    fun getInspection(id: Long): Flow<Inspection?> = db.inspectionDao().getInspectionById(id)
    
    suspend fun updateInspection(inspection: Inspection) {
        val updatedInspection = inspection.copy(lastUpdated = System.currentTimeMillis())
        db.inspectionDao().updateInspection(updatedInspection)
        Log.d("SolarRepository", "Saving local update. ID: ${updatedInspection.id}, lastUpdated: ${updatedInspection.lastUpdated}, isCompleted: ${updatedInspection.isCompleted}")
        
        val firestore = getFirestore()
        if (firestore == null) {
            _syncStatus.value = "Salvo localmente (Sem Firebase)"
        } else {
            firestore.collection("inspections").document(updatedInspection.id.toString()).set(updatedInspection)
                .addOnSuccessListener {
                    Log.d("SolarRepository", "Firestore: Inspection ${updatedInspection.id} updated.")
                    _syncStatus.value = "Sincronizado (Tempo Real Ativo)"
                }
                .addOnFailureListener { e ->
                    Log.w("SolarRepository", "Firestore error updating inspection ${updatedInspection.id}", e)
                    _syncStatus.value = "Erro ao enviar: ${e.message}"
                }
        }
    }
    
    suspend fun deleteInspection(id: Long) {
        db.inspectionDao().deleteInspectionById(id)
        val firestore = getFirestore()
        if (firestore != null) {
            firestore.collection("inspections").document(id.toString()).delete()
                .addOnSuccessListener {
                    _syncStatus.value = "Sincronizado (Tempo Real Ativo)"
                }
                .addOnFailureListener { e ->
                    Log.w("SolarRepository", "Firestore error deleting inspection $id", e)
                    _syncStatus.value = "Erro ao deletar: ${e.message}"
                }
        }
    }
    
    fun getPhotos(inspectionId: Long): Flow<List<Photo>> = db.photoDao().getPhotosForInspection(inspectionId)
    
    suspend fun addPhoto(photo: Photo) = db.photoDao().insertPhoto(photo)
    suspend fun deletePhoto(photoId: Long) = db.photoDao().deletePhoto(photoId)

    init {
        startRealtimeSync()
    }

    private fun startRealtimeSync() {
        val firestore = getFirestore()
        if (firestore == null) {
            _syncStatus.value = "Erro: Firebase não disponível"
            return
        }
        firestore.collection("inspections").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("SolarRepository", "Listen failed.", e)
                _syncStatus.value = "Erro na sincronização automática: ${e.message}"
                return@addSnapshotListener
            }
            if (snapshot != null) {
                _syncStatus.value = "Sincronizado (Tempo Real Ativo)"
                CoroutineScope(Dispatchers.IO).launch {
                    val cloudIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() }.toSet()
                    val localInspections = db.inspectionDao().getAllInspectionsSync()
                    for (localInsp in localInspections) {
                        if (localInsp.id !in cloudIds) {
                            db.inspectionDao().deleteInspectionById(localInsp.id)
                            Log.d("SolarRepository", "Realtime Sync [Delete]: Removed inspection ${localInsp.id} locally as it was deleted in cloud.")
                        }
                    }

                    for (document in snapshot.documents) {
                        try {
                            val insp = document.toObject(Inspection::class.java)
                            if (insp != null) {
                                val isCompletedValue = document.getBoolean("isCompleted") ?: document.getBoolean("completed") ?: insp.isCompleted
                                val lastUpdatedValue = document.getLong("lastUpdated") ?: insp.lastUpdated
                                val updatedInsp = insp.copy(isCompleted = isCompletedValue, lastUpdated = lastUpdatedValue)
                                
                                val existing = db.inspectionDao().getInspectionSync(updatedInsp.id)
                                if (existing == null) {
                                    db.inspectionDao().insertInspection(updatedInsp)
                                    Log.d("SolarRepository", "Conflict Resolution [Realtime Insert]: ID: ${updatedInsp.id} stored in local DB.")
                                } else {
                                    if (updatedInsp.lastUpdated > existing.lastUpdated) {
                                        db.inspectionDao().updateInspection(updatedInsp)
                                        Log.d("SolarRepository", "Conflict Resolution [Realtime Override]: ID: ${updatedInsp.id} updated. Cloud is newer (${updatedInsp.lastUpdated} > ${existing.lastUpdated}). Local status: ${existing.isCompleted} -> Cloud status: ${updatedInsp.isCompleted}")
                                    } else if (updatedInsp.lastUpdated < existing.lastUpdated) {
                                        Log.d("SolarRepository", "Conflict Resolution [Local Wins]: ID: ${updatedInsp.id}. Local is newer (${existing.lastUpdated} > ${updatedInsp.lastUpdated}). Propagating local to Firestore.")
                                        firestore.collection("inspections").document(existing.id.toString()).set(existing)
                                    } else if (existing != updatedInsp) {
                                        // Timestamps are equal but fields are different, merge/update to keep in sync
                                        db.inspectionDao().updateInspection(updatedInsp)
                                        Log.d("SolarRepository", "Conflict Resolution [Equal Timestamps Sync]: ID: ${updatedInsp.id} updated due to minor differences.")
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            Log.w("SolarRepository", "Error parsing doc realtime", ex)
                        }
                    }
                }
            }
        }
    }

    suspend fun syncFromCloud() {
        val firestore = getFirestore()
        if (firestore == null) {
            _syncStatus.value = "Erro: Firebase não disponível"
            throw Exception("Firebase não disponível")
        }
        try {
            Log.d("SolarRepository", "Manual cloud pull started.")
            _syncStatus.value = "Conectando com a nuvem..."
            val result = firestore.collection("inspections").get().await()
            _syncStatus.value = "Sincronizando registros..."
            val cloudIds = result.documents.mapNotNull { it.id.toLongOrNull() }.toSet()
            val localInspections = db.inspectionDao().getAllInspectionsSync()
            for (localInsp in localInspections) {
                if (localInsp.id !in cloudIds) {
                    db.inspectionDao().deleteInspectionById(localInsp.id)
                    Log.d("SolarRepository", "Manual Sync [Delete]: Removed inspection ${localInsp.id} locally as it does not exist in cloud.")
                }
            }

            for (document in result) {
                val insp = document.toObject(Inspection::class.java)
                if (insp != null) {
                    val isCompletedValue = document.getBoolean("isCompleted") ?: document.getBoolean("completed") ?: insp.isCompleted
                    val lastUpdatedValue = document.getLong("lastUpdated") ?: insp.lastUpdated
                    val updatedInsp = insp.copy(isCompleted = isCompletedValue, lastUpdated = lastUpdatedValue)
                    
                    val existing = db.inspectionDao().getInspectionSync(updatedInsp.id)
                    if (existing == null) {
                        db.inspectionDao().insertInspection(updatedInsp)
                        Log.d("SolarRepository", "Manual Sync [Insert]: ID: ${updatedInsp.id}")
                    } else {
                        if (updatedInsp.lastUpdated > existing.lastUpdated) {
                            db.inspectionDao().updateInspection(updatedInsp)
                            Log.d("SolarRepository", "Manual Sync [Override]: ID: ${updatedInsp.id} overwritten by newer cloud record.")
                        } else if (updatedInsp.lastUpdated < existing.lastUpdated) {
                            Log.d("SolarRepository", "Manual Sync [Local Wins]: ID: ${updatedInsp.id}. Local newer. Pushing to cloud.")
                            firestore.collection("inspections").document(existing.id.toString()).set(existing)
                        }
                    }
                }
            }
            _syncStatus.value = "Sincronizado (Tempo Real Ativo)"
            Log.d("SolarRepository", "Manual cloud pull finished successfully.")
        } catch (e: Exception) {
            Log.w("SolarRepository", "Error in manual sync: ${e.message}")
            _syncStatus.value = "Erro na sincronização manual: ${e.message}"
            throw e
        }
    }
}
