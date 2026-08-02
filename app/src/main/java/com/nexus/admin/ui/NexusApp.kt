package com.nexus.admin.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.AppNotification
import com.nexus.admin.data.entity.Business
import com.nexus.admin.data.entity.User
import com.nexus.admin.data.sync.OfflineSyncManager
import com.nexus.admin.ui.components.*
import com.nexus.admin.ui.navigation.Screen
import com.nexus.admin.ui.screens.*
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp() {
    val context = LocalContext.current
    val db = remember {
        try {
            AppDatabase.getDatabase(context)
        } catch (e: Exception) {
            Log.e("NexusApp", "Error getting database: ${e.message}", e)
            Toast.makeText(context, "Error de base de datos: ${e.message}", Toast.LENGTH_LONG).show()
            AppDatabase.getDatabase(context) // Reintentar
        }
    }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentBusiness by remember { mutableStateOf<Business?>(null) }
    var showBusinessSetup by remember { mutableStateOf(true) }
    var showUserManagement by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // ========== PANTALLA 1: LOGIN ==========
    if (currentUser == null) {
        if (showUserManagement) {
            try {
                UserManagementScreen(db = db, onBack = { showUserManagement = false })
            } catch (e: Exception) {
                Log.e("NexusApp", "Error in UserManagement: ${e.message}", e)
                showUserManagement = false
            }
        } else {
            try {
                LoginScreen(
                    db = db,
                    onLoginSuccess = { currentUser = it },
                    onFirstTimeSetup = { showUserManagement = true }
                )
            } catch (e: Exception) {
                Log.e("NexusApp", "Error in Login: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        return
    }

    // ========== PANTALLA 2: SELECCIÓN DE NEGOCIO ==========
    if (showBusinessSetup) {
        try {
            BusinessSetupScreen(
                db = db,
                onBusinessSelected = { business ->
                    currentBusiness = business
                    showBusinessSetup = false
                },
                onBack = { showBusinessSetup = false }
            )
        } catch (e: Exception) {
            Log.e("NexusApp", "Error in BusinessSetup: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            showBusinessSetup = false
        }
        return
    }

    // ========== PANTALLA 3: APP PRINCIPAL ==========
    val isAdmin = currentUser?.role == "admin"
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showNotifications by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    val syncManager = remember {
        try {
            OfflineSyncManager(context, db)
        } catch (e: Exception) {
            Log.e("NexusApp", "Error creating SyncManager: ${e.message}", e)
            OfflineSyncManager(context, db)
        }
    }
    var showSyncExportQr by remember { mutableStateOf(false) }
    var showSyncImportScanner by remember { mutableStateOf(false) }
    var syncQrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(Unit) {
        try {
            db.notificationDao().getAllNotifications().collect {
                notifications = it
                unreadCount = it.count { n -> !n.read }
            }
        } catch (e: Exception) {
            Log.e("NexusApp", "Error loading notifications: ${e.message}", e)
        }
    }

    fun navigateTo(screen: Screen) {
        try {
            selectedScreen = screen
            navController.navigate(screen.route) {
                popUpTo(Screen.Dashboard.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } catch (e: Exception) {
            Log.e("NexusApp", "Error navigating: ${e.message}", e)
        }
    }

    fun closeAllToggles() {
        isSidebarExpanded = false
        showNotifications = false
    }

    LaunchedEffect(currentUser) {
        try {
            val prefs = context.getSharedPreferences("nexus_prefs", android.content.Context.MODE_PRIVATE)
            val hasSeenHelp = prefs.getBoolean("has_seen_help", false)
            if (!hasSeenHelp) {
                showHelp = true
                prefs.edit().putBoolean("has_seen_help", true).apply()
            }
        } catch (e: Exception) {
            Log.e("NexusApp", "Error checking help: ${e.message}", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            try {
                Sidebar(
                    selectedItem = selectedScreen,
                    onItemSelected = { navigateTo(it) },
                    isExpanded = isSidebarExpanded,
                    onToggle = { isSidebarExpanded = !isSidebarExpanded },
                    onClose = { isSidebarExpanded = false },
                    isAdmin = isAdmin
                )
            } catch (e: Exception) {
                Log.e("NexusApp", "Error in Sidebar: ${e.message}", e)
            }

            Column(modifier = Modifier.weight(1f)) {
                SmallTopAppBar(
                    title = { Text(selectedScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarExpanded = !isSidebarExpanded }) {
                            Icon(Icons.Filled.Menu, if (isSidebarExpanded) "Ocultar" else "Mostrar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.Filled.HelpOutline, "Ayuda")
                        }

                        IconButton(onClick = {
                            if (isAdmin) {
                                showSyncImportScanner = true
                            } else {
                                scope.launch {
                                    try {
                                        showSyncExportQr = false
                                        syncQrBitmap = null
                                        withContext(Dispatchers.IO) {
                                            val data = syncManager.exportSalesToQr(
                                                currentUser!!.name,
                                                currentBusiness!!.code
                                            )
                                            if (data.isNotEmpty()) {
                                                syncQrBitmap = QrCodeGenerator.generateQrCode(data)
                                                withContext(Dispatchers.Main) { showSyncExportQr = true }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "⚠️ No hay ventas para exportar", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NexusApp", "Error exporting QR: ${e.message}", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Sync, "Sincronizar")
                        }

                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            IconButton(onClick = { showNotifications = !showNotifications }) {
                                Icon(Icons.Filled.Notifications, "Notificaciones")
                            }
                        }

                        Text(currentUser?.name?.firstOrNull()?.toString() ?: "?", modifier = Modifier.padding(8.dp))
                        IconButton(onClick = { showBusinessSetup = true }) { Icon(Icons.Filled.Store, "Cambiar negocio") }
                        IconButton(onClick = {
                            currentUser = null
                            currentBusiness = null
                            showBusinessSetup = true
                        }) { Icon(Icons.Filled.Logout, "Salir") }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                Box(
                    modifier = Modifier.fillMaxSize().clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { closeAllToggles() }
                ) {
                    try {
                        NavHost(navController, startDestination = Screen.Dashboard.route, modifier = Modifier.fillMaxSize()) {
                            composable(Screen.Dashboard.route) { DashboardScreen(isAdmin = isAdmin) }
                            composable(Screen.Inventory.route) { InventoryScreen(isAdmin = isAdmin) }
                            composable(Screen.Sales.route) { SalesScreen() }
                            composable(Screen.Cash.route) { if (isAdmin) CashScreen() }
                            composable(Screen.Expenses.route) { if (isAdmin) ExpensesScreen() }
                            composable(Screen.Receivables.route) { if (isAdmin) ReceivablesScreen() }
                            composable(Screen.Shrinkage.route) { if (isAdmin) ShrinkageScreen() }
                            composable(Screen.Reports.route) { if (isAdmin) ReportsScreen() }
                            composable(Screen.Backup.route) { if (isAdmin) BackupScreen() }
                        }
                    } catch (e: Exception) {
                        Log.e("NexusApp", "Error in NavHost: ${e.message}", e)
                        Text("Error cargando pantalla: ${e.message}", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

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
                        try {
                            db.notificationDao().update(n.copy(read = true))
                            showNotifications = false
                        } catch (e: Exception) {
                            Log.e("NexusApp", "Error marking notification: ${e.message}", e)
                        }
                    }
                },
                onMarkAsRead = { n ->
                    scope.launch {
                        try { db.notificationDao().update(n.copy(read = true)) } catch (e: Exception) {}
                    }
                },
                onDelete = { n ->
                    scope.launch {
                        try { db.notificationDao().delete(n) } catch (e: Exception) {}
                    }
                },
                onMarkAllRead = {
                    scope.launch {
                        try { db.notificationDao().markAllAsRead() } catch (e: Exception) {}
                    }
                }
            )
        }

        if (showSyncExportQr && syncQrBitmap != null) {
            AlertDialog(
                onDismissRequest = { showSyncExportQr = false },
                title = { Text("QR de Ventas del Día") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Muestra este QR al administrador")
                        Spacer(Modifier.height(16.dp))
                        Image(bitmap = syncQrBitmap!!.asImageBitmap(), contentDescription = "QR Sync", modifier = Modifier.size(280.dp))
                    }
                },
                confirmButton = { TextButton(onClick = { showSyncExportQr = false }) { Text("Cerrar") } }
            )
        }

        if (showSyncImportScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { data ->
                    scope.launch {
                        try {
                            val result = syncManager.importSalesFromQr(data)
                            if (result.success) {
                                showSyncImportScanner = false
                                Toast.makeText(context, "✅ ${result.salesImported} ventas de ${result.workerName}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("NexusApp", "Error importing QR: ${e.message}", e)
                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showSyncImportScanner = false }
            )
        }

        if (showHelp) {
            try {
                HelpScreen(onDismiss = { showHelp = false })
            } catch (e: Exception) {
                Log.e("NexusApp", "Error in Help: ${e.message}", e)
                showHelp = false
            }
        }
    }
}
