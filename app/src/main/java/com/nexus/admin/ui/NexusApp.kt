package com.nexus.admin.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.AppNotification
import com.nexus.admin.ui.components.*
import com.nexus.admin.ui.navigation.Screen
import com.nexus.admin.ui.screens.*
import com.nexus.admin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showNotifications by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.notificationDao().getAllNotifications().collect {
            notifications = it
            unreadCount = it.count { n -> !n.read }
        }
    }

    fun navigateTo(screen: Screen) {
        selectedScreen = screen
        navController.navigate(screen.route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun closeAllToggles() {
        isSidebarExpanded = false
        showNotifications = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Sidebar(
                selectedItem = selectedScreen,
                onItemSelected = { navigateTo(it) },
                isExpanded = isSidebarExpanded,
                onToggle = { isSidebarExpanded = !isSidebarExpanded },
                onClose = { isSidebarExpanded = false }
            )

            // Main content
            Column(modifier = Modifier.weight(1f)) {
                SmallTopAppBar(
                    title = { Text(selectedScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarExpanded = !isSidebarExpanded }) {
                            Icon(Icons.Filled.Menu, if (isSidebarExpanded) "Ocultar" else "Mostrar")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch { navController.navigate(selectedScreen.route) { popUpTo(selectedScreen.route) { inclusive = true } } }
                        }) {
                            Icon(Icons.Filled.Refresh, "Actualizar")
                        }
                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            IconButton(onClick = { showNotifications = !showNotifications }) {
                                Icon(Icons.Filled.Notifications, "Notificaciones")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Click outside to close toggles
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { closeAllToggles() }
                ) {
                    NavHost(navController, startDestination = Screen.Dashboard.route, modifier = Modifier.fillMaxSize()) {
                        composable(Screen.Dashboard.route) { DashboardScreen() }
                        composable(Screen.Inventory.route) { InventoryScreen() }
                        composable(Screen.Sales.route) { SalesScreen() }
                        composable(Screen.Cash.route) { CashScreen() }
                        composable(Screen.Expenses.route) { ExpensesScreen() }
                        composable(Screen.Receivables.route) { ReceivablesScreen() }
                        composable(Screen.Shrinkage.route) { ShrinkageScreen() }
                        composable(Screen.Restock.route) { RestockScreen() }
                        composable(Screen.Reports.route) { ReportsScreen() }
                        composable(Screen.Backup.route) { BackupScreen() }
                    }
                }
            }
        }

        // Notification panel
        AnimatedVisibility(
            visible = showNotifications,
            enter = fadeIn() + slideInVertically { -20 },
            exit = fadeOut() + slideOutVertically { -20 },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 16.dp)
        ) {
            NotificationPanel(
                notifications = notifications,
                onNotificationClick = { n ->
                    scope.launch {
                        db.notificationDao().update(n.copy(read = true))
                        showNotifications = false
                        if (n.section.isNotEmpty()) {
                            Screen.items.find { it.route == n.section }?.let { navigateTo(it) }
                        }
                    }
                },
                onMarkAsRead = { n -> scope.launch { db.notificationDao().update(n.copy(read = true)) } },
                onDelete = { n -> scope.launch { db.notificationDao().delete(n) } },
                onMarkAllRead = { scope.launch { db.notificationDao().markAllAsRead() } }
            )
        }
    }
}
