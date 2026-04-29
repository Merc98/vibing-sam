package com.example.ide.domain.apk

import android.content.Context
import com.example.ide.data.repository.AIRepository
import com.example.ide.puente.exec.ApktoolRunner

class ApkTransformationOrchestrator(
    private val context: Context,
    private val contextOrchestrator: ApkContextOrchestrator,
    private val patchApplier: PatchApplier,
    private val aiRepository: AIRepository
) {

    private val planner = LlmTransformationPlanner(aiRepository)
    private val validator = ApkPlanValidator()

    suspend fun execute(request: ApkTransformationRequest): ApkTransformationResult {
        return try {
            val workspace = contextOrchestrator.prepareWorkspace(request.source)
            val contextData = contextOrchestrator.buildContext(workspace)

            val plan = planner.plan(request, contextData)

            val validation = validator.validate(workspace, plan)
            if (!validation.valid) {
                return ApkTransformationResult(false, validation.message)
            }

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

            val buildResult = ApktoolRunner.run(
                context,
                listOf("b", workspace.decodedDir, "-o", workspace.outputApkPath)
            )

            if (buildResult.exitCode != 0) {
                return ApkTransformationResult(false, "Build failed: ${buildResult.stderr}")
            }

            return ApkTransformationResult(
                success = true,
                message = "APK transformed (unsigned)",
                outputApkPath = workspace.outputApkPath,
                preview = preview
            )
        } catch (e: Exception) {
            ApkTransformationResult(false, e.message ?: "error")
        }
    }
}
