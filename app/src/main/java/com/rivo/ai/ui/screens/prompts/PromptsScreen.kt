package com.rivo.ai.ui.screens.prompts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivo.ai.data.local.SystemPromptEntity
import com.rivo.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsScreen(vm: PromptsViewModel = hiltViewModel(), onBack: () -> Unit) {
    val prompts by vm.prompts.collectAsState()
    val msg by vm.msg.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SystemPromptEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("System Prompts", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) } }
            )
        },
        containerColor = SoftWhite
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (prompts.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(48.dp), tint = PurplePrimary)
                            Text("No prompts yet", fontWeight = FontWeight.Medium)
                            Text("Create prompts to guide AI behavior", color = MutedText, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { showAdd = true }) { Text("Create Prompt") }
                        }
                    }
                }
            }
            items(prompts, key = { it.id }) { p ->
                PromptCard(
                    p = p,
                    onSetActive = { vm.setActive(p.id) },
                    onEdit = { editing = p },
                    onDelete = { vm.delete(p.id) },
                    onToggle = { vm.toggleEnabled(p.id, it) }
                )
            }
        }
    }

    if (showAdd) PromptDialog(null, vm, { showAdd = false }, { showAdd = false })
    editing?.let { PromptDialog(it, vm, { editing = null }, { editing = null }) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        msg?.let {
            LaunchedEffect(it) { kotlinx.coroutines.delay(2500); vm.clear() }
            Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
        }
    }
}

@Composable
fun PromptCard(p: SystemPromptEntity, onSetActive: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (p.isActive) {
                        Surface(color = LavenderSurface, shape = RoundedCornerShape(8.dp)) { Text("Active", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = PurplePrimary, fontWeight = FontWeight.Bold) }
                    }
                }
                Switch(checked = p.isEnabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(6.dp))
            Text(p.promptText, color = MutedText, fontSize = 13.sp, maxLines = 3)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!p.isActive) OutlinedButton(onClick = onSetActive, shape = RoundedCornerShape(10.dp)) { Text("Set Active", fontSize = 12.sp) }
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(10.dp)) { Text("Edit", fontSize = 12.sp) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptDialog(existing: SystemPromptEntity?, vm: PromptsViewModel, onDismiss: () -> Unit, onSave: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var text by remember { mutableStateOf(existing?.promptText ?: "") }
    val apiConfigs by vm.apiConfigs.collectAsState()
    var selectedApi by remember { mutableStateOf(existing?.apiConfigId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New System Prompt" else "Edit Prompt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Prompt Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("System Prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 6)
                // API association optional
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedApi?.let { id -> apiConfigs.find { it.id == id }?.name } ?: "Global (all APIs)",
                        onValueChange = {}, readOnly = true, label = { Text("Link to API (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Global") }, onClick = { selectedApi = null; expanded = false })
                        apiConfigs.forEach { cfg ->
                            DropdownMenuItem(text = { Text(cfg.name) }, onClick = { selectedApi = cfg.id; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (existing == null) vm.create(name, text, selectedApi) else vm.update(existing.id, name, text)
                onSave()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
