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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class ToolCommand(
    val command: String,
    val note: String
)

@Composable
fun ToolingScreen() {
    val commands = listOf(
        ToolCommand(
            command = "python tools/vibing_apk_lab.py ./my_app.apk --decode --decompile",
            note = "Decodifica recursos y decompila clases para revisión manual"
        ),
        ToolCommand(
            command = "python tools/vibing_apk_lab.py --list-packages",
            note = "Lista paquetes instalados en el dispositivo conectado"
        ),
        ToolCommand(
            command = "python tools/vibing_apk_lab.py --package com.example.app",
            note = "Consulta versión, estado debuggable y rutas de instalación"
        )
    )

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Column {
        SnackbarHost(hostState = snackbarHostState)

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
                ToolkitInfoCard(
                    icon = Icons.Default.Security,
                    title = "APK Security Lab (Safe)",
                    body = "Incluye inspección de APK y flujo de decompilación para apps propias o con autorización.",
                    footnote = "No incluye payload injection ni bypass automation."
                )
            }

            item {
                ToolkitInfoCard(
                    icon = Icons.Default.Inventory2,
                    title = "Cobertura funcional",
                    body = "• Revisión de estructura APK (manifest, dex, ABIs, resources)\n" +
                        "• Export decode/decompile para auditoría manual\n" +
                        "• Reporte JSON automatizable\n" +
                        "• Inspección de paquetes vía ADB"
                )
            }

            item {
                ToolkitInfoCard(
                    icon = Icons.Default.Build,
                    title = "Creación de APK",
                    body = "Puedes construir APK debug/release con GitHub Actions usando .github/workflows/android-apk.yml"
                )
            }

            item {
                Text(
                    text = "Comandos rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(commands) { command ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                            Text(
                                text = command.command,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = command.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(command.command))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Comando copiado")
                                }
                            }
                        ) {
                            Text("Copiar comando")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolkitInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    footnote: String? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium)

            footnote?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
