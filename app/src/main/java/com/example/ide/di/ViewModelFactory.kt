package com.example.ide.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ide.data.local.DeviceRepository
import com.example.ide.data.local.ToolRepository
import com.example.ide.data.repository.AIRepository
import com.example.ide.data.repository.FileRepository
import com.example.ide.domain.apk.ApkContextOrchestrator
import com.example.ide.domain.apk.ApkImportService
import com.example.ide.domain.apk.ApkTransformationOrchestrator
import com.example.ide.domain.apk.PatchApplier
import com.example.ide.ui.viewmodel.MainViewModel

object DI {
    private var _context: Context? = null

    fun init(appContext: Context) {
        _context = appContext.applicationContext
    }

    fun getContext(): Context = _context!!

    val fileRepository: FileRepository by lazy {
        FileRepository(_context!!)
    }

    val aiRepository: AIRepository by lazy {
        AIRepository()
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(_context!!)
    }

    val toolRepository: ToolRepository by lazy {
        ToolRepository(_context!!)
    }

    val apkImportService: ApkImportService by lazy {
        ApkImportService(_context!!, deviceRepository, fileRepository)
    }

    val apkContextOrchestrator: ApkContextOrchestrator by lazy {
        ApkContextOrchestrator(_context!!, fileRepository)
    }

    val apkTransformationOrchestrator: ApkTransformationOrchestrator by lazy {
        ApkTransformationOrchestrator(
            context = _context!!,
            contextOrchestrator = apkContextOrchestrator,
            patchApplier = PatchApplier(),
            aiRepository = aiRepository
        )
    }
}

class ViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MainViewModel::class.java -> {
                MainViewModel(
                    aiRepository = DI.aiRepository,
                    fileRepository = DI.fileRepository,
                    toolRepository = DI.toolRepository,
                    appContext = DI.getContext(),
                    apkImportService = DI.apkImportService,
                    apkTransformationOrchestrator = DI.apkTransformationOrchestrator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
