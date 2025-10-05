package com.ainotebuddy.app.collaboration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Compatibility layer: provides no-op stubs for functions referenced by CollaborationManager
 * but not implemented in FirebaseCollaborationService. Replace with real implementations later.
 */

fun FirebaseCollaborationService.stopObservingCursorPositions(sessionId: String) { /* no-op */ }
fun FirebaseCollaborationService.stopObservingSelections(sessionId: String) { /* no-op */ }

fun FirebaseCollaborationService.observeCursorPositions(sessionId: String): Flow<Map<String, CursorPosition>> = flowOf(emptyMap())
fun FirebaseCollaborationService.observeSelections(sessionId: String): Flow<Map<String, SelectionRange>> = flowOf(emptyMap())

suspend fun FirebaseCollaborationService.updateCursorPosition(sessionId: String, userId: String, position: CursorPosition) { /* no-op */ }
suspend fun FirebaseCollaborationService.updateSelection(sessionId: String, userId: String, range: SelectionRange) { /* no-op */ }

suspend fun FirebaseCollaborationService.sendChatMessage(sessionId: String, userId: String, message: String) { /* no-op */ }

suspend fun FirebaseCollaborationService.inviteUserToNote(noteId: String, email: String, role: CollaborationRole) { /* no-op */ }
suspend fun FirebaseCollaborationService.acceptInvitation(invitationId: String) { /* no-op */ }
suspend fun FirebaseCollaborationService.rejectInvitation(invitationId: String) { /* no-op */ }

suspend fun FirebaseCollaborationService.getUserRole(noteId: String, userId: String): CollaborationRole = CollaborationRole.VIEWER