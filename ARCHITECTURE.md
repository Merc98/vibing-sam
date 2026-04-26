# 🏗️ VibeCode Architecture

## Overview

VibeCode follows a **Chat-Centric Architecture** where all development activities revolve around the VibeCode Chat interface. This is the core of the "Vibe Mod" concept - a unified development experience.

## Core Components

### 1. VibeCode Chat (Central Hub)
**Location**: `ui/screen/ChatScreen.kt` + `ui/viewmodel/MainViewModel.kt`

The chat is not just a feature - it's the **central command center** for:
- Code generation and modification
- APK analysis and decompilation
- Tool execution (vibing_apk_lab, etc.)
- Project management commands
- Installed app inspection

### 2. Chat Actions System
**Location**: `domain/models.kt` - `ChatAction` sealed class

All actions that can be triggered from chat:
```kotlin
sealed class ChatAction {
    data class AnalyzeFile(val filePath: String) : ChatAction()
    data class InspectApp(val packageName: String) : ChatAction()
    data class DecompileApk(val apkPath: String, val outputDir: String) : ChatAction()
    data class GenerateCode(val language: String, val description: String) : ChatAction()
    data class ListInstalledApps(val filter: String?) : ChatAction()
    data class ExecuteTool(val toolId: String, val parameters: Map<String, String>) : ChatAction()
}
```

### 3. Tool Repository
**Location**: `data/local/ToolRepository.kt`

Manages external tools like `vibing_apk_lab.py`:
- APK inspection
- Decompilation (jadx)
- Decoding (apktool)
- Package management (ADB)

Available tools:
- `vibing_apk_lab_inspect` - APK structure analysis
- `vibing_apk_lab_decode` - Decode resources
- `vibing_apk_lab_decompile` - Decompile to Java
- `vibing_apk_lab_full` - Complete analysis
- `adb_list_packages` - List installed apps
- `adb_package_info` - Package details

### 4. Device App Repository
**Location**: `data/local/DeviceAppRepository.kt`

Provides access to installed applications:
- List all apps (system + user)
- Search by name/package
- Get APK paths
- App metadata (version, flags, etc.)

### 5. AI Repository
**Location**: `data/repository/AIRepository.kt`

Unified interface for multiple AI providers:
- OpenAI (GPT-4, GPT-3.5)
- Anthropic (Claude 3)
- Google (Gemini)
- OpenRouter (multi-model)
- Chinese models (Qwen, DeepSeek, Kimi)
- Local models (offline assistance)

## Data Flow

### Chat-Centric Workflow

```
User Input (Chat)
      ↓
MainViewModel.submitChatInput()
      ↓
┌─────────────────────────────────────┐
│ Is Slash Command?                   │
├──────────────┬──────────────────────┤
│ YES          │ NO                   │
│ ↓            │ ↓                    │
│ handleSlash  │ sendChatMessage      │
│ Command()    │ (to AI model)        │
│ ↓            │                      │
│ executeChat  │ ← AI Response        │
│ Action()     │                      │
└──────────────┴──────────────────────┘
      ↓
┌─────────────────────────────────────┐
│ ChatAction Types:                   │
│ • InspectApp → DeviceAppRepository  │
│ • DecompileApk → ToolRepository     │
│ • ExecuteTool → ToolRepository      │
│ • ListInstalledApps → UI Navigation │
└─────────────────────────────────────┘
      ↓
Result displayed in Chat
```

### APK Analysis Flow

```
User selects app in Installed Apps
           ↓
Tap "Analyze in Chat"
           ↓
viewModel.submitChatInput("Analyze this APK: {path}")
           ↓
executeChatAction(DecompileApk(apkPath))
           ↓
toolRepository.executeTool("vibing_apk_lab_decompile", ...)
           ↓
Tool executes (Python script + jadx/apktool)
           ↓
Output streamed back to chat
           ↓
User can insert suggestions into project
```

## Key Design Principles

### 1. Chat as the Single Source of Truth
All development activities are initiated and tracked through chat. No fragmented workflows.

### 2. Context Awareness
The AI always knows:
- Current project
- Active file
- Recent chat history
- Selected tools/apps

### 3. Tool Unification
External tools (vibing_apk_lab, ADB, etc.) are accessed through a single interface (`/tool` command).

### 4. Progressive Disclosure
- Simple commands for common tasks (`/debug`, `/refactor`)
- Advanced options available but not overwhelming
- Tools revealed as needed

### 5. Safe Automation
- Script templates are non-invasive
- No automatic patching of third-party apps
- User confirmation for destructive actions

## File Structure

```
app/src/main/java/com/example/ide/
├── domain/
│   └── models.kt              # ChatAction, ToolCommand, etc.
├── data/
│   ├── local/
│   │   ├── DeviceAppRepository.kt   # Installed apps access
│   │   └── ToolRepository.kt        # External tool execution
│   ├── repository/
│   │   ├── AIRepository.kt          # AI provider abstraction
│   │   └── FileRepository.kt        # Project/file management
│   └── api/                         # Retrofit services
├── ui/
│   ├── screen/
│   │   ├── ChatScreen.kt            # Central hub
│   │   ├── InstalledAppsScreen.kt   # App browser
│   │   ├── ProjectsScreen.kt        # Project manager
│   │   └── EditorScreen.kt          # Code editor
│   └── viewmodel/
│       └── MainViewModel.kt         # State + actions
└── di/
    └── ViewModelFactory.kt          # Dependency injection
```

## Extension Points

### Adding New Tools
1. Add tool definition in `ToolRepository.getAvailableTools()`
2. Implement command template
3. Add slash command in `MainViewModel.handleSlashCommand()`
4. Handle action in `executeChatAction()`

### Adding New Chat Commands
1. Define new `ChatAction` subtype in `domain/models.kt`
2. Add slash command handler in `handleSlashCommand()`
3. Implement execution logic in `executeChatAction()`

### Adding AI Models
1. Add model type in `AIModelType` enum
2. Implement API service in `data/api/`
3. Add routing in `AIRepository.sendMessage()`
4. Register in `AIRepository.getAvailableModels()`

## Security Considerations

- Command execution is sandboxed
- APK analysis requires user confirmation
- API keys stored locally (encryption recommended for production)
- No code sent to external servers except AI endpoints
- Tool paths validated before execution
