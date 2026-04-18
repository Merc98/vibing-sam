package com.example.ide.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Handyman
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ide.ui.viewmodel.MainViewModel

private data class AppDestination(
    val label: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = com.example.ide.di.ViewModelFactory(context)
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val destinations = listOf(
        AppDestination("Projects") { Icon(Icons.Default.Folder, contentDescription = "Projects") },
        AppDestination("Editor") { Icon(Icons.Default.Edit, contentDescription = "Editor") },
        AppDestination("AI Chat") { Icon(Icons.Default.Chat, contentDescription = "AI Chat") },
        AppDestination("Toolkit") { Icon(Icons.Default.Handyman, contentDescription = "Toolkit") },
        AppDestination("Settings") { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(destinations[selectedTab].label) },
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
                3 -> ToolingScreen()
                4 -> SettingsScreen(viewModel)
            }
        }
    }
}
