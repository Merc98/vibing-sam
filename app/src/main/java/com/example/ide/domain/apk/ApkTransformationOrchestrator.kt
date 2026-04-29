package com.example.ide.domain.apk

import com.example.ide.data.repository.AIRepository

class ApkTransformationOrchestrator(
    private val contextOrchestrator: ApkContextOrchestrator,
    private val patchApplier: PatchApplier,
    private val aiRepository: AIRepository
) {

    private val planner = LlmTransformationPlanner(aiRepository)

    suspend fun execute(request: ApkTransformationRequest): ApkTransformationResult {
        return try {
            val workspace = contextOrchestrator.prepareWorkspace(request.source)
            val context = contextOrchestrator.buildContext(workspace)

            val plan = planner.plan(request, context)

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

            val signed = ApkSigner.sign(workspace.outputApkPath, workspace.outputApkPath)

            return ApkTransformationResult(
                success = signed,
                message = if (signed) "APK transformed and signed" else "Build ok but signing failed",
                outputApkPath = workspace.outputApkPath,
                preview = preview
            )
        } catch (e: Exception) {
            ApkTransformationResult(false, e.message ?: "error")
        }
    }
}
