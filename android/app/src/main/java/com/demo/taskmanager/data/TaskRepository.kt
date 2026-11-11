package com.demo.taskmanager.data

sealed class ApiError(message: String) : Exception(message) {
    class Validation(val fields: Map<String, String>) : ApiError("Invalid request")
    class NotFound : ApiError("Task not found")
    class Server : ApiError("Server error")
    class Network : ApiError("Network error")

    /** The gateway (HTTP 502, 503, 504) has no running API behind it. The free host may be asleep. */
    class Unavailable : ApiError("Server unavailable")
}

interface TaskRepository {
    suspend fun list(filters: TaskFilters): List<Task>
    suspend fun create(draft: TaskDraft): Task
    suspend fun update(id: String, draft: TaskDraft): Task
    suspend fun setStatus(id: String, status: Status): Task
    suspend fun delete(id: String)
    suspend fun summary(): Summary
}
