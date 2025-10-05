package com.ainotebuddy.app.data

// Mapping extensions between Room entity and domain model

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = if (isEncrypted) "" else content,
    dateCreated = createdAt,
    dateModified = updatedAt,
    category = category,
    tags = if (tags.isNotBlank()) tags.split(',').map { it.trim() }.filter { it.isNotEmpty() } else emptyList(),
    isPinned = isPinned,
    isFavorite = isFavorite,
    isStarred = isFavorite || isPinned, // star concept mapped conservatively; adjust if you have a separate field
    isEncrypted = isEncrypted,
    encryptionMetadata = encryptionMetadata,
    encryptedContent = encryptedContent
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = if (isEncrypted) "" else content,
    createdAt = dateCreated,
    updatedAt = dateModified,
    category = category,
    tags = if (tags.isNotEmpty()) tags.joinToString(",") else "",
    isPinned = isPinned,
    isFavorite = isFavorite,
    // NoteEntity doesn't have an explicit isStarred; keep using isFavorite/isPinned as sources of truth
    isEncrypted = isEncrypted,
    encryptionMetadata = encryptionMetadata,
    encryptedContent = encryptedContent
)