package com.example.ide.data.api

import com.example.ide.data.model.ChatMessage
import com.example.ide.data.model.ChatRequest
import com.example.ide.data.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// G4F (g4f.dev) API service - Free AI models
interface G4FApiService {
    @POST("api/v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun chatCompletion(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

// HuggingFace Inference API
interface HuggingFaceApiService {
    @POST("models/{modelId}")
    suspend fun generateText(
        @Path("modelId") modelId: String,
        @Header("Authorization") token: String,
        @Body request: HuggingFaceRequest
    ): Response<HuggingFaceResponse>
}

data class HuggingFaceRequest(
    val inputs: String,
    val parameters: HuggingFaceParameters? = null,
    val options: HuggingFaceOptions? = null
)

data class HuggingFaceParameters(
    val max_new_tokens: Int = 2048,
    val temperature: Double = 0.7,
    val top_p: Double = 0.95,
    val return_full_text: Boolean = false
)

data class HuggingFaceOptions(
    val use_cache: Boolean = true,
    val wait_for_model: Boolean = true
)

data class HuggingFaceResponse(
    val generated_text: String? = null,
    val error: String? = null
)
