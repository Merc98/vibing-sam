package com.example.ide.data.repository

import android.content.Context
import android.os.Environment
import com.example.ide.data.model.CodeFile
import com.example.ide.data.model.FileExtension
import com.example.ide.data.model.Project
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedInputStream
import java.io.File
import java.io.FileWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class PatchBundle(
    val fileName: String,
    val absolutePath: String,
    val patchFileCount: Int,
    val sizeBytes: Long,
    val lastModified: Long,
    val detectedLanguage: String,
    val recommendedTool: String,
    val toolUtility: String,
    val utilitySummary: String,
    val isCompatibleWithCurrentProject: Boolean,
    val compatibilityMessage: String
)

class FileRepository(
    private val context: Context
) {
    private val gson = Gson()
    private val projectsFile = File(context.filesDir, "projects.json")
    
    // Create a projects directory in Downloads
    private val projectsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IDEProjects")
    private val patchBundlesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IDEPatches")
    
    init {
        // Ensure the projects directory exists
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
        if (!patchBundlesDir.exists()) {
            patchBundlesDir.mkdirs()
        }
    }
    
    fun saveProject(project: Project) {
        val projects = getAllProjects().toMutableList()
        val existingIndex = projects.indexOfFirst { it.id == project.id }
        
        if (existingIndex >= 0) {
            projects[existingIndex] = project.copy(lastModified = System.currentTimeMillis())
        } else {
            projects.add(project)
        }

        saveProjects(projects)
        
        // Save the project files to the project directory
        saveProjectFiles(project)
    }
    
    fun getAllProjects(): List<Project> {
        return if (projectsFile.exists()) {
            try {
                val json = projectsFile.readText()
                val type = object : TypeToken<List<Project>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun deleteProject(projectId: String) {
        val projects = getAllProjects().toMutableList()
        val projectToDelete = projects.find { it.id == projectId }
        projects.removeAll { it.id == projectId }
        saveProjects(projects)
        
        // Delete project directory from file system
        projectToDelete?.let {
            val projectDir = File(projectsDir, it.name)
            if (projectDir.exists()) {
                deleteRecursively(projectDir)
            }
        }
    }
    
    private fun saveProjects(projects: List<Project>) {
        try {
            val json = gson.toJson(projects)
            projectsFile.writeText(json)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Save project files to the project directory in Downloads
     */
    private fun saveProjectFiles(project: Project) {
        try {
            // Create project directory
            val projectDir = File(projectsDir, project.name)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }
            
            // Save each file in the project
            for (file in project.files) {
                val fullFileName = if (file.name.endsWith(".${file.extension}")) {
                    file.name
                } else {
                    "${file.name}.${file.extension}"
                }
                
                val projectFile = File(projectDir, fullFileName)
                FileWriter(projectFile).use { writer ->
                    writer.write(file.content)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Save a code file directly to a project folder
     */
    fun saveFileToProject(projectName: String, fileName: String, content: String, extension: String): Result<String> {
        return try {
            // Create project directory if it doesn't exist
            val projectDir = File(projectsDir, projectName)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }
            
            // Create the file with proper extension
            val fullFileName = if (fileName.endsWith(".$extension")) fileName else "$fileName.$extension"
            val file = File(projectDir, fullFileName)
            
            // Handle file name conflicts
            var counter = 1
            var finalFile = file
            while (finalFile.exists()) {
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val finalFileName = "${nameWithoutExt}_$counter.$extension"
                finalFile = File(projectDir, finalFileName)
                counter++
            }
            
            FileWriter(finalFile).use { writer ->
                writer.write(content)
            }
            
            Result.success(finalFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun saveFileToDownloads(fileName: String, content: String, extension: String): Result<String> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fullFileName = if (fileName.endsWith(".$extension")) fileName else "$fileName.$extension"
            val file = File(downloadsDir, fullFileName)
            
            // Handle file name conflicts
            var counter = 1
            var finalFile = file
            while (finalFile.exists()) {
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val finalFileName = "${nameWithoutExt}_$counter.$extension"
                finalFile = File(downloadsDir, finalFileName)
                counter++
            }
            
            FileWriter(finalFile).use { writer ->
                writer.write(content)
            }
            
            Result.success(finalFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun createNewFile(name: String, extension: String): CodeFile {
        val fileExtension = FileExtension.fromExtension(extension)
        return CodeFile(
            name = name,
            content = getTemplateContent(fileExtension),
            extension = extension,
            language = fileExtension.language
        )
    }
    
    private fun getTemplateContent(extension: FileExtension): String {
        return when (extension) {
            FileExtension.HTML -> """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    
</body>
</html>"""
            
            FileExtension.PYTHON -> """#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def main():
    print("Hello, World!")

if __name__ == "__main__":
    main()"""
    
            FileExtension.JAVASCRIPT -> """// JavaScript file
console.log("Hello, World!");"""
    
            FileExtension.JAVA -> """public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}"""
    
            FileExtension.KOTLIN -> """fun main() {
    println("Hello, World!")
}"""
    
            FileExtension.CPP -> """#include <iostream>

int main() {
    std::cout << "Hello, World!" << std::endl;
    return 0;
}"""
    
            FileExtension.CSS -> """/* CSS Styles */
body {
    font-family: Arial, sans-serif;
    margin: 0;
    padding: 20px;
}"""
    
            else -> ""
        }
    }
    
    /**
     * Delete a file from a project both in memory and on disk
     */
    fun deleteFileFromProject(projectName: String, fileName: String, extension: String) {
        try {
            // Delete file from project directory
            val projectDir = File(projectsDir, projectName)
            if (projectDir.exists()) {
                val fullFileName = if (fileName.endsWith(".$extension")) fileName else "$fileName.$extension"
                val file = File(projectDir, fullFileName)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    /**
     * Recursively delete a directory and all its contents
     */
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        file.delete()
    }

    fun listPatchBundles(currentProject: Project? = null): List<PatchBundle> {
        if (!patchBundlesDir.exists()) return emptyList()
        val zipFiles = patchBundlesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".zip", ignoreCase = true)
        }?.toList().orEmpty()

        return zipFiles.map { zipFile ->
            val analysis = analyzePatchBundle(zipFile)
            val compatibility = evaluateCompatibility(currentProject, analysis.extensions)
            PatchBundle(
                fileName = zipFile.name,
                absolutePath = zipFile.absolutePath,
                patchFileCount = countZipFileEntries(zipFile),
                sizeBytes = zipFile.length(),
                lastModified = zipFile.lastModified(),
                detectedLanguage = analysis.detectedLanguage,
                recommendedTool = analysis.recommendedTool,
                toolUtility = analysis.toolUtility,
                utilitySummary = analysis.utilitySummary,
                isCompatibleWithCurrentProject = compatibility.first,
                compatibilityMessage = compatibility.second
            )
        }.sortedByDescending { it.lastModified }
    }

    fun applyPatchBundleToProject(project: Project, zipFileName: String): Result<Int> {
        val zipFile = File(patchBundlesDir, zipFileName)
        if (!zipFile.exists()) {
            return Result.failure(Exception("Patch bundle not found: $zipFileName"))
        }

        val projectDir = File(projectsDir, project.name)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }

        return try {
            var importedFiles = 0
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val currentEntry = entry
                    if (!currentEntry.isDirectory && isSafeZipPath(currentEntry.name)) {
                        val outputFile = File(projectDir, currentEntry.name)
                        outputFile.parentFile?.mkdirs()
                        outputFile.outputStream().use { output ->
                            zis.copyTo(output)
                        }
                        importedFiles++

                        val baseName = outputFile.nameWithoutExtension
                        val extension = outputFile.extension.ifBlank { "txt" }
                        val content = outputFile.readText()
                        val existingFileIndex = project.files.indexOfFirst {
                            it.name == baseName && it.extension.equals(extension, ignoreCase = true)
                        }
                        val updatedFile = createNewFile(baseName, extension).copy(
                            content = content,
                            isModified = false
                        )
                        if (existingFileIndex >= 0) {
                            project.files[existingFileIndex] = updatedFile
                        } else {
                            project.files.add(updatedFile)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            saveProject(project)
            Result.success(importedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPatchBundlesDirectoryPath(): String = patchBundlesDir.absolutePath

    private fun countZipFileEntries(file: File): Int {
        return try {
            var count = 0
            ZipInputStream(BufferedInputStream(file.inputStream())).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isSafeZipPath(entry.name)) {
                        count++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    private fun isSafeZipPath(path: String): Boolean {
        if (path.contains("..")) return false
        if (path.startsWith("/") || path.startsWith("\\")) return false
        return true
    }

    private data class PatchBundleAnalysis(
        val extensions: Set<String>,
        val detectedLanguage: String,
        val recommendedTool: String,
        val toolUtility: String,
        val utilitySummary: String
    )

    private fun analyzePatchBundle(zipFile: File): PatchBundleAnalysis {
        val extensionCount = linkedMapOf<String, Int>()
        var hasSmali = false
        var hasAndroidManifest = false
        var hasResourcesFolder = false
        var hasDecompiledJavaOrKt = false
        var hasApktoolMetadata = false
        var hasJadxExportHints = false
        var descriptionFromBundle: String? = null

        try {
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isSafeZipPath(entry.name)) {
                        val normalizedPath = entry.name.lowercase()
                        val extension = entry.name.substringAfterLast(".", "").lowercase()
                        if (extension.isNotBlank()) {
                            extensionCount[extension] = (extensionCount[extension] ?: 0) + 1
                        }
                        if (normalizedPath.contains("/smali") || normalizedPath.startsWith("smali")) {
                            hasSmali = true
                        }
                        if (normalizedPath.endsWith("androidmanifest.xml")) {
                            hasAndroidManifest = true
                        }
                        if (normalizedPath.startsWith("res/") || normalizedPath.contains("/res/")) {
                            hasResourcesFolder = true
                        }
                        if (normalizedPath.contains("sources/") && (extension == "java" || extension == "kt")) {
                            hasDecompiledJavaOrKt = true
                        }
                        if (normalizedPath.endsWith("apktool.yml")) {
                            hasApktoolMetadata = true
                        }
                        if (normalizedPath.contains("jadx") || normalizedPath.contains("resources/") || normalizedPath.contains("sources/")) {
                            hasJadxExportHints = true
                        }

                        val fileName = entry.name.substringAfterLast("/").lowercase()
                        if (descriptionFromBundle == null && (fileName == "patch-info.txt" || fileName == "readme.txt")) {
                            descriptionFromBundle = zis.bufferedReader().readText().trim().take(220)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
            // Keep fallback messages if ZIP can't be analyzed.
        }

        val mainExtension = extensionCount.maxByOrNull { it.value }?.key.orEmpty()
        val detectedLanguage = extensionToLanguage(mainExtension)
        val toolRecommendation = recommendTool(
            hasSmali = hasSmali,
            hasAndroidManifest = hasAndroidManifest,
            hasResourcesFolder = hasResourcesFolder,
            hasDecompiledJavaOrKt = hasDecompiledJavaOrKt,
            hasApktoolMetadata = hasApktoolMetadata,
            hasJadxExportHints = hasJadxExportHints
        )
        val utilitySummary = descriptionFromBundle
            ?.takeIf { it.isNotBlank() }
            ?: defaultUtilitySummary(detectedLanguage, extensionCount.keys)

        return PatchBundleAnalysis(
            extensions = extensionCount.keys,
            detectedLanguage = detectedLanguage,
            recommendedTool = toolRecommendation.first,
            toolUtility = toolRecommendation.second,
            utilitySummary = utilitySummary
        )
    }

    private fun evaluateCompatibility(
        currentProject: Project?,
        patchExtensions: Set<String>
    ): Pair<Boolean, String> {
        if (currentProject == null) {
            return false to "Abre un proyecto para validar si este parche aplica."
        }

        if (patchExtensions.isEmpty()) {
            return true to "Parche genérico sin extensión clara. Puedes probarlo en este proyecto."
        }

        val projectExtensions = currentProject.files.map { it.extension.lowercase() }.toSet()
        if (projectExtensions.isEmpty()) {
            return true to "Proyecto vacío. Puedes aplicar este parche para empezar."
        }

        val overlap = projectExtensions.intersect(patchExtensions)
        return if (overlap.isNotEmpty()) {
            true to "Compatible con tu proyecto (${overlap.joinToString(", ")})."
        } else {
            false to "No coincide con las extensiones actuales del proyecto (${projectExtensions.joinToString(", ")})."
        }
    }

    private fun extensionToLanguage(extension: String): String {
        return when (extension) {
            "kt" -> "Kotlin"
            "java" -> "Java"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "py" -> "Python"
            "html" -> "HTML"
            "css" -> "CSS"
            "json" -> "JSON"
            "xml" -> "XML"
            "md" -> "Markdown"
            "cpp", "cc", "cxx" -> "C++"
            "c" -> "C"
            "rs" -> "Rust"
            "go" -> "Go"
            else -> "General"
        }
    }

    private fun defaultUtilitySummary(language: String, extensions: Set<String>): String {
        if (extensions.isEmpty()) {
            return "Parche sin archivos claros. Revisa su contenido antes de usarlo."
        }
        return "Parche orientado a $language para acelerar cambios comunes y mantenimiento del proyecto."
    }

    private fun recommendTool(
        hasSmali: Boolean,
        hasAndroidManifest: Boolean,
        hasResourcesFolder: Boolean,
        hasDecompiledJavaOrKt: Boolean,
        hasApktoolMetadata: Boolean,
        hasJadxExportHints: Boolean
    ): Pair<String, String> {
        val looksLikeApktool = hasApktoolMetadata || hasSmali || (hasAndroidManifest && hasResourcesFolder)
        val looksLikeJadx = hasDecompiledJavaOrKt || hasJadxExportHints

        return when {
            looksLikeApktool && looksLikeJadx ->
                "APKTool + JADX" to "Útil para editar recursos/smali y revisar código decompilado."
            looksLikeApktool ->
                "APKTool" to "Útil para cambios de AndroidManifest, recursos (res/) y smali."
            looksLikeJadx ->
                "JADX" to "Útil para lectura de código Java/Kotlin decompilado."
            else ->
                "General ZIP" to "Bundle genérico: revisa README/patch-info para su uso."
        }
    }

}
