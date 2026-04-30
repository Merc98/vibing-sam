package com.example.ide.domain.frida

import com.example.ide.data.local.ToolRepository

class FridaService(
    private val toolRepository: ToolRepository
) {
    suspend fun attach(packageName: String): String {
        return toolRepository.startFridaServer().fold(
            onSuccess = { "Frida server ready. Attach requested for $packageName." },
            onFailure = { "Frida attach failed: ${it.message}" }
        )
    }

    suspend fun runScript(script: String): String {
        return if (script.isBlank()) {
            "Frida script is empty"
        } else {
            "Frida script queued (${script.length} chars). Device backend execution depends on active frida session."
        }
    }

    suspend fun detach(): String = "Frida session detached"
}
