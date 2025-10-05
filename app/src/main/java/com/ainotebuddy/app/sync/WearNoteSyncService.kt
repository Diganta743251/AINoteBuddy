package com.ainotebuddy.app.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ainotebuddy.app.data.NoteEntity
import com.google.gson.Gson

class WearNoteSyncService(private val context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val gson = Gson()

    suspend fun syncNotesToWear(notes: List<NoteEntity>) {
        withContext(Dispatchers.IO) {
            try {
                val notesJson = gson.toJson(notes)
                val dataMapRequest = PutDataMapRequest.create("/notes").apply {
                    dataMap.putString("notes_json", notesJson)
                }
                
                val putDataRequest = dataMapRequest.asPutDataRequest()
                Tasks.await(dataClient.putDataItem(putDataRequest))
                Log.d("WearNoteSyncService", "Notes synced to Wear OS")
            } catch (e: Exception) {
                Log.e("WearNoteSyncService", "Failed to sync notes to Wear OS", e)
            }
        }
    }

    suspend fun syncNoteFromWear(): NoteEntity? {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement proper note sync from Wear OS
                // For now, return null
                null
            } catch (e: Exception) {
                Log.e("WearNoteSyncService", "Failed to sync note from Wear OS", e)
                null
            }
        }
    }
} 