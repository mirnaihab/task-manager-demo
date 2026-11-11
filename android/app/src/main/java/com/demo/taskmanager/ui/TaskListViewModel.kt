package com.demo.taskmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.demo.taskmanager.data.ApiError
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.Summary
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.data.TaskDraft
import com.demo.taskmanager.data.TaskFilters
import com.demo.taskmanager.data.TaskRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val repository: TaskRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val today: () -> String = { LocalDate.now().toString() },
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
    }

    fun retry() {
        _state.update { it.copy(phase = Phase.Loading) }
        reload()
    }

    fun setStatusFilter(status: Status?) {
        _state.update { it.copy(filters = it.filters.copy(status = status)) }
        reload()
    }

    fun setPriorityFilter(priority: Priority?) {
        _state.update { it.copy(filters = it.filters.copy(priority = priority)) }
        reload()
    }

    fun clearFilters() {
        _state.update { it.copy(filters = TaskFilters()) }
        reload()
    }

    fun openCreate() {
        val draft = TaskDraft(title = "", description = "", priority = Priority.MEDIUM, dueDate = today())
        _state.update { it.copy(form = FormState(editingId = null, draft = draft)) }
    }

    fun openEdit(task: Task) {
        val draft = TaskDraft(task.title, task.description.orEmpty(), task.priority, task.dueDate)
        _state.update { it.copy(form = FormState(editingId = task.id, draft = draft)) }
    }

    fun closeForm() {
        _state.update { it.copy(form = null) }
    }

    fun updateDraft(draft: TaskDraft) {
        _state.update { state ->
            val form = state.form ?: return@update state
            val tooLong = draft.title.trim().length > TITLE_MAX_LENGTH
            state.copy(
                form = form.copy(
                    draft = draft,
                    titleError = if (tooLong) TitleError.TooLong else null,
                    serverErrors = emptyMap(),
                ),
            )
        }
    }

    fun save() {
        val form = _state.value.form ?: return
        if (form.isSaving) return
        val titleError = validateTitle(form.draft.title)
        if (titleError != null) {
            _state.update { it.copy(form = form.copy(titleError = titleError)) }
            return
        }
        _state.update { it.copy(form = form.copy(isSaving = true, serverErrors = emptyMap())) }
        viewModelScope.launch(dispatcher) {
            try {
                val id = form.editingId
                if (id == null) repository.create(form.draft) else repository.update(id, form.draft)
                val message = if (id == null) UiMessage.TaskCreated else UiMessage.TaskUpdated
                _state.update { it.copy(form = null, message = message) }
                refreshAfterChange()
            } catch (e: ApiError.Validation) {
                _state.update { it.copy(form = form.copy(serverErrors = e.fields, isSaving = false)) }
            } catch (_: ApiError.NotFound) {
                _state.update { it.copy(form = null, message = UiMessage.TaskGone) }
                refreshAfterChange()
            } catch (e: ApiError) {
                _state.update { it.copy(form = form.copy(isSaving = false), message = e.toMessage()) }
            }
        }
    }

    fun changeStatus(task: Task, status: Status) {
        if (task.status == status) return
        _state.update { it.copy(tasks = it.tasks.replaceById(task.copy(status = status))) }
        viewModelScope.launch(dispatcher) {
            try {
                repository.setStatus(task.id, status)
                refreshAfterChange()
            } catch (e: ApiError) {
                _state.update { it.copy(tasks = it.tasks.replaceById(task), message = e.toMessage()) }
            }
        }
    }

    fun askDelete(task: Task) {
        _state.update { it.copy(pendingDelete = task) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val task = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch(dispatcher) {
            try {
                repository.delete(task.id)
                _state.update { state ->
                    state.copy(tasks = state.tasks.filterNot { it.id == task.id }, message = UiMessage.TaskDeleted)
                }
            } catch (_: ApiError.NotFound) {
                _state.update { it.copy(message = UiMessage.TaskGone) }
            } catch (e: ApiError) {
                _state.update { it.copy(message = e.toMessage()) }
                return@launch
            }
            refreshAfterChange()
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    /** A failed load replaces the list with the error state. */
    private fun reload() = launchLoad(showErrorState = true)

    /** A failed refresh keeps the data on screen and shows a message. */
    private fun refreshAfterChange() = launchLoad(showErrorState = false)

    // The newest load wins (EDGE-P2-001).
    private fun launchLoad(showErrorState: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            _state.update { it.copy(isRefreshing = true, isWaking = false) }
            val hint = if (showErrorState) launch { showWakingHintAfterDelay() } else null
            try {
                val (tasks, summary) =
                    if (showErrorState) fetchUntilAwake() else fetch(_state.value.filters)
                _state.update {
                    it.copy(
                        tasks = tasks, summary = summary, phase = Phase.Content,
                        isRefreshing = false, isWaking = false,
                    )
                }
            } catch (e: ApiError) {
                _state.update {
                    if (showErrorState) it.copy(phase = Phase.Error, isRefreshing = false, isWaking = false)
                    else it.copy(isRefreshing = false, message = e.toMessage())
                }
            } finally {
                hint?.cancel()
            }
        }
    }

    private suspend fun showWakingHintAfterDelay() {
        delay(WAKE_HINT_DELAY_MS)
        _state.update { it.copy(isWaking = it.phase == Phase.Loading) }
    }

    /** Retries only on Unavailable, for about 90 s. Offline and server errors fail at once. */
    private suspend fun fetchUntilAwake(): Pair<List<Task>, Summary> {
        repeat(WAKE_MAX_RETRIES) {
            try {
                return fetch(_state.value.filters)
            } catch (_: ApiError.Unavailable) {
                delay(WAKE_RETRY_DELAY_MS)
            }
        }
        return fetch(_state.value.filters)
    }

    private suspend fun fetch(filters: TaskFilters): Pair<List<Task>, Summary> = coroutineScope {
        val tasks = async { repository.list(filters) }
        val summary = async { repository.summary() }
        tasks.await() to summary.await()
    }

    private fun validateTitle(title: String): TitleError? = when {
        title.isBlank() -> TitleError.Required
        title.trim().length > TITLE_MAX_LENGTH -> TitleError.TooLong
        else -> null
    }

    private fun List<Task>.replaceById(task: Task): List<Task> =
        map { if (it.id == task.id) task else it }

    private fun ApiError.toMessage(): UiMessage = when (this) {
        is ApiError.Network, is ApiError.Unavailable -> UiMessage.NetworkError
        else -> UiMessage.ServerError
    }

    companion object {
        // Free-host cold start handling (DES-P4-011). One place to tune after TASK-P4-009.
        const val WAKE_HINT_DELAY_MS = 4_000L
        const val WAKE_RETRY_DELAY_MS = 5_000L
        const val WAKE_MAX_RETRIES = 18

        fun factory(repository: TaskRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TaskListViewModel(repository) as T
            }
    }
}
