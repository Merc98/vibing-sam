package com.example.ide.domain.apk

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.security.auth.x500.X500Principal

/**
 * Android-compatible APK signing/export boundary for Vibing MOD.
 *
 * This intentionally does NOT shell out to desktop-only binaries such as
 * apksigner or zipalign. It uses the Android-compatible apksig library and an
 * app-private AndroidKeyStore signing key.
 */
class ApkExportSigner(private val context: Context) {

    fun signAndExport(rebuiltApkPath: String): ExportResult {
        val rebuilt = File(rebuiltApkPath)
        if (!rebuilt.exists() || rebuilt.length() == 0L) {
            return ExportResult(false, "Rebuilt APK not found or empty", null, signed = false)
        }

        return try {
            val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "VibingMOD/exports")
                .apply { mkdirs() }
            val signedApk = File(exportDir, "mod_${System.currentTimeMillis()}_signed.apk")

            val identity = getOrCreateSigningIdentity()
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "vibing-mod",
                identity.privateKey,
                listOf(identity.certificate)
            ).build()

            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(rebuilt)
                .setOutputApk(signedApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .build()
                .sign()

            if (!signedApk.exists() || signedApk.length() == 0L) {
                return ExportResult(false, "apksig finished but signed APK is missing", null, signed = false)
            }

            ExportResult(
                success = true,
                message = "APK signed and exported with Vibing MOD app key. Note: it cannot update the original app unless signatures match.",
                path = signedApk.absolutePath,
                signed = true
            )
        } catch (e: Exception) {
            exportUnsignedFallback(rebuilt, "Signing failed: ${e.message}")
        }
    }

    fun exportUnsignedFallback(rebuiltApk: File, reason: String): ExportResult {
        val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "VibingMOD/exports")
            .apply { mkdirs() }
        val exported = File(exportDir, "mod_${System.currentTimeMillis()}_unsigned.apk")
        rebuilt.inputStream().use { input ->
            exported.outputStream().use { output -> input.copyTo(output) }
        }
        return ExportResult(
            success = true,
            message = "$reason. Exported unsigned APK; install may fail until signing is fixed.",
            path = exported.absolutePath,
            signed = false
        )
    }

    private fun getOrCreateSigningIdentity(): SigningIdentity {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createSigningKey()
        }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val certificate = keyStore.getCertificate(KEY_ALIAS) as X509Certificate
        return SigningIdentity(privateKey, certificate)
    }

    private fun createSigningKey() {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.YEAR, 30) }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        )
            .setCertificateSubject(X500Principal("CN=Vibing MOD,O=Merc98,C=US"))
            .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            .setCertificateNotBefore(start.time)
            .setCertificateNotAfter(end.time)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE).apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    private data class SigningIdentity(
        val privateKey: PrivateKey,
        val certificate: X509Certificate
    )

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "vibing_mod_apk_signing_key"
    }
}

data class ExportResult(
    val success: Boolean,
    val message: String,
    val path: String?,
    val signed: Boolean
)
