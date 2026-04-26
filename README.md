# 📱 Vibing Sam - Chat Interactivo para Modificación de APKs

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Vibing Sam es una aplicación Android que permite modificar APKs mediante un chat interactivo. Puedes enviar comandos como "Quiero que la app sea violeta" y la aplicación modificará el APK automáticamente usando herramientas como `apktool` y `jadx`.

## 🌟 Características

- **Chat Interactivo**: Envía comandos en lenguaje natural para modificar APKs.
- **Modificación de APKs**: Cambia colores, recursos y más usando `apktool` y `jadx`.
- **Integración con Hugging Face**: Conecta con modelos de IA para análisis avanzado.
- **GitHub Actions**: Construye APKs automáticamente en cada push.
- **Análisis Estático**: Usa `jadx` para inspeccionar el código de los APKs.

## 📸 Capturas de Pantalla

<div style="display: flex; justify-content: space-around;">
  <img src="https://via.placeholder.com/200" width="200" alt="Chat Interactivo"/>
  <img src="https://via.placeholder.com/200" width="200" alt="Modificación de APK"/>
  <img src="https://via.placeholder.com/200" width="200" alt="Integración con IA"/>
</div>

## ⚡ Guía Rápida

### 📋 Requisitos
- Dispositivo Android con API 24 o superior.
- `apktool` y `jadx` instalados en tu sistema para modificar APKs.

### 🛠️ Instalación
1. Clona el repositorio:
   ```bash
   git clone https://github.com/Merc98/vibing-sam.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza las dependencias de Gradle.
4. Compila y ejecuta en tu dispositivo o emulador.

### 🔑 Configuración
1. Asegúrate de tener `apktool` y `jadx` instalados y disponibles en tu PATH.
2. Configura tus claves de API para Hugging Face en el archivo de configuración.

## 🎯 Uso

### 📁 Modificación de APKs
1. Abre la aplicación.
2. Usa el chat para enviar comandos como:
   - "Quiero que la app sea violeta"
   - "Cambia el icono de la app"
3. La aplicación modificará el APK y generará un archivo `modified.apk`.

### 🤖 Integración con IA
1. Usa el chat para conectarte con modelos de Hugging Face.
2. Envía comandos para análisis avanzado de APKs.

## 🏗️ Arquitectura Técnica

### 🧰 Construido con
- **Kotlin**: Lenguaje moderno y conciso.
- **Jetpack Compose**: Para interfaces de usuario nativas.
- **Retrofit**: Cliente HTTP para conectarse a APIs.
- **Coroutines**: Para programación asíncrona.

### 📁 Estructura del Proyecto
```
app/src/main/java/com/example/vibingsam/
├── MainActivity.kt          # Chat interactivo
├── HuggingFaceApiService.kt # Integración con Hugging Face
└── modify_apk.py            # Script para modificar APKs
```

## 📦 GitHub Actions

Este repositorio incluye un workflow de GitHub Actions para construir APKs automáticamente:
- Construye APKs en cada push a `main` o `feature/chat-mod-apk`.
- Permite construir APKs manualmente (debug/release).
- Sube los APKs generados como artefactos.

## 🤝 Contribución

¡Las contribuciones son bienvenidas! Si quieres contribuir:
1. Haz un fork del repositorio.
2. Crea una rama con tu funcionalidad (`git checkout -b feature/NuevaFuncionalidad`).
3. Haz commit de tus cambios (`git commit -m 'Añade NuevaFuncionalidad'`).
4. Haz push a la rama (`git push origin feature/NuevaFuncionalidad`).
5. Abre un Pull Request.

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---