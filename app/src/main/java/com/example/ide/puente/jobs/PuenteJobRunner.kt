package com.example.ide.puente.jobs

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class PuenteJobRunner(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _status = MutableStateFlow(PuenteJobStatus(PuenteJobType.DECODE))
    val status: StateFlow<PuenteJobStatus> = _status

    fun runDecode(targetId: String, apkPath: String, workspace: String) {
        _status.value = PuenteJobStatus(
            type = PuenteJobType.DECODE,
            state = PuenteJobState.RUNNING,
            step = "Starting decode",
            progress = 0,
            startedAt = System.currentTimeMillis()
        )

        scope.launch {
            try {
                val outDir = File(workspace, "decoded").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }

                _status.value = _status.value.copy(step = "Running apktool", progress = 30)

                val result = com.example.ide.puente.exec.ApktoolRunner.run(
                    context,
                    listOf("d", apkPath, "-o", outDir.absolutePath, "-f")
                )

                _status.value = _status.value.copy(step = "Finalizing", progress = 90)

                if (result.exitCode == 0) {
                    _status.value = _status.value.copy(
                        state = PuenteJobState.SUCCEEDED,
                        progress = 100,
                        finishedAt = System.currentTimeMillis()
                    )
                } else {
                    _status.value = _status.value.copy(
                        state = PuenteJobState.FAILED,
                        log = result.stderr
                    )
                }
            } catch (e: Exception) {
                _status.value = _status.value.copy(
                    state = PuenteJobState.FAILED,
                    log = e.message ?: "error"
                )
            }
        }
    }
}
