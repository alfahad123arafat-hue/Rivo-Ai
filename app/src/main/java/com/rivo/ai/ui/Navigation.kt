package com.rivo.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rivo.ai.ui.screens.api.ApiSettingsScreen
import com.rivo.ai.ui.screens.history.HistoryScreen
import com.rivo.ai.ui.screens.home.HomeScreen
import com.rivo.ai.ui.screens.home.HomeViewModel
import com.rivo.ai.ui.screens.prompts.PromptsScreen
import com.rivo.ai.ui.screens.settings.AppSettingsScreen
import com.rivo.ai.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "New Chat", Icons.Default.Home)
    object History : Screen("history", "Chat History", Icons.Filled.Home)
    object Api : Screen("api", "API Settings", Icons.Default.Settings)
    object Prompts : Screen("prompts", "System Prompts", Icons.Default.Star)
    object Settings : Screen("settings", "App Settings", Icons.Default.Settings)
}

@Composable
fun AppNavHost(homeViewModel: HomeViewModel) {
    val nav = rememberNavController()
    var showDrawer by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(if (showDrawer) DrawerValue.Open else DrawerValue.Closed),
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = SoftWhite) {
                DrawerContent(
                    onNavigate = { route ->
                        showDrawer = false
                        nav.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    onNewChat = {
                        showDrawer = false
                        homeViewModel.newChat()
                        nav.navigate(Screen.Home.route)
                    }
                )
            }
        },
        gesturesEnabled = showDrawer
    ) {
        NavHost(navController = nav, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                HomeScreen(
                    vm = homeViewModel,
                    onMenuClick = { showDrawer = true },
                    onModelClick = { nav.navigate(Screen.Api.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(onBack = { nav.popBackStack() }, onOpen = { id ->
                    homeViewModel.selectConversation(id)
                    nav.navigate(Screen.Home.route)
                })
            }
            composable(Screen.Api.route) {
                ApiSettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(Screen.Prompts.route) {
                PromptsScreen(onBack = { nav.popBackStack() })
            }
            composable(Screen.Settings.route) {
                AppSettingsScreen(onBack = { nav.popBackStack() })
            }
        }
    }

    // Sync drawer state
    LaunchedEffect(showDrawer) {
        // handled via rememberDrawerState initial, but we need to open/close via side effect
    }
}

@Composable
fun DrawerContent(onNavigate: (String) -> Unit, onNewChat: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(PurplePrimary), contentAlignment = Alignment.Center) {
                Text("R", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Column {
                Text("Rivo AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Premium Chat", color = MutedText, fontSize = 12.sp)
            }
        }
        Divider()
        Spacer(Modifier.height(12.dp))

        DrawerItem(Screen.Home.label, Screen.Home.icon, true) { onNewChat() }
        DrawerItem(Screen.History.label, Screen.History.icon) { onNavigate(Screen.History.route) }
        DrawerItem(Screen.Api.label, Screen.Api.icon) { onNavigate(Screen.Api.route) }
        DrawerItem(Screen.Prompts.label, Screen.Prompts.icon) { onNavigate(Screen.Prompts.route) }
        DrawerItem(Screen.Settings.label, Screen.Settings.icon) { onNavigate(Screen.Settings.route) }

        Spacer(Modifier.weight(1f))
        Text("v1.0.0 • Secure & Private", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, highlighted: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) LavenderSurface else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = if (highlighted) PurplePrimary else MutedText, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 14.sp, fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal, color = if (highlighted) DeepText else MutedText)
    }
}
