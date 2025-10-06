package com.ainotebuddy.app.di

import android.content.Context
import androidx.room.Room
import com.ainotebuddy.app.data.*
import com.ainotebuddy.app.data.repository.NoteRepository
import com.ainotebuddy.app.data.repository.NoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FixedDatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideOrganizationDao(database: AppDatabase): com.ainotebuddy.app.data.local.dao.OrganizationDao =
        database.organizationDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideTemplateDao(database: AppDatabase): com.ainotebuddy.app.data.local.dao.TemplateDao = database.templateDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): com.ainotebuddy.app.data.repository.NoteRepository
}