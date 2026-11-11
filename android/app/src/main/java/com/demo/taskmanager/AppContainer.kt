package com.demo.taskmanager

import com.demo.taskmanager.data.ApiTaskRepository
import com.demo.taskmanager.data.TaskApi
import com.demo.taskmanager.data.TaskRepository
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Manual wiring. This is the only place that reads BuildConfig. */
class AppContainer(baseUrl: String = BuildConfig.API_BASE_URL) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // No logging interceptor: request and response bodies must not reach the log (SEC-202).
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        // 90 s lets a sleeping free host wake while the proxy holds the request (DES-P4-012).
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val api: TaskApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TaskApi::class.java)

    val repository: TaskRepository = ApiTaskRepository(api, json)
}
