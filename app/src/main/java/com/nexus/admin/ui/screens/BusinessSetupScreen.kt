package com.nexus.admin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.nexus.admin.utils.QrCodeGenerator
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
            Button(onClick = { showCreateDialog = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.AddBusiness, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Crear Negocio") }
            OutlinedButton(onClick = { showJoinDialog = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Escanear QR") }
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

    // Diálogo: Crear Negocio
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }; var ownerName by remember { mutableStateOf("") }
        val generatedCode = remember { "NEXUS-${UUID.randomUUID().toString().take(8).uppercase()}" }
        val qrBitmap = remember { QrCodeGenerator.generateQrCode(generatedCode) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nuevo Negocio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Dueño *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    HorizontalDivider()
                    Text("QR de conexión:", fontWeight = FontWeight.Bold)
                    Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).align(Alignment.CenterHorizontally))
                    Text(generatedCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("Código", generatedCode)); Toast.makeText(context, "✅ Copiado", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp)); Text("Copiar código") }
                    Text("Comparte este QR con tus trabajadores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { scope.launch { if (name.isNotBlank() && ownerName.isNotBlank()) { db.businessDao().insert(Business(name = name, code = generatedCode, ownerName = ownerName)); showCreateDialog = false } } }) { Text("Crear") } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } }
        )
    }

    // Diálogo: Escanear QR para unirse
    if (showJoinDialog) {
        FloatingBarcodeScanner(
            onBarcodeScanned = { code ->
                scope.launch {
                    val business = db.businessDao().getBusinessByCode(code)
                    if (business != null) {
                        onBusinessSelected(business)
                        showJoinDialog = false
                        Toast.makeText(context, "✅ Conectado a: ${business.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        // Intentar crear si no existe
                        val newBusiness = Business(name = "Negocio Remoto", code = code, ownerName = "Remoto")
                        db.businessDao().insert(newBusiness)
                        onBusinessSelected(newBusiness)
                        showJoinDialog = false
                        Toast.makeText(context, "✅ Conectado con código: $code", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showJoinDialog = false }
        )
    }

    // Diálogo: QR de sincronización de datos
    showSyncQrDialog?.let { business ->
        var syncData by remember { mutableStateOf("") }
        var qrBitmapSync by remember { mutableStateOf<Bitmap?>(null) }
        val syncManager = remember { OfflineSyncManager(context, db) }

        LaunchedEffect(business) {
            syncData = syncManager.exportSalesToQr("Admin", business.code)
            if (syncData.isNotEmpty()) {
                qrBitmapSync = QrCodeGenerator.generateQrCode(syncData, 512)
            }
        }

        AlertDialog(
            onDismissRequest = { showSyncQrDialog = null },
            title = { Text("QR de Sincronización") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Muestra este QR al otro dispositivo", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    if (qrBitmapSync != null) {
                        Image(bitmap = qrBitmapSync!!.asImageBitmap(), contentDescription = "QR Sync", modifier = Modifier.size(280.dp).clip(RoundedCornerShape(12.dp)))
                    } else { CircularProgressIndicator() }
                }
            },
            confirmButton = { TextButton(onClick = { showSyncQrDialog = null }) { Text("Cerrar") } }
        )
    }
}
