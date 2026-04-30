package com.example.ide.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.data.model.ChatMessage
import com.example.ide.ui.viewmodel.MainViewModel
import com.example.ide.ui.viewmodel.VibingModToolCard
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val cards by viewModel.vibingToolCards.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatMessages.size, cards.size, uiState.isLoading, uiState.error) {
        val total = chatMessages.size + cards.size
        if (total > 0) {
            coroutineScope.launch { listState.animateScrollToItem(total - 1) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibing MOD Chat", fontWeight = FontWeight.Bold)
                Text(selectedModel?.name ?: "No model")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { ChatMessageItem(it) }
            items(cards) { card -> VibingCardRenderer(card, viewModel) }
            if (uiState.isLoading) {
                item { OutlinedCard { Text("Working...", modifier = Modifier.padding(12.dp)) } }
            }
            uiState.error?.let { err ->
                item { OutlinedCard { Text("Error: $err", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) } }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Send a Vibing MOD command...") }
                )
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.submitVibingModMessage(messageText)
                        messageText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun VibingCardRenderer(card: VibingModToolCard, viewModel: MainViewModel) {
    when (card.type) {
        "import" -> ImportCard(card)
        "analysis" -> AnalysisCard(card)
        "patch_preview" -> PatchPreviewCard(
            card = card,
            onApply = { viewModel.approvePendingPatch() },
            onReject = { viewModel.rejectPendingPatch() }
        )
        "build" -> BuildCard(card)
        "export" -> ExportCard(card)
        "terminal" -> TerminalOutputCard(card)
        "local_model" -> LocalModelCard(card)
        "frida" -> FridaCard(card)
        else -> OutlinedCard { Text("${card.title}: ${card.body}", modifier = Modifier.padding(12.dp)) }
    }
}

@Composable
fun ImportCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("MOD Workspace", fontWeight = FontWeight.SemiBold)
        Text(card.metadata["package"] ?: card.metadata["path"] ?: card.body)
    }
}

@Composable
fun AnalysisCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("Analysis", fontWeight = FontWeight.SemiBold)
        Text(card.body)
    }
}

@Composable
fun PatchPreviewCard(card: VibingModToolCard, onApply: () -> Unit, onReject: () -> Unit) = OutlinedCard {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Patch Preview", fontWeight = FontWeight.SemiBold)
        Text(card.body)
        Text("Risk: ${card.metadata["riskLevel"] ?: "UNKNOWN"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApply) { Text("Apply MOD") }
            Button(onClick = onReject) { Text("Reject") }
        }
    }
}

@Composable
fun BuildCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("Rebuild MOD APK", fontWeight = FontWeight.SemiBold)
        Text(card.body, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ExportCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("Export MOD APK", fontWeight = FontWeight.SemiBold)
        Text("Signed: ${card.metadata["signed"] ?: "false"}")
        Text("Path: ${card.metadata["path"] ?: "n/a"}")
        card.metadata["error"]?.takeIf { it.isNotBlank() }?.let { Text("Error: $it") }
    }
}

@Composable
fun TerminalOutputCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("Terminal", fontWeight = FontWeight.SemiBold)
        Text("cwd: ${card.metadata["cwd"] ?: "."}")
        Text(card.body, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LocalModelCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("Local Model", fontWeight = FontWeight.SemiBold)
        Text(card.body)
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(message.role, fontWeight = FontWeight.Bold)
            Text(message.content)
        }
    }
}


@Composable
fun FridaCard(card: VibingModToolCard) = OutlinedCard {
    Column(Modifier.padding(12.dp)) {
        Text("Frida", fontWeight = FontWeight.SemiBold)
        Text(card.body)
    }
}
