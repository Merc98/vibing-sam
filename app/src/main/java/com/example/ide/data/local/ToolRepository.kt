package com.example.ide.data.local

import android.content.Context
import com.example.ide.domain.ToolCategory
import com.example.ide.domain.ToolCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for executing external tools like vibing_apk_lab.py
 */
class ToolRepository(private val context: Context) {
    
    private val toolsDirectory = File(context.filesDir.parentFile, "tools")
    
    /**
     * Get available tool commands
     */
    fun getAvailableTools(): List<ToolCommand> {
        return listOf(
            ToolCommand(
                id = "vibing_apk_lab_inspect",
                name = "APK Inspector",
                description = "Inspect APK structure and metadata",
                commandTemplate = "python tools/vibing_apk_lab.py {apk_path}",
                requiresFile = true,
                supportedExtensions = listOf("apk"),
                category = ToolCategory.APK_ANALYSIS
            ),
            ToolCommand(
                id = "vibing_apk_lab_decode",
                name = "APK Decode",
                description = "Decode APK resources with apktool",
                commandTemplate = "python tools/vibing_apk_lab.py {apk_path} --decode",
                requiresFile = true,
                supportedExtensions = listOf("apk"),
                category = ToolCategory.DECOMPILATION
            ),
            ToolCommand(
                id = "vibing_apk_lab_decompile",
                name = "APK Decompile",
                description = "Decompile APK to Java source with jadx",
                commandTemplate = "python tools/vibing_apk_lab.py {apk_path} --decompile",
                requiresFile = true,
                supportedExtensions = listOf("apk"),
                category = ToolCategory.DECOMPILATION
            ),
            ToolCommand(
                id = "vibing_apk_lab_full",
                name = "APK Full Analysis",
                description = "Complete APK analysis with decode and decompile",
                commandTemplate = "python tools/vibing_apk_lab.py {apk_path} --decode --decompile",
                requiresFile = true,
                supportedExtensions = listOf("apk"),
                category = ToolCategory.APK_ANALYSIS
            ),
            ToolCommand(
                id = "adb_list_packages",
                name = "List Installed Apps",
                description = "List all installed packages from connected device",
                commandTemplate = "python tools/vibing_apk_lab.py --list-packages",
                requiresFile = false,
                category = ToolCategory.PACKAGE_MANAGEMENT
            ),
            ToolCommand(
                id = "adb_package_info",
                name = "Package Info",
                description = "Get detailed info about an installed package",
                commandTemplate = "python tools/vibing_apk_lab.py --package {package_name}",
                requiresFile = false,
                category = ToolCategory.PACKAGE_MANAGEMENT
            )
        )
    }
    
    /**
     * Execute a tool command
     * Note: Actual execution would require proper process management
     * This is a placeholder for the actual implementation
     */
    suspend fun executeTool(
        toolId: String,
        parameters: Map<String, String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val tool = getAvailableTools().find { it.id == toolId }
                ?: return@withContext Result.failure(Exception("Tool not found: $toolId"))
            
            // Build the command
            var command = tool.commandTemplate
            parameters.forEach { (key, value) ->
                command = command.replace("{$key}", value)
            }
            
            // For security reasons, we don't actually execute shell commands here
            // In a real implementation, you would use ProcessBuilder with proper validation
            // and sandboxing
            
            val result = """
                Command prepared: $command
                
                Note: For security reasons, command execution is disabled in this demo.
                To enable, implement proper ProcessBuilder with:
                1. Command whitelisting
                2. Path validation
                3. Timeout handling
                4. Output streaming
                5. Error handling
                
                The vibing_apk_lab.py tool can be run manually via:
                $command
            """.trimIndent()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if required tools are available
     */
    suspend fun checkToolAvailability(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        mapOf(
            "python" to (checkCommandExists("python3") || checkCommandExists("python")),
            "adb" to checkCommandExists("adb"),
            "apktool" to checkCommandExists("apktool"),
            "jadx" to checkCommandExists("jadx"),
            "zipalign" to checkCommandExists("zipalign"),
            "apksigner" to checkCommandExists("apksigner")
        )
    }
    
    private fun checkCommandExists(command: String): Boolean {
        return try {
            val process = ProcessBuilder(if (isWindows()) "$command.bat" else command, "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }
    
    /**
     * Get the path to the tools directory
     */
    fun getToolsDirectoryPath(): String {
        return toolsDirectory.absolutePath
    }
}
