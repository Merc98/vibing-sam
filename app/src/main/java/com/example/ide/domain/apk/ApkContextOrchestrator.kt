package com.example.ide.domain.apk

import java.io.File

class ApkContextOrchestrator(
    private val fileRepository: com.example.ide.data.repository.FileRepository
) {

    fun prepareWorkspace(source: ApkSource): ApkWorkspace {
        val apkPath = when (source) {
            is ApkSource.InstalledPackage -> fileRepository.getApkPath(source.packageName)
            is ApkSource.ApkFile -> source.path
        }

        val rootDir = File(fileRepository.getWorkspaceRoot(), "apk_${System.currentTimeMillis()}")
        val decodedDir = File(rootDir, "decoded")
        val jadxDir = File(rootDir, "jadx")
        val outputApk = File(rootDir, "output_signed.apk")

        rootDir.mkdirs()
        decodedDir.mkdirs()
        jadxDir.mkdirs()

        // APKTool decode
        com.example.ide.puente.exec.ApktoolRunner.decode(apkPath, decodedDir.absolutePath)

        // Jadx decompile
        com.example.ide.puente.analysis.JadxDecompiler.decompile(
            File(apkPath),
            jadxDir
        )

        return ApkWorkspace(
            packageName = null,
            originalApkPath = apkPath,
            rootDir = rootDir.absolutePath,
            decodedDir = decodedDir.absolutePath,
            jadxDir = jadxDir.absolutePath,
            outputApkPath = outputApk.absolutePath
        )
    }

    fun buildContext(workspace: ApkWorkspace): ApkContext {
        val manifestFile = File(workspace.decodedDir, "AndroidManifest.xml")
        val manifestSnippet = if (manifestFile.exists()) {
            manifestFile.readText().take(2000)
        } else ""

        val resourceTree = File(workspace.decodedDir)
            .walkTopDown()
            .filter { it.isFile }
            .take(100)
            .map { it.relativeTo(File(workspace.decodedDir)).path }
            .toList()

        val candidateFiles = resourceTree.take(10).map {
            val file = File(workspace.decodedDir, it)
            ApkCandidateFile(
                path = it,
                contentSnippet = file.takeIf { f -> f.exists() }?.readText()?.take(500) ?: ""
            )
        }

        return ApkContext(
            manifestSnippet = manifestSnippet,
            resourceTree = resourceTree,
            candidateFiles = candidateFiles
        )
    }
}
