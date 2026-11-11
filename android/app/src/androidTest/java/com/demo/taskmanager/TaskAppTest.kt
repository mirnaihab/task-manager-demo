package com.demo.taskmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.demo.taskmanager.FakeTaskRepository.Companion.task
import com.demo.taskmanager.data.ApiError
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.ui.TaskApp
import com.demo.taskmanager.ui.TaskListViewModel
import com.demo.taskmanager.ui.theme.TaskManagerTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test

class TaskAppTest {
    @get:Rule
    val rule = createComposeRule()

    private fun launch(initial: List<Task> = emptyList()): FakeTaskRepository {
        val repository = FakeTaskRepository(initial)
        launch(repository)
        return repository
    }

    private fun launch(repository: FakeTaskRepository) {
        val viewModel = TaskListViewModel(repository, Dispatchers.Main.immediate) { FakeTaskRepository.TODAY }
        rule.setContent { TaskManagerTheme { TaskApp(viewModel) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithTagCount("loading") == 0 }
    }

    private fun ComposeContentTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size

    private fun waitForText(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForNoText(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

    @Test
    fun createTask() {
        launch()
        rule.onNodeWithTag("add_task").performClick()
        rule.onNodeWithTag("field_title").performTextInput("Pay rent")
        rule.onNodeWithTag("form_save").performClick()
        waitForText("Task created")
        rule.onNodeWithText("Pay rent").assertIsDisplayed()
    }

    @Test
    fun titleTooLongShowsErrorAndDoesNotSend() {
        val repository = launch()
        rule.onNodeWithTag("add_task").performClick()
        rule.onNodeWithTag("field_title").performTextInput("a".repeat(101))
        rule.onNodeWithText("Title must be 100 characters or fewer").assertIsDisplayed()
        rule.onNodeWithTag("form_save").performClick()
        rule.waitForIdle()
        assert("create" !in repository.calls)
    }

    @Test
    fun serverFieldErrorShowsUnderDueDateAndKeepsFormOpen() {
        val repository = launch()
        rule.onNodeWithTag("add_task").performClick()
        rule.onNodeWithTag("field_title").performTextInput("Pay rent")
        repository.failNext(ApiError.Validation(mapOf("dueDate" to "Due date is invalid")))
        rule.onNodeWithTag("form_save").performClick()
        waitForText("Due date is invalid")
        rule.onNodeWithTag("field_title").assertIsDisplayed()
    }

    @Test
    fun filterShowsOnlyMatchingRow() {
        launch(
            listOf(
                task("1", title = "Match", priority = Priority.HIGH, status = Status.TODO),
                task("2", title = "Other", priority = Priority.LOW, status = Status.DONE),
            ),
        )
        rule.onNodeWithTag("status_chip_To Do").performClick()
        rule.onNodeWithTag("priority_chip_High").performClick()
        waitForNoText("Other")
        rule.onNodeWithText("Match").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsMessageAndClearFilters() {
        launch(listOf(task("1", status = Status.TODO)))
        rule.onNodeWithTag("status_chip_Done").performClick()
        waitForText("No tasks match these filters.")
        rule.onNodeWithText("Clear filters").performClick()
        waitForNoText("No tasks match these filters.")
        rule.onNodeWithText("Task 1").assertIsDisplayed()
    }

    @Test
    fun emptyListWithoutFiltersShowsAddHint() {
        launch()
        rule.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()
    }

    @Test
    fun statusChangeUpdatesRowAndSummary() {
        launch(listOf(task("1")))
        rule.onNodeWithTag("status_1").performClick()
        rule.onNodeWithTag("status_option_Done").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Done") and hasTestTag("status_1"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("count_done").assertTextContains("1") }.isSuccess
        }
    }

    @Test
    fun failedStatusChangeRevertsAndShowsMessage() {
        val repository = launch(listOf(task("1")))
        repository.failNext(ApiError.Server())
        rule.onNodeWithTag("status_1").performClick()
        rule.onNodeWithTag("status_option_Done").performClick()
        waitForText("Something went wrong. Try again.")
        rule.onNodeWithTag("status_1").assertTextEquals("To Do")
    }

    @Test
    fun editTaskChangesTitle() {
        launch(listOf(task("1", title = "Old title")))
        rule.onNodeWithText("Old title").performClick()
        rule.onNodeWithTag("field_title").performTextClearance()
        rule.onNodeWithTag("field_title").performTextInput("New title")
        rule.onNodeWithTag("form_save").performClick()
        waitForText("Task updated")
        rule.onNodeWithText("New title").assertIsDisplayed()
    }

    @Test
    fun deleteCancelKeepsRow() {
        launch(listOf(task("1", title = "Keep me")))
        rule.onNodeWithTag("delete_1").performClick()
        rule.onNodeWithText("Delete task?").assertIsDisplayed()
        rule.onNodeWithTag("delete_cancel").performClick()
        rule.onNodeWithText("Keep me").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmRemovesRowAndLowersTotal() {
        launch(listOf(task("1", title = "Remove me"), task("2", title = "Stay")))
        rule.onNodeWithTag("delete_1").performClick()
        rule.onNodeWithTag("delete_confirm").performClick()
        waitForNoText("Remove me")
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("count_total").assertTextContains("1") }.isSuccess
        }
    }

    @Test
    fun overdueRowShowsOverdueText() {
        launch(listOf(task("1", dueDate = "2026-10-01")))
        rule.onNodeWithText("Overdue").assertIsDisplayed()
    }

    @Test
    fun emptySummaryShowsZeros() {
        launch()
        rule.onNodeWithTag("count_total").assertTextContains("0")
        rule.onNodeWithTag("count_todo").assertTextContains("0")
        rule.onNodeWithTag("count_done").assertTextContains("0")
        rule.onNodeWithText("Overdue: 0").assertIsDisplayed()
    }

    @Test
    fun failedLoadShowsErrorStateWithRetry() {
        val repository = FakeTaskRepository(listOf(task("1")))
        repository.failNext(ApiError.Network())
        launch(repository)
        rule.onNodeWithText("Cannot reach the server.").assertIsDisplayed()
        rule.onAllNodes(hasTestTag("summary_panel")).assertCountEquals(0)
        rule.onNodeWithText("Try again").performClick()
        waitForText("Task 1")
    }

    @Test
    fun slowFirstLoadShowsWakingHintThenTheList() {
        val repository = FakeTaskRepository(listOf(task("1")))
        repository.failNext(ApiError.Unavailable())
        val viewModel = TaskListViewModel(repository, Dispatchers.Main.immediate) { FakeTaskRepository.TODAY }
        rule.setContent { TaskManagerTheme { TaskApp(viewModel) } }
        rule.onAllNodes(hasTestTag("waking-hint")).assertCountEquals(0)
        rule.waitUntil(8_000) { rule.onAllNodesWithTagCount("waking-hint") == 1 }
        rule.onNodeWithText("Waking up the server. This can take up to a minute.").assertIsDisplayed()
        rule.waitUntil(10_000) { rule.onAllNodesWithTagCount("loading") == 0 }
        waitForText("Task 1")
        rule.onAllNodes(hasTestTag("waking-hint")).assertCountEquals(0)
    }

    @Test
    fun saveButtonIsDisabledWhileSaving() {
        val repository = FakeTaskRepository().apply { gate = CompletableDeferred() }
        launch(repository)
        rule.onNodeWithTag("add_task").performClick()
        rule.onNodeWithTag("field_title").performTextInput("Pay rent")
        rule.onNodeWithTag("form_save").performClick()
        rule.waitUntil(5_000) { "create" in repository.calls }
        rule.onNodeWithTag("form_save").assertIsNotEnabled()
        repository.gate!!.complete(Unit)
        waitForText("Task created")
    }
}
