package com.example.ide.puente.analysis

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * In-process JADX wrapper so the app can produce Java sources directly
 * without shelling out to a desktop environment.
 */
object JadxDecompiler {

    data class Result(
        val success: Boolean,
        val outputDir: File,
        val generatedFiles: Int,
        val message: String
    )

    suspend fun decompile(apkFile: File, outputDir: File): Result = withContext(Dispatchers.IO) {
        if (!apkFile.exists()) {
            return@withContext Result(
                success = false,
                outputDir = outputDir,
                generatedFiles = 0,
                message = "APK not found: ${apkFile.absolutePath}"
            )
        }

        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()

        return@withContext try {
            val args = JadxArgs().apply {
                inputFile = apkFile
                outDir = outputDir
                isShowInconsistentCode = true
                isDeobfuscationOn = false
                isSkipResources = true
                threadsCount = 1
            }

            JadxDecompiler(args).use { jadx ->
                jadx.load()
                jadx.save()
            }

            val javaCount = outputDir.walkTopDown().count { it.isFile && it.extension == "java" }
            Result(
                success = true,
                outputDir = outputDir,
                generatedFiles = javaCount,
                message = "jadx completed"
            )
        } catch (t: Throwable) {
            Result(
                success = false,
                outputDir = outputDir,
                generatedFiles = 0,
                message = "jadx failed: ${t.message}"
            )
        }
    }
}
