package com.ainotebuddy.app.offline

// TODO: Remove Hilt dependencies - OfflineFirstModule disabled
/*
import android.content.Context
import androidx.work.WorkManager
import com.ainotebuddy.app.data.AppDatabase
import com.ainotebuddy.app.data.NoteDao
import com.ainotebuddy.app.data.DataIntegrityDao
import com.ainotebuddy.app.repository.AdvancedNoteRepository
import com.ainotebuddy.app.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for Enhanced Offline-First Architecture
 * Provides all offline-first components with proper dependency management
 */
@Module
@InstallIn(SingletonComponent::class)
object OfflineFirstModule {
    
    @Provides
    @Singleton
    fun provideOfflineOperationDao(database: AppDatabase): OfflineOperationDao {
        return database.offlineOperationDao()
    }
    
    @Provides
    @Singleton
    fun provideSyncStateDao(database: AppDatabase): SyncStateDao {
        return database.syncStateDao()
    }
    
    @Provides
    @Singleton
    fun provideConflictHistoryDao(database: AppDatabase): ConflictHistoryDao {
        return database.conflictHistoryDao()
    }
    
    @Provides
    @Singleton
    fun provideDataIntegrityDao(database: AppDatabase): DataIntegrityDao {
        return database.dataIntegrityDao()
    }
    
    @Provides
    @Singleton
    fun provideNetworkStateManager(@ApplicationContext context: Context): NetworkStateManager {
        return NetworkStateManager(context)
    }
    
    @Provides
    @Singleton
    fun provideConflictResolutionEngine(): ConflictResolutionEngine {
        return ConflictResolutionEngine()
    }
    
    @Provides
    @Singleton
    fun provideDataIntegrityManager(dataIntegrityDao: DataIntegrityDao): DataIntegrityManager {
        return DataIntegrityManager(dataIntegrityDao)
    }
    
    @Provides
    @Singleton
    fun provideOfflineOperationManager(
        @ApplicationContext context: Context,
        offlineOperationDao: OfflineOperationDao,
        syncStateDao: SyncStateDao,
        conflictHistoryDao: ConflictHistoryDao,
        dataIntegrityDao: DataIntegrityDao,
        noteRepository: AdvancedNoteRepository,
        networkStateManager: NetworkStateManager,
        conflictResolutionEngine: ConflictResolutionEngine,
        dataIntegrityManager: DataIntegrityManager
    ): OfflineOperationManager {
        return OfflineOperationManager(
            context,
            offlineOperationDao,
            syncStateDao,
            conflictHistoryDao,
            dataIntegrityDao,
            noteRepository,
            networkStateManager,
            conflictResolutionEngine,
            dataIntegrityManager
        )
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideOfflineWorkScheduler(workManager: WorkManager): OfflineWorkScheduler {
        return OfflineWorkScheduler(workManager)
    }
    
    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository {
        return NoteRepository(noteDao)
    }
}
*/
