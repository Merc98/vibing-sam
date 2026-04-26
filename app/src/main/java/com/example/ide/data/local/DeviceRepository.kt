package com.example.ide.data.local

import android.content.Context
import android.content.pm.PackageManager
import com.example.ide.domain.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for accessing device-level information like installed apps
 */
class DeviceRepository(private val context: Context) {
    
    private val packageManager: PackageManager = context.packageManager
    
    /**
     * Get list of installed applications
     * @param includeSystemApps Whether to include system applications
     * @param filter Optional filter by package name or app name
     */
    suspend fun getInstalledApps(
        includeSystemApps: Boolean = false,
        filter: String? = null
    ): Result<List<InstalledApp>> = withContext(Dispatchers.IO) {
        try {
            val flags = if (includeSystemApps) {
                PackageManager.GET_META_DATA
            } else {
                PackageManager.GET_META_DATA
            }
            
            val apps = packageManager.getInstalledApplications(flags)
                .mapNotNull { appInfo ->
                    // Filter out system apps if requested
                    if (!includeSystemApps && isSystemPackage(appInfo.packageName)) {
                        return@mapNotNull null
                    }
                    
                    // Apply text filter if provided
                    if (filter != null) {
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        if (!appName.contains(filter, ignoreCase = true) && 
                            !appInfo.packageName.contains(filter, ignoreCase = true)) {
                            return@mapNotNull null
                        }
                    }
                    
                    try {
                        val appLabel = packageManager.getApplicationLabel(appInfo).toString()
                        val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                        
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = appLabel,
                            versionName = packageInfo.versionName,
                            versionCode = getVersionCode(packageInfo),
                            isDebuggable = isAppDebuggable(appInfo.packageName),
                            apkPath = getApkPath(appInfo.packageName),
                            isEnabled = packageManager.getApplicationEnabledSetting(appInfo.packageName) 
                                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            isSystem = isSystemPackage(appInfo.packageName)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.appName.lowercase() }
            
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get detailed information about a specific package
     */
    suspend fun getPackageInfo(packageName: String): Result<InstalledApp?> = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appLabel = packageManager.getApplicationLabel(appInfo).toString()
            
            val installedApp = InstalledApp(
                packageName = packageName,
                appName = appLabel,
                versionName = packageInfo.versionName,
                versionCode = getVersionCode(packageInfo),
                isDebuggable = isAppDebuggable(packageName),
                apkPath = getApkPath(packageName),
                isEnabled = packageManager.getApplicationEnabledSetting(packageName) 
                    == PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                isSystem = isSystemPackage(packageName)
            )
            
            Result.success(installedApp)
        } catch (e: PackageManager.NameNotFoundException) {
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if an app is debuggable
     */
    private fun isAppDebuggable(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if a package is a system package
     */
    private fun isSystemPackage(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or 
                android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the APK path for a package
     */
    private fun getApkPath(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.sourceDir
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get version code compatible with different Android versions
     */
    @Suppress("DEPRECATION")
    private fun getVersionCode(packageInfo: android.content.pm.PackageInfo): Long {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }
    
    /**
     * Search installed apps by name or package
     */
    suspend fun searchApps(query: String, limit: Int = 50): Result<List<InstalledApp>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext getInstalledApps(includeSystemApps = false)
        }
        
        getInstalledApps(includeSystemApps = false, filter = query)
            .map { it.take(limit) }
    }
}
