package com.demo.taskmanager

import com.demo.taskmanager.data.ApiError
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.Summary
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.data.TaskDraft
import com.demo.taskmanager.data.TaskFilters
import com.demo.taskmanager.data.TaskRepository
import kotlinx.coroutines.CompletableDeferred

/** In-memory repository that mirrors the server rules. Shared by unit tests and UI tests. */
class FakeTaskRepository(
    initial: List<Task> = emptyList(),
    private val today: String = TODAY,
) : TaskRepository {
    private val tasks = initial.toMutableList()
    private var nextId = 1
    private var nextFailure: ApiError? = null

    /** Names of the calls received so far, for example "create" or "setStatus". */
    val calls = mutableListOf<String>()

    /** When set, mutation calls wait until the deferred completes. */
    var gate: CompletableDeferred<Unit>? = null

    fun failNext(error: ApiError) {
        nextFailure = error
    }

    override suspend fun list(filters: TaskFilters): List<Task> {
        record("list")
        return tasks
            .filter { filters.status == null || it.status == filters.status }
            .filter { filters.priority == null || it.priority == filters.priority }
            .map(::withOverdue)
    }

    override suspend fun create(draft: TaskDraft): Task {
        record("create")
        gate?.await()
        val task = Task(
            id = "task-${nextId++}",
            title = draft.title.trim(),
            description = draft.description.ifBlank { null },
            priority = draft.priority,
            status = Status.TODO,
            dueDate = draft.dueDate,
            overdue = false,
        )
        tasks.add(task)
        return withOverdue(task)
    }

    override suspend fun update(id: String, draft: TaskDraft): Task {
        record("update")
        gate?.await()
        val updated = replace(id) {
            it.copy(
                title = draft.title.trim(),
                description = draft.description.ifBlank { null },
                priority = draft.priority,
                dueDate = draft.dueDate,
            )
        }
        return withOverdue(updated)
    }

    override suspend fun setStatus(id: String, status: Status): Task {
        record("setStatus")
        gate?.await()
        return withOverdue(replace(id) { it.copy(status = status) })
    }

    override suspend fun delete(id: String) {
        record("delete")
        gate?.await()
        if (!tasks.removeAll { it.id == id }) throw ApiError.NotFound()
    }

    override suspend fun summary(): Summary {
        record("summary")
        val all = tasks.map(::withOverdue)
        return Summary(
            total = all.size,
            todo = all.count { it.status == Status.TODO },
            inProgress = all.count { it.status == Status.IN_PROGRESS },
            done = all.count { it.status == Status.DONE },
            overdue = all.count { it.overdue },
        )
    }

    private fun record(name: String) {
        calls.add(name)
        nextFailure?.let {
            nextFailure = null
            throw it
        }
    }

    private fun replace(id: String, change: (Task) -> Task): Task {
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) throw ApiError.NotFound()
        return change(tasks[index]).also { tasks[index] = it }
    }

    private fun withOverdue(task: Task) =
        task.copy(overdue = task.dueDate < today && task.status != Status.DONE)

    companion object {
        const val TODAY = "2026-11-02"

        fun task(
            id: String,
            title: String = "Task $id",
            priority: Priority = Priority.MEDIUM,
            status: Status = Status.TODO,
            dueDate: String = TODAY,
        ) = Task(id, title, null, priority, status, dueDate, overdue = false)
    }
}
