package com.rivo.ai.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivo.ai.data.local.ConversationEntity
import com.rivo.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel(), onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val conversations by vm.conversations.collectAsState()
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chat History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (conversations.isNotEmpty()) TextButton(onClick = { confirmClearAll = true }) { Text("Clear All", color = ErrorRed) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = SoftWhite
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Home, null, modifier = Modifier.size(56.dp), tint = MutedText)
                    Spacer(Modifier.height(12.dp))
                    Text("No conversations yet", color = MutedText)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(conversations, key = { it.id }) { conv ->
                    HistoryCard(conv, fmt.format(Date(conv.updatedAt)), onOpen = { onOpen(conv.id) }, onDelete = { confirmDeleteId = conv.id })
                }
            }
        }
    }

    confirmDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete conversation?") },
            text = { Text("This will permanently remove this chat and its messages.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); confirmDeleteId = null }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") } }
        )
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all history?") },
            text = { Text("All conversations will be deleted. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.clearAll(); confirmClearAll = false }) { Text("Clear All", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun HistoryCard(conv: ConversationEntity, dateStr: String, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(conv.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                // Handle missing API reference gracefully — show model or date only
                val subtitle = when {
                    conv.modelId.isNullOrBlank() -> dateStr
                    conv.apiConfigId == null -> "${conv.modelId} • $dateStr"
                    else -> "${conv.modelId} • $dateStr"
                }
                Text(subtitle, color = MutedText, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MutedText, modifier = Modifier.size(18.dp)) }
        }
    }
}
