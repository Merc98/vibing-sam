package com.example.ide.data.model

import com.google.gson.annotations.SerializedName

enum class AIModelType {
    LOCAL_SMART_ASSIST,
    LOCAL_QUICK_HELP,
    OPENAI_GPT4,
    OPENAI_GPT35,
    CLAUDE_3_OPUS,
    CLAUDE_3_SONNET,
    CLAUDE_3_HAIKU,
    GEMINI_PRO,
    GEMINI_FLASH,
    COHERE_COMMAND,
    MISTRAL_LARGE,
    MISTRAL_MEDIUM,
    LLAMA_70B,
    CODELLAMA_34B,
    // OpenRouter models
    OPENROUTER_AUTO,
    // Chinese free models
    LINGMA,
    QWEN,
    LONGCAT_AI,
    DEEPSEEK,
    Z_MODEL,
    KIMI
}

data class AIModel(
    val type: AIModelType,
    val name: String,
    val description: String,
    val requiresApiKey: Boolean,
    val baseUrl: String? = null,
    val maxTokens: Int = 4096,
    val supportsCoding: Boolean = true
)

data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: ErrorResponse? = null
)

data class Choice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

data class ErrorResponse(
    val message: String,
    val type: String? = null,
    val code: String? = null
)
