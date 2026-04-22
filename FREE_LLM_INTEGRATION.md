# 🚀 Integración de LLMs GRATUITOS - Vibing-SAM

## 📱 Arquitectura: TODO EN UNA PANTALLA

```
┌────────────────────────────────────────────────────────────┐
│                      VIBING-SAM MAIN                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   TOP BAR                            │  │
│  │  ☰─────────────────────────────┘│  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │    CHAT + TIPS (Collapsible / 1/4 height)         │  │
│  │  ┌─────────────────────────────────────────────────┐│  │
│  │  │ 💡 TIPS & RECOMMENDATIONS (Auto-generated)     ││  │
│  │  │ • Optimize this loop → use Stream API          ││  │
│  │  │ • Add null check here → NPE prevention         ││  │
│  │  │ • Consider async → better UX                   ││  │
│  │  └─────────────────────────────────────────────────┘│  │
│  │  ┌─────────────────────────────────────────────────┐│  │
│  │  │ 🤖 AI Chat (Multiple LLMs)                      ││  │
│  │  │ [Message input + Send button]                   ││  │
│  │  └─────────────────────────────────────────────────┘│  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

 ---
 
 ## 🆓 LLMs INTEGRADOS (Gratuitos)
 
 | Proveedor | Modelo | API | Velocidad | Calidad |
 |-----------|--------|-----|-----------|---------|
 | **HuggingFace** | Mistral 7B | `huggingface.co/api` | ⚡⚡⚡ | ⭐⭐⭐⭐ |
 | **Ollama** | Llama 2 | `localhost:11434` | ⚡⚡⚡⚡ | ⭐⭐⭐⭐ |
 | **LM Studio** | Phi 2 | `localhost:1234` | ⚡⚡⚡⚡⚡ | ⭐⭐⭐⭐ |
 | **Grok** (xAI) | Grok-1 | API gratuita | ⚡⚡ | ⭐⭐⭐⭐⭐ |
 | **Open Router** | Múltiples | `openrouter.io` | ⚡⚡⚡ | ⭐⭐⭐⭐ |
 
 ---
 
 ## 🔌 Interfaz Unificada de LLMs
 
 ```kotlin
// data/remote/LlmProvider.kt
sealed class LlmProvider {
    data class HuggingFace(val apiKey: String = "") : LlmProvider()
    data class Ollama(val baseUrl: String = "http://localhost:11434") : LlmProvider()
    data class LmStudio(val baseUrl: String = "http://localhost:1234") : LlmProvider()
    data class Grok(val apiKey: String) : LlmProvider()
    data class OpenRouter(val apiKey: String) : LlmProvider()
}

interface LlmService {
    suspend fun generateText(
        prompt: String,
        maxTokens: Int = 256
    ): String

    suspend fun generateTips(
        code: String,
        language: String
    ): List<String>

    suspend fun chat(
        message: String,
        context: String = ""
    ): String
}
```

---

## 💡 Sistema de TIPS Automáticos

```kotlin
// domain/usecase/GenerateTipsUseCase.kt
class GenerateTipsUseCase(
    private val llmService: LlmService,
    private val codeAnalyzer: CodeAnalyzer
) {
    suspend fun generateTips(code: String): List<Tip> {
        val analysis = codeAnalyzer.analyze(code)

        val prompt = """
            Analiza este código ${analysis.language}:
            ```
            $code
            ```

            Genera 3-5 tips de optimización específicos y concretos.
            Formato: "Acción → Beneficio"
        """.trimIndent()

        val response = llmService.generateText(prompt)
        return parseTips(response)
    }
}

data class Tip(
    val id: String,
    val title: String,
    val description: String,
    val category: TipCategory,
    val appliedCode: String? = null
)

