package com.example.vibingsam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                chatMessages = chatMessages + "Conectando con Hugging Face..."
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