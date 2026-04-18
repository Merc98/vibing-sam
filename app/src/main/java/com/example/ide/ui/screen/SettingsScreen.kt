package com.example.ide.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.data.model.AIModel
import com.example.ide.data.model.AIModelType
import com.example.ide.data.repository.PatchBundle
import com.example.ide.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val apiKeys by viewModel.apiKeys.collectAsStateWithLifecycle()
    val patchBundles by viewModel.patchBundles.collectAsStateWithLifecycle()
    val keyRequiredModels = remember(availableModels) { availableModels.filter { it.requiresApiKey } }
    LaunchedEffect(Unit) {
        viewModel.refreshPatchBundles()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI Model Selection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Choose your preferred AI model for code assistance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel?.name ?: "Select a model",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("AI Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.name)
                                            Text(
                                                text = model.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectModel(model)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (model.type == selectedModel?.type) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Free AI by default",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use Local Smart Assist or Local Quick Help without API keys. External providers are optional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (keyRequiredModels.isNotEmpty()) {
            item {
                Text(
                    text = "API Keys (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Text(
                    text = "Configure keys only for online providers you want to use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(keyRequiredModels.groupBy { getProviderName(it.type) }.toList()) { (provider, models) ->
                ApiKeySection(
                    provider = provider,
                    models = models,
                    apiKeys = apiKeys,
                    onApiKeyChange = { modelType, key ->
                        viewModel.setApiKey(modelType, key)
                    }
                )
            }
        }

        item {
            PatchBundlesSection(
                patchBundles = patchBundles,
                patchFolderPath = viewModel.getPatchBundlesDirectoryPath(),
                onRefresh = { viewModel.refreshPatchBundles() },
                onAutoApply = { viewModel.applyLatestCompatiblePatchToCurrentProject() },
                onApplyPatch = { fileName ->
                    viewModel.applyPatchBundleToCurrentProject(fileName)
                }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Mobile IDE v1.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "A powerful mobile IDE with AI assistance for coding on the go.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Supports multiple programming languages and AI models",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeySection(
    provider: String,
    models: List<AIModel>,
    apiKeys: Map<AIModelType, String>,
    onApiKeyChange: (AIModelType, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = provider,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Use the first model's type as representative for the provider
            val representativeType = models.first().type
            var apiKey by remember(representativeType) { 
                mutableStateOf(apiKeys[representativeType] ?: "") 
            }
            var showApiKey by remember { mutableStateOf(false) }
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { 
                    apiKey = it
                    // Set the same key for all models from this provider
                    models.forEach { model ->
                        onApiKeyChange(model.type, it)
                    }
                },
                label = { Text("$provider API Key") },
                placeholder = { Text("Enter your API key") },
                visualTransformation = if (showApiKey) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Models: ${models.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            
            if (apiKey.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "API key required to use $provider models",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getProviderName(modelType: AIModelType): String {
    return when (modelType) {
        AIModelType.LOCAL_SMART_ASSIST, AIModelType.LOCAL_QUICK_HELP -> "Local Free AI"
        AIModelType.OPENAI_GPT4, AIModelType.OPENAI_GPT35 -> "OpenAI"
        AIModelType.CLAUDE_3_OPUS, AIModelType.CLAUDE_3_SONNET, AIModelType.CLAUDE_3_HAIKU -> "Anthropic"
        AIModelType.GEMINI_PRO, AIModelType.GEMINI_FLASH -> "Google"
        AIModelType.COHERE_COMMAND -> "Cohere"
        AIModelType.MISTRAL_LARGE, AIModelType.MISTRAL_MEDIUM -> "Mistral"
        AIModelType.LLAMA_70B, AIModelType.CODELLAMA_34B -> "Meta"
        // OpenRouter models
        AIModelType.OPENROUTER_AUTO -> "OpenRouter"
        // Chinese free models
        AIModelType.LINGMA -> "Lingma"
        AIModelType.QWEN -> "Alibaba"
        AIModelType.LONGCAT_AI -> "Longcat AI"
        AIModelType.DEEPSEEK -> "DeepSeek"
        AIModelType.Z_MODEL -> "Z"
        AIModelType.KIMI -> "Moonshot AI"
    }
}

@Composable
fun PatchBundlesSection(
    patchBundles: List<PatchBundle>,
    patchFolderPath: String,
    onRefresh: () -> Unit,
    onAutoApply: () -> Unit,
    onApplyPatch: (String) -> Unit
) {
    val compatibleCount = patchBundles.count { it.isCompatibleWithCurrentProject }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Patch Bundles (ZIP)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onRefresh) {
                    Text("Refresh", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onAutoApply,
                enabled = compatibleCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                val quickActionText = if (compatibleCount > 0) {
                    "Aplicar parche recomendado (${compatibleCount} compatibles)"
                } else {
                    "Sin parches compatibles"
                }
                Text(quickActionText, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Put .zip files in: $patchFolderPath",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Flujo simple: refresca y usa 'Aplicar parche recomendado' para hacerlo en un paso.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (patchBundles.isEmpty()) {
                Text(
                    text = "No patch bundles found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                patchBundles.forEach { bundle ->
                    PatchBundleItem(bundle = bundle, onApplyPatch = onApplyPatch)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PatchBundleItem(
    bundle: PatchBundle,
    onApplyPatch: (String) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = bundle.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${bundle.patchFileCount} files • ${(bundle.sizeBytes / 1024).coerceAtLeast(1)} KB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Lenguaje detectado: ${bundle.detectedLanguage}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Herramienta recomendada: ${bundle.recommendedTool}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Utilidad: ${bundle.toolUtility}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = bundle.utilitySummary,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Updated ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(bundle.lastModified))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = if (bundle.isCompatibleWithCurrentProject) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = bundle.compatibilityMessage,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = if (bundle.isCompatibleWithCurrentProject) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFF8B0000)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onApplyPatch(bundle.fileName) },
                enabled = bundle.isCompatibleWithCurrentProject
            ) {
                Text("Apply", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
