package com.example.ide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplitWorkspaceScreen(viewModel: MainViewModel) {
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val currentFile by viewModel.currentFile.collectAsStateWithLifecycle()
    val terminalLines by viewModel.terminalLines.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPanel by remember { mutableStateOf(WorkspacePanel.Files) }
    var editorText by remember(currentFile?.id) { mutableStateOf(currentFile?.content.orEmpty()) }
    var terminalInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Luis Workspace", fontWeight = FontWeight.Bold)
                        Text(currentProject?.name ?: "No project open", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveCurrentFile() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { viewModel.runCurrentWorkspaceAction() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                    }
                    IconButton(onClick = { viewModel.rebuildCurrentApkTarget() }) {
                        Icon(Icons.Default.Build, contentDescription = "Rebuild APK")
                    }
                    IconButton(onClick = { viewModel.clearTerminal() }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Clear terminal")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                WorkspacePanel.values().forEach { panel ->
                    NavigationBarItem(
                        selected = selectedPanel == panel,
                        onClick = { selectedPanel = panel },
                        icon = { Icon(panel.icon, contentDescription = panel.label) },
                        label = { Text(panel.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            uiState.error?.let { error ->
                AssistChip(
                    modifier = Modifier.padding(8.dp),
                    onClick = { viewModel.clearError() },
                    label = { Text(error) },
                    leadingIcon = { Icon(Icons.Default.Error, contentDescription = null) }
                )
            }

            when (selectedPanel) {
                WorkspacePanel.Files -> FilesPanel(viewModel)
                WorkspacePanel.Editor -> EditorPanel(
                    fileName = currentFile?.let { "${it.name}.${it.extension}" } ?: "No file selected",
                    text = editorText,
                    onTextChange = {
                        editorText = it
                        viewModel.updateFileContent(it)
                    }
                )
                WorkspacePanel.Chat -> ChatScreen(viewModel)
                WorkspacePanel.Terminal -> TerminalPanel(
                    lines = terminalLines,
                    input = terminalInput,
                    onInputChange = { terminalInput = it },
                    onSubmit = {
                        viewModel.runTerminalCommand(terminalInput)
                        terminalInput = ""
                    }
                )
                WorkspacePanel.Apk -> ApkPanel(viewModel)
            }
        }
    }
}

@Composable
private fun FilesPanel(viewModel: MainViewModel) {
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text("Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        items(currentProject?.files.orEmpty()) { file ->
            ListItem(
                headlineContent = { Text("${file.name}.${file.extension}") },
                supportingContent = { Text(file.language) },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.selectFile(file) }
            )
            Divider()
        }
    }
}

@Composable
private fun EditorPanel(fileName: String, text: String, onTextChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
private fun TerminalPanel(
    lines: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Terminal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(8.dp)
        ) {
            items(lines) { line ->
                Text(line, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("ls, cd, cat, mkdir, touch...") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSubmit) { Text("Run") }
        }
    }
}

@Composable
private fun ApkPanel(viewModel: MainViewModel) {
    val apkResult by viewModel.lastApkTransformationResult.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("APK Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { viewModel.rebuildCurrentApkTarget() }) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Rebuild APK")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.runCurrentWorkspaceAction() }) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Run current action")
        }
        Spacer(Modifier.height(16.dp))
        apkResult?.let {
            Text("Last result", fontWeight = FontWeight.Bold)
            Text(it.message)
            Text(it.outputApkPath ?: "No output")
        }
    }
}

private enum class WorkspacePanel(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Files("Files", Icons.Default.Folder),
    Editor("Editor", Icons.Default.Code),
    Chat("Chat", Icons.Default.Chat),
    Terminal("Terminal", Icons.Default.Terminal),
    Apk("APK", Icons.Default.Android)
}
