package com.nexus.admin.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.*
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

data class BackupInfo(val date: Long, val name: String, val size: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Función para refrescar la lista de backups
    fun loadBackups() {
        val dir = File(context.getExternalFilesDir(null), "backups")
        dir.mkdirs()
        backups = dir.listFiles()
            ?.map { BackupInfo(it.lastModified(), it.name, it.length()) }
            ?.sortedByDescending { it.date }
            ?: emptyList()
    }

    LaunchedEffect(Unit) {
        loadBackups()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                exportData(context, db, uri)
                loadBackups()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                importData(context, db, uri)
                loadBackups()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Respaldos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                exportLauncher.launch("nexus_backup_${System.currentTimeMillis()}.json")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Backup, null)
            Spacer(Modifier.width(8.dp))
            Text("Crear Respaldo")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Restore, null)
            Spacer(Modifier.width(8.dp))
            Text("Restaurar Respaldo")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Red)
        ) {
            Icon(Icons.Filled.DeleteForever, null)
            Spacer(Modifier.width(8.dp))
            Text("Eliminar Todos los Datos")
        }

        Spacer(Modifier.height(16.dp))

        Text("Historial de Respaldos", fontWeight = FontWeight.SemiBold)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(backups) { backup ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.InsertDriveFile, null, tint = Blue, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(backup.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${backup.size / 1024} KB - ${Utils.formatDate(backup.date)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("⚠️ Eliminar todos los datos") },
            text = { Text("¿Está completamente seguro? Esta acción no se puede deshacer y perderá todos los registros permanentemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            db.productDao().deleteAll()
                            db.saleDao().deleteAll()
                            db.cashMovementDao().deleteAll()
                            db.expenseDao().deleteAll()
                            db.clientDao().deleteAll()
                            db.receivableDao().deleteAll()
                            db.shrinkageDao().deleteAll()
                            db.restockDao().deleteAll()
                            db.supplierDao().deleteAll()
                            db.quoteDao().deleteAll()
                            db.notificationDao().deleteAll()
                            showDeleteConfirm = false
                            loadBackups()
                            Toast.makeText(context, "✅ Todos los datos han sido eliminados", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("Eliminar Todo") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

// Exportar datos a un archivo JSON
suspend fun exportData(context: Context, db: AppDatabase, uri: android.net.Uri) {
    try {
        val data = mapOf(
            "products" to db.productDao().getAllProducts().first(),
            "sales" to db.saleDao().getAllSales().first(),
            "cashMovements" to db.cashMovementDao().getAllMovements().first(),
            "expenses" to db.expenseDao().getAllExpenses().first(),
            "clients" to db.clientDao().getAllClients().first(),
            "receivables" to db.receivableDao().getAllReceivables().first(),
            "shrinkages" to db.shrinkageDao().getAllShrinkages().first(),
            "restocks" to db.restockDao().getAllRestocks().first(),
            "suppliers" to db.supplierDao().getAllSuppliers().first(),
            "quotes" to db.quoteDao().getAllQuotes().first()
        )
        val json = Gson().toJson(data)
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }

        // Guardar copia local
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        backupDir.mkdirs()
        val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.json")
        backupFile.writeText(json)

        Toast.makeText(context, "✅ Respaldo creado exitosamente", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Importar datos desde un archivo JSON
suspend fun importData(context: Context, db: AppDatabase, uri: android.net.Uri) {
    try {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw Exception("No se pudo leer el archivo")

        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = Gson().fromJson(json, type)

        // Limpiar datos existentes
        db.productDao().deleteAll()
        db.saleDao().deleteAll()
        db.cashMovementDao().deleteAll()
        db.expenseDao().deleteAll()
        db.clientDao().deleteAll()
        db.receivableDao().deleteAll()
        db.shrinkageDao().deleteAll()
        db.restockDao().deleteAll()
        db.supplierDao().deleteAll()
        db.quoteDao().deleteAll()

        val gson = Gson()

        // Restaurar productos
        (data["products"] as? List<*>)?.forEach { item ->
            val product = gson.fromJson(gson.toJson(item), Product::class.java)
            db.productDao().insert(product)
        }

        // Restaurar ventas
        (data["sales"] as? List<*>)?.forEach { item ->
            val sale = gson.fromJson(gson.toJson(item), Sale::class.java)
            db.saleDao().insert(sale)
        }

        // Restaurar movimientos de caja
        (data["cashMovements"] as? List<*>)?.forEach { item ->
            val mov = gson.fromJson(gson.toJson(item), CashMovement::class.java)
            db.cashMovementDao().insert(mov)
        }

        // Restaurar gastos
        (data["expenses"] as? List<*>)?.forEach { item ->
            val expense = gson.fromJson(gson.toJson(item), Expense::class.java)
            db.expenseDao().insert(expense)
        }

        // Restaurar clientes
        (data["clients"] as? List<*>)?.forEach { item ->
            val client = gson.fromJson(gson.toJson(item), Client::class.java)
            db.clientDao().insert(client)
        }

        // Restaurar cuentas por cobrar
        (data["receivables"] as? List<*>)?.forEach { item ->
            val receivable = gson.fromJson(gson.toJson(item), Receivable::class.java)
            db.receivableDao().insert(receivable)
        }

        // Restaurar mermas
        (data["shrinkages"] as? List<*>)?.forEach { item ->
            val shrinkage = gson.fromJson(gson.toJson(item), Shrinkage::class.java)
            db.shrinkageDao().insert(shrinkage)
        }

        // Restaurar reabastecimientos
        (data["restocks"] as? List<*>)?.forEach { item ->
            val restock = gson.fromJson(gson.toJson(item), Restock::class.java)
            db.restockDao().insert(restock)
        }

        // Restaurar proveedores
        (data["suppliers"] as? List<*>)?.forEach { item ->
            val supplier = gson.fromJson(gson.toJson(item), Supplier::class.java)
            db.supplierDao().insert(supplier)
        }

        // Restaurar cotizaciones
        (data["quotes"] as? List<*>)?.forEach { item ->
            val quote = gson.fromJson(gson.toJson(item), Quote::class.java)
            db.quoteDao().insert(quote)
        }

        Toast.makeText(context, "✅ Datos restaurados exitosamente", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
