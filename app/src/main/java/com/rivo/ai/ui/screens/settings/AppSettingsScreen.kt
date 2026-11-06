package com.rivo.ai.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivo.ai.ui.theme.MutedText
import com.rivo.ai.ui.theme.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(vm: AppSettingsViewModel = hiltViewModel(), onBack: () -> Unit) {
    val isDark by vm.isDark.collectAsState()
    val message by vm.message.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { snackbarHost.showSnackbar(it) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("App Settings", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = SoftWhite
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dark Theme", fontSize = 14.sp)
                        Switch(checked = isDark, onCheckedChange = { vm.setDark(it) })
                    }
                    Text("Light theme is default as per design", color = MutedText, fontSize = 12.sp)
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Chat History", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Manage your local conversation storage", color = MutedText, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { confirmClear = true }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE5486A))) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Chat History")
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, tint = MutedText)
                        Text("About", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Rivo AI • Premium AI Chat", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Version 1.0.0 (Build 1)", color = MutedText, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Built with Kotlin, Jetpack Compose, Room & Retrofit. Secure API key storage via Android Keystore.", color = MutedText, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all chat history?") },
            text = { Text("All local conversations will be deleted. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear", color = Color(0xFFE5486A)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
