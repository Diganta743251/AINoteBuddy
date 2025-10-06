package com.ainotebuddy.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.ainotebuddy.app.offline.*
import java.util.Date

// Register only DateConverters globally; entity-specific converters are annotated on entities
@TypeConverters(DateConverters::class)
@Database(
    entities = [
        NoteEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ChecklistItemEntity::class,
        NoteVersionEntity::class,
        FolderEntity::class,
        // Organization entities
        com.ainotebuddy.app.data.local.entity.organization.NoteTemplateEntity::class,
        com.ainotebuddy.app.data.local.entity.organization.SmartFolderEntity::class,
        com.ainotebuddy.app.data.local.entity.organization.RecurringNoteEntity::class,
        // Templates
        com.ainotebuddy.app.data.local.entity.TemplateEntity::class,
        // Enhanced Offline-First Architecture entities
        OfflineOperationEntity::class,
        SyncStateEntity::class,
        ConflictHistoryEntity::class,
        DataIntegrityEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    // Deprecated: legacy templateDao; use OrganizationDao for templates
    abstract fun templateDao(): com.ainotebuddy.app.data.local.dao.TemplateDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun noteVersionDao(): NoteVersionDao
    abstract fun folderDao(): FolderDao

    // Organization DAO
    abstract fun organizationDao(): com.ainotebuddy.app.data.local.dao.OrganizationDao
    
    // Enhanced Offline-First Architecture DAOs
    abstract fun offlineOperationDao(): OfflineOperationDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun conflictHistoryDao(): ConflictHistoryDao
    abstract fun dataIntegrityDao(): DataIntegrityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes.db"
                )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build().also { INSTANCE = it }
            }
        }
    }
}

/**
 * Type converters for Room database
 */
class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
