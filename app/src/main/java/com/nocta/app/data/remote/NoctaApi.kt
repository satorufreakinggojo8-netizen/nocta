package com.nocta.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.Response

data class SleepSessionDto(
    val id: String,
    val date: String,
    val bedtime: String,
    val sleepAttemptTime: String,
    val estimatedSleepTime: String,
    val wakeTime: String,
    val nightAwakenings: Int,
    val sleepQuality: Int,
    val morningEnergy: Int,
    val source: String
)

data class AiCoachRequestDto(val conversationId: String?, val message: String)

/**
 * Backend contract. The AI endpoint is @Streaming — the backend returns
 * Server-Sent-Events chunks so the client can render tokens progressively
 * (see AIMessageBubble's streaming state). No LLM call happens on-device.
 */
interface NoctaApi {

    @GET("v1/sleep/sessions")
    suspend fun getSessions(@Query("since") sinceIsoDate: String): List<SleepSessionDto>

    @POST("v1/sleep/sessions")
    suspend fun upsertSession(@Body session: SleepSessionDto): Response<Unit>

    @Streaming
    @POST("v1/ai/coach")
    suspend fun askCoach(@Body request: AiCoachRequestDto): retrofit2.Response<okhttp3.ResponseBody>
}
