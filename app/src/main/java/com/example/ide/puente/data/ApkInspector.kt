package com.example.ide.puente.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.util.zip.ZipFile

object ApkInspector {

    data class Info(
        val packageName: String,
        val versionName: String?,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int,
        val permissions: List<String>,
        val activities: List<String>,
        val services: List<String>,
        val appType: String,
        val abis: List<String>
    )

    @Suppress("DEPRECATION")
    fun inspect(context: Context, apkPath: String): Info? {
        val file = File(apkPath)
        if (!file.exists()) return null

        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES

        val pm = context.packageManager
        val pkg: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getPackageArchiveInfo(apkPath, flags)
        } ?: return null

        // Ensure paths resolve when PackageManager lazy-loads them.
        pkg.applicationInfo?.apply {
            sourceDir = apkPath
            publicSourceDir = apkPath
        }

        val appInfo = pkg.applicationInfo
        val abis = mutableSetOf<String>()
        val hasFlutter: Boolean
        val hasRn: Boolean
        ZipFile(file).use { zip ->
            val names = zip.entries().toList().map { it.name }
            names.forEach { name ->
                if (name.startsWith("lib/")) {
                    val parts = name.split('/')
                    if (parts.size >= 3) abis.add(parts[1])
                }
            }
            hasFlutter = names.any { it.endsWith("libflutter.so") }
            hasRn = names.any { it.endsWith("libreactnativejni.so") || it.endsWith("libreactnative.so") }
        }

        val appType = when {
            hasFlutter -> "Flutter"
            hasRn -> "React Native"
            abis.isNotEmpty() -> "Native"
            else -> "Standard"
        }

        return Info(
            packageName = pkg.packageName ?: "<unknown>",
            versionName = pkg.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode
            else @Suppress("DEPRECATION") pkg.versionCode.toLong(),
            minSdk = appInfo?.minSdkVersion ?: -1,
            targetSdk = appInfo?.targetSdkVersion ?: -1,
            permissions = pkg.requestedPermissions?.toList().orEmpty(),
            activities = pkg.activities?.map { it.name }.orEmpty(),
            services = pkg.services?.map { it.name }.orEmpty(),
            appType = appType,
            abis = abis.sorted()
        )
    }
}
