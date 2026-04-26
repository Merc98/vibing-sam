package com.example.ide.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ide.data.local.DeviceAppRepository
import com.example.ide.data.local.InstalledApp
import com.example.ide.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appRepository = remember { DeviceAppRepository(packageManager) }
    
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var filteredApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        isLoading = true
        installedApps = appRepository.getInstalledApps(includeSystemApps = showSystemApps)
        filteredApps = installedApps
        isLoading = false
    }
    
    LaunchedEffect(searchQuery, showSystemApps, installedApps) {
        filteredApps = if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { 
                it.appName.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    fun reloadApps() {
        coroutineScope.launch {
            isLoading = true
            installedApps = appRepository.getInstalledApps(includeSystemApps = showSystemApps)
            filteredApps = installedApps
            isLoading = false
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search and filter bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showSystemApps,
                            onCheckedChange = { 
                                showSystemApps = it
                                reloadApps()
                            }
                        )
                        Text("Show system apps")
                    }
                    
                    Text(
                        text = "${filteredApps.size} apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No apps found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps) { app ->
                    AppListItem(
                        app = app,
                        onClick = { selectedApp = app },
                        onOpenApk = {
                            app.apkPath?.let { path ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(
                                        Uri.fromFile(java.io.File(path)),
                                        "application/vnd.android.package-archive"
                                    )
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle error
                                }
                            }
                        },
                        onSharePackage = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Package: ${app.packageName}\nName: ${app.appName}\nVersion: ${app.versionName}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share app info"))
                        }
                    )
                }
            }
        }
    }
    
    // App detail dialog
    selectedApp?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text(app.appName) },
            text = {
                Column {
                    Text("Package: ${app.packageName}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version: ${app.versionName ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("System app: ${if (app.isSystemApp) "Yes" else "No"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Use this app's APK path in the VibeCode Chat to analyze or decompile it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.submitChatInput("Analyze this APK: ${app.apkPath}")
                    selectedApp = null
                }) {
                    Text("Analyze in Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedApp = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AppListItem(
    app: InstalledApp,
    onClick: () -> Unit,
    onOpenApk: () -> Unit,
    onSharePackage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("System") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "v${app.versionName ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onSharePackage) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onOpenApk) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Open APK")
                }
            }
        }
    }
}
