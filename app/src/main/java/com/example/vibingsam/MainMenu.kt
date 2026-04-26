package com.example.vibingsam

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainMenu(
    onModifyAPK: () -> Unit,
    onAnalyzeWithFrida: () -> Unit,
    onAnalyzeWithGhidra: () -> Unit,
    onConnectHuggingFace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onModifyAPK,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Modificar APK")
        }

        Button(
            onClick = onAnalyzeWithFrida,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analizar con Frida")
        }

        Button(
            onClick = onAnalyzeWithGhidra,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analizar con Ghidra")
        }

        Button(
            onClick = onConnectHuggingFace,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conectar con Hugging Face")
        }
    }
}