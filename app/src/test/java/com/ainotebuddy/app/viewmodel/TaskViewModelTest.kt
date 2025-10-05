package com.ainotebuddy.app.viewmodel

import app.cash.turbine.test
import com.ainotebuddy.app.MainCoroutineRule
import com.ainotebuddy.app.data.model.Task
import com.ainotebuddy.app.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var taskRepository: TaskRepository
    private lateinit var viewModel: TaskViewModel
    private val testNoteId = 1L
    private val testTask = Task(
        id = 1,
        noteId = testNoteId,
        title = "Test Task",
        isCompleted = false,
        position = 0,
        createdAt = Date(),
        updatedAt = Date()
    )

    @Before
    fun setup() {
        taskRepository = mock()
        viewModel = TaskViewModel(taskRepository)
    }

    @Test
    fun `loadTasks should update UI state with tasks`() = runTest {
        // Given
        val tasks = listOf(testTask)
        whenever(taskRepository.getTasksForNote(testNoteId)).thenReturn(flowOf(tasks))
        whenever(taskRepository.getTaskCountForNote(testNoteId)).thenReturn(
            TaskCount(total = 1, completed = 0)
        )

        // When
        viewModel.loadTasks(testNoteId)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.tasks.size)
            assertEquals("Test Task", state.tasks[0].title)
            assertEquals(1, state.totalTasks)
            assertEquals(0, state.completedTasks)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addTask should call repository and update state`() = runTest {
        // Given
        val newTaskTitle = "New Task"
        val newTask = testTask.copy(id = 2, title = newTaskTitle)
        
        doAnswer {
            viewModel.loadTasks(testNoteId)
            Unit
        }.whenever(taskRepository).addTask(any())

        // When
        viewModel.addTask(newTaskTitle)

        // Then
        verify(taskRepository).addTask(argThat { 
            title == newTaskTitle && noteId == testNoteId 
        })
    }

    @Test
    fun `toggleTaskCompletion should update task status`() = runTest {
        // Given
        val completedTask = testTask.copy(isCompleted = true)
        
        doAnswer {
            viewModel.loadTasks(testNoteId)
            Unit
        }.whenever(taskRepository).updateTask(completedTask)

        // When
        viewModel.toggleTaskCompletion(testTask)

        // Then
        verify(taskRepository).updateTask(argThat { isCompleted })
    }

    @Test
    fun `deleteTask should remove task from repository`() = runTest {
        // Given
        doAnswer {
            viewModel.loadTasks(testNoteId)
            Unit
        }.whenever(taskRepository).deleteTask(testTask)

        // When
        viewModel.deleteTask(testTask)

        // Then
        verify(taskRepository).deleteTask(testTask)
    }

    @Test
    fun `deleteCompletedTasks should remove completed tasks`() = runTest {
        // Given
        doAnswer {
            viewModel.loadTasks(testNoteId)
            Unit
        }.whenever(taskRepository).deleteCompletedTasks(testNoteId)

        // When
        viewModel.deleteCompletedTasks()

        // Then
        verify(taskRepository).deleteCompletedTasks(testNoteId)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        // Given
        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertNull(initialState.error)
            
            // When error occurs
            viewModel.addTask("")
            
            // Clear error
            viewModel.clearError()
            
            // Then
            val stateAfterClear = awaitItem()
            assertNull(stateAfterClear.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
