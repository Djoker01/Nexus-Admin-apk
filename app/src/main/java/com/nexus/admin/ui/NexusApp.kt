package com.nexus.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var toastMessage by remember { mutableStateOf<ToastMessage?>(null) }
    
    LaunchedEffect(Unit) {
        db.notificationDao().getAllNotifications().collect {
            notifications = it
            unreadCount = it.count { n -> !n.read }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                selectedItem = selectedScreen,
                onItemSelected = { screen ->
                    selectedScreen = screen
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                // Top bar
                SmallTopAppBar(
                    title = { Text(selectedScreen.title) },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                // Refresh current screen
                                navController.navigate(selectedScreen.route) {
                                    popUpTo(selectedScreen.route) { inclusive = true }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                        
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text("$unreadCount") }
                                }
                            }
                        ) {
                            IconButton(onClick = { showNotifications = !showNotifications }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Main content
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Dashboard.route) { DashboardScreen() }
                    composable(Screen.Inventory.route) { InventoryScreen() }
                    composable(Screen.Sales.route) { SalesScreen() }
                    composable(Screen.Cash.route) { CashScreen() }
                    composable(Screen.Expenses.route) { ExpensesScreen() }
                    composable(Screen.Receivables.route) { ReceivablesScreen() }
                    composable(Screen.Shrinkage.route) { ShrinkageScreen() }
                    composable(Screen.Restock.route) { RestockScreen() }
                    composable(Screen.Suppliers.route) { SuppliersScreen() }
                    composable(Screen.Reports.route) { ReportsScreen() }
                    composable(Screen.Backup.route) { BackupScreen() }
                }
            }
        }
        
        // Notification panel
        if (showNotifications) {
            NotificationPanel(
                notifications = notifications,
                onNotificationClick = { notification ->
                    scope.launch {
                        db.notificationDao().update(notification.copy(read = true))
                        showNotifications = false
                        if (notification.section.isNotEmpty()) {
                            val screen = Screen.items.find { it.route == notification.section }
                            if (screen != null) {
                                selectedScreen = screen
                                navController.navigate(screen.route)
                            }
                        }
                    }
                },
                onMarkAsRead = { notification ->
                    scope.launch {
                        db.notificationDao().update(notification.copy(read = true))
                    }
                },
                onDelete = { notification ->
                    scope.launch {
                        db.notificationDao().delete(notification)
                    }
                },
                onMarkAllRead = {
                    scope.launch {
                        db.notificationDao().markAllAsRead()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
            )
        }
        
        // Toast messages
        ToastHost(
            toastState = remember { mutableStateOf(toastMessage) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}