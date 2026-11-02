package com.demo.taskmanager

import com.demo.taskmanager.data.ApiError
import com.demo.taskmanager.data.ApiTaskRepository
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.TaskApi
import com.demo.taskmanager.data.TaskDraft
import com.demo.taskmanager.data.TaskFilters
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiTaskRepositoryTest {
    private val server = MockWebServer()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private lateinit var repository: ApiTaskRepository

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TaskApi::class.java)
        repository = ApiTaskRepository(api, json)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun taskJson(id: String = "a1", status: String = "TODO", extra: String = "") =
        """{"id":"$id","title":"Pay rent","description":null,"priority":"HIGH","status":"$status",
        "dueDate":"2026-11-02","overdue":false,"createdAt":"x","updatedAt":"y"$extra}"""

    private fun enqueue(code: Int, body: String = "") =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    private fun failure(block: suspend () -> Unit): ApiError =
        assertThrows(ApiError::class.java) { runBlocking { block() } }

    @Test
    fun list200_mapsTasksAndSendsFilterQuery() = runBlocking {
        enqueue(200, "[${taskJson()}]")
        val tasks = repository.list(TaskFilters(Status.IN_PROGRESS, Priority.HIGH))
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/tasks?status=IN_PROGRESS&priority=HIGH", request.path)
        assertEquals("Pay rent", tasks.single().title)
        assertNull(tasks.single().description)
        assertEquals(Priority.HIGH, tasks.single().priority)
    }

    @Test
    fun list_withoutFiltersSendsNoQuery() = runBlocking {
        enqueue(200, "[]")
        repository.list(TaskFilters())
        assertEquals("/api/tasks", server.takeRequest().path)
    }

    @Test
    fun list_ignoresUnknownServerFields() = runBlocking {
        enqueue(200, "[${taskJson(extra = ",\"newField\":1")}]")
        assertEquals(1, repository.list(TaskFilters()).size)
    }

    @Test
    fun create201_sendsBodyAndMapsTask() = runBlocking {
        enqueue(201, taskJson())
        val task = repository.create(TaskDraft("Pay rent", "", Priority.HIGH, "2026-11-02"))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/tasks", request.path)
        val body = json.parseToJsonElementObject(request.body.readUtf8())
        assertEquals("Pay rent", body["title"])
        assertEquals("HIGH", body["priority"])
        assertEquals("a1", task.id)
    }

    @Test
    fun setStatus_sendsOnlyTheStatusField() = runBlocking {
        enqueue(200, taskJson(status = "DONE"))
        val task = repository.setStatus("a1", Status.DONE)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/tasks/a1", request.path)
        assertEquals(mapOf("status" to "DONE"), json.parseToJsonElementObject(request.body.readUtf8()))
        assertEquals(Status.DONE, task.status)
    }

    @Test
    fun update_sendsEmptyDescriptionToClearIt() = runBlocking {
        enqueue(200, taskJson())
        repository.update("a1", TaskDraft("T", "", Priority.LOW, "2026-11-03"))
        val body = json.parseToJsonElementObject(server.takeRequest().body.readUtf8())
        assertEquals("", body["description"])
        assertFalse(body.containsKey("status"))
    }

    @Test
    fun delete204_succeedsWithoutBody() = runBlocking {
        enqueue(204)
        repository.delete("a1")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/tasks/a1", request.path)
    }

    @Test
    fun summary200_mapsCounts() = runBlocking {
        enqueue(200, """{"total":5,"todo":2,"inProgress":1,"done":2,"overdue":1}""")
        val summary = repository.summary()
        assertEquals(5, summary.total)
        assertEquals(1, summary.inProgress)
        assertEquals(1, summary.overdue)
    }

    @Test
    fun error400_mapsFieldMessages() {
        enqueue(
            400,
            """{"error":{"code":"VALIDATION_ERROR","message":"Invalid request","fields":{"dueDate":"Bad date"}}}""",
        )
        val error = failure { repository.create(TaskDraft("T", "", Priority.LOW, "x")) }
        assertEquals(mapOf("dueDate" to "Bad date"), (error as ApiError.Validation).fields)
    }

    @Test
    fun error404_mapsToNotFound() {
        enqueue(404, """{"error":{"code":"NOT_FOUND","message":"Task not found"}}""")
        assertTrue(failure { repository.delete("a1") } is ApiError.NotFound)
    }

    @Test
    fun error500_mapsToServerWithoutLeakingTheBody() {
        enqueue(500, """{"error":{"code":"INTERNAL","message":"SELECT * FROM tasks failed at db.ts:42"}}""")
        val error = failure { repository.list(TaskFilters()) }
        assertTrue(error is ApiError.Server)
        assertFalse(error.message.orEmpty().contains("SELECT"))
    }

    @Test
    fun error500_withNonJsonBodyMapsToServer() {
        enqueue(500, "<html>Bad gateway</html>")
        assertTrue(failure { repository.list(TaskFilters()) } is ApiError.Server)
    }

    @Test
    fun malformedSuccessBodyMapsToServer() {
        enqueue(200, """{"unexpected":true}""")
        assertTrue(failure { repository.list(TaskFilters()) } is ApiError.Server)
    }

    @Test
    fun unknownEnumValueMapsToServerNotACrash() {
        enqueue(200, "[${taskJson(status = "ARCHIVED")}]")
        assertTrue(failure { repository.list(TaskFilters()) } is ApiError.Server)
    }

    @Test
    fun droppedConnectionMapsToNetwork() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        assertTrue(failure { repository.list(TaskFilters()) } is ApiError.Network)
    }
}

private fun Json.parseToJsonElementObject(text: String): Map<String, String> =
    (parseToJsonElement(text) as kotlinx.serialization.json.JsonObject).mapValues { (_, v) ->
        (v as kotlinx.serialization.json.JsonPrimitive).content
    }
