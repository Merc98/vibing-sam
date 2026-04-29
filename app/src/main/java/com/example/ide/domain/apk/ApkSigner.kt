package com.example.ide.domain.apk

import java.io.File

object ApkSigner {

    fun sign(inputApk: String, outputApk: String): Boolean {
        return try {
            val aligned = File(outputApk.replace(".apk", "_aligned.apk"))
            runCmd("zipalign -p 4 $inputApk ${aligned.absolutePath}")

            val signed = File(outputApk)
            runCmd("apksigner sign --ks debug.keystore --ks-pass pass:android --out ${signed.absolutePath} ${aligned.absolutePath}")

            signed.exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun runCmd(cmd: String) {
        Runtime.getRuntime().exec(cmd).waitFor()
    }
}
