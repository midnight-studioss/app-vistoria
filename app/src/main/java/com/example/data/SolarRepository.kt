package com.example.data

import kotlinx.coroutines.flow.Flow

class SolarRepository(private val db: AppDatabase) {
    val clients: Flow<List<Client>> = db.clientDao().getAllClients()
    
    suspend fun addClient(client: Client): Long = db.clientDao().insertClient(client)
    
    suspend fun addProject(project: Project): Long = db.projectDao().insertProject(project)
    
    suspend fun createInitialInspection(clientIdString: String): Long {
        val inspection = Inspection(projectId = null, techName = "", clientIdString = clientIdString)
        return db.inspectionDao().insertInspection(inspection)
    }
    
    fun getAllInspections(): Flow<List<Inspection>> = db.inspectionDao().getAllInspections()
    
    fun getInspection(id: Long): Flow<Inspection?> = db.inspectionDao().getInspectionById(id)
    
    suspend fun updateInspection(inspection: Inspection) = db.inspectionDao().updateInspection(inspection)
    
    fun getPhotos(inspectionId: Long): Flow<List<Photo>> = db.photoDao().getPhotosForInspection(inspectionId)
    
    suspend fun addPhoto(photo: Photo) = db.photoDao().insertPhoto(photo)
    suspend fun deletePhoto(photoId: Long) = db.photoDao().deletePhoto(photoId)
}
