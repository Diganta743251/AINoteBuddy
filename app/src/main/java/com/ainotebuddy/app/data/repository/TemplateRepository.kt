package com.ainotebuddy.app.data.repository

import com.ainotebuddy.app.domain.template.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<Template>>
    fun observeTemplate(id: Long): Flow<Template?>
    suspend fun getTemplate(id: Long): Template?
    suspend fun createTemplate(template: Template): Long
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplate(id: Long)
    suspend fun deleteTemplates(ids: List<Long>)
}
