package com.example.ide.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ide.data.model.*
import com.example.ide.data.repository.AIRepository
import com.example.ide.data.repository.FileRepository
import com.example.ide.data.repository.PatchBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val aiRepository: AIRepository,
    private val fileRepository: FileRepository
) : ViewModel() {
    data class ChatCommandOption(
        val command: String,
        val description: String
    )

    private data class StarterFile(
        val fileName: String,
        val extension: String,
        val content: String
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _currentFile = MutableStateFlow<CodeFile?>(null)
    val currentFile: StateFlow<CodeFile?> = _currentFile.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AIModel>>(emptyList())
    val availableModels: StateFlow<List<AIModel>> = _availableModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<AIModel?>(null)
    val selectedModel: StateFlow<AIModel?> = _selectedModel.asStateFlow()

    private val _apiKeys = MutableStateFlow<Map<AIModelType, String>>(emptyMap())
    val apiKeys: StateFlow<Map<AIModelType, String>> = _apiKeys.asStateFlow()
    private val _patchBundles = MutableStateFlow<List<PatchBundle>>(emptyList())
    val patchBundles: StateFlow<List<PatchBundle>> = _patchBundles.asStateFlow()
    private val _chatCommandOptions = MutableStateFlow(
        listOf(
            ChatCommandOption("/help", "Show available slash commands"),
            ChatCommandOption("/models", "List available AI models"),
            ChatCommandOption("/model", "Change model. Example: /model local quick"),
            ChatCommandOption("/settings", "Show quick settings hints"),
            ChatCommandOption("/insert_script", "Insert a safe script template into your project"),
            ChatCommandOption("/refactor", "Ask AI to refactor current file"),
            ChatCommandOption("/debug", "Ask AI to debug current file"),
            ChatCommandOption("/test", "Ask AI to suggest tests for current file")
        )
    )
    val chatCommandOptions: StateFlow<List<ChatCommandOption>> = _chatCommandOptions.asStateFlow()

    // For undo functionality
    private val deletedProjects = mutableMapOf<String, Project>()
    private val deletedFiles = mutableMapOf<String, Pair<CodeFile, String>>() // fileId -> (file, projectName)

    init {
        loadProjects()
        loadAvailableModels()
        refreshPatchBundles()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _projects.value = fileRepository.getAllProjects()
        }
    }

    private fun loadAvailableModels() {
        val models = aiRepository.getAvailableModels()
        _availableModels.value = models
        _selectedModel.value = models.firstOrNull { it.type == AIModelType.LOCAL_SMART_ASSIST }
            ?: models.firstOrNull { it.type == AIModelType.LOCAL_QUICK_HELP }
            ?: models.firstOrNull()
    }

    fun createNewProject(name: String) {
        val project = Project(name = name)
        fileRepository.saveProject(project)
        loadProjects()
        _currentProject.value = project
        refreshPatchBundles()
    }

    fun openProject(project: Project) {
        _currentProject.value = project
        _currentFile.value = project.files.firstOrNull()
        refreshPatchBundles()
    }

    fun createNewFile(name: String, extension: String) {
        val currentProj = _currentProject.value ?: return
        val newFile = fileRepository.createNewFile(name, extension)
        
        currentProj.files.add(newFile)
        fileRepository.saveProject(currentProj)
        _currentFile.value = newFile
        _currentProject.value = currentProj.copy()
    }

    fun updateFileContent(content: String) {
        val currentProj = _currentProject.value ?: return
        val currentFileValue = _currentFile.value ?: return
        
        val updatedFile = currentFileValue.copy(
            content = content,
            isModified = true,
            lastModified = System.currentTimeMillis()
        )
        
        val fileIndex = currentProj.files.indexOfFirst { it.id == currentFileValue.id }
        if (fileIndex >= 0) {
            currentProj.files[fileIndex] = updatedFile
            _currentFile.value = updatedFile
            _currentProject.value = currentProj.copy()
        }
    }

    fun saveCurrentFile() {
        val currentProj = _currentProject.value ?: return
        val currentFileValue = _currentFile.value ?: return
        
        val savedFile = currentFileValue.copy(isModified = false)
        val fileIndex = currentProj.files.indexOfFirst { it.id == currentFileValue.id }
        if (fileIndex >= 0) {
            currentProj.files[fileIndex] = savedFile
            fileRepository.saveProject(currentProj)
            _currentFile.value = savedFile
            _currentProject.value = currentProj.copy()
        }
    }

    fun appendToCurrentFile(content: String) {
        val currentFileValue = _currentFile.value ?: run {
            _uiState.value = _uiState.value.copy(error = "Open a file first to insert generated content.")
            return
        }
        val separator = if (currentFileValue.content.isBlank()) "" else "\n\n"
        updateFileContent(currentFileValue.content + separator + content)
        _uiState.value = _uiState.value.copy(message = "Generated content inserted into ${currentFileValue.name}.${currentFileValue.extension}")
    }

    fun replaceCurrentFileWithContent(content: String) {
        val currentFileValue = _currentFile.value ?: run {
            _uiState.value = _uiState.value.copy(error = "Open a file first to replace its content.")
            return
        }
        updateFileContent(content)
        _uiState.value = _uiState.value.copy(message = "${currentFileValue.name}.${currentFileValue.extension} updated with generated content")
    }

    fun openProjectFileByName(fileName: String, extension: String) {
        val project = _currentProject.value ?: return
        val found = project.files.firstOrNull { it.name == fileName && it.extension == extension }
        if (found != null) {
            _currentFile.value = found
        }
    }

    fun saveFileToDownloads(fileName: String, content: String, extension: String) {
        viewModelScope.launch {
            val result = fileRepository.saveFileToDownloads(fileName, content, extension)
            result.onSuccess { path ->
                _uiState.value = _uiState.value.copy(
                    message = "File saved to: $path",
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Save code from chat to the current project folder in Downloads
     */
    fun saveChatCodeToProject(fileName: String, codeContent: String, extension: String = "txt") {
        val currentProject = _currentProject.value ?: return
        
        viewModelScope.launch {
            val result = fileRepository.saveFileToProject(
                projectName = currentProject.name,
                fileName = fileName,
                content = codeContent,
                extension = extension
            )
            
            result.onSuccess { path ->
                _uiState.value = _uiState.value.copy(
                    message = "Code saved to project folder: $path",
                    isLoading = false
                )
                
                // Also update the in-memory project file list
                try {
                    val existingIndex = currentProject.files.indexOfFirst { it.name == fileName && it.extension == extension }
                    val newFile = fileRepository.createNewFile(fileName, extension).copy(
                        content = codeContent,
                        isModified = false
                    )
                    if (existingIndex >= 0) {
                        currentProject.files[existingIndex] = newFile
                    } else {
                        currentProject.files.add(newFile)
                    }
                    fileRepository.saveProject(currentProject)
                    loadProjects()
                    _currentProject.value = currentProject.copy()
                    _currentFile.value = newFile
                } catch (e: Exception) {
                    // Handle error silently - file is already saved to disk
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save code to project: ${error.message}",
                    isLoading = false
                )
            }
        }
    }

    fun generateStarterWebApp() {
        val projectName = "web_app_${System.currentTimeMillis()}"
        createStarterProject(
            projectName = projectName,
            files = listOf(
                StarterFile(
                    "index",
                    "html",
                    """
                    <!doctype html>
                    <html lang=\"en\">
                    <head>
                      <meta charset=\"UTF-8\" />
                      <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
                      <title>$projectName</title>
                      <link rel=\"stylesheet\" href=\"styles.css\" />
                    </head>
                    <body>
                      <main class=\"app-shell\">
                        <h1>$projectName</h1>
                        <p>Your starter web app is ready.</p>
                        <button id=\"actionButton\">Run starter action</button>
                        <section id=\"output\"></section>
                      </main>
                      <script src=\"app.js\"></script>
                    </body>
                    </html>
                    """.trimIndent()
                ),
                StarterFile(
                    "styles",
                    "css",
                    """
                    :root {
                      color-scheme: dark;
                      font-family: Inter, system-ui, sans-serif;
                    }

                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      background: #0b1220;
                      color: #f5f7ff;
                    }

                    .app-shell {
                      width: min(92vw, 560px);
                      padding: 24px;
                      border-radius: 24px;
                      background: rgba(255, 255, 255, 0.08);
                      backdrop-filter: blur(10px);
                      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.28);
                    }

                    button {
                      margin-top: 12px;
                      border: 0;
                      border-radius: 999px;
                      padding: 12px 18px;
                      cursor: pointer;
                    }
                    """.trimIndent()
                ),
                StarterFile(
                    "app",
                    "js",
                    """
                    const output = document.getElementById('output');
                    document.getElementById('actionButton').addEventListener('click', () => {
                      output.innerHTML = '<p>Starter action executed successfully.</p>';
                    });
                    """.trimIndent()
                )
            ),
            successMessage = "Starter web app created"
        )
    }

    fun generateStarterPwaApp() {
        val projectName = "pwa_app_${System.currentTimeMillis()}"
        createStarterProject(
            projectName = projectName,
            files = listOf(
                StarterFile(
                    "index",
                    "html",
                    """
                    <!doctype html>
                    <html lang=\"en\">
                    <head>
                      <meta charset=\"UTF-8\" />
                      <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
                      <meta name=\"theme-color\" content=\"#111827\" />
                      <link rel=\"manifest\" href=\"manifest.json\" />
                      <title>$projectName</title>
                    </head>
                    <body>
                      <main>
                        <h1>$projectName</h1>
                        <p>Installable web app starter.</p>
                      </main>
                      <script src=\"app.js\"></script>
                    </body>
                    </html>
                    """.trimIndent()
                ),
                StarterFile(
                    "app",
                    "js",
                    """
                    if ('serviceWorker' in navigator) {
                      window.addEventListener('load', async () => {
                        try {
                          await navigator.serviceWorker.register('./sw.js');
                          console.log('Service worker registered');
                        } catch (error) {
                          console.error('Service worker registration failed', error);
                        }
                      });
                    }
                    """.trimIndent()
                ),
                StarterFile(
                    "manifest",
                    "json",
                    """
                    {
                      \"name\": \"$projectName\",
                      \"short_name\": \"Starter PWA\",
                      \"start_url\": \"./index.html\",
                      \"display\": \"standalone\",
                      \"background_color\": \"#111827\",
                      \"theme_color\": \"#111827\"
                    }
                    """.trimIndent()
                ),
                StarterFile(
                    "sw",
                    "js",
                    """
                    self.addEventListener('install', event => {
                      self.skipWaiting();
                    });

                    self.addEventListener('activate', event => {
                      event.waitUntil(self.clients.claim());
                    });
                    """.trimIndent()
                )
            ),
            successMessage = "Starter installable web app created"
        )
    }

    private fun createStarterProject(
        projectName: String,
        files: List<StarterFile>,
        successMessage: String
    ) {
        createNewProject(projectName)
        val project = _currentProject.value ?: return

        viewModelScope.launch {
            val createdFiles = mutableListOf<CodeFile>()
            for (file in files) {
                val result = fileRepository.saveFileToProject(
                    projectName = projectName,
                    fileName = file.fileName,
                    content = file.content,
                    extension = file.extension
                )
                result.onSuccess {
                    createdFiles.add(
                        fileRepository.createNewFile(file.fileName, file.extension).copy(
                            content = file.content,
                            isModified = false
                        )
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to generate ${file.fileName}.${file.extension}: ${error.message}",
                        isLoading = false
                    )
                    return@launch
                }
            }

            project.files.clear()
            project.files.addAll(createdFiles)
            fileRepository.saveProject(project)
            loadProjects()
            _currentProject.value = project.copy()
            _currentFile.value = createdFiles.firstOrNull()
            _uiState.value = _uiState.value.copy(
                message = "$successMessage: $projectName",
                isLoading = false
            )
        }
    }

    fun selectFile(file: CodeFile) {
        _currentFile.value = file
    }

    fun selectModel(model: AIModel) {
        _selectedModel.value = model
    }

    fun setApiKey(modelType: AIModelType, apiKey: String) {
        val currentKeys = _apiKeys.value.toMutableMap()
        currentKeys[modelType] = apiKey
        _apiKeys.value = currentKeys
    }

    fun refreshPatchBundles() {
        _patchBundles.value = fileRepository.listPatchBundles(_currentProject.value)
    }

    fun getPatchBundlesDirectoryPath(): String {
        return fileRepository.getPatchBundlesDirectoryPath()
    }

    fun applyPatchBundleToCurrentProject(zipFileName: String) {
        val project = _currentProject.value
        if (project == null) {
            _uiState.value = _uiState.value.copy(
                error = "Open a project first to apply patch bundles."
            )
            return
        }

        viewModelScope.launch {
            val selectedPatch = _patchBundles.value.firstOrNull { it.fileName == zipFileName }
            if (selectedPatch != null && !selectedPatch.isCompatibleWithCurrentProject) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = selectedPatch.compatibilityMessage
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = fileRepository.applyPatchBundleToProject(project, zipFileName)
            result.onSuccess { importedCount ->
                loadProjects()
                _currentProject.value = project.copy(lastModified = System.currentTimeMillis())
                refreshPatchBundles()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Patch applied: $importedCount file(s) imported from $zipFileName"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Patch apply failed: ${error.message}"
                )
            }
        }
    }

    fun applyLatestCompatiblePatchToCurrentProject() {
        val latestCompatiblePatch = _patchBundles.value.firstOrNull { it.isCompatibleWithCurrentProject }
        if (latestCompatiblePatch == null) {
            _uiState.value = _uiState.value.copy(
                error = "No hay parches compatibles listos para aplicar."
            )
            return
        }
        applyPatchBundleToCurrentProject(latestCompatiblePatch.fileName)
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                // Save project for potential undo
                val projectToDelete = _projects.value.find { it.id == projectId }
                projectToDelete?.let {
                    deletedProjects[projectId] = it
                }
                
                fileRepository.deleteProject(projectId)
                loadProjects()
                _uiState.value = _uiState.value.copy(
                    message = "Project deleted. Swipe to undo.",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete project: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun undoDeleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                val projectToRestore = deletedProjects[projectId]
                projectToRestore?.let {
                    fileRepository.saveProject(it)
                    loadProjects()
                    deletedProjects.remove(projectId)
                    _uiState.value = _uiState.value.copy(
                        message = "Project restored",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to restore project: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun deleteFileFromProject(projectName: String, fileName: String, extension: String) {
        viewModelScope.launch {
            try {
                // Save file for potential undo
                val currentProj = _currentProject.value
                currentProj?.let {
                    val fileToDelete = it.files.find { file -> 
                        file.name == fileName && file.extension == extension 
                    }
                    fileToDelete?.let { file ->
                        deletedFiles[file.id] = Pair(file, projectName)
                    }
                }
                
                fileRepository.deleteFileFromProject(projectName, fileName, extension)
                _uiState.value = _uiState.value.copy(
                    message = "File deleted. Swipe to undo.",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete file: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun undoDeleteFile(fileId: String) {
        viewModelScope.launch {
            try {
                val fileToRestore = deletedFiles[fileId]
                fileToRestore?.let { (file, projectName) ->
                    val result = fileRepository.saveFileToProject(
                        projectName = projectName,
                        fileName = file.name,
                        content = file.content,
                        extension = file.extension
                    )
                    
                    result.onSuccess {
                        // Update project in memory
                        val currentProj = _currentProject.value
                        currentProj?.let {
                            val updatedProject = it.copy()
                            updatedProject.files.add(file)
                            _currentProject.value = updatedProject
                            loadProjects()
                            deletedFiles.remove(fileId)
                            _uiState.value = _uiState.value.copy(
                                message = "File restored",
                                isLoading = false
                            )
                        }
                    }?.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to restore file: ${error.message}",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to restore file: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun sendChatMessage(message: String) {
        val model = _selectedModel.value ?: return
        val apiKey = _apiKeys.value[model.type].orEmpty()

        if (model.requiresApiKey && apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "API key required for ${model.name}")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val userMessage = ChatMessage(role = "user", content = message)
            val currentMessages = _chatMessages.value.toMutableList()
            currentMessages.add(userMessage)
            _chatMessages.value = currentMessages

            // Add system message for coding context if we have a current file
            val messages = mutableListOf<ChatMessage>()
            _currentFile.value?.let { file ->
                messages.add(ChatMessage(
                    role = "system", 
                    content = "You are a coding assistant. The user is working on a ${file.language} file named ${file.name}. Current file content:\n\n${file.content}"
                ))
            }
            messages.addAll(currentMessages)

            val result = aiRepository.sendMessage(model.type, apiKey, messages)
            
            result.onSuccess { response ->
                val assistantMessage = ChatMessage(role = "assistant", content = response)
                currentMessages.add(assistantMessage)
                _chatMessages.value = currentMessages
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message,
                    isLoading = false
                )
            }
        }
    }

    fun submitChatInput(input: String) {
        val trimmedInput = input.trim()
        if (trimmedInput.isBlank()) return

        if (trimmedInput.startsWith("/")) {
            handleSlashCommand(trimmedInput)
            return
        }

        sendChatMessage(trimmedInput)
    }

    private fun handleSlashCommand(commandInput: String) {
        val parts = commandInput.split(" ", limit = 2)
        when (parts.first().lowercase()) {
            "/help" -> pushAssistantMessage(
                """
                Available commands:
                • /help
                • /models
                • /model <name>
                • /settings
                • /insert_script
                • /refactor
                • /debug
                • /test
                """.trimIndent()
            )

            "/models" -> {
                val list = _availableModels.value.mapIndexed { index, model ->
                    "${index + 1}. ${model.name}"
                }.joinToString("\n")
                pushAssistantMessage("Available models:\n$list")
            }

            "/model" -> {
                val requested = parts.getOrNull(1)?.trim().orEmpty()
                if (requested.isBlank()) {
                    pushAssistantMessage("Use: /model <name>. Example: /model local smart")
                    return
                }

                val normalized = requested.lowercase()
                val foundModel = _availableModels.value.firstOrNull { model ->
                    model.name.lowercase().contains(normalized) ||
                        model.type.name.lowercase().contains(normalized.replace(" ", "_"))
                }

                if (foundModel == null) {
                    pushAssistantMessage("Model not found: \"$requested\". Run /models to see options.")
                } else {
                    _selectedModel.value = foundModel
                    _uiState.value = _uiState.value.copy(message = "Model changed to ${foundModel.name}")
                    pushAssistantMessage("Switched model to: ${foundModel.name}")
                }
            }

            "/settings" -> {
                pushAssistantMessage(
                    "Quick settings:\n" +
                        "• Use Settings tab to set provider API keys.\n" +
                        "• Use /model to switch quickly from chat.\n" +
                        "• Local models work without API keys."
                )
            }

            "/insert_script" -> {
                insertSafeScriptTemplate()
            }

            "/refactor", "/debug", "/test" -> {
                val quickPrompt = when (parts.first().lowercase()) {
                    "/refactor" -> "Refactor the current file with cleaner structure and explain the changes."
                    "/debug" -> "Review the current file and point out likely bugs with fixes."
                    else -> "Create practical tests for the current file and explain expected outcomes."
                }
                sendChatMessage(quickPrompt)
            }

            else -> {
                pushAssistantMessage("Unknown command: ${parts.first()}. Run /help.")
            }
        }
    }

    private fun insertSafeScriptTemplate() {
        val project = _currentProject.value
        if (project == null) {
            pushAssistantMessage("Open a project first, then run /insert_script.")
            return
        }

        val scriptName = "automation_helper"
        val extension = "py"
        val scriptContent = """
            # Safe automation helper template for your own project files.
            # This script is intentionally non-invasive and does not patch third-party apps.
            from pathlib import Path

            def summarize_project(root: str) -> None:
                project_root = Path(root)
                files = [p for p in project_root.rglob("*") if p.is_file()]
                print(f"Found {len(files)} files in {project_root}")
                for p in files[:20]:
                    print("-", p.relative_to(project_root))

            if __name__ == "__main__":
                summarize_project(".")
        """.trimIndent()

        saveChatCodeToProject(scriptName, scriptContent, extension)
        pushAssistantMessage("Inserted script template: $scriptName.$extension in project ${project.name}.")
    }

    private fun pushAssistantMessage(content: String) {
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(ChatMessage(role = "assistant", content = content))
        _chatMessages.value = currentMessages
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
