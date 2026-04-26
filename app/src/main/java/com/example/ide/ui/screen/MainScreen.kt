package com.example.ide.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ide.ui.viewmodel.MainViewModel

private data class AppDestination(
    val label: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel(
        factory = com.example.ide.di.ViewModelFactory()
    )
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val currentFile by viewModel.currentFile.collectAsStateWithLifecycle()
    val isG4FAuthenticated by viewModel.isG4FAuthenticated.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(2) } // Start at AI Chat tab (index 2)
    val destinations = listOf(
        AppDestination("Projects") { Icon(Icons.Default.Folder, contentDescription = "Projects") },
        AppDestination("Editor") { Icon(Icons.Default.Edit, contentDescription = "Editor") },
        AppDestination("VibeCode Chat") { Icon(Icons.Default.Chat, contentDescription = "VibeCode Chat") },
        AppDestination("Installed Apps") { Icon(Icons.Default.Inventory, contentDescription = "Installed Apps") },
        AppDestination("Settings") { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )

    val subtitle = when (selectedTab) {
        1 -> currentFile?.let { "${it.name}.${it.extension}" } ?: currentProject?.name
        3 -> currentProject?.name
        else -> null
    }

    // Mostrar pantalla de login si no está autenticado
    if (!isG4FAuthenticated) {
        LoginScreen(onLoginComplete = {
            // Login completado, continuar con la app normal
        })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text(destinations[selectedTab].label)
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = destination.icon,
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ProjectsScreen(viewModel)
                1 -> EditorScreen(viewModel)
                2 -> ChatScreen(viewModel)
                3 -> InstalledAppsScreen(viewModel)
                4 -> SettingsScreen(viewModel)
            }
        }
    }
}
