package com.example.ide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.data.model.ChatMessage
import com.example.ide.ui.viewmodel.MainViewModel
import com.example.ide.ui.viewmodel.VibingModToolCard
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val cards by viewModel.vibingToolCards.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatMessages.size, cards.size, uiState.isLoading, uiState.error) {
        val total = chatMessages.size + cards.size
        if (total > 0) coroutineScope.launch { listState.animateScrollToItem(total - 1) }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatMessages.isEmpty() && cards.isEmpty()) item { EmptyAgentState(selectedModel?.name ?: "Local/Cloud model") }
            items(chatMessages) { ChatMessageItem(it) }
            items(cards) { VibingCardRenderer(it, viewModel) }
            if (uiState.isLoading) item { AgentStatusCard("Working...", "Running Vibing MOD tools") }
            uiState.error?.let { item { ErrorCard(VibingModToolCard("error", "Error", it)) } }
        }

        AgentComposer(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                val text = messageText.trim()
                if (text.isNotEmpty()) {
                    viewModel.submitVibingModMessage(text)
                    messageText = ""
                }
            },
            selectedModelName = selectedModel?.name ?: "model"
        )
    }
}

@Composable
private fun EmptyAgentState(modelName: String) {
    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AgentLogo(112)
        Spacer(Modifier.height(24.dp))
        Text("Vibing MOD Agent", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("Chat-first APK lab educativo/personal • $modelName", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("Prueba: importar APK, analizar, planear patch, preview, aplicar, rebuild, firmar/exportar, Frida o terminal.")
    }
}

@Composable
private fun AgentLogo(sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Psychology, null, modifier = Modifier.size((sizeDp * 0.58f).dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AgentComposer(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, selectedModelName: String) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)) {
        Column(Modifier.padding(10.dp)) {
            OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), placeholder = { Text("Agent: import /path/app.apk, analiza, patch..., rebuild, frida, terminal ls") }, minLines = 1, maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { onValueChange("import /sdcard/Download/app.apk") }, label = { Text("Import") }, leadingIcon = { Icon(Icons.Default.Folder, null) })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = { onValueChange("terminal pwd") }, label = { Text("Terminal") }, leadingIcon = { Icon(Icons.Default.Terminal, null) })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = { }, label = { Text(selectedModelName) })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSend) { Icon(Icons.Default.Send, contentDescription = "Send") }
            }
        }
    }
}

@Composable
private fun VibingCardRenderer(card: VibingModToolCard, viewModel: MainViewModel) {
    when (card.type) {
        "import" -> ImportCard(card)
        "analysis" -> AnalysisCard(card)
        "patch_preview" -> PatchPreviewCard(card, viewModel::approvePendingPatch, viewModel::rejectPendingPatch)
        "build" -> BuildCard(card)
        "export" -> ExportCard(card)
        "terminal" -> TerminalOutputCard(card)
        "local_model" -> LocalModelCard(card)
        "frida" -> FridaCard(card)
        "error" -> ErrorCard(card)
        "auth" -> AuthCard(card)
        "model_download" -> ModelDownloadCard(card)
        else -> ToolCardShell(card.title, card.body)
    }
}

@Composable
private fun ToolCardShell(title: String, body: String, icon: @Composable (() -> Unit)? = null, content: @Composable (() -> Unit)? = null) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { icon?.invoke(); if (icon != null) Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold) }
            if (body.isNotBlank()) Text(body)
            content?.invoke()
        }
    }
}

@Composable fun ImportCard(card: VibingModToolCard) = ToolCardShell("APK Imported", card.body.ifBlank { card.metadata.toString() }, { Icon(Icons.Default.Folder, null) })
@Composable fun AnalysisCard(card: VibingModToolCard) = ToolCardShell("APK Analysis", card.body, { Icon(Icons.Default.Psychology, null) })
@Composable fun PatchPreviewCard(card: VibingModToolCard, onApply: () -> Unit, onReject: () -> Unit) = ToolCardShell("Patch Preview", card.body, { Icon(Icons.Default.AutoAwesome, null) }) { Text("Risk: ${card.metadata["riskLevel"] ?: "LOW"}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onApply) { Text("Apply") }; OutlinedButton(onClick = onReject) { Text("Cancel") } } }
@Composable fun BuildCard(card: VibingModToolCard) = ToolCardShell("Build", "${card.body}\n\nstdout:\n${card.metadata["stdout"].orEmpty()}\n\nstderr:\n${card.metadata["stderr"].orEmpty()}", { Icon(Icons.Default.Build, null) })
@Composable fun ExportCard(card: VibingModToolCard) = ToolCardShell("Export", "Signed: ${card.metadata["signed"] ?: "false"}\nPath: ${card.metadata["path"] ?: "n/a"}\n${card.body}", { Icon(Icons.Default.Download, null) })
@Composable fun TerminalOutputCard(card: VibingModToolCard) = ToolCardShell("Terminal", "cwd: ${card.metadata["cwd"] ?: "."}\n${card.body}", { Icon(Icons.Default.Terminal, null) })
@Composable fun LocalModelCard(card: VibingModToolCard) = ToolCardShell("Local Model", card.body, { Icon(Icons.Default.Psychology, null) })
@Composable fun FridaCard(card: VibingModToolCard) = ToolCardShell("Frida", card.body, { Icon(Icons.Default.BugReport, null) })
@Composable fun ErrorCard(card: VibingModToolCard) = ToolCardShell("Error", card.body, { Icon(Icons.Default.Error, null) })
@Composable fun AuthCard(card: VibingModToolCard) = ToolCardShell("Auth / AI Strategy", card.body, { Icon(Icons.Default.Lock, null) })
@Composable fun ModelDownloadCard(card: VibingModToolCard) = ToolCardShell("Model Download", card.body, { Icon(Icons.Default.Download, null) })
@Composable fun AgentStatusCard(title: String, body: String) = ToolCardShell(title, body)

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Card(Modifier.fillMaxWidth(0.86f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) { Text(if (isUser) "Tú" else "Agent", fontWeight = FontWeight.Bold); Text(message.content) }
        }
    }
}
