package com.example.ide.puente.ui.frida

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ide.puente.data.TargetStore
import com.example.ide.puente.frida.FridaGadgetInjector
import com.example.ide.puente.frida.FridaTemplates
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridaScreen(targetId: String) {
    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }

    if (target == null) {
        Text("Target $targetId not found", modifier = Modifier.padding(16.dp))
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Gadget Inject") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Script Editor") }
            )
        }

        when (selectedTab) {
            0 -> GadgetInjectTab(target.id)
            1 -> ScriptEditorTab()
        }
    }
}

@Composable
private fun GadgetInjectTab(targetId: String) {    val context = LocalContext.current
    val store = remember { TargetStore.get(context) }
    val target = remember(targetId) { store.find(targetId) } ?: return
    val scope = rememberCoroutineScope()

    var log by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Frida gadget injection", fontWeight = FontWeight.Bold)
        Text(
            "Pipeline: decode → drop gadget.so → patch launcher Activity.smali " +
                    "→ rebuild → sign. Output placed in workspace/patched_frida_signed.apk.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            enabled = !running,
            onClick = {
                running = true
                log = ""
                scope.launch {
                    FridaGadgetInjector.run(context, target).collect { line ->
                        log += "$line\n"
                    }
                    running = false
                }
            }
        ) {
            Text(if (running) "Injecting…" else "Inject Frida Gadget")
        }

        if (running) CircularProgressIndicator()

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

@Composable
private fun ScriptEditorTab() {
    val clipboard = LocalClipboardManager.current
    var script by remember { mutableStateOf(FridaTemplates.SSL_PINNING_BYPASS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Frida script editor", fontWeight = FontWeight.Bold)
        Text(
            "Preset templates below. Copy the final script and run it on your desktop via `frida -U -l script.js -n <package>`.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TemplateButtonsRow(scriptSetter = { script = it })

        OutlinedTextField(
            value = script,
            onValueChange = { script = it },
            modifier = Modifier
                .fillMaxWidth(),
            label = { Text("script.js") },
            minLines = 8,
            maxLines = 18
        )

        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(script)) }) {
            Text("Copy script")
        }
    }
}

@Composable
private fun TemplateButtonsRow(scriptSetter: (String) -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(onClick = { scriptSetter(FridaTemplates.SSL_PINNING_BYPASS) }) { Text("SSL unpin") }
        OutlinedButton(onClick = { scriptSetter(FridaTemplates.ROOT_DETECTION_BYPASS) }) { Text("Anti-root") }
        OutlinedButton(onClick = { scriptSetter(FridaTemplates.METHOD_TRACER) }) { Text("Trace") }
    }
}
