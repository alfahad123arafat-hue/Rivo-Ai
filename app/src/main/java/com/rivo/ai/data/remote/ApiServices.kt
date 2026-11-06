package com.rivo.ai.data.remote

import retrofit2.Response
import retrofit2.http.*

interface OpenAIService {
    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") auth: String
    ): Response<OpenAIModelListResponse>

    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: OpenAIChatRequest
    ): Response<OpenAIChatResponse>

    @GET
    suspend fun listModelsCustom(
        @Url url: String,
        @Header("Authorization") auth: String?,
        @Header("x-api-key") apiKeyHeader: String? = null
    ): Response<OpenAIModelListResponse>
}

interface GeminiService {
    @GET
    suspend fun listModels(
        @Url url: String,
        @Query("key") apiKey: String
    ): Response<GeminiModelListResponse>

    @POST
    suspend fun generateContent(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>

    @POST
    suspend fun streamGenerateContent(
        @Url url: String,
        @Query("key") apiKey: String,
        @Query("alt") alt: String = "sse",
        @Body request: GeminiGenerateRequest
    ): Response<okhttp3.ResponseBody>
}
