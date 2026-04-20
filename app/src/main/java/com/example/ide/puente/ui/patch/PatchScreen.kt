package com.example.ide.puente.ui.patch

import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ide.puente.sign.PuenteSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PatchScreen(targetId: String) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) }
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf("idle") }
    var log by remember { mutableStateOf("") }

    if (target == null) {
        Text("Target $targetId not found", modifier = Modifier.padding(16.dp))
        return
    }

    val decodedDir = remember(targetId) { File(target.workspacePath, "decoded") }
    val rebuiltApk = remember(targetId) { File(target.workspacePath, "rebuilt.apk") }
    val finalApk = remember(targetId) { File(target.workspacePath, "patched_signed.apk") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Patch & Sign pipeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Requires a prior apktool decode in Static tab (decoded/ folder).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            enabled = stage == "idle" && decodedDir.exists(),
            onClick = {
                scope.launch {
                    stage = "rebuilding"
                    log = "▶ apktool b ${decodedDir.absolutePath}\n"
                    val result = ApktoolRunner.run(
                        context,
                        listOf("b", decodedDir.absolutePath, "-o", rebuiltApk.absolutePath)
                    )
                    log += result.stdout + "\n" + result.stderr + "\nexit=${result.exitCode}\n\n"

                    if (result.exitCode != 0 || !rebuiltApk.exists()) {
                        stage = "idle"
                        return@launch
                    }

                    stage = "signing"
                    log += "▶ apksig zipalign + sign → ${finalApk.absolutePath}\n"
                    val signOutcome = withContext(Dispatchers.IO) {
                        PuenteSigner.zipAlignAndSign(context, rebuiltApk, finalApk)
                    }
                    log += signOutcome.detail + "\n"
                    stage = if (signOutcome.success) "ready" else "idle"
                }
            }
        ) {
            Text(if (stage == "idle") "Rebuild + Sign" else stage)
        }

        if (stage == "rebuilding" || stage == "signing") CircularProgressIndicator()

        if (finalApk.exists()) {
            OutlinedButton(
                onClick = {
                    val authority = "${context.packageName}.fileprovider"
                    val uri: Uri = FileProvider.getUriForFile(context, authority, finalApk)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text("Install patched APK")
            }
        }

        if (log.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        log,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
