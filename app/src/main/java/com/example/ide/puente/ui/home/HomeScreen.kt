package com.example.ide.puente.ui.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ide.puente.binary.BinaryManager
import com.example.ide.puente.data.ApkImporter
import com.example.ide.puente.data.TargetStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(onOpenTarget: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val targets by store.targets.collectAsState()
    val scope = rememberCoroutineScope()

    var bootstrapStatus by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }

    // Extract bundled binaries once
    LaunchedEffect(Unit) {
        val bm = BinaryManager(context)
        val report = bm.extractAll()
        bootstrapStatus = if (report.success) {
            "Binaries ready (apktool ${report.manifestVersion ?: "?"})"
        } else {
            "Bootstrap issues: " + report.failed.joinToString { "${it.first}=${it.second}" }
        }
    }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val displayName = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: "imported.apk"

        importing = true
        scope.launch {
            try {
                val target = ApkImporter.import(context, uri, displayName)
                store.upsert(target)
            } finally {
                importing = false
            }
        }
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
                text = "Puente — self-contained reverse engineering",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Import target APK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = bootstrapStatus ?: "Preparing binaries…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            apkPicker.launch(arrayOf(
                                "application/vnd.android.package-archive",
                                "application/octet-stream",
                                "*/*"
                            ))
                        },
                        enabled = !importing
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.height(0.dp))
                        Text(if (importing) "Importing…" else "Pick APK")
                    }
                    if (importing) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent targets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (targets.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Spacer(Modifier.height(6.dp))
                        Text("No targets yet. Import an APK to begin.")
                    }
                }
            }
        } else {
            items(targets) { target ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(end = 4.dp)) {
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = target.packageName ?: target.id.take(16),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${target.sizeBytes / 1024} KB • imported ${
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        .format(Date(target.importedAt))
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpenTarget(target.id) }) {
                                    Text("Open")
                                }
                                IconButton(onClick = {
                                    scope.launch { store.remove(target.id) }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove target",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
