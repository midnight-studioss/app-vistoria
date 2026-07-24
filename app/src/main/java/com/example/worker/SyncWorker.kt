package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.SolarRepository
import com.example.data.SyncState
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = SolarRepository(db)
            
            val pendingInspections = repository.getPendingInspections()
            
            if (pendingInspections.isEmpty()) {
                Log.d("SyncWorker", "No pending inspections to sync.")
                return@withContext Result.success()
            }
            
            Log.d("SyncWorker", "Found ${pendingInspections.size} pending inspections. Starting sync...")
            
            var allSuccess = true
            
            for (inspection in pendingInspections) {
                try {
                    // Update to SYNCING
                    val syncingInspection = inspection.copy(syncState = SyncState.SYNCING)
                    repository.updateInspection(syncingInspection)
                    
                    // Sync to Firestore
                    repository.syncInspectionToCloud(syncingInspection)
                    
                    // Mark as SYNCED
                    val syncedInspection = syncingInspection.copy(syncState = SyncState.SYNCED)
                    repository.updateInspection(syncedInspection)
                    
                    // Generate PDF and save automatically
                    val pdfBytes = PdfGenerator.createAndSavePdf(applicationContext, syncedInspection, "BR SOLAR")
                    PdfGenerator.sendAutomaticEmailsOnly(applicationContext, syncedInspection, pdfBytes)
                    
                    Log.d("SyncWorker", "Successfully synced inspection: ${inspection.id}")
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync inspection: ${inspection.id}", e)
                    // Mark as SYNC_FAILED
                    val failedInspection = inspection.copy(syncState = SyncState.SYNC_FAILED)
                    repository.updateInspection(failedInspection)
                    allSuccess = false
                }
            }
            
            if (allSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error in sync worker", e)
            Result.retry()
        }
    }
}
