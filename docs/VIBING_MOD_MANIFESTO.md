# Vibing MOD Manifesto

## Product shift

This project is no longer just **Vibing Code**.

The core product direction is **Vibing MOD**: an AI-assisted Android modification workspace focused on already-created applications.

Instead of starting from blank code, the user starts from an existing APK or an installed Android app and asks for precise, functional modifications.

## Core promise

> Select an installed app or APK file, describe the desired modification in natural language, preview the exact plan, apply verified patches, rebuild the APK, and continue editing manually if needed.

## Android-only scope

Vibing MOD is designed for Android phones, especially Samsung/Android devices.

It is not an iPhone/iOS product. iOS app modification has a completely different signing, sandboxing, and distribution model and is out of scope.

## Primary user flow

1. User selects an installed Android app or APK file.
2. App imports the APK into a safe workspace.
3. APKTool decodes resources, manifest, and smali.
4. JADX extracts readable source context when possible.
5. The AI receives a constrained context and generates a structured JSON plan.
6. The app validates the plan before applying it.
7. The app shows preview: files, operations, risk, and expected result.
8. The patch engine modifies resources/XML/smali only through allowed operations.
9. APKTool rebuilds the modified APK.
10. Signing/export happens through Android-compatible signing logic.
11. The user can inspect and adjust files through a Replit-style mobile workspace.

## Product pillars

### 1. Modify, do not hallucinate

The AI must not simply answer with text. It must propose concrete, verifiable operations.

Bad:

```text
Here is how you could change the chat color...
```

Good:

```json
{
  "summary": "Change chat bubble color to blue",
  "targetFiles": ["res/values/colors.xml", "res/layout/chat_item.xml"],
  "operations": [
    {
      "type": "update_resource_value",
      "path": "res/values/colors.xml",
      "key": "chat_bubble_color",
      "value": "#2196F3"
    }
  ]
}
```

### 2. Preview before patch

Every AI-generated change must be inspectable before it is applied.

The user should see:

- summary
- target files
- operation count
- risk level
- whether smali is required

### 3. Verified operations only

The patch system should prefer safe operations:

- update resource values
- update XML attributes
- replace known text with known text
- add resource/assets files

Smali modification is allowed only when clearly required and should be treated as higher risk.

### 4. Replit-style workspace for Android

The workspace should feel like a compact Android-native Replit:

- Files
- Editor
- Chat
- Terminal
- APK tools
- Run/Rebuild action

The terminal is not a full Linux terminal. It is an Android sandbox terminal limited to safe workspace commands.

### 5. Safety boundaries

Vibing MOD is for legitimate modification workflows:

- apps owned by the user
- test APKs
- internal apps
- accessibility/UI customization
- learning/research on permitted apps
- benign resource and layout changes

The system should refuse or block flows aimed at:

- malware
- credential theft
- spyware
- payment bypass
- account abuse
- stealth/persistence
- data exfiltration
- unauthorized access

## Naming guidance

Use **Vibing MOD** in user-facing copy where the feature is about APK/app modification.

Use **VibeCode** only for the coding/editor side.

Suggested labels:

- Vibing MOD Chat
- MOD Workspace
- APK Mod Planner
- Rebuild MOD APK
- Preview Patch
- Apply MOD

## Architecture direction

```text
ReplitWorkspaceScreen
  -> MainViewModel
  -> ApkImportService
  -> ApkContextOrchestrator
  -> LlmTransformationPlanner
  -> ApkPlanValidator
  -> PatchApplier
  -> ApkTransformationOrchestrator
  -> APK build/sign/export
```

## Success criteria

A successful Vibing MOD flow means:

1. The user asks for a specific change to an existing app.
2. The system identifies the right files.
3. The AI returns a structured plan.
4. The plan is validated.
5. The user sees the preview.
6. The app applies the patch.
7. The APK rebuilds.
8. The user can inspect/edit the result manually.

## Current implementation priorities

1. Complete `MainViewModel` integration for terminal/APK actions.
2. Add signed APK export using Android-compatible signing.
3. Improve AI plan retry/repair for invalid JSON.
4. Add file tree browsing for decoded APK workspaces.
5. Add user-facing copy that says Vibing MOD instead of generic Vibing Code when modifying APKs.
