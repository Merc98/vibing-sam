# 📱 VibeCode Mobile - AI-Powered Code Editor with Vibing Code Concept

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

A revolutionary mobile IDE that brings **Vibing Code** concept to life - an AI-first development environment where your chat with the agent is the central hub for all coding activities.

## 🌟 The VibeCode Concept

**Vibing Code** is not just another IDE - it's a paradigm shift where:

1. **Chat-Centric Development**: Everything starts from a conversation with your AI agent
2. **Context-Aware Assistance**: The AI understands your projects, installed apps, and code
3. **Seamless Integration**: Insert programs, analyze APKs, refactor code - all from the chat
4. **No More Tool Fragmentation**: All your development tools unified in one place

## 🚀 Key Features

### 💬 VibeCode Chat (The Heart of the Experience)
- **Context-Aware AI**: Works with your current file and project automatically
- **Slash Commands**: 
  - `/refactor`, `/debug`, `/test` - Code improvement commands
  - `/insert_script` - Insert safe script templates
  - `/model <name>` - Switch AI models quickly
  - `/analyze_app <package>` - Analyze installed apps
  - `/decompile_apk <path>` - Decompile APK files
  - `/list_apps` - Browse installed applications
  - `/tool <id>` - Execute development tools
- **Multi-Model Support**: OpenAI GPT-4, Claude 3, Gemini Pro, and more
- **Code Insertion**: Directly insert AI-generated code into your projects
- **APK Analysis**: Analyze installed apps or APK files through chat
- **Tool Integration**: Execute vibing_apk_lab tools for decompilation, analysis, etc.

### 📁 Project Management
- Create and manage multiple coding projects
- Auto-organization in Downloads/IDEProjects
- Support for 20+ programming languages
- File templates for quick starts

### 📱 Installed Apps Integration
- Browse all installed applications on your device
- Search and filter by name or package
- View app details (version, package name, APK path)
- **Direct integration with VibeCode Chat**: Select an app and ask the AI to analyze it
- No more manual path copying - click to analyze

### ✏️ Smart Code Editor
- Syntax-aware editing experience
- Multi-language support (HTML, CSS, JS, Python, Java, Kotlin, etc.)
- Auto-save to project folders
- Export functionality

### 🤖 AI Models Supported
- **OpenAI**: GPT-4, GPT-3.5 Turbo
- **Anthropic**: Claude 3 Opus/Sonnet/Haiku
- **Google**: Gemini Pro, Gemini 1.5 Flash
- **Others**: Cohere, Mistral, Llama, CodeLlama
- **Chinese Models**: Qwen, DeepSeek, Kimi (via OpenRouter)

## 🏗️ Clean Architecture

```
app/src/main/java/com/example/ide/
├── data/
│   ├── api/              # API services for AI models
│   ├── local/            # Local repositories (DeviceAppRepository)
│   ├── model/            # Data models
│   └── repository/       # Business logic repositories
├── ui/
│   ├── screen/           # Jetpack Compose screens
│   │   ├── ChatScreen.kt         # VibeCode Chat (central hub)
│   │   ├── ProjectsScreen.kt     # Project management
│   │   ├── EditorScreen.kt       # Code editor
│   │   ├── InstalledAppsScreen.kt # App browser & analyzer
│   │   └── SettingsScreen.kt     # Configuration
│   ├── viewmodel/        # State management
│   └── theme/            # Material Design 3 theming
└── di/                   # Dependency injection
```

## ⚡ Quick Start

### Prerequisites
- Android 7.0+ (API 24)
- API keys for your preferred AI models

### Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run

### Configure AI
1. Go to Settings
2. Add your API keys (OpenAI, Anthropic, Google, etc.)
3. Select your preferred model
4. Start vibing!

## 🎯 How to Use VibeCode

### 1. Start a Project
- Navigate to **Projects** tab
- Create a new project or generate a starter web app/PWA

### 2. Open the Chat
- Switch to **VibeCode Chat** tab (this is where the magic happens)
- The AI already knows your current project and file

### 3. Browse Installed Apps (Optional)
- Go to **Installed Apps** tab
- Search for any app on your device
- Tap an app → "Analyze in Chat" → instantly sends context to AI

### 4. Code with AI
- Ask questions about your code
- Request refactoring, debugging, or tests
- Use slash commands for quick actions
- Insert generated code directly into your files

### Example Workflow
```
1. User: Opens Installed Apps, finds "com.example.myapp"
2. User: Taps "Analyze in Chat" 
3. Chat auto-fills: "Analyze this APK: /data/app/..."
4. User: Adds "Check for security issues and suggest improvements"
5. AI: Provides detailed analysis and code suggestions
6. User: Clicks to insert suggestions directly into project

OR use slash commands directly in chat:
- /analyze_app com.example.myapp
- /decompile_apk /path/to/app.apk
- /tool vibing_apk_lab_decompose {apk_path}
```

## 🔐 Security Notes

- API keys are stored locally using Android DataStore
- For production: Implement encryption using AndroidX Security
- APK analysis is for apps you own or have permission to test
- No code is sent to external servers except AI API endpoints

## 🛣️ Roadmap

- [ ] Syntax highlighting in editor
- [ ] Local AI models support (MLC, llama.cpp)
- [ ] Real-time collaboration
- [ ] Git integration
- [ ] Plugin system
- [ ] Terminal emulator
- [ ] Advanced APK analysis tools

## 📄 License

MIT License - see LICENSE file for details.

---

<p align="center">
  <strong>Made with ❤️ for developers who vibe with code!</strong>
</p>
