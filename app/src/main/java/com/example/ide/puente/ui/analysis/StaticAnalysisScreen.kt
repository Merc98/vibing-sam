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
import com.example.ide.data.model.AIModelType
import com.example.ide.data.repository.AIRepository
import com.example.ide.puente.analysis.AnalysisPreflight
import com.example.ide.puente.analysis.JadxDecompiler
import com.example.ide.puente.analysis.LlmPatchOrchestrator
import com.example.ide.puente.analysis.NativeDecompiler
import com.example.ide.puente.data.TargetStore
import com.example.ide.puente.exec.ApktoolRunner
import com.example.ide.puente.sign.PuenteSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val aiRepository = remember { AIRepository() }

    var running by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf("") }
    var fileTree by remember { mutableStateOf<List<String>>(emptyList()) }
    var nativeSoPath by remember { mutableStateOf("") }
    var nativeSymbolOrAddress by remember { mutableStateOf("main") }
    var preflight by remember { mutableStateOf(target?.let { AnalysisPreflight.snapshot(context, it) }) }

    // AI patch workflow states
    var patchGoal by remember { mutableStateOf("Cambiar texto de onboarding y habilitar endpoint de staging") }
    var apiKey by remember { mutableStateOf("") }
    var modelId by remember { mutableStateOf("OPENROUTER_AUTO") }
    var patchPreview by remember { mutableStateOf<LlmPatchOrchestrator.PreviewResult?>(null) }

    if (target == null) {
        Text("Target $targetId not found", modifier = Modifier.padding(16.dp))
        return
    }

    val decodedDir = remember(targetId) { File(target.workspacePath, "decoded") }
    val rebuiltApk = remember(targetId) { File(target.workspacePath, "rebuilt_from_ai.apk") }
    val signedApk = remember(targetId) { File(target.workspacePath, "patched_ai_signed.apk") }

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

    fun autoDetectNativeSo(dir: File) {
        if (nativeSoPath.isNotBlank()) return
        val guess = dir.walkTopDown().firstOrNull { it.isFile && it.extension == "so" }
        if (guess != null) {
            nativeSoPath = guess.absolutePath
            appendLog("Auto-detected native library: ${guess.name}")
        }
    }

    fun parseModel(input: String): AIModelType = runCatching { AIModelType.valueOf(input.trim().uppercase(Locale.US)) }
        .getOrDefault(AIModelType.LOCAL_SMART_ASSIST)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Static toolchain (apktool + jadx + native + AI patch)",
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
                                    decodedDir.apply {
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
                                decodedDir.apply {
                                    if (exists()) deleteRecursively()
                                    mkdirs()
                                }
                                val result = ApktoolRunner.run(
                                    context,
                                    listOf("d", target.apkPath, "-o", decodedDir.absolutePath, "-f")
                                )
                                appendLog("apktool decode exit=${result.exitCode}")
                                if (result.stderr.isNotBlank()) appendLog(result.stderr.take(500))
                                refreshFileTree(decodedDir)
                                autoDetectNativeSo(decodedDir)
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4) AI patch studio (preview -> verify -> rebuild)", fontWeight = FontWeight.SemiBold)
                Text(
                    "Flujo tipo Replit: describes objetivo, generamos preview de cambios, validamos rebuild y luego firmamos APK.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("AIModelType (ej. OPENROUTER_AUTO o LOCAL_SMART_ASSIST)") }
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key (si aplica)") }
                )
                OutlinedTextField(
                    value = patchGoal,
                    onValueChange = { patchGoal = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Objetivo funcional que quieres aplicar") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !running && decodedDir.exists(),
                        onClick = {
                            running = true
                            scope.launch {
                                try {
                                    val preview = LlmPatchOrchestrator.generatePreview(
                                        decodedDir = decodedDir,
                                        userGoal = patchGoal,
                                        model = parseModel(modelId),
                                        apiKey = apiKey,
                                        aiRepository = aiRepository
                                    )
                                    patchPreview = preview
                                    if (preview.success) {
                                        appendLog("AI preview ready: ${preview.items.size} file changes")
                                    } else {
                                        appendLog("AI preview failed: ${preview.error}")
                                    }
                                } finally {
                                    running = false
                                }
                            }
                        }
                    ) { Text("Generate preview") }

                    Button(
                        enabled = !running && patchPreview?.success == true,
                        onClick = {
                            running = true
                            scope.launch {
                                try {
                                    val preview = patchPreview ?: return@launch
                                    val apply = LlmPatchOrchestrator.applyPlan(decodedDir, preview.rawModelOutput)
                                    apply.onSuccess { count ->
                                        appendLog("Applied $count AI file edits")
                                        refreshFileTree(decodedDir)
                                    }.onFailure {
                                        appendLog("Apply failed: ${it.message}")
                                    }
                                } finally {
                                    running = false
                                }
                            }
                        }
                    ) { Text("Apply edits") }
                }

                Button(
                    enabled = !running && decodedDir.exists(),
                    onClick = {
                        running = true
                        scope.launch {
                            try {
                                val build = ApktoolRunner.run(
                                    context,
                                    listOf("b", decodedDir.absolutePath, "-o", rebuiltApk.absolutePath)
                                )
                                appendLog("Rebuild result exit=${build.exitCode}")
                                if (build.stderr.isNotBlank()) appendLog(build.stderr.take(800))
                                if (build.exitCode == 0 && rebuiltApk.exists()) {
                                    val sign = withContext(Dispatchers.IO) {
                                        PuenteSigner.zipAlignAndSign(context, rebuiltApk, signedApk)
                                    }
                                    appendLog(sign.detail)
                                }
                            } finally {
                                running = false
                            }
                        }
                    }
                ) {
                    Text("Verify + rebuild + sign APK")
                }

                patchPreview?.let { preview ->
                    Text(
                        text = if (preview.success) "Preview: ${preview.summary}" else "Preview error: ${preview.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (preview.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    preview.items.take(4).forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(item.path, fontWeight = FontWeight.SemiBold)
                                Text(item.reason, style = MaterialTheme.typography.bodySmall)
                                Text("--- before ---\n${item.beforeSnippet}", style = MaterialTheme.typography.bodySmall)
                                Text("--- after ---\n${item.afterSnippet}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (running) CircularProgressIndicator()

        if (fileTree.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
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
