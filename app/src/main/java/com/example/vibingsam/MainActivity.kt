package com.example.vibingsam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibingSamTheme {
                ChatScreen()
            }
        }
    }
}

@Composable
fun ChatScreen() {
    var userInput by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<String>()) }

    Column(modifier = Modifier.padding(16.dp)) {
        chatMessages.forEach { message ->
            Text(text = message, modifier = Modifier.padding(4.dp))
        }
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Ej: 'Quiero que la app sea violeta'") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            if (userInput.isNotEmpty()) {
                chatMessages = chatMessages + "Usuario: $userInput"
                // Lógica para procesar el comando y modificar el APK
                userInput = ""
            }
        }) {
            Text("Modificar APK")
        }
    }
}

@Composable
fun VibingSamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme.light(),
        content = content
    )
}