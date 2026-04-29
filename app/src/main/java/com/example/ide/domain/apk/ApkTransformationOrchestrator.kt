package com.example.ide.domain.apk

import com.example.ide.data.repository.AIRepository

class ApkTransformationOrchestrator(
    private val contextOrchestrator: ApkContextOrchestrator,
    private val patchApplier: PatchApplier,
    private val aiRepository: AIRepository
) {

    suspend fun execute(request: ApkTransformationRequest): ApkTransformationResult {
        return try {
            val workspace = contextOrchestrator.prepareWorkspace(request.source)
            val context = contextOrchestrator.buildContext(workspace)

            val plan = generatePlan(request, context)

            val preview = PatchPreview(
                summary = plan.summary,
                files = plan.targetFiles,
                riskLevel = plan.riskLevel,
                operationsCount = plan.operations.size
            )

            if (plan.riskLevel == RiskLevel.BLOCKED) {
                return ApkTransformationResult(false, "Blocked operation", preview = preview)
            }

            patchApplier.apply(workspace, plan)

            com.example.ide.puente.exec.ApktoolRunner.build(workspace.decodedDir, workspace.outputApkPath)

            return ApkTransformationResult(true, "APK transformed", workspace.outputApkPath, preview)
        } catch (e: Exception) {
            ApkTransformationResult(false, e.message ?: "error")
        }
    }

    private suspend fun generatePlan(request: ApkTransformationRequest, context: ApkContext): ApkTransformationPlan {
        val prompt = """
You are an APK transformation planner.
Return ONLY JSON.

User goal: ${request.userGoal}

Manifest:
${context.manifestSnippet}

Files:
${context.resourceTree.take(20)}
        """.trimIndent()

        val response = aiRepository.sendMessage(request.model, request.apiKey, listOf(
            com.example.ide.data.model.ChatMessage("user", prompt)
        ))

        val text = response.getOrNull() ?: return fallbackPlan(request)

        return parsePlan(text) ?: fallbackPlan(request)
    }

    private fun fallbackPlan(request: ApkTransformationRequest): ApkTransformationPlan {
        return ApkTransformationPlan(
            summary = "Fallback simple replace",
            targetFiles = listOf("res/values/colors.xml"),
            operations = listOf(
                PatchOperation.ReplaceText(
                    path = "res/values/colors.xml",
                    before = "#FFFFFF",
                    after = "#0000FF"
                )
            )
        )
    }

    private fun parsePlan(text: String): ApkTransformationPlan? {
        return null // TODO JSON parser real
    }
}
