package com.example.ide.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ToolCommand(
    val command: String,
    val note: String
)

@Composable
fun ToolingScreen() {
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

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
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
                        Icon(Icons.Default.Security, contentDescription = null)
                        Text(
                            text = "APK Security Lab (Safe)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Includes APK inspection and decompilation workflows for apps you own or are authorized to assess.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No payload injection / no bypass automation is provided in this app.",
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
                        text = "Use the CI workflow (.github/workflows/android-apk.yml) to build debug/release APK artifacts from GitHub Actions.",
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