enum class TipCategory {
    PERFORMANCE, READABILITY, SECURITY, BEST_PRACTICE, BUG_PREVENTION
}
```

---

## 🎯 MainScreen - TODO EN UNO

```kotlin
// ui/screen/MainScreen.kt
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val tips by viewModel.tips.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val selectedLlm by viewModel.selectedLlm.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP BAR
        TopBar(
            selectedLlm = selectedLlm,
            onLlmSelected = { viewModel.setLlm(it) },
            onSync = { viewModel.syncCode() }
        )

        // MAIN EDITOR (70% height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        ) {
            CodeEditor(
                code = state.code,
                onCodeChange = {
                    viewModel.updateCode(it)
                    viewModel.generateTipsAsync(it) // Tips en tiempo real
                },
                language = state.selectedLanguage
            )
        }

        // DIVIDER
        Divider()

        // BOTTOM PANEL (30% height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
        ) {
            Column {
                // TIPS SECTION
                if (tips.isNotEmpty()) {
                    TipsPanel(
                        tips = tips,
                        onTipApply = { tip -> viewModel.applyTip(tip) }
                    )
                    Divider()
                }

                // CHAT SECTION
                ChatPanel(
                    messages = chatMessages,
                    selectedLlm = selectedLlm,
                    onSendMessage = { message ->
                        viewModel.sendChatMessage(message)
                    }
                )
            }
        }
    }
}
```

---

## 🤖 ViewModel - Orquestación

```kotlin
// viewmodel/MainViewModel.kt
@HiltViewModel
class MainViewModel @Inject constructor(
    private val codeRepository: CodeRepository,
    private val llmFactory: LlmServiceFactory,
    private val tipsUseCase: GenerateTipsUseCase,
    private val chatUseCase: ChatUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _tips = MutableStateFlow<List<Tip>>(emptyList())
    val tips: StateFlow<List<Tip>> = _tips.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedLlm = MutableStateFlow<LlmProvider>(LlmProvider.Ollama())
    val selectedLlm: StateFlow<LlmProvider> = _selectedLlm.asStateFlow()

    fun updateCode(newCode: String) {
        _state.update { it.copy(code = newCode) }
    }

    fun generateTipsAsync(code: String) {
        viewModelScope.launch {
            try {
                val tips = tipsUseCase.generateTips(code)
                _tips.value = tips
            } catch (e: Exception) {
                _tips.value = emptyList()
            }
        }
    }

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            val llmService = llmFactory.create(_selectedLlm.value)
            val response = llmService.chat(
                message = message,
                context = _state.value.code
            )

            _chatMessages.value = _chatMessages.value + listOf(
                ChatMessage(message, ChatMessage.Role.USER),
                ChatMessage(response, ChatMessage.Role.ASSISTANT)
            )
        }
    }

    fun setLlm(provider: LlmProvider) {
        _selectedLlm.value = provider
    }
}

