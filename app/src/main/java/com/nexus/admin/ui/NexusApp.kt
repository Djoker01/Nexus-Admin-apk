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
import androidx.compose.ui.zIndex
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
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentBusiness by remember { mutableStateOf<Business?>(null) }
    var showBusinessSetup by remember { mutableStateOf(true) }
    var showUserManagement by remember { mutableStateOf(false) }
    var pendingWorker by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }

    // ========== SPLASH SCREEN (2.5 segundos) ==========
    LaunchedEffect(Unit) {
        if (showSplash) {
            kotlinx.coroutines.delay(2500)
            showSplash = false
        }
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    // ========== PANTALLA 1: LOGIN O UNIRSE COMO TRABAJADOR ==========
    if (currentUser == null) {
        if (pendingWorker) {
            // Trabajador va directo a unirse sin pedir PIN admin
            BusinessSetupScreen(
                db = db,
                onBusinessSelected = { business ->
                    currentBusiness = business
                    showBusinessSetup = false
                    pendingWorker = false
                },
                onBack = { pendingWorker = false }
            )
            return
        }

        if (showUserManagement) {
            UserManagementScreen(db = db, onBack = { showUserManagement = false })
        } else {
            LoginScreen(
                db = db,
                onLoginSuccess = { currentUser = it },
                onFirstTimeSetup = { pendingWorker = true }
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

    val syncManager = remember { OfflineSyncManager(context, db) }
    var showSyncExportQr by remember { mutableStateOf(false) }
    var showSyncImportScanner by remember { mutableStateOf(false) }
    var syncQrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showSyncMenu by remember { mutableStateOf(false) }

    // Mostrar tutorial superpuesto la primera vez
    LaunchedEffect(currentBusiness) {
        if (currentBusiness != null) {
            val prefs = context.getSharedPreferences("nexus_prefs", android.content.Context.MODE_PRIVATE)
            val key = "onboarding_shown_${currentUser?.role ?: "unknown"}"
            val hasSeenOnboarding = prefs.getBoolean(key, false)
            if (!hasSeenOnboarding) {
                showOnboarding = true
                prefs.edit().putBoolean(key, true).apply()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            db.notificationDao().getAllNotifications().collect {
                notifications = it
                unreadCount = it.count { n -> !n.read }
            }
        } catch (e: Exception) {
            Log.e("NexusApp", "Error notifications: ${e.message}", e)
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
        // Contenido principal
        Row(modifier = Modifier.fillMaxSize()) {
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
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.Filled.HelpOutline, "Ayuda")
                        }

                        IconButton(onClick = { showSyncMenu = true }) {
                            Icon(Icons.Filled.Sync, "Sincronizar")
                        }

                        DropdownMenu(expanded = showSyncMenu, onDismissRequest = { showSyncMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isAdmin) "📤 Exportar datos" else "📤 Exportar mis ventas") },
                                onClick = {
                                    showSyncMenu = false
                                    scope.launch {
                                        try {
                                            showSyncExportQr = false
                                            syncQrBitmap = null
                                            withContext(Dispatchers.IO) {
                                                val data = syncManager.exportSalesToQr(currentUser!!.name, currentBusiness!!.code, isAdmin = isAdmin)
                                                if (data.isNotEmpty()) {
                                                    syncQrBitmap = QrCodeGenerator.generateQrCode(data)
                                                    withContext(Dispatchers.Main) { showSyncExportQr = true }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Upload, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isAdmin) "📥 Importar datos" else "📥 Actualizar desde Admin") },
                                onClick = { showSyncMenu = false; showSyncImportScanner = true },
                                leadingIcon = { Icon(Icons.Filled.Download, null) }
                            )
                        }

                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            IconButton(onClick = { showNotifications = !showNotifications }) {
                                Icon(Icons.Filled.Notifications, "Notificaciones")
                            }
                        }

                        Text(currentUser?.name?.firstOrNull()?.toString() ?: "?", modifier = Modifier.padding(8.dp))
                        IconButton(onClick = { showBusinessSetup = true }) { Icon(Icons.Filled.Store, "Cambiar negocio") }
                        IconButton(onClick = { currentUser = null; currentBusiness = null; showBusinessSetup = true }) { Icon(Icons.Filled.Logout, "Salir") }
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

        // ========== ONBOARDING SUPERPUESTO ==========
        if (showOnboarding) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                color = MaterialTheme.colorScheme.background
            ) {
                OnboardingTutorial(
                    isAdmin = isAdmin,
                    onFinish = { showOnboarding = false }
                )
            }
        }

        // ========== NOTIFICACIONES ==========
        AnimatedVisibility(
            visible = showNotifications,
            enter = fadeIn() + slideInVertically { -20 },
            exit = fadeOut() + slideOutVertically { -20 },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .zIndex(20f)
        ) {
            NotificationPanel(
                notifications = notifications,
                onNotificationClick = { n ->
                    scope.launch {
                        try { db.notificationDao().update(n.copy(read = true)); showNotifications = false }
                        catch (e: Exception) {}
                    }
                },
                onMarkAsRead = { n -> scope.launch { try { db.notificationDao().update(n.copy(read = true)) } catch (_: Exception) {} } },
                onDelete = { n -> scope.launch { try { db.notificationDao().delete(n) } catch (_: Exception) {} } },
                onMarkAllRead = { scope.launch { try { db.notificationDao().markAllAsRead() } catch (_: Exception) {} } }
            )
        }

        // ========== QR DE EXPORTACIÓN ==========
        if (showSyncExportQr && syncQrBitmap != null) {
            AlertDialog(
                onDismissRequest = { showSyncExportQr = false },
                title = { Text("📤 Exportar Datos") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Muestra este QR al otro dispositivo")
                        Spacer(Modifier.height(16.dp))
                        Image(bitmap = syncQrBitmap!!.asImageBitmap(), contentDescription = "QR Sync", modifier = Modifier.size(280.dp))
                    }
                },
                confirmButton = { TextButton(onClick = { showSyncExportQr = false }) { Text("Cerrar") } }
            )
        }

        // ========== ESCÁNER DE IMPORTACIÓN ==========
        if (showSyncImportScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { data ->
                    scope.launch {
                        try {
                            val result = syncManager.importSalesFromQr(data)
                            if (result.success) {
                                showSyncImportScanner = false
                                Toast.makeText(context, "✅ ${result.salesImported} ventas, ${result.productsUpdated} productos de ${result.workerName}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {}
                    }
                },
                onDismiss = { showSyncImportScanner = false }
            )
        }

        // ========== AYUDA ==========
        if (showHelp) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f),
                color = MaterialTheme.colorScheme.background
            ) {
                HelpScreen(isAdmin = isAdmin, onDismiss = { showHelp = false })
            }
        }
    }
}
