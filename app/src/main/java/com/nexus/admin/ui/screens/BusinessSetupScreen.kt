package com.nexus.admin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.Business
import com.nexus.admin.data.entity.User
import com.nexus.admin.ui.components.FloatingBarcodeScanner
import com.nexus.admin.utils.QrCodeGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    db: AppDatabase,
    onBusinessSelected: (Business) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var businesses by remember { mutableStateOf<List<Business>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf<Business?>(null) }

    LaunchedEffect(Unit) {
        try {
            db.businessDao().getAllBusinesses().collect { businesses = it }
        } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis Negocios", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("CREAR NUEVO NEGOCIO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("UNIRSE COMO TRABAJADOR", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(24.dp))

        if (businesses.isNotEmpty()) {
            Text("Negocios guardados:", fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(businesses) { business ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Store, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(business.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text("Código: ${business.code}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { showQrDialog = business }) {
                                Icon(Icons.Filled.QrCode, "Mostrar QR", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                            IconButton(onClick = { onBusinessSelected(business) }) {
                                Icon(Icons.Filled.ArrowForward, "Entrar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== CREAR NEGOCIO ==========
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var ownerName by remember { mutableStateOf("") }
        val code = remember { "NX-${UUID.randomUUID().toString().take(6).uppercase()}" }
        val qrBitmap = remember(code) {
            try { QrCodeGenerator.generateQrCode(code, 400) } catch (e: Exception) { null }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nuevo Negocio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre del negocio *") }, singleLine = true)
                    OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Tu nombre (Admin) *") }, singleLine = true)
                    
                    HorizontalDivider()
                    Text("Código QR del negocio:", fontWeight = FontWeight.Bold)
                    Text("El trabajador debe escanear este QR para conectarse", style = MaterialTheme.typography.bodySmall)
                    
                    Box(modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        if (qrBitmap != null) {
                            Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        } else {
                            Text("Error al generar QR", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Código", code))
                        Toast.makeText(context, "✅ Código copiado", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Text("Copiar código")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank() || ownerName.isBlank()) {
                        Toast.makeText(context, "Completa los campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    try {
                        runBlocking {
                            db.businessDao().insert(Business(name = name.trim(), code = code, ownerName = ownerName.trim()))
                        }
                        showCreateDialog = false
                        Toast.makeText(context, "✅ Negocio creado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) { Text("CREAR NEGOCIO", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } }
        )
    }

    // ========== UNIRSE COMO TRABAJADOR ==========
    if (showJoinDialog) {
        var joinCode by remember { mutableStateOf("") }
        var workerName by remember { mutableStateOf("") }
        var workerPin by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }
        var isJoining by remember { mutableStateOf(false) }

        if (showScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { code ->
                    joinCode = code.trim().uppercase()
                    showScanner = false
                },
                onDismiss = { showScanner = false }
            )
        }

        AlertDialog(
            onDismissRequest = { if (!isJoining) showJoinDialog = false },
            title = { Text("Unirse como Trabajador", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(joinCode, { joinCode = it.trim().uppercase() }, label = { Text("Código del negocio *") }, singleLine = true, enabled = !isJoining)
                    Button(onClick = { showScanner = true }, modifier = Modifier.fillMaxWidth(), enabled = !isJoining) {
                        Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ESCANEAR QR")
                    }
                    HorizontalDivider()
                    OutlinedTextField(workerName, { workerName = it }, label = { Text("Tu nombre *") }, singleLine = true, enabled = !isJoining)
                    OutlinedTextField(workerPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) workerPin = it }, label = { Text("PIN de 4 dígitos *") }, singleLine = true, enabled = !isJoining)
                    if (isJoining) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Buscando negocio...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
    Button(onClick = {
        if (joinCode.isBlank() || workerName.isBlank() || workerPin.length != 4) {
            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return@Button
        }
        
        isJoining = true
        
        // Ejecutar en hilo secundario
        Thread {
            try {
                runBlocking {
                    // Buscar el negocio localmente
                    val existingBusiness = db.businessDao().getAllBusinesses()
                        .first()
                        .find { it.code.equals(joinCode, ignoreCase = true) }
                    
                    // Si no existe, crearlo con el código escaneado
                    val business = existingBusiness ?: run {
                        val newBusiness = Business(
                            name = "Negocio ($joinCode)",  // Nombre temporal
                            code = joinCode,
                            ownerName = "Admin Remoto"
                        )
                        db.businessDao().insert(newBusiness)
                        newBusiness
                    }
                    
                    // Crear usuario trabajador
                    db.userDao().insert(
                        User(name = workerName.trim(), pin = workerPin, role = "worker")
                    )
                    
                    // Volver al hilo principal
                    Handler(Looper.getMainLooper()).post {
                        showJoinDialog = false
                        onBusinessSelected(business)
                        Toast.makeText(
                            context,
                            "✅ Conectado a: ${business.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    isJoining = false
                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }, enabled = !isJoining, modifier = Modifier.fillMaxWidth()) {
        if (isJoining) Text("Buscando...") else Text("UNIRSE AL NEGOCIO", fontWeight = FontWeight.Bold)
    }
}

    // ========== MOSTRAR QR DE NEGOCIO EXISTENTE ==========
    showQrDialog?.let { business ->
        val qrBitmap = remember(business.code) {
            try { QrCodeGenerator.generateQrCode(business.code, 400) } catch (e: Exception) { null }
        }

        AlertDialog(
            onDismissRequest = { showQrDialog = null },
            title = { Text("QR de: ${business.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Muestra este QR a tus trabajadores para que se conecten", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                        if (qrBitmap != null) {
                            Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        } else {
                            Text("Error al generar QR", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Código: ${business.code}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Código", business.code))
                                Toast.makeText(context, "✅ Código copiado", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Text("Copiar código")
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQrDialog = null }) { Text("Cerrar") } }
        )
    }
}
