package com.ainotebuddy.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TemplateEntity::class,
        ChecklistItemEntity::class,
        NoteVersionEntity::class,
        FolderEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun templateDao(): TemplateDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun noteVersionDao(): NoteVersionDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
