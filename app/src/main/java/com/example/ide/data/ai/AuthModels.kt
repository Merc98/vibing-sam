package com.example.ide.data.ai

import com.example.ide.data.model.AIModelType

enum class AIAuthMethod {
    OAUTH,
    WEB_SESSION,
    DEVICE_CODE,
    LOCAL_MODEL,
    API_KEY_FALLBACK
}

data class AIProviderAuthConfig(
    val modelType: AIModelType,
    val displayName: String,
    val preferredAuthMethod: AIAuthMethod,
    val supportsApiKeyFallback: Boolean,
    val authStartUrl: String? = null,
    val deviceCodeEndpoint: String? = null,
    val tokenRefreshEndpoint: String? = null
)

data class AIAuthSession(
    val modelType: AIModelType,
    val authMethod: AIAuthMethod,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAtEpochMs: Long? = null,
    val localModelPath: String? = null,
    val isAuthenticated: Boolean = false
)

interface AIAuthProvider {
    val config: AIProviderAuthConfig
    suspend fun getSession(): AIAuthSession?
    suspend fun startAuth(): AIAuthStartResult
    suspend fun refresh(session: AIAuthSession): AIAuthSession?
    suspend fun clear()
}

sealed class AIAuthStartResult {
    data class OpenUrl(val url: String) : AIAuthStartResult()
    data class DeviceCode(val verificationUrl: String, val userCode: String) : AIAuthStartResult()
    data class Ready(val session: AIAuthSession) : AIAuthStartResult()
    data class Unsupported(val reason: String) : AIAuthStartResult()
}
