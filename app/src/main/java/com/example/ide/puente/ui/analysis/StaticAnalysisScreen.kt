package com.example.ide.puente.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.ide.puente.analysis.AnalysisPreflight
import com.example.ide.puente.analysis.JadxDecompiler
import com.example.ide.puente.analysis.NativeDecompiler
import com.example.ide.puente.data.TargetStore
import com.example.ide.puente.exec.ApktoolRunner
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StaticAnalysisScreen(targetId: String) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) }
    val scope = rememberCoroutineScope()

    var running by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf("") }
    var fileTree by remember { mutableStateOf<List<String>>(emptyList()) }
    var nativeSoPath by remember { mutableStateOf("") }
    var nativeSymbolOrAddress by remember { mutableStateOf("main") }
    var preflight by remember { mutableStateOf(target?.let { AnalysisPreflight.snapshot(context, it) }) }

    if (target == null) {
        Text("Target $targetId not found", modifier = Modifier.padding(16.dp))
        return
    }

    fun appendLog(message: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        logLines = buildString {
            if (logLines.isNotBlank()) {
                append(logLines.trimEnd())
                append("\n")
            }
            append("[$ts] ")
            append(message)
        }
    }

    fun refreshFileTree(outDir: File) {
        fileTree = if (outDir.exists()) {
            outDir.walkTopDown()
                .filter { it.isFile }
                .take(120)
                .map { it.relativeTo(outDir).path }
                .toList()
        } else emptyList()
    }

    fun autoDetectNativeSo(decodedDir: File) {
        if (nativeSoPath.isNotBlank()) return
        val guess = decodedDir.walkTopDown()
            .firstOrNull { it.isFile && it.extension == "so" }
        if (guess != null) {
            nativeSoPath = guess.absolutePath
            appendLog("Auto-detected native library: ${guess.name}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Static toolchain (apktool + jadx + native)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            target.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Readiness checks", fontWeight = FontWeight.SemiBold)
                preflight?.checks?.forEach { check ->
                    val icon = if (check.ok) "✅" else "❌"
                    Text("$icon ${check.name}: ${check.detail}", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !running, onClick = {
                        preflight = AnalysisPreflight.snapshot(context, target)
                        appendLog("Preflight refreshed: ${preflight?.passed ?: 0}/${preflight?.checks?.size ?: 0} checks OK")
                    }) {
                        Text("Refresh checks")
                    }
                    Button(
                        enabled = !running && (preflight?.apktoolReady == true),
                        onClick = {
                            running = true
                            scope.launch {
                                try {
                                    appendLog("Starting full pipeline")
                                    val decodedDir = File(target.workspacePath, "decoded").apply {
                                        if (exists()) deleteRecursively()
                                        mkdirs()
                                    }
                                    val decode = ApktoolRunner.run(
                                        context,
                                        listOf("d", target.apkPath, "-o", decodedDir.absolutePath, "-f")
                                    )
                                    appendLog("apktool finished with exit=${decode.exitCode}")
                                    if (decode.stderr.isNotBlank()) appendLog("apktool stderr: ${decode.stderr.take(240)}")
                                    refreshFileTree(decodedDir)
                                    autoDetectNativeSo(decodedDir)

                                    val jadxOut = File(target.workspacePath, "jadx")
                                    val jadx = JadxDecompiler.decompile(File(target.apkPath), jadxOut)
                                    appendLog("jadx: ${jadx.message} (java=${jadx.generatedFiles})")
                                    if (jadx.success) refreshFileTree(jadxOut)

                                    if (nativeSoPath.isNotBlank() && (preflight?.nativeDecompilerReady == true)) {
                                        val native = NativeDecompiler.run(context, File(nativeSoPath), nativeSymbolOrAddress)
                                        appendLog("native: ${native.detail}")
                                    } else {
                                        appendLog("native step skipped (missing .so path or native binary)")
                                    }

                                    val report = File(target.workspacePath, "analysis-report.txt")
                                    report.writeText(logLines)
                                    appendLog("Report exported: ${report.absolutePath}")
                                } finally {
                                    running = false
                                }
                            }
                        }
                    ) {
                        Text("Run full pipeline")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1) APK decode", fontWeight = FontWeight.SemiBold)
                Button(
                    enabled = !running,
                    onClick = {
                        running = true
                        scope.launch {
                            try {
                                val outDir = File(target.workspacePath, "decoded").apply {
                                    if (exists()) deleteRecursively()
                                    mkdirs()
                                }
                                val result = ApktoolRunner.run(
                                    context,
                                    listOf("d", target.apkPath, "-o", outDir.absolutePath, "-f")
                                )
                                appendLog("apktool decode exit=${result.exitCode}")
                                if (result.stderr.isNotBlank()) appendLog(result.stderr.take(500))
                                refreshFileTree(outDir)
                                autoDetectNativeSo(outDir)
                            } finally {
                                running = false
                            }
                        }
                    }
                ) {
                    Text(if (running) "Running…" else "Run apktool d")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2) Java decompilation (jadx-core)", fontWeight = FontWeight.SemiBold)
                Button(
                    enabled = !running,
                    onClick = {
                        running = true
                        scope.launch {
                            try {
                                val outDir = File(target.workspacePath, "jadx")
                                val result = JadxDecompiler.decompile(
                                    apkFile = File(target.apkPath),
                                    outputDir = outDir
                                )
                                appendLog("jadx: ${result.message} (java=${result.generatedFiles})")
                                refreshFileTree(outDir)
                            } finally {
                                running = false
                            }
                        }
                    }
                ) { Text("Run JADX") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3) Native decompilation (r2ghidra-dec)", fontWeight = FontWeight.SemiBold)
                Text(
                    "Drop r2ghidra-dec in filesDir/bin, then run with .so path and symbol/address.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nativeSoPath,
                    onValueChange = { nativeSoPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Absolute path to .so") },
                    placeholder = { Text("${target.workspacePath}/decoded/lib/arm64-v8a/libtarget.so") }
                )
                OutlinedTextField(
                    value = nativeSymbolOrAddress,
                    onValueChange = { nativeSymbolOrAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Symbol or address") },
                    placeholder = { Text("main or sym.Java_pkg_Class_func or 0x1234") }
                )
                Button(
                    enabled = !running && nativeSoPath.isNotBlank(),
                    onClick = {
                        running = true
                        scope.launch {
                            try {
                                val result = NativeDecompiler.run(
                                    context = context,
                                    targetSo = File(nativeSoPath),
                                    symbolOrAddress = nativeSymbolOrAddress
                                )
                                appendLog("native: ${result.detail} (exit=${result.exitCode})")
                                if (result.stdout.isNotBlank()) appendLog(result.stdout.take(800))
                                if (result.stderr.isNotBlank()) appendLog(result.stderr.take(800))
                            } finally {
                                running = false
                            }
                        }
                    }
                ) {
                    Text("Run native decompile")
                }
            }
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
