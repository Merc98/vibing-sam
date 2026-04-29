package com.example.ide.domain.apk

import android.content.Context
import com.example.ide.data.repository.FileRepository
import com.example.ide.puente.analysis.JadxDecompiler
import com.example.ide.puente.exec.ApktoolRunner
import java.io.File

class ApkContextOrchestrator(
    private val context: Context,
    private val fileRepository: FileRepository
) {

    suspend fun prepareWorkspace(source: ApkSource): ApkWorkspace {
        val resolved = resolveApk(source)
        val rootDir = File(context.filesDir, "apk_transform_${System.currentTimeMillis()}")
        val decodedDir = File(rootDir, "decoded")
        val jadxDir = File(rootDir, "jadx")
        val unsignedApk = File(rootDir, "rebuilt_unsigned.apk")

        rootDir.mkdirs()
        decodedDir.mkdirs()
        jadxDir.mkdirs()

        val decodeResult = ApktoolRunner.run(
            context = context,
            args = listOf("d", "-f", resolved.apkPath, "-o", decodedDir.absolutePath)
        )
        require(decodeResult.exitCode == 0) {
            "apktool decode failed: ${decodeResult.stderr.ifBlank { decodeResult.stdout }}"
        }

        val jadxResult = JadxDecompiler.decompile(File(resolved.apkPath), jadxDir)
        require(jadxResult.success) { jadxResult.message }

        return ApkWorkspace(
            packageName = resolved.packageName,
            originalApkPath = resolved.apkPath,
            rootDir = rootDir.absolutePath,
            decodedDir = decodedDir.absolutePath,
            jadxDir = jadxDir.absolutePath,
            outputApkPath = unsignedApk.absolutePath
        )
    }

    fun buildContext(workspace: ApkWorkspace): ApkContext {
        val decodedRoot = File(workspace.decodedDir)
        val manifestFile = File(decodedRoot, "AndroidManifest.xml")
        val manifestSnippet = manifestFile.safeRead(3000)

        val files = decodedRoot.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(decodedRoot).path }
            .toList()

        val prioritized = files.sortedWith(compareByDescending<String> { path ->
            when {
                path.endsWith("AndroidManifest.xml") -> 100
                path.endsWith("res/values/colors.xml") -> 95
                path.endsWith("res/values/styles.xml") -> 90
                path.endsWith("res/values/strings.xml") -> 85
                path.startsWith("res/layout") -> 80
                path.startsWith("res/drawable") -> 60
                path.endsWith(".smali") -> 30
                else -> 10
            }
        }.thenBy { it })

        val candidateFiles = prioritized.take(40).mapNotNull { relativePath ->
            val file = File(decodedRoot, relativePath)
            val snippet = file.safeRead(1800)
            if (snippet.isBlank()) null else ApkCandidateFile(relativePath, snippet)
        }

        return ApkContext(
            manifestSnippet = manifestSnippet,
            resourceTree = prioritized.take(250),
            candidateFiles = candidateFiles
        )
    }

    private fun resolveApk(source: ApkSource): ResolvedApk {
        return when (source) {
            is ApkSource.ApkFile -> ResolvedApk(packageName = null, apkPath = source.path)
            is ApkSource.InstalledPackage -> {
                val target = fileRepository.getApkTargetByPackage(source.packageName)
                    ?: error("APK target not imported for package: ${source.packageName}")
                ResolvedApk(packageName = target.packageName, apkPath = target.apkPath)
            }
        }
    }

    private fun File.safeRead(limit: Int): String {
        return try {
            if (!exists() || length() > 2_000_000) "" else readText().take(limit)
        } catch (_: Exception) {
            ""
        }
    }

    private data class ResolvedApk(
        val packageName: String?,
        val apkPath: String
    )
}
