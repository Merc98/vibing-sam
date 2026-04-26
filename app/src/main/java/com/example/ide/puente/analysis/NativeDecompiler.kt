package com.example.ide.puente.analysis

import android.content.Context
import com.example.ide.puente.binary.BinaryManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin adapter for optional r2ghidra-dec style binaries dropped in filesDir/bin.
 * Expected binary name: r2ghidra-dec
 */
object NativeDecompiler {

    data class Result(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val detail: String
    )

    suspend fun run(
        context: Context,
        targetSo: File,
        symbolOrAddress: String
    ): Result = withContext(Dispatchers.IO) {
        val bin = File(BinaryManager(context).binDir(), "r2ghidra-dec")
        if (!bin.exists()) {
            return@withContext Result(
                success = false,
                stdout = "",
                stderr = "",
                exitCode = -1,
                detail = "Missing native decompiler binary: ${bin.absolutePath}"
            )
        }
        if (!targetSo.exists()) {
            return@withContext Result(
                success = false,
                stdout = "",
                stderr = "",
                exitCode = -1,
                detail = "Shared object not found: ${targetSo.absolutePath}"
            )
        }

        val command = mutableListOf(
            bin.absolutePath,
            "-e", "asm.arch=arm",
            "-e", "asm.bits=64",
            "-q",
            "-A",
            targetSo.absolutePath
        )
        if (symbolOrAddress.isNotBlank()) {
            command += listOf("-c", "s $symbolOrAddress; pdg")
        }

        return@withContext try {
            val proc = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            val exit = proc.waitFor()
            Result(
                success = exit == 0,
                stdout = stdout,
                stderr = stderr,
                exitCode = exit,
                detail = if (exit == 0) "r2ghidra analysis complete" else "r2ghidra exit=$exit"
            )
        } catch (t: Throwable) {
            Result(
                success = false,
                stdout = "",
                stderr = t.stackTraceToString(),
                exitCode = -1,
                detail = "Native analysis error: ${t.message}"
            )
        }
    }
}
