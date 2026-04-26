package com.example.ide.puente.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Simple JSON-backed registry of imported APK targets. Lives at
 * filesDir/puente/targets.json so it survives app restarts.
 */
class TargetStore private constructor(context: Context) {

    private val gson = Gson()
    private val storeDir = File(context.filesDir, "puente").apply { if (!exists()) mkdirs() }
    private val storeFile = File(storeDir, "targets.json")

    private val _targets = MutableStateFlow(load())
    val targets: StateFlow<List<ApkTarget>> = _targets.asStateFlow()

    suspend fun upsert(target: ApkTarget) = withContext(Dispatchers.IO) {
        val list = _targets.value.toMutableList()
        val existingIdx = list.indexOfFirst { it.id == target.id }
        if (existingIdx >= 0) list[existingIdx] = target else list.add(0, target)
        _targets.value = list.toList()
        persist(list)
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val list = _targets.value.filterNot { it.id == id }
        _targets.value = list
        persist(list)
    }

    fun find(id: String): ApkTarget? = _targets.value.firstOrNull { it.id == id }

    private fun load(): List<ApkTarget> {
        if (!storeFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ApkTarget>>() {}.type
            gson.fromJson(storeFile.readText(), type) ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun persist(list: List<ApkTarget>) {
        storeFile.writeText(gson.toJson(list))
    }

    companion object {
        @Volatile private var INSTANCE: TargetStore? = null
        fun get(context: Context): TargetStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TargetStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}
