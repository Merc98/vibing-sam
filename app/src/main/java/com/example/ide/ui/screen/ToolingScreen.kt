package com.example.ide.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.ui.viewmodel.MainViewModel
import java.io.BufferedInputStream
import java.util.Locale
import java.util.zip.ZipInputStream

data class ToolCommand(
    val command: String,
    val note: String
)

private data class ArchivePreview(
    val displayName: String,
    val sizeBytes: Long,
    val entryCount: Int,
    val sampleEntries: List<String>,
    val suggestedTool: String,
    val summary: String,
    val fileType: String
)

@Composable
fun ToolingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val commands = listOf(
        ToolCommand(
            "python tools/vibing_apk_lab.py ./my_app.apk --decode --decompile",
            "Decode resources + decompile classes for manual review"
        ),
        ToolCommand(
            "python tools/vibing_apk_lab.py --list-packages",
            "Inspect installed packages from connected device"
        ),
        ToolCommand(
            "python tools/vibing_apk_lab.py --package com.example.app",
            "Read package metadata (version/debuggable/install paths)"
        )
    )
    var archivePreview by remember { mutableStateOf<ArchivePreview?>(null) }
    var archiveError by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val archivePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isAnalyzing = true
        archiveError = null
        archivePreview = null
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        archivePreview = try {
            inspectArchive(context, uri)
        } catch (e: Exception) {
            archiveError = e.message ?: "Failed to inspect file"
            null
        }
        isAnalyzing = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Toolkit",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Text(
                            text = "Create apps faster",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a starter web app or installable PWA, then open and edit the generated files in the Editor.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
        }

        currentProject?.let { project ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Current project files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (project.files.isEmpty()) {
                            Text(
                                text = "No files yet. Generate a starter app or create a file from the editor.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            project.files.take(8).forEach { file ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${file.name}.${file.extension}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    AssistChip(
                                        onClick = { viewModel.selectFile(file) },
                                        label = { Text("Open") }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Text(
                            text = "Select APK / ZIP / file",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Load a file from the phone and inspect structure, size and detected entries here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        archivePicker.launch(arrayOf(
                            "application/vnd.android.package-archive",
                            "application/zip",
                            "*/*"
                        ))
                    }) {
                        Text("Open file")
                    }
                }
            }
        }

        if (isAnalyzing) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Analyzing file…")
                    }
                }
            }
        }

        archiveError?.let { message ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Could not open file",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        archivePreview?.let { preview ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Text(
                                text = preview.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Type: ${preview.fileType}")
                        Text("Size: ${formatBytes(preview.sizeBytes)}")
                        Text("Detected entries: ${preview.entryCount}")
                        Text("Suggested tool: ${preview.suggestedTool}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(preview.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (preview.sampleEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "Detected files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(preview.sampleEntries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Text(entry, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Text(
                            text = "APK Security Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inspection and decompilation workflows for apps you own or are authorized to assess.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Text(
                            text = "What this covers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• APK structure checks (manifest/dex/ABIs/resources)")
                    Text("• Decode/decompile exports for manual auditing")
                    Text("• JSON report generation for automation pipelines")
                    Text("• Package inspection via ADB")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Text(
                            text = "APK creation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use GitHub Actions to build and download the APK artifact from the Actions tab.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "Quick commands",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(commands) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null)
                        Text(
                            text = item.command,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistChip(onClick = { }, label = { Text(item.note) })
                }
            }
        }
    }
}

private fun inspectArchive(context: android.content.Context, uri: Uri): ArchivePreview {
    val resolver = context.contentResolver
    var name = "archivo"
    var sizeBytes = 0L

    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: name
            }
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }

    val lowerName = name.lowercase(Locale.getDefault())
    val fileType = when {
        lowerName.endsWith(".apk") -> "APK"
        lowerName.endsWith(".zip") -> "ZIP"
        else -> "File"
    }

    val sampleEntries = mutableListOf<String>()
    var entryCount = 0
    var hasManifest = false
    var hasDex = false
    var hasResources = false
    var hasSmali = false

    resolver.openInputStream(uri)?.use { stream ->
        ZipInputStream(BufferedInputStream(stream)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entryCount += 1
                    if (sampleEntries.size < 18) {
                        sampleEntries.add(entry.name)
                    }
                    val normalized = entry.name.lowercase(Locale.getDefault())
                    if (normalized.endsWith("androidmanifest.xml")) hasManifest = true
                    if (normalized.endsWith(".dex")) hasDex = true
                    if (normalized.startsWith("res/") || normalized.contains("/res/")) hasResources = true
                    if (normalized.startsWith("smali") || normalized.contains("/smali")) hasSmali = true
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    val suggestedTool = when {
        hasSmali || (hasManifest && hasResources) -> "APKTool"
        hasDex -> "JADX"
        else -> "ZIP browser"
    }

    val summary = buildString {
        append("Package ready for inspection. ")
        append("Manifest: ${if (hasManifest) "yes" else "no"}. ")
        append("DEX: ${if (hasDex) "yes" else "no"}. ")
        append("Resources: ${if (hasResources) "yes" else "no"}. ")
        append("Smali: ${if (hasSmali) "yes" else "no"}.")
    }

    return ArchivePreview(
        displayName = name,
        sizeBytes = sizeBytes,
        entryCount = entryCount,
        sampleEntries = sampleEntries,
        suggestedTool = suggestedTool,
        summary = summary,
        fileType = fileType
    )
}

private fun formatBytes(size: Long): String {
    if (size <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        size >= mb -> String.format(Locale.US, "%.2f MB", size / mb)
        size >= kb -> String.format(Locale.US, "%.1f KB", size / kb)
        else -> "$size B"
    }
}
