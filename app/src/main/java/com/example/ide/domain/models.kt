package com.example.ide.domain

/**
 * Represents an installed application on the device
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long?,
    val isDebuggable: Boolean,
    val apkPath: String?,
    val isEnabled: Boolean,
    val isSystem: Boolean
)

/**
 * Represents a code analysis result from tools like vibing_apk_lab
 */
data class AnalysisResult(
    val filePath: String,
    val fileType: String,
    val sizeBytes: Long,
    val structure: List<FileEntry>,
    val metadata: Map<String, String>,
    val warnings: List<String>,
    val suggestions: List<String>
)

/**
 * Represents a file entry in an archive or directory structure
 */
data class FileEntry(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val type: String? = null
)

/**
 * Tool command for executing external utilities
 */
data class ToolCommand(
    val id: String,
    val name: String,
    val description: String,
    val commandTemplate: String,
    val requiresFile: Boolean = false,
    val supportedExtensions: List<String> = emptyList(),
    val category: ToolCategory
)

enum class ToolCategory {
    APK_ANALYSIS,
    CODE_GENERATION,
    DECOMPILATION,
    SECURITY_AUDIT,
    PACKAGE_MANAGEMENT,
    FILE_INSPECTION
}

/**
 * Chat action that can be triggered from the chat interface
 */
sealed class ChatAction {
    data class AnalyzeFile(val filePath: String) : ChatAction()
    data class InspectApp(val packageName: String) : ChatAction()
    data class DecompileApk(val apkPath: String, val outputDir: String) : ChatAction()
    data class GenerateCode(val language: String, val description: String) : ChatAction()
    data class ListInstalledApps(val filter: String?) : ChatAction()
    data class ExecuteTool(val toolId: String, val parameters: Map<String, String>) : ChatAction()
}
