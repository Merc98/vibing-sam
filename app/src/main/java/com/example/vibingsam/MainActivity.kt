package com.example.vibingsam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibingSamTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var showMenu by remember { mutableStateOf(true) }
    var chatMessages by remember { mutableStateOf(listOf("Checkpoint made 20 days ago", "Worked for 5 minutes", "Published your app 19 days ago")) }
    var userInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var modelDownloading by remember { mutableStateOf(false) }
    var modelDownloadProgress by remember { mutableStateOf(0) }

    // Permisos para almacenamiento
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            chatMessages = chatMessages + "Permiso de almacenamiento concedido."
        } else {
            chatMessages = chatMessages + "Permiso de almacenamiento denegado."
        }
    }

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    if (showMenu) {
        MainMenu(
            onModifyAPK = {
                chatMessages = chatMessages + "Modificando APK..."
                showMenu = false
            },
            onAnalyzeWithFrida = {
                chatMessages = chatMessages + "Analizando con Frida..."
                showMenu = false
            },
            onAnalyzeWithGhidra = {
                chatMessages = chatMessages + "Analizando con Ghidra..."
                showMenu = false
            },
            onConnectHuggingFace = {
                modelDownloading = true
                coroutineScope.launch {
                    try {
                        val modelId = "google/bert-base-uncased"
                        val fileName = "pytorch_model.bin"
                        val response = ApiClient.service.downloadModelFile(modelId, fileName)
                        val file = File(context.getExternalFilesDir(null), fileName)
                        val outputStream = FileOutputStream(file)
                        response.body()?.use { body ->
                            val totalSize = body.contentLength()
                            val buffer = ByteArray(1024)
                            var bytesRead: Int
                            var bytesDownloaded = 0L
                            while (body.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                modelDownloadProgress = (bytesDownloaded * 100 / totalSize).toInt()
                            }
                        }
                        outputStream.close()
                        chatMessages = chatMessages + "Modelo descargado: ${file.absolutePath}"
                    } catch (e: Exception) {
                        chatMessages = chatMessages + "Error al descargar el modelo: ${e.message}"
                    } finally {
                        modelDownloading = false
                    }
                }
                showMenu = false
            }
        )
    } else {
        ChatScreen(chatMessages, userInput, onInputChange = { userInput = it }, onSend = {
            if (userInput.isNotBlank()) {
                chatMessages = chatMessages + "Usuario: $userInput"
                userInput = ""
            }
        })
    }

    if (modelDownloading) {
        AlertDialog(
            onDismissRequest = { modelDownloading = false },
            title = { Text("Descargando Modelo") },
            text = { LinearProgressIndicator(progress = { modelDownloadProgress / 100f }) },
            confirmButton = {}
        )
    }
}

@Composable
fun ChatScreen(chatMessages: List<String>, userInput: String, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            chatMessages.forEach { message ->
                Text(
                    text = "• $message",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                label = { Text("Escribe un comando", color = Color.White) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
                    focusedIndicatorColor = Color.Blue,
                    unfocusedIndicatorColor = Color.Gray
                )
            )

            Button(
                onClick = onSend,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
fun VibingSamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme.dark(
            background = Color.Black,
            primary = Color.Blue,
            secondary = Color.Gray
        ),
        content = content
    )
}