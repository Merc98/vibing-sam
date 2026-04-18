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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
            // Some providers do not grant persistable permissions; continue with transient access.
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
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Text(
                            text = "AI gratis lista",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El flujo básico usa Local Smart Assist / Local Quick Help sin API key. Los modelos externos quedan como opcionales en Settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Text(
                            text = "Seleccionar APK / ZIP / archivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Carga un archivo desde el teléfono y revisa estructura, tamaño y entradas detectadas aquí mismo.",
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
                        Text("Abrir archivo")
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
                        Text("Analizando archivo…")
                    }
                }
            }
        }

        archiveError?.let { message ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No se pudo abrir el archivo",
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
                        Text("Tipo: ${preview.fileType}")
                        Text("Tamaño: ${formatBytes(preview.sizeBytes)}")
                        Text("Entradas detectadas: ${preview.entryCount}")
                        Text("Herramienta sugerida: ${preview.suggestedTool}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(preview.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (preview.sampleEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "Archivos detectados",
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
                            text = "APK Security Lab (Safe)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Incluye flujos de inspección y decompilación para apps propias o autorizadas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No payload injection / no bypass automation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = "Qué cubre",
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
                            text = "Creación de APK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usa el workflow de GitHub Actions para compilar y bajar el artifact del APK desde Actions.",
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
        else -> "Archivo"
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
        append("Se detectó un paquete listo para inspección. ")
        append("Manifest: ${if (hasManifest) "sí" else "no"}. ")
        append("DEX: ${if (hasDex) "sí" else "no"}. ")
        append("Recursos: ${if (hasResources) "sí" else "no"}. ")
        append("Smali: ${if (hasSmali) "sí" else "no"}.")
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
