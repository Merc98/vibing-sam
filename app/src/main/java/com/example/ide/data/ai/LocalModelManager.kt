package com.example.ide.data.ai

import android.app.ActivityManager
import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Local HuggingFace/GGUF model manager for Vibing MOD.
 *
 * This manager downloads and configures local model files, but does not pretend
 * inference is ready until a llama.cpp/gguf runtime is wired into the app.
 */
class LocalModelManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "local_models").apply { mkdirs() }

    fun recommendedModels(): List<LocalModelSpec> {
        val ramGb = deviceRamGb()
        return when {
            ramGb < 4 -> listOf(
                LocalModelCatalog.qwenCoder05bQ4,
                LocalModelCatalog.smolLm2_360mQ4
            )
            ramGb < 8 -> listOf(
                LocalModelCatalog.qwenCoder05bQ5,
                LocalModelCatalog.qwenCoder05bQ4,
                LocalModelCatalog.tinyLlama11bQ4
            )
            else -> listOf(
                LocalModelCatalog.qwenCoder05bQ6,
                LocalModelCatalog.qwenCoder05bQ5,
                LocalModelCatalog.tinyLlama11bQ4,
                LocalModelCatalog.smolLm2_360mQ4
            )
        }
    }

    fun allSupportedModels(): List<LocalModelSpec> = listOf(
        LocalModelCatalog.qwenCoder05bQ4,
        LocalModelCatalog.qwenCoder05bQ5,
        LocalModelCatalog.qwenCoder05bQ6,
        LocalModelCatalog.smolLm2_360mQ4,
        LocalModelCatalog.tinyLlama11bQ4
    )

    fun status(modelId: String): LocalModelStatus {
        val model = allSupportedModels().firstOrNull { it.id == modelId }
            ?: return LocalModelStatus(null, false, false, false, null, "Unknown model: $modelId")
        val file = modelFile(model)
        val downloaded = file.exists() && file.length() > 0L
        return LocalModelStatus(
            model = model,
            downloaded = downloaded,
            configured = downloaded,
            inferenceRuntimeReady = false,
            localPath = if (downloaded) file.absolutePath else null,
            message = if (downloaded) {
                "Model downloaded and configured. Local GGUF runtime pending."
            } else {
                "Model not downloaded."
            }
        )
    }

    fun modelFile(model: LocalModelSpec): File = File(modelsDir, model.fileName)

    suspend fun downloadModel(
        model: LocalModelSpec,
        onProgress: (DownloadProgress) -> Unit = {}
    ): LocalModelStatus {
        val target = modelFile(model)
        target.parentFile?.mkdirs()

        val connection = URL(model.downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true

        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("Download failed ${connection.responseCode}: ${connection.responseMessage}")
        }

        val total = connection.contentLengthLong.takeIf { it > 0 } ?: model.sizeBytes
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(DownloadProgress(downloaded, total))
                }
            }
        }

        return LocalModelStatus(
            model = model,
            downloaded = true,
            configured = true,
            inferenceRuntimeReady = false,
            localPath = target.absolutePath,
            message = "Downloaded ${model.displayName}. Runtime integration pending."
        )
    }

    private fun deviceRamGb(): Int {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return (info.totalMem / (1024L * 1024L * 1024L)).toInt().coerceAtLeast(1)
    }
}

data class LocalModelSpec(
    val id: String,
    val displayName: String,
    val repo: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val format: String,
    val quantization: String,
    val strengths: List<String>,
    val chatTemplate: String
)

data class LocalModelStatus(
    val model: LocalModelSpec?,
    val downloaded: Boolean,
    val configured: Boolean,
    val inferenceRuntimeReady: Boolean,
    val localPath: String?,
    val message: String
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val percent: Int get() = if (totalBytes <= 0) 0 else ((downloadedBytes * 100) / totalBytes).toInt()
}

object LocalModelCatalog {
    // Best default for Vibing MOD: code-specialized, small, Apache-2.0.
    val qwenCoder05bQ4 = LocalModelSpec(
        id = "qwen2_5_coder_0_5b_q4_k_m",
        displayName = "Qwen2.5 Coder 0.5B Instruct Q4_K_M",
        repo = "Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF",
        fileName = "qwen2.5-coder-0.5b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-0.5b-instruct-q4_k_m.gguf",
        sizeBytes = 491L * 1024L * 1024L,
        format = "GGUF",
        quantization = "Q4_K_M",
        strengths = listOf("code", "structured JSON", "small Android devices", "APK/XML reasoning"),
        chatTemplate = "qwen"
    )

    val qwenCoder05bQ5 = LocalModelSpec(
        id = "qwen2_5_coder_0_5b_q5_k_m",
        displayName = "Qwen2.5 Coder 0.5B Instruct Q5_K_M",
        repo = "Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF",
        fileName = "qwen2.5-coder-0.5b-instruct-q5_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-0.5b-instruct-q5_k_m.gguf",
        sizeBytes = 522L * 1024L * 1024L,
        format = "GGUF",
        quantization = "Q5_K_M",
        strengths = listOf("code", "better quality than Q4", "JSON planning"),
        chatTemplate = "qwen"
    )

    val qwenCoder05bQ6 = LocalModelSpec(
        id = "qwen2_5_coder_0_5b_q6_k",
        displayName = "Qwen2.5 Coder 0.5B Instruct Q6_K",
        repo = "Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF",
        fileName = "qwen2.5-coder-0.5b-instruct-q6_k.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-0.5b-instruct-q6_k.gguf",
        sizeBytes = 650L * 1024L * 1024L,
        format = "GGUF",
        quantization = "Q6_K",
        strengths = listOf("code", "higher quality", "stronger planning on high-RAM phones"),
        chatTemplate = "qwen"
    )

    val smolLm2_360mQ4 = LocalModelSpec(
        id = "smollm2_360m_q4_k_m",
        displayName = "SmolLM2 360M Instruct Q4_K_M",
        repo = "prithivMLmods/SmolLM2-360M-Instruct-GGUF",
        fileName = "SmolLM2-360M-Instruct.Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/prithivMLmods/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct.Q4_K_M.gguf",
        sizeBytes = 271L * 1024L * 1024L,
        format = "GGUF",
        quantization = "Q4_K_M",
        strengths = listOf("very small", "fast", "general chat", "low RAM phones"),
        chatTemplate = "chatml"
    )

    val tinyLlama11bQ4 = LocalModelSpec(
        id = "tinyllama_1_1b_chat_q4_k_m",
        displayName = "TinyLlama 1.1B Chat Q4_K_M",
        repo = "Arivukkarasu/TinyLlama-1.1B-Chat-GGUF",
        fileName = "TinyLlama-1.1B-Chat-Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/Arivukkarasu/TinyLlama-1.1B-Chat-GGUF/resolve/main/TinyLlama-1.1B-Chat-Q4_K_M.gguf",
        sizeBytes = 668L * 1024L * 1024L,
        format = "GGUF",
        quantization = "Q4_K_M",
        strengths = listOf("general assistant", "larger than 0.5B", "mid/high RAM phones"),
        chatTemplate = "llama"
    )
}
