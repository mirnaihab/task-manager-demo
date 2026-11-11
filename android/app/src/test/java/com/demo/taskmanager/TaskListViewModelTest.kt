package com.demo.taskmanager

import com.demo.taskmanager.FakeTaskRepository.Companion.task
import com.demo.taskmanager.data.ApiError
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.TaskDraft
import com.demo.taskmanager.ui.Phase
import com.demo.taskmanager.ui.TaskListViewModel
import com.demo.taskmanager.ui.TitleError
import com.demo.taskmanager.ui.UiMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repository: FakeTaskRepository): TaskListViewModel {
        val vm = TaskListViewModel(repository, dispatcher) { FakeTaskRepository.TODAY }
        advanceUntilIdle()
        return vm
    }

    private val validDraft = TaskDraft("Pay rent", "", Priority.MEDIUM, FakeTaskRepository.TODAY)

    @Test
    fun req201_loadsTasksAndSummary() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1"), task("2", status = Status.DONE))))
        val state = vm.state.value
        assertEquals(Phase.Content, state.phase)
        assertEquals(2, state.tasks.size)
        assertEquals(2, state.summary.total)
        assertEquals(1, state.summary.done)
    }

    @Test
    fun req201_startsInLoadingPhase() = runTest(dispatcher) {
        val vm = TaskListViewModel(FakeTaskRepository(), dispatcher) { FakeTaskRepository.TODAY }
        assertEquals(Phase.Loading, vm.state.value.phase)
    }

    @Test
    fun req203_filtersCombine() = runTest(dispatcher) {
        val repo = FakeTaskRepository(
            listOf(
                task("1", priority = Priority.HIGH, status = Status.TODO),
                task("2", priority = Priority.LOW, status = Status.DONE),
                task("3", priority = Priority.HIGH, status = Status.DONE),
            ),
        )
        val vm = viewModel(repo)
        vm.setStatusFilter(Status.TODO)
        vm.setPriorityFilter(Priority.HIGH)
        advanceUntilIdle()
        assertEquals(listOf("1"), vm.state.value.tasks.map { it.id })
    }

    @Test
    fun req204_clearFiltersReloadsEverything() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1"), task("2", status = Status.DONE))))
        vm.setStatusFilter(Status.IN_PROGRESS)
        advanceUntilIdle()
        assertTrue(vm.state.value.tasks.isEmpty())
        vm.clearFilters()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.tasks.size)
        assertFalse(vm.state.value.filters.isActive)
    }

    @Test
    fun edgeP2001_newestFilterWins() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1", status = Status.TODO), task("2", status = Status.DONE))))
        vm.setStatusFilter(Status.TODO)
        vm.setStatusFilter(Status.DONE)
        advanceUntilIdle()
        assertEquals(listOf("2"), vm.state.value.tasks.map { it.id })
    }

    @Test
    fun req205_openCreateUsesDefaults() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository())
        vm.openCreate()
        val draft = vm.state.value.form!!.draft
        assertEquals("", draft.title)
        assertEquals(Priority.MEDIUM, draft.priority)
        assertEquals(FakeTaskRepository.TODAY, draft.dueDate)
    }

    @Test
    fun req206_saveCreatesClosesFormAndRefreshes() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository())
        vm.openCreate()
        vm.updateDraft(validDraft)
        vm.save()
        advanceUntilIdle()
        val state = vm.state.value
        assertNull(state.form)
        assertEquals(UiMessage.TaskCreated, state.message)
        assertEquals(listOf("Pay rent"), state.tasks.map { it.title })
        assertEquals(1, state.summary.total)
    }

    @Test
    fun req206_saveEditUpdatesTask() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1", title = "Old"))))
        vm.openEdit(vm.state.value.tasks.first())
        vm.updateDraft(vm.state.value.form!!.draft.copy(title = "New"))
        vm.save()
        advanceUntilIdle()
        assertEquals(UiMessage.TaskUpdated, vm.state.value.message)
        assertEquals("New", vm.state.value.tasks.first().title)
    }

    @Test
    fun req207_validationFieldsStayOnForm() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft)
        repo.failNext(ApiError.Validation(mapOf("dueDate" to "Due date must be a real date")))
        vm.save()
        advanceUntilIdle()
        val form = vm.state.value.form!!
        assertEquals("Due date must be a real date", form.serverErrors["dueDate"])
        assertFalse(form.isSaving)
    }

    @Test
    fun req208_blankTitleIsRejectedOnDevice() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft.copy(title = "   "))
        vm.save()
        advanceUntilIdle()
        assertEquals(TitleError.Required, vm.state.value.form!!.titleError)
        assertFalse(repo.calls.contains("create"))
    }

    @Test
    fun req208_titleOver100IsRejectedOnDevice() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft.copy(title = "a".repeat(101)))
        assertEquals(TitleError.TooLong, vm.state.value.form!!.titleError)
        vm.save()
        advanceUntilIdle()
        assertFalse(repo.calls.contains("create"))
    }

    @Test
    fun req208_title100WithPaddingIsAccepted() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft.copy(title = "  " + "a".repeat(100) + "  "))
        vm.save()
        advanceUntilIdle()
        assertTrue(repo.calls.contains("create"))
    }

    @Test
    fun edgeP2003_secondSaveWhileSavingSendsOneRequest() = runTest(dispatcher) {
        val repo = FakeTaskRepository().apply { gate = CompletableDeferred() }
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft)
        vm.save()
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.calls.count { it == "create" })
        assertTrue(vm.state.value.form!!.isSaving)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertNull(vm.state.value.form)
    }

    @Test
    fun edgeP2004_editOfDeletedTaskClosesFormWithMessage() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        val vm = viewModel(repo)
        vm.openEdit(vm.state.value.tasks.first())
        repo.failNext(ApiError.NotFound())
        vm.save()
        advanceUntilIdle()
        assertNull(vm.state.value.form)
        assertEquals(UiMessage.TaskGone, vm.state.value.message)
    }

    @Test
    fun edgeP2009_networkFailureOnSaveKeepsFormOpen() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft)
        repo.failNext(ApiError.Network())
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.state.value.form)
        assertEquals(UiMessage.NetworkError, vm.state.value.message)
        assertEquals("Pay rent", vm.state.value.form!!.draft.title)
    }

    @Test
    fun req210_changeStatusUpdatesRowAndSummary() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1"))))
        vm.changeStatus(vm.state.value.tasks.first(), Status.DONE)
        assertEquals(Status.DONE, vm.state.value.tasks.first().status)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.summary.done)
    }

    @Test
    fun req210_failedStatusChangeRevertsRow() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        val vm = viewModel(repo)
        repo.failNext(ApiError.Server())
        vm.changeStatus(vm.state.value.tasks.first(), Status.DONE)
        advanceUntilIdle()
        assertEquals(Status.TODO, vm.state.value.tasks.first().status)
        assertEquals(UiMessage.ServerError, vm.state.value.message)
    }

    @Test
    fun req211_cancelDeleteChangesNothing() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        val vm = viewModel(repo)
        vm.askDelete(vm.state.value.tasks.first())
        assertNotNull(vm.state.value.pendingDelete)
        vm.cancelDelete()
        advanceUntilIdle()
        assertNull(vm.state.value.pendingDelete)
        assertEquals(1, vm.state.value.tasks.size)
        assertFalse(repo.calls.contains("delete"))
    }

    @Test
    fun req211_confirmDeleteRemovesRowAndUpdatesSummary() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1"), task("2"))))
        vm.askDelete(vm.state.value.tasks.first())
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("2"), vm.state.value.tasks.map { it.id })
        assertEquals(1, vm.state.value.summary.total)
        assertEquals(UiMessage.TaskDeleted, vm.state.value.message)
    }

    @Test
    fun req211_failedDeleteKeepsRowAndShowsMessage() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        val vm = viewModel(repo)
        vm.askDelete(vm.state.value.tasks.first())
        repo.failNext(ApiError.Network())
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.tasks.size)
        assertEquals(UiMessage.NetworkError, vm.state.value.message)
    }

    @Test
    fun req212_emptySummaryHasZeros() = runTest(dispatcher) {
        val state = viewModel(FakeTaskRepository()).state.value
        assertEquals(0, state.summary.total)
        assertEquals(0, state.summary.overdue)
        assertEquals(Phase.Content, state.phase)
    }

    @Test
    fun req213_overdueComesFromServerData() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1", dueDate = "2026-10-01"))))
        assertTrue(vm.state.value.tasks.first().overdue)
        assertEquals(1, vm.state.value.summary.overdue)
    }

    @Test
    fun req214_failedLoadShowsErrorStateThenRetryRecovers() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        repo.failNext(ApiError.Network())
        val vm = viewModel(repo)
        assertEquals(Phase.Error, vm.state.value.phase)
        vm.retry()
        advanceUntilIdle()
        assertEquals(Phase.Content, vm.state.value.phase)
        assertEquals(1, vm.state.value.tasks.size)
    }

    // Cold start (REQ-408 to REQ-411). Delays run in virtual time, so these tests do not wait.
    private val FakeTaskRepository.listCalls get() = calls.count { it == "list" }

    @Test
    fun req408_unavailableLoadRetriesUntilTheServerAnswers() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        repo.failNext(ApiError.Unavailable(), times = 3)
        val vm = viewModel(repo)
        assertEquals(Phase.Content, vm.state.value.phase)
        assertEquals(1, vm.state.value.tasks.size)
        assertEquals(4, repo.listCalls)
    }

    @Test
    fun req411_unavailableForTheWholeBudgetEndsInTheErrorState() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        repo.failNext(ApiError.Unavailable(), times = 1_000)
        val vm = viewModel(repo)
        assertEquals(Phase.Error, vm.state.value.phase)
        assertFalse(vm.state.value.isWaking)
        assertEquals(19, repo.listCalls)
    }

    @Test
    fun edgeP4003_offlineFailsAtOnceWithoutRetry() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        repo.failNext(ApiError.Network(), times = 1_000)
        val vm = viewModel(repo)
        assertEquals(Phase.Error, vm.state.value.phase)
        assertEquals(1, repo.listCalls)
    }

    @Test
    fun req409_hintShowsAfterFourSecondsAndClearsWhenTheListLoads() = runTest(dispatcher) {
        val repo = FakeTaskRepository(listOf(task("1")))
        repo.failNext(ApiError.Unavailable(), times = 3)
        val vm = TaskListViewModel(repo, dispatcher) { FakeTaskRepository.TODAY }
        advanceTimeBy(3_999)
        runCurrent()
        assertFalse(vm.state.value.isWaking)
        advanceTimeBy(2)
        runCurrent()
        assertTrue(vm.state.value.isWaking)
        assertEquals(Phase.Loading, vm.state.value.phase)
        advanceUntilIdle()
        assertFalse(vm.state.value.isWaking)
        assertEquals(Phase.Content, vm.state.value.phase)
    }

    @Test
    fun req409_noHintWhenTheLoadIsFast() = runTest(dispatcher) {
        val vm = viewModel(FakeTaskRepository(listOf(task("1"))))
        advanceTimeBy(10_000)
        runCurrent()
        assertFalse(vm.state.value.isWaking)
    }

    @Test
    fun req410_unavailableWriteIsNotRetried() = runTest(dispatcher) {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.updateDraft(validDraft)
        repo.failNext(ApiError.Unavailable(), times = 1_000)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.calls.count { it == "create" })
        assertNotNull(vm.state.value.form)
        assertEquals(UiMessage.NetworkError, vm.state.value.message)
    }
}
