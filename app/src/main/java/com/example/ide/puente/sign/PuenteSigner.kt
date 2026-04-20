package com.example.ide.puente.sign

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

/**
 * Signs APKs fully in-process using apksig + AndroidKeyStore. A self-signed
 * RSA key + certificate pair is created on first use inside AndroidKeyStore
 * (alias "puente-signing") and reused afterwards.
 */
object PuenteSigner {

    private const val ALIAS = "puente-signing"
    private const val KEYSTORE = "AndroidKeyStore"

    data class Result(val success: Boolean, val detail: String)

    fun zipAlignAndSign(context: Context, inputApk: File, outputApk: File): Result {
        return try {
            val (privateKey, certificate) = ensureKeyPair()

            val config = ApkSigner.SignerConfig.Builder(
                "puente",
                privateKey,
                listOf(certificate)
            ).build()

            if (outputApk.exists()) outputApk.delete()

            ApkSigner.Builder(listOf(config))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(false)
                .build()
                .sign()

            Result(true, "Signed APK: ${outputApk.absolutePath} (${outputApk.length()} B)")
        } catch (t: Throwable) {
            Result(false, "Signing failed: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun ensureKeyPair(): Pair<PrivateKey, X509Certificate> {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(ALIAS)) {
            val key = ks.getKey(ALIAS, null) as PrivateKey
            val cert = ks.getCertificate(ALIAS) as X509Certificate
            return key to cert
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE)
        val now = Date()
        val end = Date(now.time + 365L * 24 * 3600 * 1000 * 30) // ~30 years

        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSerialNumber(BigInteger.valueOf(now.time))
            .setCertificateSubject(X500Principal("CN=Puente, O=Puente, C=US"))
            .setCertificateNotBefore(now)
            .setCertificateNotAfter(end)
            .build()

        kpg.initialize(spec)
        kpg.generateKeyPair()

        val loaded = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val key = loaded.getKey(ALIAS, null) as PrivateKey
        val cert = loaded.getCertificate(ALIAS) as X509Certificate
        return key to cert
    }
}
