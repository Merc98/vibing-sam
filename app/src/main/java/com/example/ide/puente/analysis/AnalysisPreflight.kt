package com.example.ide.puente.analysis

import android.content.Context
import com.example.ide.puente.binary.BinaryManager
import com.example.ide.puente.binary.BinaryTool
import com.example.ide.puente.data.ApkTarget
import java.io.File

/**
 * Lightweight pre-flight diagnostics so users know exactly what's missing
 * before running heavy analysis jobs.
 */
object AnalysisPreflight {

    data class Check(
        val name: String,
        val ok: Boolean,
        val detail: String
    )

    data class Snapshot(
        val checks: List<Check>,
        val apktoolReady: Boolean,
        val nativeDecompilerReady: Boolean
    ) {
        val passed: Int get() = checks.count { it.ok }
        val failed: Int get() = checks.size - passed
    }

    fun snapshot(context: Context, target: ApkTarget): Snapshot {
        val bm = BinaryManager(context)
        val apkFile = File(target.apkPath)
        val workspace = File(target.workspacePath)
        val nativeBin = File(bm.binDir(), "r2ghidra-dec")

        val checks = listOf(
            Check(
                name = "APK target exists",
                ok = apkFile.exists() && apkFile.isFile,
                detail = apkFile.absolutePath
            ),
            Check(
                name = "Workspace is writable",
                ok = (workspace.exists() || workspace.mkdirs()) && workspace.canWrite(),
                detail = workspace.absolutePath
            ),
            Check(
                name = "apktool payload extracted",
                ok = bm.isAvailable(BinaryTool.APKTOOL_JAR),
                detail = bm.getPath(BinaryTool.APKTOOL_JAR).absolutePath
            ),
            Check(
                name = "frida gadget present",
                ok = bm.isAvailable(BinaryTool.FRIDA_GADGET),
                detail = bm.getPath(BinaryTool.FRIDA_GADGET).absolutePath
            ),
            Check(
                name = "native decompiler present (optional)",
                ok = nativeBin.exists() && nativeBin.canExecute(),
                detail = nativeBin.absolutePath
            )
        )

        return Snapshot(
            checks = checks,
            apktoolReady = checks[0].ok && checks[1].ok && checks[2].ok,
            nativeDecompilerReady = checks.last().ok
        )
    }
}
