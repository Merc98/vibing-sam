package com.example.ide.puente.exec

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Thin ProcessBuilder wrapper that streams stdout + stderr lines via Flow.
 * The last emission is always "EXIT:<code>".
 */
object ToolRunner {

    fun run(command: List<String>, workDir: File? = null, env: Map<String, String> = emptyMap()): Flow<String> =
        callbackFlow {
            val pb = ProcessBuilder(command).apply {
                workDir?.let { directory(it) }
                if (env.isNotEmpty()) environment().putAll(env)
                redirectErrorStream(false)
            }
            val proc = try {
                pb.start()
            } catch (t: Throwable) {
                trySend("EXIT:-1 (${t.javaClass.simpleName}: ${t.message})")
                close()
                return@callbackFlow
            }

            val scope = CoroutineScope(coroutineContext)

            scope.launch {
                BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                    lines.forEach { trySend(it) }
                }
            }
            scope.launch {
                BufferedReader(InputStreamReader(proc.errorStream)).useLines { lines ->
                    lines.forEach { trySend("[stderr] $it") }
                }
            }

            val exit = try {
                proc.waitFor()
            } catch (t: InterruptedException) {
                proc.destroyForcibly()
                -130
            }
            trySend("EXIT:$exit")
            close()

            awaitClose { if (proc.isAlive) proc.destroyForcibly() }
        }.flowOn(Dispatchers.IO)
}
