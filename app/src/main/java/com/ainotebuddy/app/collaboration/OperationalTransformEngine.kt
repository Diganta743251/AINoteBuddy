package com.ainotebuddy.app.collaboration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.math.*

/**
 * Operational Transform Engine for conflict-free collaborative editing
 * Implements the core OT algorithm for real-time collaborative text editing
 */
class OperationalTransformEngine {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Transform an operation against another operation for concurrent editing
     */
    fun transform(
        operation1: CollaborativeOperation,
        operation2: CollaborativeOperation,
        priority: TransformPriority = TransformPriority.LEFT_WINS
    ): Pair<CollaborativeOperation, CollaborativeOperation> {
        
        return when {
            operation1 is CollaborativeOperation.Insert && operation2 is CollaborativeOperation.Insert -> {
                transformInsertInsert(operation1, operation2, priority)
            }
            operation1 is CollaborativeOperation.Insert && operation2 is CollaborativeOperation.Delete -> {
                transformInsertDelete(operation1, operation2)
            }
            operation1 is CollaborativeOperation.Delete && operation2 is CollaborativeOperation.Insert -> {
                val (op2Prime, op1Prime) = transformInsertDelete(operation2, operation1)
                Pair(op1Prime, op2Prime)
            }
            operation1 is CollaborativeOperation.Delete && operation2 is CollaborativeOperation.Delete -> {
                transformDeleteDelete(operation1, operation2)
            }
            else -> {
                // Handle other operation types (Retain, Format)
                transformComplexOperations(operation1, operation2, priority)
            }
        }
    }
    
