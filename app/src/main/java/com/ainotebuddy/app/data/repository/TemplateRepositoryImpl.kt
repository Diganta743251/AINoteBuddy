package com.ainotebuddy.app.data.repository

import com.ainotebuddy.app.data.local.dao.TemplateDao
import com.ainotebuddy.app.data.mappers.toDomain
import com.ainotebuddy.app.data.mappers.toEntity
import com.ainotebuddy.app.domain.template.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val dao: TemplateDao
) : TemplateRepository {
    override fun observeTemplates(): Flow<List<Template>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTemplate(id: Long): Flow<Template?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getTemplate(id: Long): Template? =
        dao.getById(id)?.toDomain()

    override suspend fun createTemplate(template: Template): Long =
        dao.insert(template.toEntity().copy(id = 0L))

    override suspend fun updateTemplate(template: Template) =
        dao.update(template.toEntity())

    override suspend fun deleteTemplate(id: Long) =
        dao.deleteByIds(listOf(id))

    override suspend fun deleteTemplates(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }
}