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
    var balance by remember { mutableStateOf(0.0) }
    var todayIncome by remember { mutableStateOf(0.0) }
    var todayExpense by remember { mutableStateOf(0.0) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        db.cashMovementDao().getAllMovements().collect { movementList ->
            movements = movementList
            balance = movementList.sumOf { if (it.type == "Ingreso") it.amount else -it.amount }
            
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val todayMovements = movementList.filter { it.date in todayStart..todayEnd }
            todayIncome = todayMovements.filter { it.type == "Ingreso" }.sumOf { it.amount }
            todayExpense = todayMovements.filter { it.type == "Egreso" }.sumOf { it.amount }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Caja", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Movimiento")
            }
        }
        
        // Balance Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = if (balance >= 0) GreenLight else RedLight)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Saldo Actual", style = MaterialTheme.typography.bodyMedium, color = Gray500)
                Text(
                    "$${Utils.formatCurrency(balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) GreenDark else Red
                )
            }
        }
        
        // KPIs Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Ingresos Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        "$${Utils.formatCurrency(todayIncome)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Egresos Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        "$${Utils.formatCurrency(todayExpense)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Red
                    )
                }
            }
        }
        
        // Movements List
        Text(
            "Historial de Movimientos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(movements) { movement ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (movement.type == "Ingreso") Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = if (movement.type == "Ingreso") Green else Red,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movement.description, fontWeight = FontWeight.Medium)
                            Text(Utils.formatDate(movement.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text(
                            "${if (movement.type == "Ingreso") "+" else "-"}$${Utils.formatCurrency(movement.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (movement.type == "Ingreso") Green else Red
                        )
                    }
                }
            }
        }
    }
    
    // Add Movement Dialog
    if (showAddDialog) {
        var type by remember { mutableStateOf("Ingreso") }
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var typeExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Movimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Type selector
                    Box {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(type)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            DropdownMenuItem(text = { Text("Ingreso") }, onClick = { type = "Ingreso"; typeExpanded = false })
                            DropdownMenuItem(text = { Text("Egreso") }, onClick = { type = "Egreso"; typeExpanded = false })
                        }
                    }
                    
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Monto") },
                        leadingIcon = { Text("$") },
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.cashMovementDao().insert(
                            CashMovement(
                                type = type,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                description = description
                            )
                        )
                        showAddDialog = false
                    }
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}