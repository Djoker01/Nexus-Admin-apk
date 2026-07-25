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
import com.nexus.admin.data.entity.*
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivablesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var receivables by remember { mutableStateOf<List<Receivable>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var showAddReceivable by remember { mutableStateOf(false) }
    var showAddClient by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Receivable?>(null) }
    
    var totalPending by remember { mutableStateOf(0.0) }
    var pendingCount by remember { mutableStateOf(0) }
    var totalCollected by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        db.receivableDao().getAllReceivables().collect { receivableList ->
            receivables = receivableList
            totalPending = receivableList.filter { it.status != "paid" }.sumOf { it.balance }
            pendingCount = receivableList.count { it.status != "paid" }
            totalCollected = receivableList.sumOf { it.totalAmount - it.balance }
        }
        db.clientDao().getAllClients().collect { clients = it }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cuentas por Cobrar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAddClient = true }) {
                    Text("+ Cliente")
                }
                Button(onClick = { showAddReceivable = true }) {
                    Text("+ Cuenta")
                }
            }
        }
        
        // KPIs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pendiente", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(totalPending)}", fontWeight = FontWeight.Bold, color = Red)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Cuentas", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$pendingCount", fontWeight = FontWeight.Bold, color = Yellow)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Cobrado", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(totalCollected)}", fontWeight = FontWeight.Bold, color = Green)
                }
            }
        }
        
        // List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(receivables) { receivable ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(receivable.clientName, fontWeight = FontWeight.SemiBold)
                                Text(receivable.concept, style = MaterialTheme.typography.bodySmall)
                                Text(Utils.formatDate(receivable.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$${Utils.formatCurrency(receivable.balance)}",
                                    fontWeight = FontWeight.Bold,
                                    color = when (receivable.status) {
                                        "paid" -> Green
                                        "partial" -> Yellow
                                        else -> Red
                                    }
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            when (receivable.status) {
                                                "paid" -> "Pagado"
                                                "partial" -> "Parcial"
                                                else -> "Pendiente"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                        
                        if (receivable.status != "paid") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showPaymentDialog = receivable },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Abonar")
                                }
                            }
                        }
                        
                        if (receivable.payments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Abonos:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            receivable.payments.forEach { payment ->
                                Text(
                                    "${Utils.formatDate(payment.date)}: $${Utils.formatCurrency(payment.amount)} - ${payment.method}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add Client Dialog
    if (showAddClient) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddClient = false },
            title = { Text("Nuevo Cliente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Dirección") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.clientDao().insert(Client(name = name, phone = phone, email = email, address = address))
                        showAddClient = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddClient = false }) { Text("Cancelar") }
            }
        )
    }
    
    // Add Receivable Dialog
    if (showAddReceivable) {
        var clientName by remember { mutableStateOf("") }
        var concept by remember { mutableStateOf("") }
        var totalAmount by remember { mutableStateOf("") }
        var clientExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showAddReceivable = false },
            title = { Text("Nueva Cuenta por Cobrar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        OutlinedButton(onClick = { clientExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(clientName.ifEmpty { "Seleccionar Cliente" })
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = clientExpanded, onDismissRequest = { clientExpanded = false }) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.name) },
                                    onClick = { clientName = client.name; clientExpanded = false }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(value = concept, onValueChange = { concept = it }, label = { Text("Concepto") })
                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("Monto Total") },
                        leadingIcon = { Text("$") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val amount = totalAmount.toDoubleOrNull() ?: 0.0
                        db.receivableDao().insert(
                            Receivable(
                                clientName = clientName,
                                concept = concept,
                                totalAmount = amount,
                                balance = amount,
                                status = "pending"
                            )
                        )
                        showAddReceivable = false
                    }
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddReceivable = false }) { Text("Cancelar") }
            }
        )
    }
    
    // Payment Dialog
    showPaymentDialog?.let { receivable ->
        var paymentAmount by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("Efectivo") }
        var methodExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showPaymentDialog = null },
            title = { Text("Registrar Abono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cliente: ${receivable.clientName}")
                    Text("Pendiente: $${Utils.formatCurrency(receivable.balance)}")
                    
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Monto a abonar") },
                        leadingIcon = { Text("$") }
                    )
                    
                    Box {
                        OutlinedButton(onClick = { methodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Método: $paymentMethod")
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = { paymentMethod = method; methodExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && amount <= receivable.balance) {
                            val newBalance = receivable.balance - amount
                            val newStatus = when {
                                newBalance == 0.0 -> "paid"
                                newBalance < receivable.totalAmount -> "partial"
                                else -> "pending"
                            }
                            
                            val updatedPayments = receivable.payments + Payment(amount = amount, method = paymentMethod)
                            db.receivableDao().update(
                                receivable.copy(
                                    balance = newBalance,
                                    status = newStatus,
                                    payments = updatedPayments
                                )
                            )
                            
                            if (paymentMethod == "Efectivo") {
                                db.cashMovementDao().insert(
                                    CashMovement(
                                        type = "Ingreso",
                                        amount = amount,
                                        description = "Abono: ${receivable.clientName} - ${receivable.concept}"
                                    )
                                )
                            }
                            
                            showPaymentDialog = null
                        }
                    }
                }) { Text("Registrar Abono") }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = null }) { Text("Cancelar") }
            }
        )
    }
}