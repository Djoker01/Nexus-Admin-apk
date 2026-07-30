package com.nexus.admin.ui.screens

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
    var todayIncome by remember { mutableDoubleStateOf(0.0) }
    var todayExpense by remember { mutableDoubleStateOf(0.0) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.cashMovementDao().getAllMovements().collect {
            movements = it.sortedByDescending { m -> m.date }
            balance = it.sumOf { m -> if (m.type == "Ingreso") m.amount else -m.amount }
            
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val todayMovements = it.filter { m -> m.date in todayStart..todayEnd }
            todayIncome = todayMovements.filter { m -> m.type == "Ingreso" }.sumOf { m -> m.amount }
            todayExpense = todayMovements.filter { m -> m.type == "Egreso" }.sumOf { m -> m.amount }
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
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Movimiento")
            }
        }

        // Balance principal
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (balance >= 0) GreenLight else RedLight
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Saldo Actual", style = MaterialTheme.typography.bodyMedium, color = Gray500)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$${Utils.formatCurrency(balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) GreenDark else Red
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // KPIs del día
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowUpward, null, tint = Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ingresos Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    }
                    Text(
                        "$${Utils.formatCurrency(todayIncome)}",
                        fontWeight = FontWeight.Bold,
                        color = Green,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, null, tint = Red, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Egresos Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    }
                    Text(
                        "$${Utils.formatCurrency(todayExpense)}",
                        fontWeight = FontWeight.Bold,
                        color = Red,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Historial de movimientos
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
            if (movements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AccountBalance, null, modifier = Modifier.size(64.dp), tint = Gray300)
                            Spacer(Modifier.height(16.dp))
                            Text("No hay movimientos registrados", color = Gray500)
                        }
                    }
                }
            }
            items(movements) { mov ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono según tipo
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (mov.type == "Ingreso") GreenLight else RedLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (mov.type == "Ingreso") Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                    null,
                                    tint = if (mov.type == "Ingreso") Green else Red,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(mov.description, fontWeight = FontWeight.Medium)
                            Text(
                                Utils.formatDate(mov.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                        
                        Text(
                            "${if (mov.type == "Ingreso") "+" else "-"}$${Utils.formatCurrency(mov.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (mov.type == "Ingreso") Green else Red
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
            title = { Text("Registrar Movimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Selector de tipo
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de movimiento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ingreso") },
                                onClick = { type = "Ingreso"; typeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Egreso") },
                                onClick = { type = "Egreso"; typeExpanded = false }
                            )
                        }
                    }

                    // Monto
                    OutlinedTextField(
                        amount, { amount = it },
                        label = { Text("Monto *") },
                        leadingIcon = { Text("$") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Descripción
                    OutlinedTextField(
                        description, { description = it },
                        label = { Text("Descripción") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Mostrar efecto en el saldo
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val newBalance = if (type == "Ingreso") balance + amt else balance - amt
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (type == "Egreso" && amt > balance) RedLight else BlueLight
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Saldo actual: $${Utils.formatCurrency(balance)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${if (type == "Ingreso") "+" else "-"}$${Utils.formatCurrency(amt)}",
                                    color = if (type == "Ingreso") Green else Red,
                                    fontWeight = FontWeight.Bold
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    "Nuevo saldo: $${Utils.formatCurrency(newBalance)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (newBalance >= 0) Green else Red
                                )
                                if (type == "Egreso" && amt > balance) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "⚠️ Saldo insuficiente. Faltan $${Utils.formatCurrency(amt - balance)}",
                                        color = Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && description.isNotBlank()) {
                                // Validar saldo para egresos
                                if (type == "Egreso" && amt > balance) {
                                    Toast.makeText(context, "❌ Saldo insuficiente en caja", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                // Registrar movimiento
                                db.cashMovementDao().insert(
                                    CashMovement(
                                        type = type,
                                        amount = amt,
                                        description = description,
                                        date = System.currentTimeMillis()
                                    )
                                )

                                showAddDialog = false
                                Toast.makeText(
                                    context,
                                    "✅ ${if (type == "Ingreso") "Ingreso" else "Egreso"} registrado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = (amount.toDoubleOrNull() ?: 0.0) > 0 && description.isNotBlank()
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
