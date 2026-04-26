package com.example.ide.puente.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ide.puente.ui.analysis.StaticAnalysisScreen
import com.example.ide.puente.ui.frida.FridaScreen
import com.example.ide.puente.ui.home.HomeScreen
import com.example.ide.puente.ui.info.ApkInfoScreen
import com.example.ide.puente.ui.patch.PatchScreen
import com.example.ide.puente.ui.report.ReportScreen

object Routes {
    const val HOME = "home"
    const val INFO = "info/{targetId}"
    const val ANALYSIS = "analysis/{targetId}"
    const val PATCH = "patch/{targetId}"
    const val FRIDA = "frida/{targetId}"
    const val REPORT = "report/{targetId}"

    fun info(id: String) = "info/$id"
    fun analysis(id: String) = "analysis/$id"
    fun patch(id: String) = "patch/$id"
    fun frida(id: String) = "frida/$id"
    fun report(id: String) = "report/$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuenteApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current

    val titleFor: (String?) -> String = { route ->
        when {
            route == null -> "Puente"
            route.startsWith("home") -> "Puente"
            route.startsWith("info") -> "APK Info"
            route.startsWith("analysis") -> "Static Analysis"
            route.startsWith("patch") -> "Patch & Sign"
            route.startsWith("frida") -> "Frida"
            route.startsWith("report") -> "Report"
            else -> "Puente"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(titleFor(backStackEntry?.destination?.route)) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTarget = { id -> navController.navigate(Routes.info(id)) }
                )
            }
            composable(
                Routes.INFO,
                arguments = listOf(navArgument("targetId") { type = NavType.StringType })
            ) { entry ->
                ApkInfoScreen(
                    targetId = entry.arguments?.getString("targetId").orEmpty(),
                    onOpenAnalysis = { navController.navigate(Routes.analysis(it)) },
                    onOpenPatch = { navController.navigate(Routes.patch(it)) },
                    onOpenFrida = { navController.navigate(Routes.frida(it)) },
                    onOpenReport = { navController.navigate(Routes.report(it)) }
                )
            }
            composable(
                Routes.ANALYSIS,
                arguments = listOf(navArgument("targetId") { type = NavType.StringType })
            ) { entry ->
                StaticAnalysisScreen(targetId = entry.arguments?.getString("targetId").orEmpty())
            }
            composable(
                Routes.PATCH,
                arguments = listOf(navArgument("targetId") { type = NavType.StringType })
            ) { entry ->
                PatchScreen(targetId = entry.arguments?.getString("targetId").orEmpty())
            }
            composable(
                Routes.FRIDA,
                arguments = listOf(navArgument("targetId") { type = NavType.StringType })
            ) { entry ->
                FridaScreen(targetId = entry.arguments?.getString("targetId").orEmpty())
            }
            composable(
                Routes.REPORT,
                arguments = listOf(navArgument("targetId") { type = NavType.StringType })
            ) { entry ->
                ReportScreen(targetId = entry.arguments?.getString("targetId").orEmpty())
            }
        }
    }
}