data class MainUiState(
    val code: String = "",
    val selectedLanguage: String = "kotlin",
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## 📡 Implementación de Servicios LLM

### 1. HuggingFace (API)
```kotlin
// data/remote/HuggingFaceLlmService.kt
class HuggingFaceLlmService(
    private val apiKey: String,
    private val client: OkHttpClient
) : LlmService {

    override suspend fun generateText(prompt: String, maxTokens: Int): String {
        val request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.1")
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                """
                {
                    "inputs": "$prompt",
                    "parameters": {
                        "max_length": $maxTokens
                    }
                }
                """.toByteArray()
            ))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return client.newCall(request).execute().body?.string() ?: ""
    }
}
```

### 2. Ollama (Local)
```kotlin
// data/remote/OllamaLlmService.kt
class OllamaLlmService(
    private val baseUrl: String = "http://localhost:11434",
    private val client: OkHttpClient
) : LlmService {

    override suspend fun generateText(prompt: String, maxTokens: Int): String {
        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                """
                {
                    "model": "llama2",
                    "prompt": "$prompt",
                    "stream": false
                }
                """.toByteArray()
            ))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().body?.string() ?: ""
        }
    }
}
```

### 3. LM Studio (Local)
```kotlin
// data/remote/LmStudioLlmService.kt
class LmStudioLlmService(
    private val baseUrl: String = "http://localhost:1234",
    private val client: OkHttpClient
) : LlmService {

    override suspend fun generateText(prompt: String, maxTokens: Int): String {
        val request = Request.Builder()
            .url("$baseUrl/v1/completions")
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                """
                {
                    "prompt": "$prompt",
                    "max_tokens": $maxTokens,
                    "temperature": 0.7
                }
                """.toByteArray()
            ))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().body?.string() ?: ""
        }
    }
}
```

### 4. Grok (xAI - Gratuito con límites)
```kotlin
// data/remote/GrokLlmService.kt
class GrokLlmService(
    private val apiKey: String,
    private val client: OkHttpClient
) : LlmService {

    override suspend fun generateText(prompt: String, maxTokens: Int): String {
        val request = Request.Builder()
            .url("https://api.x.ai/v1/chat/completions")
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                """
                {
                    "model": "grok-1",
                    "messages": [
                        {
                            "role": "user",
                            "content": "$prompt"
                        }
                    ],
                    "max_tokens": $maxTokens
                }
                """.toByteArray()
            ))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().body?.string() ?: ""
        }
    }
}
```

---

## 🏭 Factory Pattern para LLMs

```kotlin
// data/remote/LlmServiceFactory.kt
class LlmServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    fun create(provider: LlmProvider): LlmService = when (provider) {
        is LlmProvider.HuggingFace ->
            HuggingFaceLlmService(provider.apiKey, okHttpClient)

        is LlmProvider.Ollama ->
            OllamaLlmService(provider.baseUrl, okHttpClient)

        is LlmProvider.LmStudio ->
            LmStudioLlmService(provider.baseUrl, okHttpClient)

        is LlmProvider.Grok ->
            GrokLlmService(provider.apiKey, okHttpClient)

        is LlmProvider.OpenRouter ->
            OpenRouterLlmService(provider.apiKey, okHttpClient)
    }
}
```

---

## 🎨 Componentes UI

### Tips Panel
```kotlin
// ui/components/TipsPanel.kt
@Composable
fun TipsPanel(
    tips: List<Tip>,
    onTipApply: (Tip) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        item {
            Text(
                "💡 Recomendaciones",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(tips) { tip ->
            TipCard(
                tip = tip,
                onApply = { onTipApply(tip) }
            )
        }
    }
}

@Composable
fun TipCard(
    tip: Tip,
    onApply: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(tip.title, fontWeight = FontWeight.Bold)
            Text(tip.description, style = MaterialTheme.typography.bodySmall)
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onApply, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Aplicar")
                }
            }
        }
    }
}
```

### Chat Panel
```kotlin
// ui/components/ChatPanel.kt
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    selectedLlm: LlmProvider,
    onSendMessage: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                ChatBubble(
                    message = msg,
                    isUser = msg.role == ChatMessage.Role.USER
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pregunta sobre el código...") },
                maxLines = 2
            )

            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        onSendMessage(message)
                        message = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Enviar")
            }
        }
    }
}
```

---

## 📋 Build.gradle.kts Updates

```gradle
dependencies {
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")

    // DI
    implementation("com.google.dagger:hilt-android:2.46")
    kapt("com.google.dagger:hilt-compiler:2.46")

    // UI
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
}
```

---

## 🚀 Setup Inicial (Sin Pago)

### Opción 1: Ollama (Completamente Local)
```bash
# Instalar Ollama
brew install ollama  # macOS
# Descargar modelo
ollama pull llama2
# Ejecutar
ollama serve
```

### Opción 2: LM Studio (GUI Local)
```
1. Descargar desde lmstudio.ai
2. Instalar modelo Phi-2
3. Ejecutar servidor en localhost:1234
```

### Opción 3: HuggingFace (Gratuito, Online)
```
1. Crear cuenta en huggingface.co
2. Ir a Settings → Access Tokens
3. Crear token gratuito (ilimitado para modelos públicos)
4. Usar API gratuita
```

### Opción 4: Grok (Gratuito, Limited)
```
1. Obtener API key en x.ai
2. Usar para primeros requests gratis
```

---

## ✅ Checklist de Implementación

- [ ] Crear interfaz LlmService
- [ ] - [ ] Implementar OllamaLlmService
- [ ] - [ ] Implementar HuggingFaceLlmService
- [ ] - [ ] Implementar LmStudioLlmService
- [ ] - [ ] Implementar GrokLlmService
- [ ] - [ ] Crear GenerateTipsUseCase
- [ ] - [ ] Implementar MainScreen
- [ ] - [ ] Crear MainViewModel
- [ ] - [ ] Implementar TipsPanel
- [ ] - [ ] Implementar ChatPanel
- [ ] - [ ] Configurar Inyección de Dependencias
- [ ] - [ ] Probar con Ollama local
- [ ] - [ ] Documentar setup para usuarios
- [ ]
- [ ] ---
- [ ]
- [ ] ## 🎯 Flujo de Usuario
- [ ]
- [ ] ```
- [ ] 1. Usuario abre app
- [ ]    ↓
- [ ]    2. Selecciona LLM (Ollama, HuggingFace, etc)
- [ ]       ↓
- [ ]   3. Escribe/pega código
- [ ]      ↓
- [ ]  4. App genera TIPS automáticamente
- [ ]     ↓
- [ ] 5. Usuario puede aplicar tips o chatear
- [ ]    ↓
- [ ]    6. Todo se guarda en local DB
- [ ]    ```
- [ ]
- [ ]    ¡Listo para compilar sin paywalls! 🚀Menu │ 🔄 Sync │ 🎨 Theme │ 💬 Model Selector   │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          MAIN EDITOR AREA (3/4 height)             │  │
│  │  ┌─────────────────────────────────────────────────┐│  │
│  │  │ 📝 CODE EDITOR                                 ││  │
│  │  │  (Syntax highlighting + Auto-complete)         ││  │
│  │  │                                                 ││  │
│  │  └────────────────────
