package com.example.ide.data.api

import com.example.ide.data.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface OpenAIApiService {
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

interface ClaudeApiService {
    @POST("v1/messages")
    @Headers("Content-Type: application/json")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeRequest
    ): Response<ClaudeResponse>
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

// OpenRouter API service
interface OpenRouterApiService {
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): Response<ChatResponse>
}

// Claude-specific models
data class ClaudeRequest(
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val messages: List<ClaudeMessage>,
    val temperature: Double = 0.7
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    @SerializedName("stop_reason")
    val stopReason: String?,
    val usage: ClaudeUsage
)

data class ClaudeContent(
    val type: String,
    val text: String
)

data class ClaudeUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int
)

// Gemini-specific models
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 4096
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    @SerializedName("usageMetadata")
    val usageMetadata: GeminiUsageMetadata?
)

data class GeminiCandidate(
    val content: GeminiContent,
    @SerializedName("finishReason")
    val finishReason: String?
)

data class GeminiUsageMetadata(
    @SerializedName("promptTokenCount")
    val promptTokenCount: Int,
    @SerializedName("candidatesTokenCount")
    val candidatesTokenCount: Int,
    @SerializedName("totalTokenCount")
    val totalTokenCount: Int
)

// OpenRouter-specific models
data class OpenRouterRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)
