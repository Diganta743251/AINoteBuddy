package com.ainotebuddy.app.data.mappers

import com.ainotebuddy.app.data.local.entity.TemplateEntity
import com.ainotebuddy.app.domain.template.Template

fun TemplateEntity.toDomain(): Template = Template(
    id = id,
    name = name,
    description = description,
    icon = icon,
    category = category
)

fun Template.toEntity(): TemplateEntity = TemplateEntity(
    id = id,
    name = name,
    description = description,
    icon = icon,
    category = category
)