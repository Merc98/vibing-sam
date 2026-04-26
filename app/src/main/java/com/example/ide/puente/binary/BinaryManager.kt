package com.example.ide.puente.binary

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Properties

/**
 * Extracts Puente binaries from the APK's assets/bin/ to the app's private
 * filesDir/bin/ directory.
 *
 * Execution from externalStorage is forbidden by SELinux on modern Android;
 * filesDir (== /data/data/<pkg>/files) is the only supported exec root.
 */
class BinaryManager(private val context: Context) {

    private val binDir: File = File(context.filesDir, "bin").apply { if (!exists()) mkdirs() }
    private val markerFile = File(binDir, ".extracted")

    data class ExtractionReport(
        val success: Boolean,
        val extracted: List<String>,
        val failed: List<Pair<String, String>>,
        val manifestVersion: String?
    )

    suspend fun extractAll(force: Boolean = false): ExtractionReport = withContext(Dispatchers.IO) {
        val extracted = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()

        if (!force && markerFile.exists()) {
            return@withContext ExtractionReport(
                success = true,
                extracted = emptyList(),
                failed = emptyList(),
                manifestVersion = readManifest()?.getProperty("apktool.version")
            )
        }

        BinaryTool.values().forEach { tool ->
            try {
                extract(tool)
                extracted.add(tool.extractedName)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to extract ${tool.assetName}: ${t.message}")
                failed.add(tool.assetName to (t.message ?: "unknown error"))
            }
        }

        if (failed.isEmpty()) {
            markerFile.writeText(System.currentTimeMillis().toString())
        }

        ExtractionReport(
            success = failed.isEmpty(),
            extracted = extracted,
            failed = failed,
            manifestVersion = readManifest()?.getProperty("apktool.version")
        )
    }

    fun getPath(tool: BinaryTool): File = File(binDir, tool.extractedName)

    fun isAvailable(tool: BinaryTool): Boolean {
        val f = getPath(tool)
        return f.exists() && f.length() > 0
    }

    fun binDir(): File = binDir

    private fun extract(tool: BinaryTool) {
        val out = File(binDir, tool.extractedName)
        context.assets.open("bin/${tool.assetName}").use { input ->
            if (tool.isXzCompressed) {
                XZInputStream(input).use { xz ->
                    out.outputStream().use { o -> xz.copyTo(o) }
                }
            } else {
                out.outputStream().use { o -> input.copyTo(o) }
            }
        }
        if (tool.isExecutable) {
            out.setExecutable(true, false)
            out.setReadable(true, false)
        }
    }

    fun verify(): Map<String, VerifyResult> {
        val manifest = readManifest() ?: return emptyMap()
        val results = mutableMapOf<String, VerifyResult>()

        val apkChecksum = manifest.getProperty("apktool.sha256")
        if (apkChecksum != null) {
            val current = sha256(getPath(BinaryTool.APKTOOL_JAR))
            results["apktool.jar"] = VerifyResult(
                expected = apkChecksum,
                actual = current,
                matches = current.equals(apkChecksum, ignoreCase = true)
            )
        }
        return results
    }

    private fun readManifest(): Properties? {
        val f = File(binDir, "manifest.properties")
        if (!f.exists()) return null
        return Properties().apply { f.inputStream().use { load(it) } }
    }

    private fun sha256(f: File): String {
        if (!f.exists()) return ""
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(f).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    data class VerifyResult(val expected: String, val actual: String, val matches: Boolean)

    companion object {
        private const val TAG = "PuenteBinaryManager"
    }
}
