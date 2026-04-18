package com.example.ide.data.repository

import com.example.ide.data.api.*
import com.example.ide.data.model.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AIRepository {
    
    private val openAIService = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAIApiService::class.java)
    
    private val claudeService = Retrofit.Builder()
        .baseUrl("https://api.anthropic.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ClaudeApiService::class.java)
    
    private val geminiService = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApiService::class.java)
        
    // OpenRouter service
    private val openRouterService = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenRouterApiService::class.java)

    suspend fun sendMessage(
        model: AIModelType,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        return try {
            when (model) {
                AIModelType.LOCAL_SMART_ASSIST,
                AIModelType.LOCAL_QUICK_HELP -> {
                    sendLocalFreeAssistantMessage(model, messages)
                }
                AIModelType.OPENAI_GPT4, AIModelType.OPENAI_GPT35 -> {
                    sendOpenAIMessage(model, apiKey, messages)
                }
                AIModelType.CLAUDE_3_OPUS, AIModelType.CLAUDE_3_SONNET, AIModelType.CLAUDE_3_HAIKU -> {
                    sendClaudeMessage(model, apiKey, messages)
                }
                AIModelType.GEMINI_PRO, AIModelType.GEMINI_FLASH -> {
                    sendGeminiMessage(model, apiKey, messages)
                }
                AIModelType.OPENROUTER_AUTO -> {
                    sendOpenRouterMessage(model, apiKey, messages)
                }
                // Chinese free models
                AIModelType.LINGMA,
                AIModelType.QWEN,
                AIModelType.LONGCAT_AI,
                AIModelType.DEEPSEEK,
                AIModelType.Z_MODEL,
                AIModelType.KIMI -> {
                    sendChineseFreeModelMessage(model, apiKey, messages)
                }
                else -> {
                    Result.failure(Exception("Model not yet implemented"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendLocalFreeAssistantMessage(
        model: AIModelType,
        messages: List<ChatMessage>
    ): Result<String> {
        val userPrompt = messages.lastOrNull { it.role == "user" }?.content?.trim().orEmpty()
        if (userPrompt.isBlank()) {
            return Result.failure(Exception("Please provide a message first"))
        }

        val assistantName = when (model) {
            AIModelType.LOCAL_QUICK_HELP -> "Quick Help"
            else -> "Smart Assist"
        }

        val response = buildString {
            append("[$assistantName - Free & local]\n")
            append("I can help without API keys. Here's a simple plan:\n")
            append("1) Clarify the expected output.\n")
            append("2) Implement the smallest working change.\n")
            append("3) Test and iterate.\n\n")
            append("Your request summary:\n")
            append(userPrompt.take(700))
            if (userPrompt.length > 700) append("...")
        }

        return Result.success(response)
    }

    private suspend fun sendOpenAIMessage(
        model: AIModelType,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        val modelName = when (model) {
            AIModelType.OPENAI_GPT4 -> "gpt-4"
            AIModelType.OPENAI_GPT35 -> "gpt-3.5-turbo"
            else -> "gpt-3.5-turbo"
        }
        
        val request = ChatRequest(
            model = modelName,
            messages = messages,
            maxTokens = 4096,
            temperature = 0.7
        )
        
        val response = openAIService.chatCompletion("Bearer $apiKey", request)
        
        return if (response.isSuccessful) {
            val chatResponse = response.body()
            val content = chatResponse?.choices?.firstOrNull()?.message?.content
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("No response content"))
            }
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    }

    private suspend fun sendClaudeMessage(
        model: AIModelType,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        val modelName = when (model) {
            AIModelType.CLAUDE_3_OPUS -> "claude-3-opus-20240229"
            AIModelType.CLAUDE_3_SONNET -> "claude-3-sonnet-20240229"
            AIModelType.CLAUDE_3_HAIKU -> "claude-3-haiku-20240307"
            else -> "claude-3-sonnet-20240229"
        }
        
        val claudeMessages = messages.filter { it.role != "system" }.map { 
            ClaudeMessage(it.role, it.content) 
        }
        
        val request = ClaudeRequest(
            model = modelName,
            maxTokens = 4096,
            messages = claudeMessages,
            temperature = 0.7
        )
        
        val response = claudeService.createMessage(apiKey, "2023-06-01", request)
        
        return if (response.isSuccessful) {
            val claudeResponse = response.body()
            val content = claudeResponse?.content?.firstOrNull()?.text
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("No response content"))
            }
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    }

    private suspend fun sendGeminiMessage(
        model: AIModelType,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        val modelName = when (model) {
            AIModelType.GEMINI_PRO -> "gemini-pro"
            AIModelType.GEMINI_FLASH -> "gemini-1.5-flash"
            else -> "gemini-pro"
        }
        
        val content = messages.joinToString("\n") { "${it.role}: ${it.content}" }
        
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(content))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7,
                maxOutputTokens = 4096
            )
        )
        
        val response = geminiService.generateContent(modelName, apiKey, request)
        
        return if (response.isSuccessful) {
            val geminiResponse = response.body()
            val responseText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                Result.success(responseText)
            } else {
                Result.failure(Exception("No response content"))
            }
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    }

    private suspend fun sendOpenRouterMessage(
        model: AIModelType,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        val modelName = when (model) {
            AIModelType.OPENROUTER_AUTO -> "openrouter/auto"
            else -> "openrouter/auto"
        }
        
        val request = OpenRouterRequest(
            model = modelName,
            messages = messages,
            maxTokens = 4096,
            temperature = 0.7
        )
        
        val response = openRouterService.chatCompletion("Bearer $apiKey", request)
        
        return if (response.isSuccessful) {
            val chatResponse = response.body()
            val content = chatResponse?.choices?.firstOrNull()?.message?.content
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("No response content"))
            }
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    }

    private suspend fun sendChineseFreeModelMessage(
        model: AIModelType,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        // For Chinese free models, we'll use a generic approach
        // These models might be accessed through different endpoints
        // For now, we'll treat them as OpenAI-compatible APIs
        val modelName = when (model) {
            AIModelType.LINGMA -> "lingma"
            AIModelType.QWEN -> "qwen"
            AIModelType.LONGCAT_AI -> "longcat-ai"
            AIModelType.DEEPSEEK -> "deepseek"
            AIModelType.Z_MODEL -> "z"
            AIModelType.KIMI -> "kimi"
            else -> "unknown"
        }
        
        // We'll assume these models use OpenAI-compatible APIs
        // In a real implementation, you might need to customize this per model
        val request = ChatRequest(
            model = modelName,
            messages = messages,
            maxTokens = 4096,
            temperature = 0.7
        )
        
        // For now, we'll use the OpenAI service as a fallback
        // In a real implementation, you would set up specific services for each provider
        val response = openAIService.chatCompletion("Bearer $apiKey", request)
        
        return if (response.isSuccessful) {
            val chatResponse = response.body()
            val content = chatResponse?.choices?.firstOrNull()?.message?.content
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("No response content"))
            }
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    }

    fun getAvailableModels(): List<AIModel> {
        return listOf(
            AIModel(
                AIModelType.LOCAL_SMART_ASSIST,
                "Local Smart Assist (Free)",
                "A built-in offline helper that works without API keys",
                false
            ),
            AIModel(
                AIModelType.LOCAL_QUICK_HELP,
                "Local Quick Help (Free)",
                "Fast built-in suggestions for code tasks (no API key)",
                false
            ),
            AIModel(AIModelType.OPENAI_GPT4, "GPT-4", "OpenAI's most capable model", true),
            AIModel(AIModelType.OPENAI_GPT35, "GPT-3.5 Turbo", "Fast and efficient OpenAI model", true),
            AIModel(AIModelType.CLAUDE_3_OPUS, "Claude 3 Opus", "Anthropic's most powerful model", true),
            AIModel(AIModelType.CLAUDE_3_SONNET, "Claude 3 Sonnet", "Balanced performance and speed", true),
            AIModel(AIModelType.CLAUDE_3_HAIKU, "Claude 3 Haiku", "Fast and lightweight", true),
            AIModel(AIModelType.GEMINI_PRO, "Gemini Pro", "Google's advanced AI model", true),
            AIModel(AIModelType.GEMINI_FLASH, "Gemini 1.5 Flash", "Fast and efficient Gemini model", true),
            AIModel(AIModelType.COHERE_COMMAND, "Command", "Cohere's command model", true),
            AIModel(AIModelType.MISTRAL_LARGE, "Mistral Large", "Mistral's largest model", true),
            AIModel(AIModelType.MISTRAL_MEDIUM, "Mistral Medium", "Balanced Mistral model", true),
            AIModel(AIModelType.LLAMA_70B, "Llama 2 70B", "Meta's large language model", true),
            AIModel(AIModelType.CODELLAMA_34B, "Code Llama 34B", "Specialized for code generation", true),
            // OpenRouter models
            AIModel(AIModelType.OPENROUTER_AUTO, "OpenRouter Auto", "Automatically select the best model", true, "https://openrouter.ai"),
            // Chinese free models
            AIModel(AIModelType.LINGMA, "Lingma", "Chinese AI model", true),
            AIModel(AIModelType.QWEN, "Qwen", "Alibaba's Qwen model", true),
            AIModel(AIModelType.LONGCAT_AI, "Longcat AI", "Longcat AI model", true),
            AIModel(AIModelType.DEEPSEEK, "DeepSeek", "DeepSeek AI model", true),
            AIModel(AIModelType.Z_MODEL, "Z Model", "Z AI model", true),
            AIModel(AIModelType.KIMI, "Kimi", "Moonshot AI's Kimi model", true)
        )
    }
}