    /**
     * Transform two concurrent insert operations
     */
    private fun transformInsertInsert(
        insert1: CollaborativeOperation.Insert,
        insert2: CollaborativeOperation.Insert,
        priority: TransformPriority
    ): Pair<CollaborativeOperation.Insert, CollaborativeOperation.Insert> {
        
        return when {
            insert1.position < insert2.position -> {
                // insert1 comes before insert2, insert2 position shifts right
                val insert2Prime = insert2.copy(
                    position = insert2.position + insert1.content.length
                )
                Pair(insert1, insert2Prime)
            }
            insert1.position > insert2.position -> {
                // insert2 comes before insert1, insert1 position shifts right
                val insert1Prime = insert1.copy(
                    position = insert1.position + insert2.content.length
                )
                Pair(insert1Prime, insert2)
            }
            else -> {
                // Same position, use priority to determine order
                when (priority) {
                    TransformPriority.LEFT_WINS -> {
                        val insert2Prime = insert2.copy(
                            position = insert2.position + insert1.content.length
                        )
                        Pair(insert1, insert2Prime)
                    }
                    TransformPriority.RIGHT_WINS -> {
                        val insert1Prime = insert1.copy(
                            position = insert1.position + insert2.content.length
                        )
                        Pair(insert1Prime, insert2)
                    }
                    TransformPriority.TIMESTAMP -> {
                        if (insert1.timestamp <= insert2.timestamp) {
                            val insert2Prime = insert2.copy(
                                position = insert2.position + insert1.content.length
                            )
                            Pair(insert1, insert2Prime)
                        } else {
                            val insert1Prime = insert1.copy(
                                position = insert1.position + insert2.content.length
                            )
                            Pair(insert1Prime, insert2)
                        }
                    }
                    TransformPriority.USER_ID -> {
                        if (insert1.userId <= insert2.userId) {
                            val insert2Prime = insert2.copy(
                                position = insert2.position + insert1.content.length
                            )
                            Pair(insert1, insert2Prime)
                        } else {
                            val insert1Prime = insert1.copy(
                                position = insert1.position + insert2.content.length
                            )
                            Pair(insert1Prime, insert2)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Transform insert operation against delete operation
     */
    private fun transformInsertDelete(
        insert: CollaborativeOperation.Insert,
        delete: CollaborativeOperation.Delete
    ): Pair<CollaborativeOperation.Insert, CollaborativeOperation.Delete> {
        
        return when {
            insert.position <= delete.position -> {
                // Insert comes before delete, delete position shifts right
                val deletePrime = delete.copy(
                    position = delete.position + insert.content.length
                )
                Pair(insert, deletePrime)
            }
            insert.position >= delete.position + delete.length -> {
                // Insert comes after delete, insert position shifts left
                val insertPrime = insert.copy(
                    position = insert.position - delete.length
                )
                Pair(insertPrime, delete)
            }
            else -> {
                // Insert is within delete range, adjust both operations
                val insertPrime = insert.copy(
                    position = delete.position
                )
                val deleteLength = delete.length + insert.content.length
                val deletePrime = delete.copy(
                    length = deleteLength
                )
                Pair(insertPrime, deletePrime)
            }
        }
    }
    
    /**
     * Transform two concurrent delete operations
     */
    private fun transformDeleteDelete(
        delete1: CollaborativeOperation.Delete,
        delete2: CollaborativeOperation.Delete
    ): Pair<CollaborativeOperation.Delete, CollaborativeOperation.Delete> {
        
        val d1Start = delete1.position
        val d1End = delete1.position + delete1.length
        val d2Start = delete2.position
        val d2End = delete2.position + delete2.length
        
        return when {
            d1End <= d2Start -> {
                // delete1 comes completely before delete2
                val delete2Prime = delete2.copy(
                    position = delete2.position - delete1.length
                )
                Pair(delete1, delete2Prime)
            }
            d2End <= d1Start -> {
                // delete2 comes completely before delete1
                val delete1Prime = delete1.copy(
                    position = delete1.position - delete2.length
                )
                Pair(delete1Prime, delete2)
            }
            else -> {
                // Overlapping deletes - merge them
                val mergedStart = min(d1Start, d2Start)
                val mergedEnd = max(d1End, d2End)
                val mergedLength = mergedEnd - mergedStart
                
                val delete1Prime = delete1.copy(
                    position = mergedStart,
                    length = mergedLength
                )
                
                // delete2 becomes a no-op since it's merged into delete1
                val delete2Prime = delete2.copy(
                    position = mergedStart,
                    length = 0
                )
                
                Pair(delete1Prime, delete2Prime)
            }
        }
    }
    
    /**
     * Transform complex operations (Retain, Format)
     */
    private fun transformComplexOperations(
        operation1: CollaborativeOperation,
        operation2: CollaborativeOperation,
        priority: TransformPriority
    ): Pair<CollaborativeOperation, CollaborativeOperation> {
        
        // For now, return operations as-is
        // In a full implementation, this would handle Retain and Format operations
        return Pair(operation1, operation2)
    }
    
    /**
     * Apply a sequence of operations to text content
     */
    fun applyOperations(
        content: String,
        operations: List<CollaborativeOperation>
    ): String {
        var result = content
        val sortedOps = operations.sortedBy { it.timestamp }
        
        for (operation in sortedOps) {
            result = applyOperation(result, operation)
        }
        
        return result
    }
    
    /**
     * Apply a single operation to text content
     */
    fun applyOperation(
        content: String,
        operation: CollaborativeOperation
    ): String {
        return when (operation) {
            is CollaborativeOperation.Insert -> {
                val position = operation.position.coerceIn(0, content.length)
                content.substring(0, position) + operation.content + content.substring(position)
            }
            is CollaborativeOperation.Delete -> {
                val start = operation.position.coerceIn(0, content.length)
                val end = (start + operation.length).coerceIn(start, content.length)
                content.substring(0, start) + content.substring(end)
            }
            is CollaborativeOperation.Retain -> {
                // Retain operations don't change content, just maintain position
                content
            }
            is CollaborativeOperation.Format -> {
                // Format operations don't change text content in this simple implementation
                content
            }
        }
    }
    
    /**
     * Generate operation from text changes
     */
    fun generateOperation(
        userId: String,
        oldContent: String,
        newContent: String,
        version: Int
    ): List<CollaborativeOperation> {
        val operations = mutableListOf<CollaborativeOperation>()
        val diff = computeDiff(oldContent, newContent)
        
        for (change in diff) {
            val operationId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            
            when (change.type) {
                DiffType.INSERT -> {
                    operations.add(
                        CollaborativeOperation.Insert(
                            operationId = operationId,
                            userId = userId,
                            timestamp = timestamp,
                            version = version,
                            position = change.position,
                            content = change.content
                        )
                    )
                }
                DiffType.DELETE -> {
                    operations.add(
                        CollaborativeOperation.Delete(
                            operationId = operationId,
                            userId = userId,
                            timestamp = timestamp,
                            version = version,
                            position = change.position,
                            length = change.length
                        )
                    )
                }
            }
        }
        
        return operations
    }
    
    /**
     * Compute diff between two strings
     */
    private fun computeDiff(oldContent: String, newContent: String): List<DiffChange> {
        // Simple diff implementation - in production, use a more sophisticated algorithm
        val changes = mutableListOf<DiffChange>()
        
        if (oldContent == newContent) {
            return changes
        }
        
        // Find common prefix
        var commonPrefixLength = 0
        val minLength = min(oldContent.length, newContent.length)
        while (commonPrefixLength < minLength && 
               oldContent[commonPrefixLength] == newContent[commonPrefixLength]) {
            commonPrefixLength++
        }
        
        // Find common suffix
        var commonSuffixLength = 0
        val oldSuffixStart = oldContent.length - 1
        val newSuffixStart = newContent.length - 1
        while (commonSuffixLength < minLength - commonPrefixLength &&
               oldContent[oldSuffixStart - commonSuffixLength] == 
               newContent[newSuffixStart - commonSuffixLength]) {
            commonSuffixLength++
        }
        
        val oldMiddle = oldContent.substring(
            commonPrefixLength, 
            oldContent.length - commonSuffixLength
        )
        val newMiddle = newContent.substring(
            commonPrefixLength, 
            newContent.length - commonSuffixLength
        )
        
        // Generate operations for the middle part
        if (oldMiddle.isNotEmpty()) {
            changes.add(
                DiffChange(
                    type = DiffType.DELETE,
                    position = commonPrefixLength,
                    length = oldMiddle.length,
                    content = oldMiddle
                )
            )
        }
        
        if (newMiddle.isNotEmpty()) {
            changes.add(
                DiffChange(
                    type = DiffType.INSERT,
                    position = commonPrefixLength,
                    length = newMiddle.length,
                    content = newMiddle
                )
            )
        }
        
        return changes
    }
    
    /**
     * Validate operation sequence for consistency
     */
    fun validateOperations(operations: List<CollaborativeOperation>): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check for valid positions
        for (operation in operations) {
            when (operation) {
                is CollaborativeOperation.Insert -> {
                    if (operation.position < 0) {
                        errors.add("Insert operation has negative position: ${operation.position}")
                    }
                    if (operation.content.isEmpty()) {
                        errors.add("Insert operation has empty content")
                    }
                }
                is CollaborativeOperation.Delete -> {
                    if (operation.position < 0) {
                        errors.add("Delete operation has negative position: ${operation.position}")
                    }
                    if (operation.length <= 0) {
                        errors.add("Delete operation has invalid length: ${operation.length}")
                    }
                }
                is CollaborativeOperation.Retain -> {
                    if (operation.length < 0) {
                        errors.add("Retain operation has negative length: ${operation.length}")
                    }
                }
                is CollaborativeOperation.Format -> {
                    if (operation.startPosition < 0 || operation.endPosition < operation.startPosition) {
                        errors.add("Format operation has invalid range: ${operation.startPosition}-${operation.endPosition}")
                    }
                }
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Priority rules for operation transformation
 */
enum class TransformPriority {
    LEFT_WINS,      // First operation takes priority
    RIGHT_WINS,     // Second operation takes priority
    TIMESTAMP,      // Earlier timestamp wins
    USER_ID         // Lexicographically smaller user ID wins
}

/**
 * Types of text differences
 */
enum class DiffType {
    INSERT,
    DELETE
}

/**
 * Represents a change in text content
 */
data class DiffChange(
    val type: DiffType,
    val position: Int,
    val length: Int,
    val content: String
)

/**
 * Result of operation validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
