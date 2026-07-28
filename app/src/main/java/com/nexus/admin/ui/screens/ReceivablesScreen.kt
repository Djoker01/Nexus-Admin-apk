package com.nexus.admin.ui.screens

import androidx.compose.foundation.clickable
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
    var showPayment by remember { mutableStateOf<Receivable?>(null) }

    var totalPending by remember { mutableDoubleStateOf(0.0) }
    var pendingCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.receivableDao().getAllReceivables().collect {
            receivables = it
            totalPending = it.filter { r -> r.status != "paid" }.sumOf { r -> r.balance }
            pendingCount = it.count { r -> r.status != "paid" }
        }
        db.clientDao().getAllClients().collect { clients = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ctas por Cobrar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAddClient = true }) { Text("+ Cliente") }
                Button(onClick = { showAddReceivable = true }) { Text("+ Cuenta") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Pendiente", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(totalPending)}", fontWeight = FontWeight.Bold, color = Red)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Cuentas", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$pendingCount", fontWeight = FontWeight.Bold, color = Yellow)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(receivables) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(r.clientName, fontWeight = FontWeight.SemiBold)
                                Text(r.concept, style = MaterialTheme.typography.bodySmall)
                                Text(Utils.formatDate(r.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${Utils.formatCurrency(r.balance)}", fontWeight = FontWeight.Bold,
                                    color = when(r.status) { "paid" -> Green; "partial" -> Yellow; else -> Red })
                                SuggestionChip(onClick = {}, label = {
                                    Text(when(r.status) { "paid" -> "Pagado"; "partial" -> "Parcial"; else -> "Pendiente" })
                                })
                            }
                        }
                        if (r.status != "paid") {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = { showPayment = r }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                                    Icon(Icons.Filled.Payments, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Abonar")
                                }
                            }
                        }
                        if (r.payments.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Abonos:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            r.payments.forEach { p ->
                                Text("${Utils.formatDate(p.date)}: $${Utils.formatCurrency(p.amount)} - ${p.method}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Client Dialog
    if (showAddClient) {
        var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddClient = false },
            title = { Text("Nuevo Cliente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre *") })
                    OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") })
                    OutlinedTextField(email, { email = it }, label = { Text("Email") })
                    OutlinedTextField(address, { address = it }, label = { Text("Dirección") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (name.isNotBlank()) {
                            db.clientDao().insert(Client(name = name, phone = phone, email = email, address = address))
                            showAddClient = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddClient = false }) { Text("Cancelar") } }
        )
    }

    // Add Receivable Dialog - CORREGIDO con selección de cliente
    if (showAddReceivable) {
        var selectedClient by remember { mutableStateOf<Client?>(null) }
        var concept by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var clientExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddReceivable = false },
            title = { Text("Nueva Cuenta por Cobrar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Selector de cliente mejorado
                    ExposedDropdownMenuBox(
                        expanded = clientExpanded,
                        onExpandedChange = { clientExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedClient?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cliente *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = clientExpanded,
                            onDismissRequest = { clientExpanded = false }
                        ) {
                            if (clients.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay clientes registrados") },
                                    onClick = { clientExpanded = false }
                                )
                            } else {
                                clients.forEach { client ->
                                    DropdownMenuItem(
                                        text = { Text(client.name) },
                                        onClick = {
                                            selectedClient = client
                                            clientExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(concept, { concept = it }, label = { Text("Concepto") })
                    OutlinedTextField(amount, { amount = it }, label = { Text("Monto Total") }, leadingIcon = { Text("$") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (selectedClient != null && amt > 0 && concept.isNotBlank()) {
                            db.receivableDao().insert(
                                Receivable(
                                    clientName = selectedClient!!.name,
                                    concept = concept,
                                    totalAmount = amt,
                                    balance = amt,
                                    status = "pending"
                                )
                            )
                            showAddReceivable = false
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showAddReceivable = false }) { Text("Cancelar") } }
        )
    }

    // Payment Dialog
    showPayment?.let { receivable ->
        var payAmount by remember { mutableStateOf("") }
        var payMethod by remember { mutableStateOf("Efectivo") }
        var methodExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPayment = null },
            title = { Text("Registrar Abono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cliente: ${receivable.clientName}")
                    Text("Pendiente: $${Utils.formatCurrency(receivable.balance)}")
                    OutlinedTextField(payAmount, { payAmount = it }, label = { Text("Monto") }, leadingIcon = { Text("$") }, singleLine = true)
                    
                    Box {
                        OutlinedButton(onClick = { methodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Método: $payMethod"); Icon(Icons.Filled.ArrowDropDown, null)
                        }
                        DropdownMenu(methodExpanded, { methodExpanded = false }) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach { m ->
                                DropdownMenuItem(text = { Text(m) }, onClick = { payMethod = m; methodExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && amt <= receivable.balance) {
                            val newBalance = receivable.balance - amt
                            val newStatus = when {
                                newBalance == 0.0 -> "paid"
                                newBalance < receivable.totalAmount -> "partial"
                                else -> "pending"
                            }
                            db.receivableDao().update(receivable.copy(
                                balance = newBalance,
                                status = newStatus,
                                payments = receivable.payments + Payment(amount = amt, date = System.currentTimeMillis(), method = payMethod)
                            ))
                            if (payMethod == "Efectivo") {
                                db.cashMovementDao().insert(
    CashMovement(
        type = "Ingreso",
        amount = amt,
        description = "Abono: ${receivable.clientName}",
        date = System.currentTimeMillis()
    )
)
                            showPayment = null
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showPayment = null }) { Text("Cancelar") } }
        )
    }
}
