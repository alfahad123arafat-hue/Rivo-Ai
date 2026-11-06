package com.rivo.ai.ui.screens.api

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivo.ai.data.local.ApiConfigEntity
import com.rivo.ai.data.local.ProviderType
import com.rivo.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(vm: ApiSettingsViewModel = hiltViewModel(), onBack: () -> Unit) {
    val configs by vm.apiConfigs.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ApiConfigEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("API Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) } }
            )
        },
        containerColor = SoftWhite
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (configs.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(48.dp), tint = PurplePrimary)
                            Spacer(Modifier.height(12.dp))
                            Text("No API configurations", fontWeight = FontWeight.Medium)
                            Text("Add your AI provider to start chatting", color = MutedText, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showAdd = true }, shape = RoundedCornerShape(12.dp)) { Text("Add Configuration") }
                        }
                    }
                }
            }
            items(configs, key = { it.id }) { cfg ->
                ApiConfigCard(
                    cfg = cfg,
                    isActive = cfg.isActive,
                    onSetActive = { vm.setActive(cfg.id) },
                    onTest = { vm.testConnection(cfg) },
                    onFetch = { vm.fetchModels(cfg) },
                    onEdit = { editing = cfg },
                    onDelete = { vm.deleteConfig(cfg.id) }
                )
            }
        }
    }

    if (showAdd) {
        ApiConfigDialog(
            existing = null,
            vm = vm,
            onDismiss = { showAdd = false },
            onSave = { _, _, _, _, _ -> showAdd = false }
        )
    }
    editing?.let { cfg ->
        ApiConfigDialog(
            existing = cfg,
            vm = vm,
            onDismiss = { editing = null },
            onSave = { _, _, _, _, _ -> editing = null }
        )
    }

    message?.let {
        LaunchedEffect(it) { kotlinx.coroutines.delay(3000); vm.clearMessages() }
    }
    error?.let {
        LaunchedEffect(it) { kotlinx.coroutines.delay(4000); vm.clearMessages() }
    }

    // Snackbars via Box
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            message?.let { Snackbar { Text(it) } }
            error?.let { Snackbar(containerColor = ErrorRed) { Text(it) } }
        }
    }
}

@Composable
fun ApiConfigCard(
    cfg: ApiConfigEntity,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onTest: () -> Unit,
    onFetch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().border(if (isActive) 1.5.dp else 0.dp, if (isActive) PurplePrimary else Color.Transparent, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(cfg.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (isActive) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(LavenderSurface).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("Active", fontSize = 10.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("${cfg.providerType} • ${cfg.baseUrl}", color = MutedText, fontSize = 12.sp, maxLines = 1)
                    Text("Model: ${cfg.modelId.ifBlank { "Not set" }}", color = MutedText, fontSize = 12.sp)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = MutedText) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) OutlinedButton(onClick = onSetActive, shape = RoundedCornerShape(10.dp)) { Text("Set Active", fontSize = 12.sp) }
                OutlinedButton(onClick = onTest, shape = RoundedCornerShape(10.dp)) { Text("Test", fontSize = 12.sp) }
                OutlinedButton(onClick = onFetch, shape = RoundedCornerShape(10.dp)) { Text("Fetch Models", fontSize = 12.sp) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigDialog(
    existing: ApiConfigEntity?,
    vm: ApiSettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var provider by remember { mutableStateOf(existing?.providerType ?: ProviderType.OPENAI.name) }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: if (provider == ProviderType.GEMINI.name) "https://generativelanguage.googleapis.com" else "https://api.openai.com") }
    var apiKey by remember { mutableStateOf(existing?.let { vm.getApiKey(it.id) } ?: "") }
    var modelId by remember { mutableStateOf(existing?.modelId ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val models by vm.models.collectAsState()
    val isLoading by vm.isLoadingModels.collectAsState()

    // Update baseUrl when provider changes and not editing
    LaunchedEffect(provider) {
        if (existing == null) {
            baseUrl = when (provider) {
                ProviderType.GEMINI.name -> "https://generativelanguage.googleapis.com"
                ProviderType.OPENAI.name -> "https://api.openai.com"
                else -> baseUrl
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add API Configuration" else "Edit Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("API Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                // Provider dropdown
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(ProviderType.OPENAI.name, ProviderType.GEMINI.name, ProviderType.CUSTOM.name).forEach { p ->
                            DropdownMenuItem(text = { Text(p) }, onClick = { provider = p; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("API Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (existing != null) vm.fetchModels(existing.copy(name = name, providerType = provider, baseUrl = baseUrl, modelId = modelId))
                            else vm.fetchModelsForNew(provider, baseUrl, apiKey)
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        else Text("Fetch Models", fontSize = 12.sp)
                    }
                }

                if (models.isNotEmpty()) {
                    Text("Available models:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, BorderLight, RoundedCornerShape(8.dp)).background(Color.White)) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(models.size) { idx ->
                                val m = models[idx]
                                Row(modifier = Modifier.fillMaxWidth().clickable { modelId = m.id }.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(m.id, fontSize = 12.sp)
                                    if (modelId == m.id) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = PurplePrimary)
                                }
                                Divider()
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = modelId, onValueChange = { modelId = it }, label = { Text("Model ID (or select from fetched)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (existing == null) vm.createConfig(name, provider, baseUrl, apiKey, modelId) { onSave(name, provider, baseUrl, apiKey, modelId) }
                else vm.updateConfig(existing.id, name, provider, baseUrl, if (apiKey.isBlank()) null else apiKey, modelId) { onSave(name, provider, baseUrl, apiKey, modelId) }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
