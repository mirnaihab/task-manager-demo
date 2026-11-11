package com.demo.taskmanager.ui

import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Summary
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.data.TaskDraft
import com.demo.taskmanager.data.TaskFilters

enum class Phase { Loading, Content, Error }

/** One-shot snackbar texts. The UI maps each value to a string resource. */
enum class UiMessage { TaskCreated, TaskUpdated, TaskDeleted, TaskGone, NetworkError, ServerError }

enum class TitleError { Required, TooLong }

data class FormState(
    val editingId: String?,
    val draft: TaskDraft,
    val titleError: TitleError? = null,
    val serverErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
)

data class UiState(
    val filters: TaskFilters = TaskFilters(),
    val tasks: List<Task> = emptyList(),
    val summary: Summary = Summary(),
    val phase: Phase = Phase.Loading,
    val isRefreshing: Boolean = false,
    /** True when the first load runs longer than the hint delay. The screen shows a waking message. */
    val isWaking: Boolean = false,
    val form: FormState? = null,
    val pendingDelete: Task? = null,
    val message: UiMessage? = null,
)

const val TITLE_MAX_LENGTH = 100
