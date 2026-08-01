package com.nexus.admin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.Business
import com.nexus.admin.data.entity.User
import com.nexus.admin.data.sync.OfflineSyncManager
import com.nexus.admin.ui.components.FloatingBarcodeScanner
import com.nexus.admin.utils.QrCodeGenerator
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    db: AppDatabase,
    onBusinessSelected: (Business) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var businesses by remember { mutableStateOf<List<Business>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showSyncQrDialog by remember { mutableStateOf<Business?>(null) }

    LaunchedEffect(Unit) { db.businessDao().getAllBusinesses().collect { businesses = it } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Mis Negocios", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") }
        }
        Text("Selecciona o crea un negocio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showCreateDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.AddBusiness, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Crear Negocio")
            }
            OutlinedButton(onClick = { showJoinDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unirse como Trabajador")
            }
        }
        Spacer(Modifier.height(24.dp))

        if (businesses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Store, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No hay negocios", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text("Negocios guardados:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(businesses) { business ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { onBusinessSelected(business) }) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Store, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(business.name, fontWeight = FontWeight.SemiBold)
                                Text("Dueño: ${business.ownerName}", style = MaterialTheme.typography.bodySmall)
                                Text("Código: ${business.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showSyncQrDialog = business }) { Icon(Icons.Filled.QrCode, "QR Sync", tint = MaterialTheme.colorScheme.primary) }
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // ========== DIÁLOGO: CREAR NEGOCIO (ADMIN) ==========
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var ownerName by remember { mutableStateOf("") }
        val generatedCode = remember { "NEXUS-${UUID.randomUUID().toString().take(8).uppercase()}" }
        var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

LaunchedEffect(generatedCode) {
    withContext(Dispatchers.IO) {
        qrBitmap = QrCodeGenerator.generateQrCode(generatedCode)
    }
}

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nuevo Negocio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre del negocio *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Tu nombre (Administrador) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    HorizontalDivider()
                    Text("Código QR de conexión:", fontWeight = FontWeight.Bold)
                    Text("Muestra este QR a tus trabajadores para que escaneen y se conecten como TRABAJADORES", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).align(Alignment.CenterHorizontally))
                    Text(generatedCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Código", generatedCode))
                        Toast.makeText(context, "✅ Código copiado", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Text("Copiar código")
                    }
                    Text("Los trabajadores usarán este QR para conectarse a tu negocio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (name.isNotBlank() && ownerName.isNotBlank()) {
                            db.businessDao().insert(Business(name = name, code = generatedCode, ownerName = ownerName))
                            showCreateDialog = false
                            Toast.makeText(context, "✅ Negocio creado. Eres el Administrador", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Crear Negocio") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } }
        )
    }

    // ========== DIÁLOGO: UNIRSE COMO TRABAJADOR (CON PIN PERSONALIZADO) ==========
    if (showJoinDialog) {
        var joinCode by remember { mutableStateOf("") }
        var workerName by remember { mutableStateOf("") }
        var workerPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }
        var step by remember { mutableIntStateOf(1) }

        if (showScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { code -> joinCode = code; showScanner = false },
                onDismiss = { showScanner = false }
            )
        } else if (step == 1) {
            // Paso 1: Ingresar código del negocio
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { Text("Unirse como Trabajador") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pide el código QR al dueño del negocio", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(joinCode, { joinCode = it.uppercase() }, label = { Text("Código del negocio *") }, singleLine = true, placeholder = { Text("NEXUS-XXXXXXXX") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { showScanner = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.QrCodeScanner, "Escanear QR", modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Escanear QR")
                            }
                        }
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Text("⚠️ Te unirás como TRABAJADOR. Solo tendrás acceso a Dashboard, Inventario y Ventas.", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            if (joinCode.isNotBlank()) {
                                val business = db.businessDao().getBusinessByCode(joinCode)
                                if (business != null) step = 2
                                else Toast.makeText(context, "❌ Código no válido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("Continuar") }
                },
                dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("Cancelar") } }
            )
        } else {
            // Paso 2: Ingresar nombre y crear PIN
            AlertDialog(
                onDismissRequest = { showJoinDialog = false; step = 1 },
                title = { Text("Crear tu cuenta de Trabajador") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Establece tu nombre y crea un PIN de 4 dígitos para acceder", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(workerName, { workerName = it }, label = { Text("Tu nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        HorizontalDivider()
                        Text("Crea tu PIN de acceso:", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(workerPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) workerPin = it }, label = { Text("PIN (4 dígitos) *") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(confirmPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("Confirmar PIN *") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(), isError = confirmPin.isNotEmpty() && workerPin != confirmPin)
                        if (confirmPin.isNotEmpty() && workerPin != confirmPin) Text("Los PIN no coinciden", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text("🔐 Recuerda tu PIN. Lo necesitarás para acceder a la aplicación.", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            if (workerName.isNotBlank() && workerPin.length == 4 && workerPin == confirmPin) {
                                val business = db.businessDao().getBusinessByCode(joinCode)
                                if (business != null) {
                                    val existingUser = db.userDao().getUserByPin(workerPin)
                                    if (existingUser != null) {
                                        Toast.makeText(context, "❌ Ese PIN ya está en uso. Elige otro.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    db.userDao().insert(User(name = workerName, pin = workerPin, role = "worker"))
                                    onBusinessSelected(business)
                                    showJoinDialog = false; step = 1
                                    Toast.makeText(context, "✅ Conectado a: ${business.name}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "❌ Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("Crear Cuenta") }
                },
                dismissButton = { TextButton(onClick = { step = 1 }) { Text("Atrás") } }
            )
        }
    }

    // ========== DIÁLOGO: QR DE SINCRONIZACIÓN ==========
    showSyncQrDialog?.let { business ->
        var syncData by remember { mutableStateOf("") }
        var qrBitmapSync by remember { mutableStateOf<Bitmap?>(null) }
        val syncManager = remember { OfflineSyncManager(context, db) }

        LaunchedEffect(business) {
            syncData = syncManager.exportSalesToQr("Admin", business.code)
            if (syncData.isNotEmpty()) qrBitmapSync = QrCodeGenerator.generateQrCode(syncData, 512)
        }

        AlertDialog(
            onDismissRequest = { showSyncQrDialog = null },
            title = { Text("QR de Sincronización") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Muestra este QR al otro dispositivo", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    if (qrBitmapSync != null) Image(bitmap = qrBitmapSync!!.asImageBitmap(), contentDescription = "QR Sync", modifier = Modifier.size(280.dp).clip(RoundedCornerShape(12.dp)))
                    else CircularProgressIndicator()
                }
            },
            confirmButton = { TextButton(onClick = { showSyncQrDialog = null }) { Text("Cerrar") } }
        )
    }
}
