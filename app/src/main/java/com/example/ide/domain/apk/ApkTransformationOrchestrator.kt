package com.example.ide.domain.apk

import android.content.Context
import com.example.ide.data.repository.AIRepository
import com.example.ide.puente.exec.ApktoolRunner
import java.io.File

class ApkTransformationOrchestrator(
    private val context: Context,
    private val contextOrchestrator: ApkContextOrchestrator,
    private val patchApplier: PatchApplier,
    private val aiRepository: AIRepository
) {

    private val planner = LlmTransformationPlanner(aiRepository)
    private val validator = ApkPlanValidator()
    private val exporter = ApkExportSigner(context)

    suspend fun execute(request: ApkTransformationRequest): ApkTransformationResult {
        return try {
            val workspace = contextOrchestrator.prepareWorkspace(request.source)
            val contextData = contextOrchestrator.buildContext(workspace)

            val plan = planner.plan(request, contextData)
            val preview = PatchPreview(
                summary = plan.summary,
                files = plan.targetFiles,
                riskLevel = plan.riskLevel,
                operationsCount = plan.operations.size
            )

            if (plan.riskLevel == RiskLevel.BLOCKED) {
                return ApkTransformationResult(false, "Blocked operation: ${plan.summary}", preview = preview)
            }

            val validation = validator.validate(workspace, plan)
            if (!validation.valid) {
                return ApkTransformationResult(false, validation.message, preview = preview)
            }

            patchApplier.apply(workspace, plan)

            val build = buildUnsignedApk(workspace)
            if (!build.success) {
                return ApkTransformationResult(false, build.message, preview = preview)
            }

            val exported = exporter.signAndExport(workspace.outputApkPath)

            ApkTransformationResult(
                success = exported.success,
                message = exported.message,
                outputApkPath = exported.path ?: workspace.outputApkPath,
                preview = preview
            )
        } catch (e: Exception) {
            ApkTransformationResult(false, e.message ?: "Unknown Vibing MOD error")
        }
    }

    private suspend fun buildUnsignedApk(workspace: ApkWorkspace): BuildStepResult {
        val decodedDir = File(workspace.decodedDir)
        if (!decodedDir.exists() || !decodedDir.isDirectory) {
            return BuildStepResult(false, "Decoded APK directory does not exist: ${workspace.decodedDir}")
        }
        if (!File(decodedDir, "AndroidManifest.xml").exists()) {
            return BuildStepResult(false, "Decoded APK is invalid: AndroidManifest.xml missing in ${workspace.decodedDir}")
        }

        val outputApk = File(workspace.outputApkPath)
        outputApk.parentFile?.mkdirs()
        if (outputApk.exists() && !outputApk.delete()) {
            return BuildStepResult(false, "Could not replace existing output APK: ${outputApk.absolutePath}")
        }

        val result = ApktoolRunner.run(
            context = context,
            args = listOf("b", decodedDir.absolutePath, "-o", outputApk.absolutePath)
        )

        if (result.exitCode != 0) {
            return BuildStepResult(
                false,
                "APK build failed (exit ${result.exitCode}).\nSTDERR:\n${result.stderr}\nSTDOUT:\n${result.stdout}"
            )
        }

        if (!outputApk.exists() || outputApk.length() == 0L) {
            return BuildStepResult(
                false,
                "APK build reported success but output is missing or empty: ${outputApk.absolutePath}.\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}"
            )
        }

        return BuildStepResult(true, "APK build OK: ${outputApk.absolutePath}")
    }

    private data class BuildStepResult(
        val success: Boolean,
        val message: String
    )
}
