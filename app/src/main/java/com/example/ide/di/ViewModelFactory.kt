package com.example.ide.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ide.data.local.DeviceRepository
import com.example.ide.data.local.ToolRepository
import com.example.ide.data.repository.AIRepository
import com.example.ide.data.repository.FileRepository
import com.example.ide.ui.viewmodel.MainViewModel

/**
 * Simple dependency injection container
 */
object DI {
    private lateinit var context: Context
    
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
    
    val fileRepository: FileRepository by lazy {
        FileRepository(context)
    }
    
    val aiRepository: AIRepository by lazy {
        AIRepository()
    }
    
    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(context)
    }
    
    val toolRepository: ToolRepository by lazy {
        ToolRepository(context)
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
                    toolRepository = DI.toolRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}