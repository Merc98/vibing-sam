package com.example.ide.domain.apk

import android.content.Context
import android.net.Uri
import com.example.ide.data.local.DeviceRepository
import com.example.ide.data.repository.FileRepository
import com.example.ide.puente.data.ApkTarget
import java.io.File
import java.security.MessageDigest

class ApkImportService(
    private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val fileRepository: FileRepository
) {

    suspend fun importInstalledPackage(packageName: String): ApkTarget {
        val app = deviceRepository.getPackageInfo(packageName).getOrThrow()
            ?: error("Installed package not found: $packageName")
        val sourcePath = app.apkPath ?: error("APK path unavailable for $packageName")
        val sourceFile = File(sourcePath)
        require(sourceFile.exists()) { "APK file does not exist: $sourcePath" }

        return importFile(
            sourceFile = sourceFile,
            displayName = app.appName,
            sourceUri = sourcePath,
            packageName = app.packageName,
            versionName = app.versionName,
            versionCode = app.versionCode
        )
    }

    fun importApkFile(path: String): ApkTarget {
        val sourceFile = File(path)
        require(sourceFile.exists()) { "APK file does not exist: $path" }
        return importFile(
            sourceFile = sourceFile,
            displayName = sourceFile.nameWithoutExtension,
            sourceUri = path,
            packageName = null,
            versionName = null,
            versionCode = null
        )
    }

    fun importApkUri(uri: Uri): ApkTarget {
        val temp = File(context.cacheDir, "import_${System.currentTimeMillis()}.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open APK URI: $uri")

        return importFile(
            sourceFile = temp,
            displayName = temp.nameWithoutExtension,
            sourceUri = uri.toString(),
            packageName = null,
            versionName = null,
            versionCode = null
        )
    }

    private fun importFile(
        sourceFile: File,
        displayName: String,
        sourceUri: String,
        packageName: String?,
        versionName: String?,
        versionCode: Long?
    ): ApkTarget {
        val id = sha256(sourceFile)
        val workspace = File(context.filesDir, "workspace/$id").apply { mkdirs() }
        val copiedApk = File(workspace, "original.apk")
        sourceFile.inputStream().use { input ->
            copiedApk.outputStream().use { output -> input.copyTo(output) }
        }

        val target = ApkTarget(
            id = id,
            displayName = displayName,
            sourceUri = sourceUri,
            workspacePath = workspace.absolutePath,
            apkPath = copiedApk.absolutePath,
            sizeBytes = copiedApk.length(),
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode
        )
        fileRepository.saveApkTarget(target)
        return target
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
