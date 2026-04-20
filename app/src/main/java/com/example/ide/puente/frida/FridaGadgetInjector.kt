package com.example.ide.puente.frida

import android.content.Context
import com.example.ide.puente.binary.BinaryManager
import com.example.ide.puente.binary.BinaryTool
import com.example.ide.puente.data.ApkTarget
import com.example.ide.puente.exec.ApktoolRunner
import com.example.ide.puente.sign.PuenteSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Automates the Frida-gadget injection pipeline end-to-end:
 *
 * 1. apktool d source.apk -o workspace/smali/
 * 2. Copy frida-gadget.so → workspace/smali/lib/arm64-v8a/libfrida-gadget.so
 * 3. Find launcher Activity .smali (parsed from AndroidManifest.xml)
 * 4. Inject System.loadLibrary("frida-gadget") into its onCreate
 * 5. apktool b → workspace/rebuilt_frida.apk
 * 6. apksig zipalign + sign → workspace/patched_frida_signed.apk
 *
 * Reference: https://github.com/Stanislav-Povolotsky/pentest--android--apk-patcher
 */
object FridaGadgetInjector {

    fun run(context: Context, target: ApkTarget): Flow<String> = flow {
        val ws = File(target.workspacePath)
        val smaliDir = File(ws, "smali_frida").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        val rebuilt = File(ws, "rebuilt_frida.apk")
        val signed = File(ws, "patched_frida_signed.apk")

        emit("▶ step 1 — apktool decode to ${smaliDir.name}")
        val decode = ApktoolRunner.run(
            context,
            listOf("d", target.apkPath, "-o", smaliDir.absolutePath, "-f")
        )
        emit(decode.stdout.ifBlank { "(decode stdout empty)" })
        if (decode.stderr.isNotBlank()) emit("stderr: ${decode.stderr}")
        if (decode.exitCode != 0) {
            emit("✗ decode failed (exit=${decode.exitCode}). Pipeline aborted.")
            return@flow
        }

        emit("▶ step 2 — placing frida-gadget arm64 into lib/arm64-v8a/")
        val bm = BinaryManager(context)
        val gadget = bm.getPath(BinaryTool.FRIDA_GADGET)
        if (!gadget.exists()) {
            emit("✗ frida-gadget binary missing from filesDir/bin. Re-run BinaryManager.extractAll().")
            return@flow
        }
        val libDir = File(smaliDir, "lib/arm64-v8a").apply { mkdirs() }
        val dest = File(libDir, "libfrida-gadget.so")
        gadget.copyTo(dest, overwrite = true)
        emit("  wrote ${dest.absolutePath} (${dest.length()} B)")

        emit("▶ step 3 — locating launcher Activity from decoded manifest")
        val launcher = SmaliPatcher.findLauncherActivityClass(smaliDir)
        if (launcher == null) {
            emit("✗ no launcher Activity found in AndroidManifest.xml. Aborting.")
            return@flow
        }
        emit("  launcher = $launcher")

        emit("▶ step 4 — patching onCreate with System.loadLibrary(\"frida-gadget\")")
        val patchResult = SmaliPatcher.injectLoadLibrary(smaliDir, launcher, "frida-gadget")
        emit("  ${patchResult.detail}")
        if (!patchResult.success) {
            emit("✗ smali patching failed. Aborting.")
            return@flow
        }

        emit("▶ step 5 — apktool build → ${rebuilt.name}")
        val build = ApktoolRunner.run(
            context,
            listOf("b", smaliDir.absolutePath, "-o", rebuilt.absolutePath)
        )
        emit(build.stdout.ifBlank { "(build stdout empty)" })
        if (build.stderr.isNotBlank()) emit("stderr: ${build.stderr}")
        if (build.exitCode != 0 || !rebuilt.exists()) {
            emit("✗ rebuild failed (exit=${build.exitCode}). Aborting.")
            return@flow
        }

        emit("▶ step 6 — zipalign + sign via apksig → ${signed.name}")
        val signResult = PuenteSigner.zipAlignAndSign(context, rebuilt, signed)
        emit("  ${signResult.detail}")

        if (signResult.success) {
            emit("✓ pipeline complete. Install via Patch screen.")
        } else {
            emit("✗ signing step failed.")
        }
    }.flowOn(Dispatchers.IO)
}
