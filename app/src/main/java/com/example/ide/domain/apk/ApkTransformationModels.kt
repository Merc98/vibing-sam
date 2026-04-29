package com.example.ide.domain.apk

import com.example.ide.data.model.AIModelType

// Core models for APK transformation pipeline

data class ApkTransformationRequest(
    val source: ApkSource,
    val userGoal: String,
    val model: AIModelType,
    val apiKey: String
)

sealed class ApkSource {
    data class InstalledPackage(val packageName: String) : ApkSource()
    data class ApkFile(val path: String) : ApkSource()
}

data class ApkWorkspace(
    val packageName: String?,
    val originalApkPath: String,
    val rootDir: String,
    val decodedDir: String,
    val jadxDir: String,
    val outputApkPath: String
)

data class ApkContext(
    val manifestSnippet: String,
    val resourceTree: List<String>,
    val candidateFiles: List<ApkCandidateFile>
)

data class ApkCandidateFile(
    val path: String,
    val contentSnippet: String
)

data class ApkTransformationPlan(
    val summary: String,
    val targetFiles: List<String>,
    val operations: List<PatchOperation>,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresSmali: Boolean = false
)

enum class RiskLevel { LOW, MEDIUM, HIGH, BLOCKED }

sealed class PatchOperation {
    abstract val path: String

    data class ReplaceText(
        override val path: String,
        val before: String,
        val after: String
    ) : PatchOperation()

    data class UpdateResourceValue(
        override val path: String,
        val key: String,
        val value: String
    ) : PatchOperation()

    data class UpdateXmlAttribute(
        override val path: String,
        val selector: String,
        val attribute: String,
        val value: String
    ) : PatchOperation()

    data class AddFile(
        override val path: String,
        val content: String
    ) : PatchOperation()
}

data class PatchPreview(
    val summary: String,
    val files: List<String>,
    val riskLevel: RiskLevel,
    val operationsCount: Int
)

data class ApkTransformationResult(
    val success: Boolean,
    val message: String,
    val outputApkPath: String? = null,
    val preview: PatchPreview? = null
)
