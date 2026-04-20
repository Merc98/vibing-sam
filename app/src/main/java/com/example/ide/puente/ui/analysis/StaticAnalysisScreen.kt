package com.example.ide.puente.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ide.puente.data.TargetStore
import com.example.ide.puente.exec.ApktoolRunner
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StaticAnalysisScreen(targetId: String) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) }
    val scope = rememberCoroutineScope()

    var running by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf("") }
    var fileTree by remember { mutableStateOf<List<String>>(emptyList()) }

    if (target == null) {
        Text("Target $targetId not found", modifier = Modifier.padding(16.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "APKTool decompile (in-process via DexClassLoader)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            target.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            enabled = !running,
            onClick = {
                running = true
                logLines = ""
                scope.launch {
                    val outDir = File(target.workspacePath, "decoded").apply {
                        if (exists()) deleteRecursively()
                        mkdirs()
                    }
                    val result = ApktoolRunner.run(
                        context,
                        listOf("d", target.apkPath, "-o", outDir.absolutePath, "-f")
                    )
                    logLines = buildString {
                        if (result.stdout.isNotBlank()) {
                            appendLine("── stdout ──")
                            append(result.stdout)
                        }
                        if (result.stderr.isNotBlank()) {
                            appendLine("── stderr ──")
                            append(result.stderr)
                        }
                        appendLine()
                        append("exit=${result.exitCode}")
                    }
                    fileTree = if (outDir.exists()) {
                        outDir.walkTopDown()
                            .filter { it.isFile }
                            .take(50)
                            .map { it.relativeTo(outDir).path }
                            .toList()
                    } else emptyList()
                    running = false
                }
            }
        ) {
            Text(if (running) "Running…" else "Run apktool d")
        }

        if (running) CircularProgressIndicator()

        if (fileTree.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(fileTree) { entry ->
                        Text(entry, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (logLines.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logLines,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
