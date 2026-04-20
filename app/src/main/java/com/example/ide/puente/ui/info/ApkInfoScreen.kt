package com.example.ide.puente.ui.info

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ide.puente.data.ApkInspector
import com.example.ide.puente.data.TargetStore

@Composable
fun ApkInfoScreen(
    targetId: String,
    onOpenAnalysis: (String) -> Unit,
    onOpenPatch: (String) -> Unit,
    onOpenFrida: (String) -> Unit,
    onOpenReport: (String) -> Unit
) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) }
    var info by remember { mutableStateOf<ApkInspector.Info?>(null) }

    LaunchedEffect(targetId) {
        target?.apkPath?.let { path ->
            val parsed = ApkInspector.inspect(context, path)
            info = parsed
            if (parsed != null) {
                store.upsert(
                    target.copy(
                        packageName = parsed.packageName,
                        versionName = parsed.versionName,
                        versionCode = parsed.versionCode,
                        minSdk = parsed.minSdk,
                        targetSdk = parsed.targetSdk,
                        appType = parsed.appType
                    )
                )
            }
        }
    }

    if (target == null) {
        Text("Target $targetId not found", modifier = Modifier.padding(16.dp))
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                target.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "sha256: ${target.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        info?.let { parsed ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Package",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(parsed.packageName)
                        Text(
                            "v${parsed.versionName ?: "?"} (${parsed.versionCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick = {}, label = { Text("min ${parsed.minSdk}") })
                            AssistChip(onClick = {}, label = { Text("target ${parsed.targetSdk}") })
                            AssistChip(onClick = {}, label = { Text(parsed.appType) })
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("ABIs", fontWeight = FontWeight.Bold)
                        Text(
                            if (parsed.abis.isEmpty()) "No native libs"
                            else parsed.abis.joinToString(", ")
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Permissions (${parsed.permissions.size})",
                            fontWeight = FontWeight.Bold
                        )
                        if (parsed.permissions.isEmpty()) {
                            Text(
                                "No requested permissions.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            parsed.permissions.take(12).forEach {
                                Text(
                                    it.removePrefix("android.permission."),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (parsed.permissions.size > 12) {
                                Text(
                                    "+${parsed.permissions.size - 12} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Operations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenAnalysis(targetId) }) { Text("Static") }
                        OutlinedButton(onClick = { onOpenPatch(targetId) }) { Text("Patch") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onOpenFrida(targetId) }) { Text("Frida") }
                        OutlinedButton(onClick = { onOpenReport(targetId) }) { Text("Report") }
                    }
                }
            }
        }
    }
}
