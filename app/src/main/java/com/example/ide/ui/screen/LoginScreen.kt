package com.example.ide.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ide.di.ViewModelFactory
import com.example.ide.ui.viewmodel.MainViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    onLoginComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: MainViewModel = viewModel()
    
    var isLoading by remember { mutableStateOf(true) }
    var loginStatus by remember { mutableStateOf("Iniciando sesión automáticamente...") }
    var showWebView by remember { mutableStateOf(false) }
    var webViewUrl by remember { mutableStateOf("") }
    
    // G4F.dev auto-login - simplified for now
    LaunchedEffect(Unit) {
        viewModel.setG4FAuthenticated(true)
        onLoginComplete()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Color(0xFF0D1117) // Fondo oscuro estilo Replit
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo/Icono
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF00E5A1) // Verde Replit
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = Color(0xFF0D1117),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            
            // Título
            Text(
                text = "VibeCode AI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Tu IDE móvil con IA integrada",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Indicador de carga
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5A1),
                    strokeWidth = 3.dp
                )
                
                Text(
                    text = loginStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00E5A1),
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Tarjeta de estado exitoso
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C2128)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E5A1),
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "¡Listo para comenzar!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "G4F Free Models: Autenticado\nHuggingFace: Listo para usar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onLoginComplete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5A1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Ir al Chat",
                                color = Color(0xFF0D1117),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Información adicional
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C2128).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Filled.AutoAwesome,
                        title = "G4F Free Models",
                        description = "IA gratuita con autenticación automática"
                    )
                    
                    FeatureItem(
                        icon = Icons.Filled.Download,
                        title = "HuggingFace",
                        description = "Descarga modelos y ejecuta localmente"
                    )
                    
                    FeatureItem(
                        icon = Icons.Filled.Code,
                        title = "Totalmente Offline",
                        description = "Funciona sin conexión después de descargar"
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00E5A1),
            modifier = Modifier.size(24.dp)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
