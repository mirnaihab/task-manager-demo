package com.demo.taskmanager.data

import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class ApiTaskRepository(
    private val api: TaskApi,
    private val json: Json,
) : TaskRepository {

    override suspend fun list(filters: TaskFilters): List<Task> =
        call { api.listTasks(filters.status, filters.priority) }.map { it.toDomain() }

    override suspend fun create(draft: TaskDraft): Task =
        call {
            api.createTask(CreateTaskBody(draft.title, draft.description, draft.priority, draft.dueDate))
        }.toDomain()

    override suspend fun update(id: String, draft: TaskDraft): Task =
        call {
            api.updateTask(
                id,
                UpdateTaskBody(
                    title = draft.title,
                    description = draft.description,
                    priority = draft.priority,
                    dueDate = draft.dueDate,
                ),
            )
        }.toDomain()

    override suspend fun setStatus(id: String, status: Status): Task =
        call { api.updateTask(id, UpdateTaskBody(status = status)) }.toDomain()

    override suspend fun delete(id: String) {
        execute { api.deleteTask(id) }
    }

    override suspend fun summary(): Summary = call { api.summary() }.toDomain()

    private suspend fun <T> call(request: suspend () -> Response<T>): T =
        execute(request).body() ?: throw ApiError.Server()

    private suspend fun <T> execute(request: suspend () -> Response<T>): Response<T> {
        val response = try {
            request()
        } catch (_: IOException) {
            throw ApiError.Network()
        } catch (_: SerializationException) {
            throw ApiError.Server()
        }
        if (!response.isSuccessful) throw response.toApiError()
        return response
    }

    // Only the error code and the field messages leave this class. The raw body never does.
    private fun Response<*>.toApiError(): ApiError {
        if (code() in GATEWAY_CODES) return ApiError.Unavailable()
        val envelope = try {
            errorBody()?.string()?.let { json.decodeFromString<ErrorEnvelope>(it) }
        } catch (_: SerializationException) {
            null
        } catch (_: IOException) {
            return ApiError.Network()
        }
        return when {
            code() == 404 -> ApiError.NotFound()
            code() == 400 && envelope?.error?.code == "VALIDATION_ERROR" ->
                ApiError.Validation(envelope.error.fields)
            else -> ApiError.Server()
        }
    }
}

private val GATEWAY_CODES = setOf(502, 503, 504)

private fun TaskDto.toDomain() = Task(id, title, description, priority, status, dueDate, overdue)

private fun SummaryDto.toDomain() = Summary(total, todo, inProgress, done, overdue)
