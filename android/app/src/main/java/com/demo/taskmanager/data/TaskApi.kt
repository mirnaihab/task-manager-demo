package com.demo.taskmanager.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApi {
    @GET("api/tasks")
    suspend fun listTasks(
        @Query("status") status: Status?,
        @Query("priority") priority: Priority?,
    ): Response<List<TaskDto>>

    @POST("api/tasks")
    suspend fun createTask(@Body body: CreateTaskBody): Response<TaskDto>

    @PATCH("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: UpdateTaskBody): Response<TaskDto>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @GET("api/summary")
    suspend fun summary(): Response<SummaryDto>
}

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: Priority,
    val status: Status,
    val dueDate: String,
    val overdue: Boolean,
)

@Serializable
data class SummaryDto(
    val total: Int,
    val todo: Int,
    val inProgress: Int,
    val done: Int,
    val overdue: Int,
)

@Serializable
data class CreateTaskBody(
    val title: String,
    val description: String,
    val priority: Priority,
    val dueDate: String,
)

/** Unset fields stay out of the JSON body (explicitNulls = false). An empty description clears it. */
@Serializable
data class UpdateTaskBody(
    val title: String? = null,
    val description: String? = null,
    val priority: Priority? = null,
    val status: Status? = null,
    val dueDate: String? = null,
)

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val fields: Map<String, String> = emptyMap(),
)
