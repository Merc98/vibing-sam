package com.example.ide.puente.jobs

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AnalysisRunner(private val context: Context) {

      private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

          private val _state = MutableStateFlow(UiJobState())
              val state: StateFlow<UiJobState> = _state

      fun runFull(targetApk: String, workspace: String) {
                scope.launch {

                              fun update(step: String, progress: Int, extra: String = "") {
                                                _state.value = _state.value.copy(
                                                                      running = true,
                                                                      step = step,
                                                                      progress = progress,
                                                                      log = _state.value.log + "\n$step\n$extra"
                                                                  )
                              }

                                          try {
                                                            val decoded = File(workspace, "decoded").apply {
                                                                                  if (exists()) deleteRecursively()
                                                                                                      mkdirs()
                                                            }

                                                                            update("Decoding APK", 20)

                                                                                            val decode = com.example.ide.puente.exec.ApktoolRunner.run(
                                                                                                                  context,
                                                                                                                  listOf("d", targetApk, "-o", decoded.absolutePath, "-f")
                                                                                                                                  )

                                                                                                            update("Decompile (jadx)", 60)
                                                                                                            
                                                                                                                            val jadxOut = File(workspace, "jadx")
                                                                                                                            
                                                                                                                                            com.example.ide.puente.analysis.JadxDecompiler.decompile(
                                                                                                                                                                  File(targetApk),
                                                                                                                                                                  jadxOut
                                                                                                                                                              )
                                                                                                                                            
                                                                                                                                                            update("Finalizing", 90)
                                                                                                                                                            
                                                                                                                                                                            _state.value = _state.value.copy(
                                                                                                                                                                                                  running = false,
                                                                                                                                                                                                  progress = 100,
                                                                                                                                                                                                  step = "Done"
                                                                                                                                                                                              )
                                                                                                                                                                            
                                          } catch (e: Exception) {
                                                            _state.value = _state.value.copy(
                                                                                  running = false,
                                                                                  step = "Error",
                                                                                  log = e.message ?: "unknown"
                                                                              )
                                          }
                }
      }
}
