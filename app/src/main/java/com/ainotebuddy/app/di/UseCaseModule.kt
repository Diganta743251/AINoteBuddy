package com.ainotebuddy.app.di

import com.ainotebuddy.app.ai.local.AINlp
import com.ainotebuddy.app.ai.local.AISummarizer
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.usecase.AutoTagNote
import com.ainotebuddy.app.usecase.SummarizeNote
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideSummarizeNote(
        noteRepository: NoteRepository,
        summarizer: AISummarizer
    ): SummarizeNote = SummarizeNote(noteRepository, summarizer)

    @Provides
    @Singleton
    fun provideAutoTagNote(
        noteRepository: NoteRepository,
        nlp: AINlp
    ): AutoTagNote = AutoTagNote(noteRepository, nlp)
}