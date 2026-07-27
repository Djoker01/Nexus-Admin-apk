package com.nexus.admin.ui.screens

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
import com.nexus.admin.data.entity.CashMovement
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var movements by remember { mutableStateOf<List<CashMovement>>(emptyList()) }
    var balance by remember { mutableDoubleStateOf(0.0) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.cashMovementDao().getAllMovements().collect {
            movements = it.sortedByDescending { m -> m.date }
            balance = it.sumOf { m -> if (m.type == "Ingreso") m.amount else -m.amount }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Caja", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Movimiento") }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = if (balance >= 0) GreenLight else RedLight)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Saldo Actual", color = Gray500)
                Text("$${Utils.formatCurrency(balance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = if (balance >= 0) GreenDark else Red)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(movements) { mov ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (mov.type == "Ingreso") Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, null, tint = if (mov.type == "Ingreso") Green else Red, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mov.description, fontWeight = FontWeight.Medium)
                            Text(Utils.formatDate(mov.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text("${if (mov.type == "Ingreso") "+" else "-"}$${Utils.formatCurrency(mov.amount)}", fontWeight = FontWeight.Bold, color = if (mov.type == "Ingreso") Green else Red)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var type by remember { mutableStateOf("Ingreso") }
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var typeExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Movimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(type); Icon(Icons.Filled.ArrowDropDown, null) }
                        DropdownMenu(typeExpanded, { typeExpanded = false }) {
                            DropdownMenuItem(text = { Text("Ingreso") }, onClick = { type = "Ingreso"; typeExpanded = false })
                            DropdownMenuItem(text = { Text("Egreso") }, onClick = { type = "Egreso"; typeExpanded = false })
                        }
                    }
                    OutlinedTextField(amount, { amount = it }, label = { Text("Monto") }, leadingIcon = { Text("$") })
                    OutlinedTextField(description, { description = it }, label = { Text("Descripción") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.cashMovementDao().insert(CashMovement(type = type, amount = amount.toDoubleOrNull() ?: 0.0, description = description))
                        showAddDialog = false
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}
