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

    LaunchedEffect(Unit) {
    val dir = File(context.getExternalFilesDir(null), "backups")
    dir.mkdirs()
    backups = dir.listFiles()?.map { BackupInfo(it.lastModified(), it.name, it.length()) }?.sortedByDescending { it.date } ?: emptyList()
}

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { scope.launch { exportData(context, db, it); refreshBackups() } }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch { importData(context, db, it); refreshBackups() } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Respaldos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Button(onClick = { exportLauncher.launch("nexus_backup_${System.currentTimeMillis()}.json") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Backup, null); Spacer(Modifier.width(8.dp)); Text("Crear Respaldo")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Restore, null); Spacer(Modifier.width(8.dp)); Text("Restaurar Respaldo")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
            Icon(Icons.Filled.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Eliminar Todo")
        }
        Spacer(Modifier.height(16.dp))
        Text("Historial", fontWeight = FontWeight.SemiBold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(backups) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.InsertDriveFile, null, tint = Blue, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, fontWeight = FontWeight.Medium)
                            Text("${b.size / 1024} KB - ${Utils.formatDate(b.date)}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("⚠️ Eliminar todos los datos") },
            text = { Text("¿Está seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.productDao().deleteAll(); db.saleDao().deleteAll(); db.cashMovementDao().deleteAll()
                        db.expenseDao().deleteAll(); db.clientDao().deleteAll(); db.receivableDao().deleteAll()
                        db.shrinkageDao().deleteAll(); db.restockDao().deleteAll(); db.supplierDao().deleteAll()
                        db.quoteDao().deleteAll(); db.notificationDao().deleteAll()
                        showDeleteConfirm = false
                        Toast.makeText(context, "Datos eliminados", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}

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
        val file = File(context.getExternalFilesDir(null), "backups/backup_${System.currentTimeMillis()}.json")
        file.parentFile?.mkdirs(); file.writeText(json)
        Toast.makeText(context, "✅ Respaldo creado exitosamente", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

suspend fun importData(context: Context, db: AppDatabase, uri: android.net.Uri) {
    try {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = Gson().fromJson(json, type)
        
        // Limpiar datos existentes
        db.productDao().deleteAll(); db.saleDao().deleteAll(); db.cashMovementDao().deleteAll()
        db.expenseDao().deleteAll(); db.clientDao().deleteAll(); db.receivableDao().deleteAll()
        db.shrinkageDao().deleteAll(); db.restockDao().deleteAll(); db.supplierDao().deleteAll()
        db.quoteDao().deleteAll()

        // Restaurar productos
        val productsJson = Gson().toJson(data["products"])
        val products: List<Product> = Gson().fromJson(productsJson, object : TypeToken<List<Product>>() {}.type)
        products.forEach { db.productDao().insert(it) }

        // Restaurar ventas
        val salesJson = Gson().toJson(data["sales"])
        val sales: List<Sale> = Gson().fromJson(salesJson, object : TypeToken<List<Sale>>() {}.type)
        sales.forEach { db.saleDao().insert(it) }

        // Restaurar movimientos de caja
        val cashJson = Gson().toJson(data["cashMovements"])
        val cashMovements: List<CashMovement> = Gson().fromJson(cashJson, object : TypeToken<List<CashMovement>>() {}.type)
        cashMovements.forEach { db.cashMovementDao().insert(it) }

        // Restaurar gastos
        val expensesJson = Gson().toJson(data["expenses"])
        val expenses: List<Expense> = Gson().fromJson(expensesJson, object : TypeToken<List<Expense>>() {}.type)
        expenses.forEach { db.expenseDao().insert(it) }

        // Restaurar clientes
        val clientsJson = Gson().toJson(data["clients"])
        val clients: List<Client> = Gson().fromJson(clientsJson, object : TypeToken<List<Client>>() {}.type)
        clients.forEach { db.clientDao().insert(it) }

        // Restaurar cuentas por cobrar
        val receivablesJson = Gson().toJson(data["receivables"])
        val receivables: List<Receivable> = Gson().fromJson(receivablesJson, object : TypeToken<List<Receivable>>() {}.type)
        receivables.forEach { db.receivableDao().insert(it) }

        // Restaurar mermas
        val shrinkagesJson = Gson().toJson(data["shrinkages"])
        val shrinkages: List<Shrinkage> = Gson().fromJson(shrinkagesJson, object : TypeToken<List<Shrinkage>>() {}.type)
        shrinkages.forEach { db.shrinkageDao().insert(it) }

        // Restaurar reabastecimientos
        val restocksJson = Gson().toJson(data["restocks"])
        val restocks: List<Restock> = Gson().fromJson(restocksJson, object : TypeToken<List<Restock>>() {}.type)
        restocks.forEach { db.restockDao().insert(it) }

        // Restaurar proveedores
        val suppliersJson = Gson().toJson(data["suppliers"])
        val suppliers: List<Supplier> = Gson().fromJson(suppliersJson, object : TypeToken<List<Supplier>>() {}.type)
        suppliers.forEach { db.supplierDao().insert(it) }

        // Restaurar cotizaciones
        val quotesJson = Gson().toJson(data["quotes"])
        val quotes: List<Quote> = Gson().fromJson(quotesJson, object : TypeToken<List<Quote>>() {}.type)
        quotes.forEach { db.quoteDao().insert(it) }

        Toast.makeText(context, "✅ Datos restaurados exitosamente", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
