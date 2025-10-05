package com.ainotebuddy.app.di

// TODO: Remove Hilt dependencies - TaskModule disabled
/*
import com.ainotebuddy.app.data.dao.TaskDao
import com.ainotebuddy.app.repository.TaskRepository
import com.ainotebuddy.app.repository.TaskRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing task-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object TaskModule {

    /**
     * Provides the [TaskRepository] implementation.
     */
    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }
}
*/
