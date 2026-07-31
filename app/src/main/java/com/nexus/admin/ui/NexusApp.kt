package com.nexus.admin.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.widget.Toast
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentBusiness by remember { mutableStateOf<Business?>(null) }
    var showBusinessSetup by remember { mutableStateOf(true) }
    var showUserManagement by remember { mutableStateOf(false) }

    // ========== PANTALLA 1: LOGIN ==========
    if (currentUser == null) {
        if (showUserManagement) {
            UserManagementScreen(db = db, onBack = { showUserManagement = false })
        } else {
            LoginScreen(
                db = db,
                onLoginSuccess = { currentUser = it },
                onFirstTimeSetup = { showUserManagement = true }
            )
        }
        return
    }

    // ========== PANTALLA 2: SELECCIÓN DE NEGOCIO ==========
    if (showBusinessSetup) {
        BusinessSetupScreen(
            db = db,
            onBusinessSelected = { business ->
                currentBusiness = business
                showBusinessSetup = false
            },
            onBack = { showBusinessSetup = false }
        )
        return
    }

    // ========== PANTALLA 3: APP PRINCIPAL ==========
    val isAdmin = currentUser?.role == "admin"
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showNotifications by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    // Sync manager
    val syncManager = remember { OfflineSyncManager(context, db) }
    var showSyncExportQr by remember { mutableStateOf(false) }
    var showSyncImportScanner by remember { mutableStateOf(false) }
    var syncQrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

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
            // Sidebar con filtro por rol
            Sidebar(
                selectedItem = selectedScreen,
                onItemSelected = { navigateTo(it) },
                isExpanded = isSidebarExpanded,
                onToggle = { isSidebarExpanded = !isSidebarExpanded },
                onClose = { isSidebarExpanded = false },
                isAdmin = isAdmin
            )

            Column(modifier = Modifier.weight(1f)) {
                SmallTopAppBar(
                    title = { Text(selectedScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarExpanded = !isSidebarExpanded }) {
                            Icon(Icons.Filled.Menu, if (isSidebarExpanded) "Ocultar" else "Mostrar")
                        }
                    },
                    actions = {
                        // Botón Sync (Admin y Trabajador)
                        IconButton(onClick = {
                            scope.launch {
                                if (isAdmin) {
                                    showSyncImportScanner = true
                                } else {
                                    syncQrBitmap = null
                                    val data = syncManager.exportSalesToQr(currentUser!!.name, currentBusiness!!.code)
                                    if (data.isNotEmpty()) {
                                        syncQrBitmap = QrCodeGenerator.generateQrCode(data)
                                        showSyncExportQr = true
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Sync, "Sincronizar")
                        }

                        // Notificaciones
                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            IconButton(onClick = { showNotifications = !showNotifications }) {
                                Icon(Icons.Filled.Notifications, "Notificaciones")
                            }
                        }

                        // Usuario actual
                        Text(currentUser?.name?.firstOrNull()?.toString() ?: "?", modifier = Modifier.padding(8.dp))
                        
                        // Cerrar sesión
                        IconButton(onClick = {
                            currentUser = null
                            currentBusiness = null
                            showBusinessSetup = true
                        }) {
                            Icon(Icons.Filled.Logout, "Salir")
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                Box(
                    modifier = Modifier.fillMaxSize().clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { closeAllToggles() }
                ) {
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
                }
            }
        }

        // Panel de notificaciones
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
                    }
                },
                onMarkAsRead = { n -> scope.launch { db.notificationDao().update(n.copy(read = true)) } },
                onDelete = { n -> scope.launch { db.notificationDao().delete(n) } },
                onMarkAllRead = { scope.launch { db.notificationDao().markAllAsRead() } }
            )
        }

        // Diálogo: Exportar QR (Trabajador)
        if (showSyncExportQr && syncQrBitmap != null) {
            AlertDialog(
                onDismissRequest = { showSyncExportQr = false },
                title = { Text("QR de Ventas del Día") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Muestra este QR al administrador para sincronizar")
                        Spacer(Modifier.height(16.dp))
                        Image(
                            bitmap = syncQrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Sync",
                            modifier = Modifier.size(280.dp)
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { showSyncExportQr = false }) { Text("Cerrar") } }
            )
        }

        // Escáner: Importar QR (Admin)
        if (showSyncImportScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { data ->
                    scope.launch {
                        val result = syncManager.importSalesFromQr(data)
                        if (result.success) {
                            showSyncImportScanner = false
                            Toast.makeText(context, "✅ ${result.salesImported} ventas de ${result.workerName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showSyncImportScanner = false }
            )
        }
    }
}
