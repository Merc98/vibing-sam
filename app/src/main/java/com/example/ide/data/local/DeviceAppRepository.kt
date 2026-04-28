package com.example.ide.data.local

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long?,
    val isSystemApp: Boolean,
    val apkPath: String?,
    val icon: Drawable? = null
)

class DeviceAppRepository(
    private val packageManager: PackageManager
) {
    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val apps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        apps.mapNotNull { pkg ->
            try {
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                
                if (!includeSystemApps && isSystem) return@mapNotNull null
                
                InstalledApp(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    versionName = pkg.versionName,
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pkg.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode.toLong()
                    },
                    isSystemApp = isSystem,
                    apkPath = appInfo.sourceDir
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }
    
    suspend fun searchApps(query: String, includeSystemApps: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        getInstalledApps(includeSystemApps).filter {
            it.appName.contains(query, ignoreCase = true) || 
            it.packageName.contains(query, ignoreCase = true)
        }
    }
    
    suspend fun getAppByPackageName(packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        try {
            val pkg = packageManager.getPackageInfo(packageName, 0)
            val appInfo = pkg.applicationInfo ?: return@withContext null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            InstalledApp(
                packageName = pkg.packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                versionName = pkg.versionName,
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                },
                isSystemApp = isSystem,
                apkPath = appInfo.sourceDir
            )
        } catch (e: Exception) {
            null
        }
    }
}
