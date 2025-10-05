package com.ainotebuddy.app.data.mapper

import com.ainotebuddy.app.data.local.entity.organization.NoteTemplateEntity
import com.ainotebuddy.app.data.local.entity.organization.RecurringNoteEntity
import com.ainotebuddy.app.data.local.entity.organization.SmartFolderEntity
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.RecurringNote
import com.ainotebuddy.app.data.model.organization.SmartFolder

fun SmartFolder.toEntity(): SmartFolderEntity = SmartFolderEntity.fromModel(this)

fun NoteTemplate.toEntity(): NoteTemplateEntity = NoteTemplateEntity.fromModel(this)

fun RecurringNote.toEntity(): RecurringNoteEntity = RecurringNoteEntity.fromModel(this)
