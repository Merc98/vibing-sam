package com.example.ide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
            if (chatMessages.isEmpty() && cards.isEmpty()) {
                item { EmptyAgentState(selectedModel?.name ?: "Local/Cloud model") }
            }
            items(chatMessages) { ChatMessageItem(it) }
            items(cards) { VibingCardRenderer(it, viewModel) }
            if (uiState.isLoading) item { AgentStatusCard("Working...", "Running Vibing MOD tools") }
            uiState.error?.let { item { AgentStatusCard("Error", it) } }
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AgentLogo(112)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Hablemos. ¿Cómo puedo ayudarte hoy?",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text("Vibing MOD Agent • $modelName", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AgentLogo(sizeDp: Int) {
    Box(
        modifier = Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size((sizeDp * 0.58f).dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AgentComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    selectedModelName: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Make, test, iterate...") },
                minLines = 1,
                maxLines = 4
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { }) { Icon(Icons.Default.Add, contentDescription = "Attach APK") }
                AssistChip(onClick = { }, label = { Text("Plan") }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) })
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
        else -> ToolCardShell(card.title, card.body)
    }
}

@Composable
private fun ToolCardShell(title: String, body: String, icon: @Composable (() -> Unit)? = null, content: @Composable (() -> Unit)? = null) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.invoke()
                if (icon != null) Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            if (body.isNotBlank()) Text(body)
            content?.invoke()
        }
    }
}

@Composable
fun ImportCard(card: VibingModToolCard) = ToolCardShell(
    title = "APK Imported",
    body = card.metadata["package"] ?: card.metadata["path"] ?: card.body,
    icon = { Icon(Icons.Default.Folder, null) }
)

@Composable
fun AnalysisCard(card: VibingModToolCard) = ToolCardShell(
    title = "Analyzing APK",
    body = card.body,
    icon = { Icon(Icons.Default.Psychology, null) }
)

@Composable
fun PatchPreviewCard(card: VibingModToolCard, onApply: () -> Unit, onReject: () -> Unit) = ToolCardShell(
    title = "Patch Preview",
    body = card.body,
    icon = { Icon(Icons.Default.AutoAwesome, null) }
) {
    Text("Risk: ${card.metadata["riskLevel"] ?: "UNKNOWN"}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onApply) { Text("Apply MOD") }
        Button(onClick = onReject) { Text("Cancel") }
    }
}

@Composable
fun BuildCard(card: VibingModToolCard) = ToolCardShell(
    title = "Rebuild MOD APK",
    body = card.body,
    icon = { Icon(Icons.Default.Build, null) }
)

@Composable
fun ExportCard(card: VibingModToolCard) = ToolCardShell(
    title = "Export MOD APK",
    body = "Signed: ${card.metadata["signed"] ?: "false"}\nPath: ${card.metadata["path"] ?: "n/a"}",
    icon = { Icon(Icons.Default.Download, null) }
)

@Composable
fun TerminalOutputCard(card: VibingModToolCard) = ToolCardShell(
    title = "Terminal",
    body = "cwd: ${card.metadata["cwd"] ?: "."}\n${card.body}",
    icon = { Icon(Icons.Default.Terminal, null) }
)

@Composable
fun LocalModelCard(card: VibingModToolCard) = ToolCardShell(
    title = "Local Model",
    body = card.body,
    icon = { Icon(Icons.Default.Psychology, null) }
)

@Composable
fun AgentStatusCard(title: String, body: String) = ToolCardShell(title = title, body = body)

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(if (isUser) "Tú" else "Agent", fontWeight = FontWeight.Bold)
                Text(message.content)
            }
        }
    }
}
