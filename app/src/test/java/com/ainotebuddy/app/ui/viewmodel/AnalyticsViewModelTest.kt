package com.ainotebuddy.app.ui.viewmodel

import com.ainotebuddy.app.MainCoroutineRule
import com.ainotebuddy.app.data.FakeAnalyticsRepository
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.TagUsage
import com.ainotebuddy.app.data.repository.ExportFormat
import com.ainotebuddy.app.ui.components.analytics.TimeRange
import com.ainotebuddy.app.util.getDatesInRange
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: AnalyticsViewModel
    private lateinit var repository: FakeAnalyticsRepository

    private val testDate = LocalDate.now()
    private val testTag = "test-tag"
    private val testNoteId = 1L

    @Before
    fun setup() {
        repository = FakeAnalyticsRepository()
        viewModel = AnalyticsViewModel(repository)
    }

    @Test
    fun `loadAnalyticsData with default time range loads data successfully`() = runTest {
        // Given - Initial state
        assertThat(viewModel.uiState.value).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.Loading::class.java)

        // When - Load data
        viewModel.loadAnalyticsData()

        // Then - Verify success state
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.Success::class.java)
    }

    @Test
    fun `loadAnalyticsData with custom time range updates data`() = runTest {
        // Given - Initial state
        val timeRange = TimeRange.WEEK

        // When - Load data with custom time range
        viewModel.loadAnalyticsData(timeRange)

        // Then - Verify time range is updated and data is loaded
        assertThat(viewModel.selectedTimeRange.value).isEqualTo(timeRange)
        assertThat(viewModel.uiState.value).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.Success::class.java)
    }

    @Test
    fun `loadAnalyticsData with error shows error state`() = runTest {
        // Given - Repository that throws an error
        val errorMessage = "Test error"
        repository.shouldThrowError = true
        repository.errorMessage = errorMessage

        // When - Load data
        viewModel.loadAnalyticsData()

        // Then - Verify error state
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.Error::class.java)
        assertThat((state as AnalyticsViewModel.AnalyticsUiState.Error).message).contains(errorMessage)
    }

    @Test
    fun `trackNoteView updates analytics data`() = runTest {
        // Given - Initial state
        val initialViewCount = viewModel.noteStats.value[ActivityType.VIEWED] ?: 0

        // When - Track a note view
        viewModel.trackNoteView(testNoteId)

        // Then - Verify view count is incremented
        val updatedViewCount = viewModel.noteStats.value[ActivityType.VIEWED] ?: 0
        assertThat(updatedViewCount).isGreaterThan(initialViewCount)
    }

    @Test
    fun `exportReport with valid data returns success`() = runTest {
        // Given - Valid data in the view model
        viewModel.loadAnalyticsData()

        // When - Export report
        viewModel.exportReport(
            format = ExportFormat.PDF,
            startDate = testDate.minusDays(7),
            endDate = testDate
        )

        // Then - Verify export success
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.ExportSuccess::class.java)
    }

    @Test
    fun `exportReport with error shows export error`() = runTest {
        // Given - Repository that throws an error during export
        repository.shouldThrowError = true
        val errorMessage = "Export failed"
        repository.errorMessage = errorMessage

        // When - Export report
        viewModel.exportReport(ExportFormat.PDF)

        // Then - Verify export error
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.ExportError::class.java)
        assertThat((state as AnalyticsViewModel.AnalyticsUiState.ExportError).message).contains(errorMessage)
    }

    @Test
    fun `refreshData reloads analytics data`() = runTest {
        // Given - Initial data loaded
        viewModel.loadAnalyticsData()
        val initialTagUsage = viewModel.tagUsage.first()

        // When - Add a new tag and refresh
        repository.addTagUsage(TagUsage("new-tag", 5))
        viewModel.refreshData()

        // Then - Verify data is refreshed
        val updatedTagUsage = viewModel.tagUsage.first()
        assertThat(updatedTagUsage).hasSize(initialTagUsage.size + 1)
        assertThat(updatedTagUsage.any { it.tag == "new-tag" }).isTrue()
    }

    @Test
    fun `loadAnalyticsData updates activity heatmap`() = runTest {
        // Given - Test activity data
        val testActivities = listOf(
            NoteActivity(
                noteId = 1,
                activityType = ActivityType.VIEWED,
                timestamp = System.currentTimeMillis(),
                durationMs = 1000
            )
        )
        repository.setTestActivities(testActivities)

        // When - Load data
        viewModel.loadAnalyticsData()

        // Then - Verify heatmap is updated
        val heatmap = viewModel.activityHeatmap.first()
        assertThat(heatmap).isNotEmpty()
    }

    @Test
    fun `loadAnalyticsData updates tag usage`() = runTest {
        // Given - Test tag usage
        val testTags = listOf(
            TagUsage("tag1", 5),
            TagUsage("tag2", 3)
        )
        repository.setTestTagUsage(testTags)

        // When - Load data
        viewModel.loadAnalyticsData()

        // Then - Verify tag usage is updated
        val tags = viewModel.tagUsage.first()
        assertThat(tags).hasSize(2)
        assertThat(tags[0].tag).isEqualTo("tag1")
        assertThat(tags[0].count).isEqualTo(5)
    }

    @Test
    fun `time range selection updates data`() = runTest {
        // Given - Initial data loaded with default time range
        viewModel.loadAnalyticsData()
        val initialTagUsage = viewModel.tagUsage.first()

        // When - Change time range
        val newTimeRange = TimeRange.MONTH
        viewModel.loadAnalyticsData(newTimeRange)

        // Then - Verify time range is updated and data is reloaded
        assertThat(viewModel.selectedTimeRange.value).isEqualTo(newTimeRange)
        val updatedTagUsage = viewModel.tagUsage.first()
        assertThat(updatedTagUsage).isNotEqualTo(initialTagUsage)
    }

    @Test
    fun `exportReport with CSV format generates CSV report`() = runTest {
        // Given - Test data
        val testTags = listOf(TagUsage("test", 1))
        repository.setTestTagUsage(testTags)
        viewModel.loadAnalyticsData()

        // When - Export as CSV
        viewModel.exportReport(ExportFormat.CSV)

        // Then - Verify CSV export was triggered
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.ExportSuccess::class.java)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        // Given - Error state
        repository.shouldThrowError = true
        viewModel.loadAnalyticsData()
        assertThat(viewModel.uiState.value).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.Error::class.java)

        // When - Clear error
        repository.shouldThrowError = false
        viewModel.clearError()

        // Then - Verify error is cleared
        assertThat(viewModel.uiState.value).isInstanceOf(AnalyticsViewModel.AnalyticsUiState.Success::class.java)
    }
}
