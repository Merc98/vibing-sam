# Technical Audit — vibing-sam

Date: 2026-04-21

## 1) Project identification
- **Primary stack**: Android app (Kotlin + Jetpack Compose) built with **Gradle Kotlin DSL**.
- **Type**: Mobile IDE + APK reverse-engineering helper workflows (“Puente”) with AI chat integrations.
- **Entrypoints**:
  - `com.example.ide.MainActivity` (main launcher)
  - `com.example.ide.puente.PuenteActivity` (reverse-engineering workspace)
- **Build system**: `./gradlew` + Android Gradle Plugin (`app/build.gradle.kts`).
- **Extra tooling**: Python scripts in `tools/` for offline APK lab analysis.

## 2) Critical defects found and fixed
1. **Repository contamination with generated artifacts**
   - `app/build/**` and `local.properties` were versioned.
   - This creates noisy diffs, unstable CI, and machine-specific breakage.
   - **Fix**: removed tracked generated files and hardened `.gitignore` with `**/build/`, `*.apk`, `local.properties`.

2. **Machine-specific SDK config committed**
   - `local.properties` pointed to a Windows path from a specific machine.
   - Breaks builds on Linux/macOS and CI if trusted blindly.
   - **Fix**: removed tracked `local.properties` and added `local.properties.example` template.

3. **Storage model mismatch (Android scoped storage)**
   - `FileRepository` relied on legacy public Downloads APIs.
   - On modern Android this causes reliability issues and permission friction.
   - **Fix**: switched project/patch roots to app-scoped Documents dir (`getExternalFilesDir`) with internal fallback.

4. **Over-permissioned manifest for legacy external storage**
   - READ/WRITE external storage permissions were still declared.
   - Not required for SAF + app-scoped file handling.
   - **Fix**: removed unused READ/WRITE storage permissions; kept INTERNET + package install permission.

## 3) What still requires environment setup (not code defects)
- Android SDK path must be configured locally using `local.properties`.
- NDK/ABI tools and optional binaries (apktool/frida payloads) are downloaded during build step as configured in Gradle task graph.

## 4) Suggested next hardening steps
- Add integration tests around Puente flows (import -> decode -> rebuild -> sign).
- Persist AI provider selection/API keys via DataStore (currently mainly runtime state in VM).
- Add checksum enforcement for downloaded binary artifacts before extraction/execution.
