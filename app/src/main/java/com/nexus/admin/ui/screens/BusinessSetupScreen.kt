package com.nexus.admin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    // Cargar negocios guardados
    LaunchedEffect(Unit) {
        try {
            db.businessDao().getAllBusinesses().collect { businesses = it }
        } catch (e: Exception) {
            Log.e("BusinessSetup", "Error: ${e.message}", e)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mis Negocios", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") }
        }
        Text("Selecciona o crea un negocio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // BOTONES PRINCIPALES
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { 
                    Log.d("BusinessSetup", "Botón Crear Negocio presionado")
                    showCreateDialog = true 
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.AddBusiness, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Crear Negocio")
            }
            OutlinedButton(
                onClick = { 
                    Log.d("BusinessSetup", "Botón Unirse presionado")
                    showJoinDialog = true 
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unirse como Trabajador")
            }
        }
        Spacer(Modifier.height(24.dp))

        // Lista de negocios
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onBusinessSelected(business) }
                    ) {
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

    // ========== DIÁLOGO: CREAR NEGOCIO ==========
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var ownerName by remember { mutableStateOf("") }
        val generatedCode = remember { "NEXUS-${UUID.randomUUID().toString().take(8).uppercase()}" }
        var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var isGeneratingQr by remember { mutableStateOf(true) }

        // Generar QR
        LaunchedEffect(generatedCode) {
            withContext(Dispatchers.IO) {
                try {
                    qrBitmap = QrCodeGenerator.generateQrCode(generatedCode)
                } catch (e: Exception) {
                    Log.e("BusinessSetup", "Error QR: ${e.message}", e)
                }
                isGeneratingQr = false
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nuevo Negocio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        name, { name = it },
                        label = { Text("Nombre del negocio *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        ownerName, { ownerName = it },
                        label = { Text("Tu nombre (Administrador) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                    Text("Código QR de conexión:", fontWeight = FontWeight.Bold)
                    Text("Muestra este QR a tus trabajadores", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        if (isGeneratingQr) CircularProgressIndicator()
                        else if (qrBitmap != null) Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                    }
                    
                    Text(generatedCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    
                    OutlinedButton(onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Código", generatedCode))
                            Toast.makeText(context, "✅ Código copiado", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al copiar", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Text("Copiar código")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank() || ownerName.isBlank()) {
                        Toast.makeText(context, "❌ Completa todos los campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    try {
                        runBlocking {
                            db.businessDao().insert(Business(name = name, code = generatedCode, ownerName = ownerName))
                        }
                        showCreateDialog = false
                        Toast.makeText(context, "✅ Negocio creado exitosamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("BusinessSetup", "Error insert: ${e.message}", e)
                        Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("Crear Negocio")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } }
        )
    }

    // ========== DIÁLOGO: UNIRSE COMO TRABAJADOR ==========
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
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { Text("Unirse como Trabajador") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pide el código QR al dueño del negocio")
                        OutlinedTextField(joinCode, { joinCode = it.uppercase() }, label = { Text("Código del negocio *") }, singleLine = true, placeholder = { Text("NEXUS-XXXXXXXX") }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = { showScanner = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.QrCodeScanner, "Escanear", modifier = Modifier.size(20.dp))
                            Text("Escanear QR")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (joinCode.isBlank()) return@Button
                        try {
                            val business = runBlocking { db.businessDao().getBusinessByCode(joinCode) }
                            if (business != null) step = 2
                            else Toast.makeText(context, "❌ Código no válido", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Continuar") }
                },
                dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("Cancelar") } }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false; step = 1 },
                title = { Text("Crear tu cuenta") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(workerName, { workerName = it }, label = { Text("Tu nombre *") }, singleLine = true)
                        OutlinedTextField(workerPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) workerPin = it }, label = { Text("PIN (4 dígitos) *") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                        OutlinedTextField(confirmPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("Confirmar PIN *") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (workerName.isBlank() || workerPin.length != 4 || workerPin != confirmPin) return@Button
                        try {
                            val business = runBlocking { db.businessDao().getBusinessByCode(joinCode) }
                            if (business != null) {
                                runBlocking { db.userDao().insert(User(name = workerName, pin = workerPin, role = "worker")) }
                                onBusinessSelected(business)
                                showJoinDialog = false; step = 1
                                Toast.makeText(context, "✅ Conectado a: ${business.name}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Crear Cuenta") }
                },
                dismissButton = { TextButton(onClick = { step = 1 }) { Text("Atrás") } }
            )
        }
    }

    // ========== DIÁLOGO: QR DE SINCRONIZACIÓN ==========
    showSyncQrDialog?.let { business ->
        var qrBitmapSync by remember { mutableStateOf<Bitmap?>(null) }
        var isGeneratingSyncQr by remember { mutableStateOf(true) }

        LaunchedEffect(business) {
            withContext(Dispatchers.IO) {
                try {
                    val syncManager = OfflineSyncManager(context, db)
                    val data = syncManager.exportSalesToQr("Admin", business.code)
                    if (data.isNotEmpty()) qrBitmapSync = QrCodeGenerator.generateQrCode(data, 512)
                } catch (e: Exception) {
                    Log.e("BusinessSetup", "Error sync: ${e.message}", e)
                }
                isGeneratingSyncQr = false
            }
        }

        AlertDialog(
            onDismissRequest = { showSyncQrDialog = null },
            title = { Text("QR de Sincronización") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isGeneratingSyncQr) CircularProgressIndicator()
                    else if (qrBitmapSync != null) Image(bitmap = qrBitmapSync!!.asImageBitmap(), contentDescription = "QR Sync", modifier = Modifier.size(280.dp).clip(RoundedCornerShape(12.dp)))
                    else Text("Sin datos")
                }
            },
            confirmButton = { TextButton(onClick = { showSyncQrDialog = null }) { Text("Cerrar") } }
        )
    }
}
