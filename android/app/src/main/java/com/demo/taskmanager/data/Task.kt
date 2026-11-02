package com.demo.taskmanager.data

enum class Priority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
}

enum class Status(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done"),
}

/** [dueDate] uses the API format yyyy-MM-dd, so no time zone conversion happens in the app. */
data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val priority: Priority,
    val status: Status,
    val dueDate: String,
    val overdue: Boolean,
)

data class Summary(
    val total: Int = 0,
    val todo: Int = 0,
    val inProgress: Int = 0,
    val done: Int = 0,
    val overdue: Int = 0,
)

data class TaskFilters(
    val status: Status? = null,
    val priority: Priority? = null,
) {
    val isActive: Boolean get() = status != null || priority != null
}

data class TaskDraft(
    val title: String,
    val description: String,
    val priority: Priority,
    val dueDate: String,
)
