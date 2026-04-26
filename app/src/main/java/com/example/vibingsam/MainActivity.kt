package com.example.vibingsam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    text = message,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Ej: 'Quiero que la app sea violeta'") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        chatMessages = chatMessages + "Usuario: $userInput"
                        coroutineScope.launch {
                            // Lógica para procesar el comando y modificar el APK
                            chatMessages = chatMessages + "Sistema: Procesando comando..."
                        }
                        userInput = ""
                    }
                },
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
            primary = androidx.compose.ui.graphics.Color(0xFFBB86FC),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3)
        ),
        content = content
    )
}