package com.example.ide.puente.ui.report

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ide.puente.data.ApkInspector
import com.example.ide.puente.data.TargetStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(targetId: String) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) } ?: return
    var info by remember { mutableStateOf<ApkInspector.Info?>(null) }

    LaunchedEffect(targetId) {
        info = ApkInspector.inspect(context, target.apkPath)
    }

    val report = remember(info) {
        val p = info
        buildString {
            appendLine("# Puente report — ${target.displayName}")
            appendLine()
            appendLine("- generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("- sha256: `${target.id}`")
            appendLine("- size: ${target.sizeBytes} bytes")
            if (p != null) {
                appendLine()
                appendLine("## Package")
                appendLine("- package: `${p.packageName}`")
                appendLine("- version: ${p.versionName} (${p.versionCode})")
                appendLine("- minSdk: ${p.minSdk}")
                appendLine("- targetSdk: ${p.targetSdk}")
                appendLine("- app type: ${p.appType}")
                appendLine("- ABIs: ${p.abis.joinToString(", ").ifEmpty { "(none)" }}")
                appendLine()
                appendLine("## Permissions (${p.permissions.size})")
                p.permissions.forEach { appendLine("- $it") }
                appendLine()
                appendLine("## Activities (${p.activities.size})")
                p.activities.take(25).forEach { appendLine("- $it") }
                if (p.activities.size > 25) appendLine("- …+${p.activities.size - 25} more")
            }
        }
    }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { it.write(report.toByteArray()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Markdown report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Button(onClick = {
            exporter.launch("${target.displayName.substringBeforeLast('.')}.puente.md")
        }) {
            Text("Export as .md")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
