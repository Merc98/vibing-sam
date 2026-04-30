package com.example.ide.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.data.model.ChatMessage
import com.example.ide.ui.viewmodel.MainViewModel
import com.example.ide.ui.viewmodel.VibingModToolCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val cards by viewModel.vibingToolCards.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commandOptions by viewModel.chatCommandOptions.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState(); val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(chatMessages.size + cards.size) { if (chatMessages.isNotEmpty() || cards.isNotEmpty()) coroutineScope.launch { listState.animateScrollToItem((chatMessages.size + cards.size).coerceAtLeast(1)-1) } }
    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth().padding(8.dp)) { Row(Modifier.padding(12.dp), horizontalArrangement=Arrangement.SpaceBetween){ Text("Vibing MOD Chat", fontWeight=FontWeight.Bold); Text(selectedModel?.name?:"Sin modelo") } }
        LazyColumn(state=listState, modifier=Modifier.weight(1f).padding(horizontal=8.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(chatMessages) { ChatMessageItem(it, viewModel=viewModel) }
            items(cards) { card -> VibingCardRenderer(card, viewModel) }
        }
        Card(Modifier.fillMaxWidth().padding(8.dp)) { Row(Modifier.padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=messageText,onValueChange={messageText=it},modifier=Modifier.weight(1f),placeholder={Text("Mensaje...")})
            IconButton(onClick={ if(messageText.isNotBlank()){ viewModel.submitVibingModMessage(messageText); messageText="" } }){ Icon(Icons.Default.Send,null)}
        }}
    }
}

@Composable private fun VibingCardRenderer(card: VibingModToolCard, viewModel: MainViewModel){ when(card.type){
    "import"->ImportCard(card)
    "analysis"->AnalysisCard(card)
    "patch_preview"->PatchPreviewCard(card, onApply={viewModel.approvePendingPatch()})
    "build"->BuildCard(card)
    "export"->ExportCard(card)
    "terminal"->TerminalOutputCard(card)
    else -> AssistCard{ Text(card.title+": "+card.body, Modifier.padding(12.dp)) }
}}
@Composable fun ImportCard(card: VibingModToolCard)=AssistCard{ Text("ImportCard: ${card.metadata["package"] ?: card.metadata["path"] ?: card.body}", Modifier.padding(12.dp)) }
@Composable fun AnalysisCard(card: VibingModToolCard)=AssistCard{ Text("AnalysisCard: ${card.body}", Modifier.padding(12.dp)) }
@Composable fun PatchPreviewCard(card: VibingModToolCard,onApply:()->Unit)=AssistCard{ Column(Modifier.padding(12.dp)){ Text("PatchPreviewCard: ${card.body}"); Button(onClick=onApply){ Text("Apply MOD") } } }
@Composable fun BuildCard(card: VibingModToolCard)=AssistCard{ Text("BuildCard: ${card.body}", Modifier.padding(12.dp)) }
@Composable fun ExportCard(card: VibingModToolCard)=AssistCard{ Text("ExportCard signed=${card.metadata["signed"]} path=${card.metadata["path"]} err=${card.metadata["error"]}", Modifier.padding(12.dp)) }
@Composable fun TerminalOutputCard(card: VibingModToolCard)=AssistCard{ Text("TerminalOutputCard [${card.metadata["cwd"]}]\n${card.body}", Modifier.padding(12.dp)) }

@Composable
fun ChatMessageItem(message: ChatMessage, isLoading: Boolean = false, viewModel: MainViewModel) {
    Card { Text("${message.role}: ${message.content}", Modifier.padding(12.dp)) }
}
