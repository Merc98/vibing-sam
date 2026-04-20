#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_PROPERTIES="$PROJECT_ROOT/local.properties"

echo "== Vibing Android Compile Doctor =="

if [[ ! -x "$PROJECT_ROOT/gradlew" ]]; then
  echo "[fix] gradlew is not executable. Applying chmod +x gradlew"
  chmod +x "$PROJECT_ROOT/gradlew"
fi

SDK_PATH="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK_PATH" && -f "$LOCAL_PROPERTIES" ]]; then
  SDK_PATH=$(sed -n 's/^sdk\.dir=//p' "$LOCAL_PROPERTIES" | sed 's#\\:#:#g' | sed 's#\\\\#/#g')
fi

if [[ -z "$SDK_PATH" ]]; then
  cat <<'EOF'
[error] Android SDK path not found.
Set one of the following:
  - ANDROID_HOME=/path/to/Android/Sdk
  - ANDROID_SDK_ROOT=/path/to/Android/Sdk
Or create local.properties with:
  sdk.dir=/path/to/Android/Sdk
EOF
  exit 1
fi

if [[ ! -d "$SDK_PATH" ]]; then
  echo "[error] SDK directory does not exist: $SDK_PATH"
  exit 1
fi

echo "[ok] SDK path: $SDK_PATH"

if [[ -x "$SDK_PATH/cmdline-tools/latest/bin/sdkmanager" ]]; then
  SDKMANAGER="$SDK_PATH/cmdline-tools/latest/bin/sdkmanager"
elif command -v sdkmanager >/dev/null 2>&1; then
  SDKMANAGER="$(command -v sdkmanager)"
else
  SDKMANAGER=""
fi

if [[ -n "$SDKMANAGER" ]]; then
  echo "[info] Ensuring required SDK packages are installed"
  yes | "$SDKMANAGER" --licenses >/dev/null || true
  "$SDKMANAGER" "platform-tools" "platforms;android-36" "build-tools;36.0.0"
else
  echo "[warn] sdkmanager not found; skipping package installation"
fi

echo "[run] ./gradlew :app:assembleDebug"
( cd "$PROJECT_ROOT" && ./gradlew :app:assembleDebug )

echo "[ok] Build finished. APK path: app/build/outputs/apk/debug/app-debug.apk"
