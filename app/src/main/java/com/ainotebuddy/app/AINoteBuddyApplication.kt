package com.ainotebuddy.app

import android.app.Application
import androidx.multidex.MultiDexApplication
// import androidx.room.Room
import com.ainotebuddy.app.data.AppDatabase
import com.ainotebuddy.app.repository.AdvancedNoteRepository
import com.ainotebuddy.app.settings.BackupRestoreManager
import com.ainotebuddy.app.sync.GoogleDriveSyncService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class AINoteBuddyApplication : Application() {
    
    // Database - temporarily commented out due to Room dependency issues
    // private lateinit var database: AppDatabase
    
    // Repositories
    lateinit var advancedNoteRepository: AdvancedNoteRepository
    lateinit var backupRestoreManager: BackupRestoreManager
    lateinit var googleDriveSyncService: GoogleDriveSyncService
    
    // Google Drive API
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var driveService: Drive
    
    // Coroutine scope for background operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Sign-In
        initializeGoogleSignIn()
        
        // Initialize Google Drive API
        initializeGoogleDrive()
        
        // Initialize database - temporarily commented out
        // initializeDatabase()
        
        // Initialize repositories with stub implementations
        initializeRepositories()
        
        // Initialize sync service
        initializeSyncService()
        
        // Perform initial sync if user is signed in
        performInitialSync()
    }
    
    private fun initializeGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    
    private fun initializeGoogleDrive() {
        val credential = GoogleAccountCredential.usingOAuth2(
            this,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("AINoteBuddy")
            .build()
    }
    
    // Database initialization - temporarily commented out
    /*
    private fun initializeDatabase() {
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ainotebuddy_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    */
    
    private fun initializeRepositories() {
        // Initialize with stub implementations since database is disabled
        advancedNoteRepository = AdvancedNoteRepository(
            noteDao = null, // TODO: Implement when Room is available
            categoryDao = null, // TODO: Implement when Room is available
            tagDao = null, // TODO: Implement when Room is available
            templateDao = null, // TODO: Implement when Room is available
            checklistItemDao = null, // TODO: Implement when Room is available
            aiService = null // TODO: Implement when AI is available
        )
        backupRestoreManager = BackupRestoreManager(
            context = this,
            repository = advancedNoteRepository
        )
    }
    
    private fun initializeSyncService() {
        googleDriveSyncService = GoogleDriveSyncService(this)
    }
    
    private fun performInitialSync() {
        applicationScope.launch {
            try {
                // Check if user is signed in
                val account = GoogleSignIn.getLastSignedInAccount(this@AINoteBuddyApplication)
                if (account != null) {
                    // Perform initial sync
                    // googleDriveSyncService.syncNotes(advancedNoteRepository, driveService)
                }
            } catch (e: Exception) {
                // Handle sync errors
                e.printStackTrace()
            }
        }
    }
    
    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient
    
    fun getDriveService(): Drive = driveService
}
