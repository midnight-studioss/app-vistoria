package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getProjectsForClient(clientId: Long): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long
}

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY date DESC")
    fun getAllInspections(): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE projectId = :projectId")
    fun getInspectionForProject(projectId: Long): Flow<Inspection?>
    
    @Query("SELECT * FROM inspections WHERE id = :id")
    fun getInspectionById(id: Long): Flow<Inspection?>

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getInspectionSync(id: Long): Inspection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: Inspection): Long
    
    @Update
    suspend fun updateInspection(inspection: Inspection)

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun deleteInspectionById(id: Long)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE inspectionId = :inspectionId")
    fun getPhotosForInspection(inspectionId: Long): Flow<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo)
    
    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: Long)
}
