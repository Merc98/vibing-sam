package com.example.ide.puente.analysis

import com.example.ide.data.model.AIModelType
import com.example.ide.data.model.ChatMessage
import com.example.ide.data.repository.AIRepository
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.File

/**
 * Minimal LLM-driven edit orchestrator:
 * 1) asks model for JSON operations
 * 2) parses and previews file mutations
 * 3) applies accepted operations to decoded workspace
 */
object LlmPatchOrchestrator {

    data class PatchOperation(
        val path: String,
        val action: String,
        val content: String,
        val reason: String = ""
    )

    data class PatchPlan(
        val summary: String,
        val operations: List<PatchOperation>
    )

    data class PreviewItem(
        val path: String,
        val reason: String,
        val beforeSnippet: String,
        val afterSnippet: String
    )

    data class PreviewResult(
        val success: Boolean,
        val summary: String,
        val items: List<PreviewItem>,
        val rawModelOutput: String,
        val error: String? = null
    )

    private val gson = Gson()

    suspend fun generatePreview(
        decodedDir: File,
        userGoal: String,
        model: AIModelType,
        apiKey: String,
        aiRepository: AIRepository
    ): PreviewResult {
        if (!decodedDir.exists()) {
            return PreviewResult(
                success = false,
                summary = "",
                items = emptyList(),
                rawModelOutput = "",
                error = "Decoded folder not found: ${decodedDir.absolutePath}"
            )
        }

        val sampleFiles = decodedDir.walkTopDown()
            .filter { it.isFile && it.extension in setOf("xml", "smali", "json", "txt") }
            .take(8)
            .toList()

        val inventory = buildString {
            appendLine("Decoded root: ${decodedDir.absolutePath}")
            appendLine("Candidate files:")
            sampleFiles.forEach { file ->
                val rel = file.relativeTo(decodedDir).path
                appendLine("- $rel")
                val content = file.readText()
                appendLine("  snippet: ${content.take(500).replace("\n", " ")}")
            }
        }

        val systemPrompt = """
            You are an Android APK patch planner.
            Return ONLY valid JSON with this schema:
            {
              "summary": "short summary",
              "operations": [
                {"path":"relative/path", "action":"replace_file|append_file", "content":"...", "reason":"why"}
              ]
            }
            Rules:
            - path must stay inside decoded folder.
            - max 6 operations.
            - Prefer XML/smali edits only.
            - No markdown, no prose outside JSON.
        """.trimIndent()

        val userPrompt = """
            Goal from user:
            $userGoal

            Workspace inventory:
            $inventory
        """.trimIndent()

        val response = aiRepository.sendMessage(
            model = model,
            apiKey = apiKey,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )

        val output = response.getOrElse { e ->
            return PreviewResult(
                success = false,
                summary = "",
                items = emptyList(),
                rawModelOutput = "",
                error = "LLM request failed: ${e.message}"
            )
        }

        val plan = tryParsePlan(output) ?: return PreviewResult(
            success = false,
            summary = "",
            items = emptyList(),
            rawModelOutput = output,
            error = "Model response was not valid patch JSON"
        )

        val previewItems = mutableListOf<PreviewItem>()
        for (op in plan.operations) {
            val safeFile = resolveSafe(decodedDir, op.path) ?: continue
            val before = if (safeFile.exists()) safeFile.readText() else ""
            val after = when (op.action.lowercase()) {
                "replace_file" -> op.content
                "append_file" -> before + "\n" + op.content
                else -> before
            }
            previewItems += PreviewItem(
                path = op.path,
                reason = op.reason,
                beforeSnippet = before.take(800),
                afterSnippet = after.take(800)
            )
        }

        return PreviewResult(
            success = previewItems.isNotEmpty(),
            summary = plan.summary,
            items = previewItems,
            rawModelOutput = output,
            error = if (previewItems.isEmpty()) "No supported operations generated" else null
        )
    }

    fun applyPlan(decodedDir: File, rawModelOutput: String): Result<Int> {
        val plan = tryParsePlan(rawModelOutput)
            ?: return Result.failure(IllegalArgumentException("Invalid patch JSON"))

        var applied = 0
        plan.operations.forEach { op ->
            val safeFile = resolveSafe(decodedDir, op.path) ?: return@forEach
            safeFile.parentFile?.mkdirs()
            val before = if (safeFile.exists()) safeFile.readText() else ""
            val after = when (op.action.lowercase()) {
                "replace_file" -> op.content
                "append_file" -> before + "\n" + op.content
                else -> before
            }
            if (after != before) {
                safeFile.writeText(after)
                applied++
            }
        }

        return Result.success(applied)
    }

    private fun resolveSafe(root: File, relativePath: String): File? {
        val candidate = File(root, relativePath)
        val canonicalRoot = root.canonicalFile
        val canonicalCandidate = try {
            candidate.canonicalFile
        } catch (_: Throwable) {
            return null
        }
        return if (canonicalCandidate.path.startsWith(canonicalRoot.path)) canonicalCandidate else null
    }

    private fun tryParsePlan(raw: String): PatchPlan? {
        val json = extractJsonObject(raw) ?: return null
        return try {
            gson.fromJson(json, PatchPlan::class.java)
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }
}
