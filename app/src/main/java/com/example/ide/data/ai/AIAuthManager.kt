package com.example.ide.data.ai

import com.example.ide.data.model.AIModelType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AIAuthManager(
    private val providers: Map<AIModelType, AIAuthProvider>
) {

    private val mutex = Mutex()
    private val sessions = mutableMapOf<AIModelType, AIAuthSession>()

    suspend fun ensureSession(model: AIModelType): AIAuthSession? = mutex.withLock {
        val existing = sessions[model]
        if (existing != null && existing.isAuthenticated && !isExpired(existing)) {
            return existing
        }

        val provider = providers[model] ?: return null

        val current = provider.getSession()
        if (current != null && current.isAuthenticated && !isExpired(current)) {
            sessions[model] = current
            return current
        }

        val start = provider.startAuth()
        return when (start) {
            is AIAuthStartResult.Ready -> {
                sessions[model] = start.session
                start.session
            }
            else -> null
        }
    }

    suspend fun startInteractiveAuth(model: AIModelType): AIAuthStartResult {
        val provider = providers[model] ?: return AIAuthStartResult.Unsupported("No provider")
        return provider.startAuth()
    }

    suspend fun clear(model: AIModelType) {
        providers[model]?.clear()
        sessions.remove(model)
    }

    private fun isExpired(session: AIAuthSession): Boolean {
        val exp = session.expiresAtEpochMs ?: return false
        return System.currentTimeMillis() > exp
    }
}
