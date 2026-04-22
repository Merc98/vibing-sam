package com.example.ide.puente.jobs

enum class PuenteJobType {
    DECODE,
    DECOMPILE,
    FULL_PIPELINE,
    REBUILD_SIGN
}

enum class PuenteJobState {
    IDLE,
    RUNNING,
    SUCCEEDED,
    FAILED
}

data class PuenteJobStatus(
    val type: PuenteJobType,
    val state: PuenteJobState = PuenteJobState.IDLE,
    val step: String = "",
    val progress: Int = 0,
    val log: String = "",
    val startedAt: Long? = null,
    val finishedAt: Long? = null
)
