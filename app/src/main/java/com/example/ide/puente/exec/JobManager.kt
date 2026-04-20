package com.example.ide.puente.exec

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight registry of long-running Puente jobs (apktool decode, Frida run,
 * gadget injection, etc.). Output lines are appended in memory and exposed
 * through a StateFlow so the UI can observe live progress.
 */
object JobManager {

    data class Job(
        val id: String,
        val title: String,
        val startedAt: Long = System.currentTimeMillis(),
        val lines: List<String> = emptyList(),
        val status: JobStatus = JobStatus.RUNNING,
        val exitCode: Int? = null
    )

    enum class JobStatus { RUNNING, SUCCESS, FAILURE, CANCELLED }

    private val _jobs = MutableStateFlow<Map<String, Job>>(emptyMap())
    val jobs: StateFlow<Map<String, Job>> = _jobs.asStateFlow()

    private val liveCancelHandles = ConcurrentHashMap<String, () -> Unit>()

    fun start(title: String, cancelHandle: () -> Unit = {}): String {
        val id = UUID.randomUUID().toString().take(8)
        liveCancelHandles[id] = cancelHandle
        _jobs.value = _jobs.value + (id to Job(id = id, title = title))
        return id
    }

    fun appendLine(id: String, line: String) {
        val existing = _jobs.value[id] ?: return
        _jobs.value = _jobs.value + (id to existing.copy(lines = existing.lines + line))
    }

    fun finish(id: String, exitCode: Int) {
        val existing = _jobs.value[id] ?: return
        val status = if (exitCode == 0) JobStatus.SUCCESS else JobStatus.FAILURE
        _jobs.value = _jobs.value + (id to existing.copy(status = status, exitCode = exitCode))
        liveCancelHandles.remove(id)
    }

    fun cancel(id: String) {
        liveCancelHandles.remove(id)?.invoke()
        val existing = _jobs.value[id] ?: return
        _jobs.value = _jobs.value + (id to existing.copy(status = JobStatus.CANCELLED))
    }

    fun clear() {
        _jobs.value = emptyMap()
        liveCancelHandles.clear()
    }
}
