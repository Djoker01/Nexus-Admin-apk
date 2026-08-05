package com.nexus.admin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.Business
import com.nexus.admin.data.entity.User
import com.nexus.admin.ui.components.FloatingBarcodeScanner
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
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.AddBusiness, null)
            Spacer(Modifier.width(8.dp))
            Text("Crear Nuevo Negocio")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.PersonAdd, null)
            Spacer(Modifier.width(8.dp))
            Text("Unirse como Trabajador")
        }

        Spacer(Modifier.height(24.dp))

        if (businesses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay negocios creados aún", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("Negocios guardados:", fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(businesses) { business ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onBusinessSelected(business) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Store, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(business.name, fontWeight = FontWeight.SemiBold)
                                Text("Código: ${business.code}", style = MaterialTheme.typography.bodySmall)
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

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nuevo Negocio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre del negocio *") }, singleLine = true)
                    OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Tu nombre *") }, singleLine = true)
                    
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Código del negocio:", fontWeight = FontWeight.Bold)
                            Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Código", code))
                                Toast.makeText(context, "✅ Código copiado", Toast.LENGTH_SHORT).show()
                            }) { Text("Copiar código") }
                        }
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
                            db.businessDao().insert(Business(name = name, code = code, ownerName = ownerName))
                        }
                        showCreateDialog = false
                        Toast.makeText(context, "✅ Negocio creado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Crear") }
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

        if (showScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { code ->
                    joinCode = code.trim().uppercase()
                    showScanner = false
                    Toast.makeText(context, "Código: $joinCode", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showScanner = false }
            )
        }

        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Unirse como Trabajador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(joinCode, { joinCode = it.trim().uppercase() }, label = { Text("Código del negocio *") }, singleLine = true)
                    OutlinedButton(onClick = { showScanner = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.QrCodeScanner, "Escanear", modifier = Modifier.size(20.dp))
                        Text("Escanear QR")
                    }
                    HorizontalDivider()
                    OutlinedTextField(workerName, { workerName = it }, label = { Text("Tu nombre *") }, singleLine = true)
                    OutlinedTextField(workerPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) workerPin = it }, label = { Text("PIN (4 dígitos) *") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (joinCode.isBlank() || workerName.isBlank() || workerPin.length != 4) {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    try {
                        val allBusinesses = runBlocking {
                            var list = emptyList<Business>()
                            db.businessDao().getAllBusinesses().collect { list = it }
                            list
                        }
                        val business = allBusinesses.find { it.code.equals(joinCode, ignoreCase = true) }
                        
                        if (business != null) {
                            runBlocking {
                                db.userDao().insert(User(name = workerName, pin = workerPin, role = "worker"))
                            }
                            onBusinessSelected(business)
                            showJoinDialog = false
                            Toast.makeText(context, "✅ Conectado a: ${business.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ Código no encontrado: $joinCode", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Unirse") }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("Cancelar") } }
        )
    }
}
