package com.example.ide.ui.screen

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.data.model.CodeFile
import com.example.ide.data.model.FileExtension
import com.example.ide.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: MainViewModel) {
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val currentFile by viewModel.currentFile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showQuickInsertDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var selectedExtension by remember { mutableStateOf(FileExtension.TXT) }
    var saveFileName by remember { mutableStateOf("") }
    var saveExtension by remember { mutableStateOf("") }

    if (currentProject == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No project selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Open a project from the Projects tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.generateStarterWebApp() }) {
                        Text("Generate Web App")
                    }
                    OutlinedButton(onClick = { viewModel.generateStarterPwaApp() }) {
                        Text("Generate PWA")
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentProject?.name ?: "No Project",
                style = MaterialTheme.typography.titleMedium
            )

            Row {
                IconButton(onClick = { showCreateFileDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New File")
                }

                IconButton(
                    onClick = { viewModel.saveCurrentFile() },
                    enabled = currentFile?.isModified == true
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }

                IconButton(
                    onClick = { showSaveDialog = true },
                    enabled = currentFile != null
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Save to Downloads")
                }

                IconButton(
                    onClick = { showQuickInsertDialog = true },
                    enabled = currentFile != null
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Quick Insert")
                }

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                IconButton(
                    onClick = {
                        currentFile?.let {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it.content))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Copied ${it.name}.${it.extension}")
                            }
                        }
                    },
                    enabled = currentFile != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy content")
                }

                currentFile?.let { file ->
                    if (file.extension.equals("html", ignoreCase = true)) {
                        IconButton(
                            onClick = {
                                try {
                                    val projectDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    val ideProjectsDir = java.io.File(projectDir, "IDEProjects")
                                    val projectFolder = java.io.File(ideProjectsDir, currentProject?.name ?: "")
                                    val htmlFile = java.io.File(projectFolder, "${file.name}.${file.extension}")

                                    if (htmlFile.exists()) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            htmlFile
                                        )

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "text/html")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }

                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("No browser app found to open HTML file")
                                            }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("HTML file not found")
                                        }
                                    }
                                } catch (e: Exception) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error opening HTML file: ${e.message}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run HTML")
                        }
                    }
                }
            }
        }

        EditorActionStrip(
            currentFile = currentFile,
            onGenerateWebApp = { viewModel.generateStarterWebApp() },
            onGeneratePwa = { viewModel.generateStarterPwaApp() },
            onOpenQuickInsert = { showQuickInsertDialog = true }
        )

        HorizontalDivider()

        val project = currentProject
        if (project != null && project.files.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(project.files) { file ->
                    FileTab(
                        file = file,
                        isSelected = file.id == currentFile?.id,
                        onClick = { viewModel.selectFile(file) },
                        onDelete = {
                            viewModel.deleteFileFromProject(
                                project.name,
                                file.name,
                                file.extension
                            )
                            val updatedProject = project.copy()
                            updatedProject.files.removeIf { it.id == file.id }
                            viewModel.openProject(updatedProject)
                        }
                    )
                }
            }
            HorizontalDivider()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val file = currentFile
            if (file != null) {
                CodeEditor(
                    file = file,
                    onContentChange = { viewModel.updateFileContent(it) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No file selected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (project != null && project.files.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to create a file or use Quick actions above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }

    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create New File") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExtension.language,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("File Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            FileExtension.values().forEach { extension ->
                                DropdownMenuItem(
                                    text = { Text("${extension.language} (.${extension.extension})") },
                                    onClick = {
                                        selectedExtension = extension
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName.trim(), selectedExtension.extension)
                            newFileName = ""
                            showCreateFileDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save to Downloads") },
            text = {
                Column {
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        placeholder = { Text(currentFile?.name ?: "") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = saveExtension,
                        onValueChange = { saveExtension = it },
                        label = { Text("Extension") },
                        singleLine = true,
                        placeholder = { Text(currentFile?.extension ?: "") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = currentFile
                        val fileName = saveFileName.ifBlank { file?.name ?: "untitled" }
                        val extension = saveExtension.ifBlank { file?.extension ?: "txt" }
                        val content = file?.content ?: ""

                        viewModel.saveFileToDownloads(fileName, content, extension)
                        showSaveDialog = false
                        saveFileName = ""
                        saveExtension = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQuickInsertDialog) {
        QuickInsertDialog(
            currentFile = currentFile,
            onDismiss = { showQuickInsertDialog = false },
            onAppend = { snippet ->
                viewModel.appendToCurrentFile(snippet)
                showQuickInsertDialog = false
            },
            onReplace = { snippet ->
                viewModel.replaceCurrentFileWithContent(snippet)
                showQuickInsertDialog = false
            }
        )
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
}

@Composable
private fun EditorActionStrip(
    currentFile: CodeFile?,
    onGenerateWebApp: () -> Unit,
    onGeneratePwa: () -> Unit,
    onOpenQuickInsert: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onGenerateWebApp, label = { Text("Web App") })
            AssistChip(onClick = onGeneratePwa, label = { Text("Installable PWA") })
            AssistChip(
                onClick = onOpenQuickInsert,
                enabled = currentFile != null,
                label = { Text("Insert code") }
            )
        }
        if (currentFile != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Working file: ${currentFile.name}.${currentFile.extension}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun QuickInsertDialog(
    currentFile: CodeFile?,
    onDismiss: () -> Unit,
    onAppend: (String) -> Unit,
    onReplace: (String) -> Unit
) {
    val extension = currentFile?.extension?.lowercase().orEmpty()
    val snippet = remember(extension) { defaultSnippetForExtension(extension) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert generated code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Use a quick generated starter for the current file.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = snippet,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAppend(snippet) }) {
                    Text("Append")
                }
                TextButton(onClick = { onReplace(snippet) }) {
                    Text("Replace")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun defaultSnippetForExtension(extension: String): String {
    return when (extension) {
        "html" -> """
            <section class="generated-panel">
              <h2>Generated section</h2>
              <p>This block was inserted from the editor tools.</p>
            </section>
        """.trimIndent()
        "css" -> """
            .generated-panel {
              padding: 16px;
              border-radius: 18px;
              background: rgba(255, 255, 255, 0.08);
            }
        """.trimIndent()
        "js" -> """
            export function runGeneratedAction() {
              console.log('Generated action ready');
            }
        """.trimIndent()
        "kt" -> """
            fun generatedHelperMessage(): String {
                return "Generated Kotlin helper ready"
            }
        """.trimIndent()
        "py" -> """
            def generated_helper() -> str:
                return "Generated Python helper ready"
        """.trimIndent()
        "json" -> """
            {
              "generated": true,
              "note": "Replace with your data"
            }
        """.trimIndent()
        "md" -> """
            # Generated section

            - update me
            - save me
            - ship me
        """.trimIndent()
        else -> "Generated content block\n- update me\n- save me\n- run me"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTab(
    file: CodeFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        color = bg,
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = fg
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${file.name}.${file.extension}${if (file.isModified) " •" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete ${file.name}",
                    modifier = Modifier.size(14.dp),
                    tint = fg
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Delete '${file.name}.${file.extension}'? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItem(
    file: CodeFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteFile: (CodeFile) -> Unit = { _ -> }
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (file.isModified) {
                    Text(
                        text = "Modified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete File",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete the file '${file.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(file)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CodeEditor(
    file: CodeFile,
    onContentChange: (String) -> Unit
) {
    var content by remember(file.id) { mutableStateOf(file.content) }

    LaunchedEffect(file.content) {
        content = file.content
    }

    BasicTextField(
        value = content,
        onValueChange = {
            content = it
            onContentChange(it)
        },
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) { innerTextField ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (content.isEmpty()) {
                Text(
                    text = "Start typing your ${file.language} code...",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }
            innerTextField()
        }
    }
}
