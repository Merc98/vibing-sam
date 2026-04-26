package com.example.ide.puente.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ApkImporter {

    /**
     * Streams the content:// source into filesDir/workspace/<sha256>/<name>.apk
     * returning the resulting ApkTarget (without package metadata yet — that is
     * filled in by ApkInfoScreen using PackageManager).
     */
    suspend fun import(context: Context, source: Uri, displayName: String): ApkTarget = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "puente_import").apply { mkdirs() }
        val tempFile = File(tempDir, "staging_${System.currentTimeMillis()}.apk")
        val resolver = context.contentResolver

        val md = MessageDigest.getInstance("SHA-256")
        var size = 0L
        resolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Cannot open URI $source" }
            FileOutputStream(tempFile).use { out ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                    out.write(buf, 0, n)
                    size += n
                }
            }
        }
        val sha = md.digest().joinToString("") { "%02x".format(it) }

        val workspaceRoot = File(context.filesDir, "workspace/$sha").apply { mkdirs() }
        val finalApk = File(workspaceRoot, "source.apk")
        if (finalApk.exists()) finalApk.delete()
        tempFile.copyTo(finalApk, overwrite = true)
        tempFile.delete()

        ApkTarget(
            id = sha,
            displayName = displayName,
            sourceUri = source.toString(),
            workspacePath = workspaceRoot.absolutePath,
            apkPath = finalApk.absolutePath,
            sizeBytes = size
        )
    }
}
