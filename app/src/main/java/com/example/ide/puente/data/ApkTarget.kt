package com.example.ide.puente.data

data class ApkTarget(
    val id: String,                 // sha256 of source file
    val displayName: String,
    val sourceUri: String,          // original content:// URI as string (for reference)
    val workspacePath: String,      // absolute path inside filesDir/workspace/<sha256>
    val apkPath: String,            // absolute path to copied .apk inside workspace
    val sizeBytes: Long,
    val importedAt: Long = System.currentTimeMillis(),
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
    val appType: String? = null     // Flutter / ReactNative / Native / Standard
)
