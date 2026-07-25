package com.nexus.admin.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.*
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    data class BackupInfo(val date: Long, val size: Long, val auto: Boolean)
    
    LaunchedEffect(Unit) {
        // Load backups from SharedPreferences
        val prefs = context.getSharedPreferences("nexus_backups", Context.MODE_PRIVATE)
        val backupsJson = prefs.getString("backup_list", "[]") ?: "[]"
        backups = Gson().fromJson(backupsJson, Array<BackupInfo>::class.java).toList()
    }
    
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                exportBackupToUri(context, db, it)
            }
        }
    }
    
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                importBackupFromUri(context, db, it)
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Respaldos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        // Storage info
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = BlueLight)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = Blue, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Respaldos realizados: ${backups.size}", fontWeight = FontWeight.Medium)
                    val totalSize = backups.sumOf { it.size }
                    Text("Tamaño total: ${(totalSize / 1024).toInt()} KB", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { createBackupLauncher.launch("nexus_backup_${Utils.formatDate(System.currentTimeMillis())}.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Respaldo")
            }
            
            OutlinedButton(
                onClick = { restoreBackupLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restaurar Respaldo")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                                putExtra(Intent.EXTRA_TITLE, "nexus_export_${System.currentTimeMillis()}.json")
                            }
                            // Launch export
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exportar Todo")
                }
                
                OutlinedButton(
                    onClick = { restoreBackupLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Importar")
                }
            }
            
            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar Todos los Datos")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Backup history
        Text(
            "Historial de Respaldos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(backups) { backup ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (backup.auto) Icons.Filled.Schedule else Icons.Filled.Backup,
                            contentDescription = null,
                            tint = if (backup.auto) Yellow else Blue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Utils.formatDate(backup.date), fontWeight = FontWeight.Medium)
                            Text("${(backup.size / 1024).toInt()} KB", style = MaterialTheme.typography.bodySmall)
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(if (backup.auto) "Auto" else "Manual") }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("⚠️ Eliminar Todos los Datos") },
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

suspend fun exportBackupToUri(context: Context, db: AppDatabase, uri: Uri) {
    val data = mapOf(
        "products" to db.productDao().getAllProducts(),
        "sales" to db.saleDao().getAllSales(),
        "cashMovements" to db.cashMovementDao().getAllMovements(),
        "expenses" to db.expenseDao().getAllExpenses(),
        "clients" to db.clientDao().getAllClients(),
        "receivables" to db.receivableDao().getAllReceivables(),
        "shrinkages" to db.shrinkageDao().getAllShrinkages(),
        "restocks" to db.restockDao().getAllRestocks(),
        "suppliers" to db.supplierDao().getAllSuppliers(),
        "quotes" to db.quoteDao().getAllQuotes()
    )
    
    val json = Gson().toJson(data)
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(json.toByteArray())
    }
    
    // Save backup info
    val prefs = context.getSharedPreferences("nexus_backups", Context.MODE_PRIVATE)
    val backups = Gson().fromJson(prefs.getString("backup_list", "[]"), Array<BackupInfo>::class.java).toMutableList()
    backups.add(BackupInfo(System.currentTimeMillis(), json.toByteArray().size.toLong(), false))
    prefs.edit().putString("backup_list", Gson().toJson(backups)).apply()
}

suspend fun importBackupFromUri(context: Context, db: AppDatabase, uri: Uri) {
    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
    val data = Gson().fromJson(json, Map::class.java)
    
    // Restore data...
}