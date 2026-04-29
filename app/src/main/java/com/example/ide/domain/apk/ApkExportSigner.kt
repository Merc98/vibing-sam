package com.example.ide.domain.apk

import android.content.Context
import java.io.File

/**
 * Android-compatible export signer boundary.
 *
 * The app should not shell out to desktop-only zipalign/apksigner binaries.
 * This class is the single boundary for signing/export. The current safe
 * implementation exports the rebuilt APK and clearly marks it unsigned.
 *
 * Next implementation step: wire com.android.apksig signing here with an
 * app-managed debug keystore created inside app-private storage.
 */
class ApkExportSigner(private val context: Context) {

    fun exportUnsigned(rebuiltApkPath: String): ExportResult {
        val rebuilt = File(rebuiltApkPath)
        if (!rebuilt.exists()) {
            return ExportResult(false, "Rebuilt APK not found", null, signed = false)
        }

        val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "VibingMOD/exports")
            .apply { mkdirs() }
        val exported = File(exportDir, "mod_${System.currentTimeMillis()}_unsigned.apk")
        rebuilt.inputStream().use { input ->
            exported.outputStream().use { output -> input.copyTo(output) }
        }

        return ExportResult(
            success = true,
            message = "APK exported unsigned. Signing via apksig is required before install.",
            path = exported.absolutePath,
            signed = false
        )
    }
}

data class ExportResult(
    val success: Boolean,
    val message: String,
    val path: String?,
    val signed: Boolean
)
