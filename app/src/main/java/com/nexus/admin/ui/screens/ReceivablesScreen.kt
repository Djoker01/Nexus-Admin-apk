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
            totalPending = it.filter { r -> r.status != "paid" }.sumOf { it.balance }
            pendingCount = it.count { r -> r.status != "paid" }
        }
        db.clientDao().getAllClients().collect { clients = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ctas por Cobrar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAddClient = true }) { Text("+ Cliente") }
                Button(onClick = { showAddReceivable = true }) { Text("+ Cuenta") }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Pendiente"); Text("$${Utils.formatCurrency(totalPending)}", fontWeight = FontWeight.Bold, color = Red) } }
            Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Cuentas"); Text("$pendingCount", fontWeight = FontWeight.Bold, color = Yellow) } }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(receivables) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(r.clientName, fontWeight = FontWeight.SemiBold); Text(r.concept, style = MaterialTheme.typography.bodySmall) }
                            Text("$${Utils.formatCurrency(r.balance)}", fontWeight = FontWeight.Bold, color = when(r.status) { "paid" -> Green; "partial" -> Yellow; else -> Red })
                        }
                        if (r.status != "paid") {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { showPayment = r }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { Text("Abonar") }
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

    // Add Receivable Dialog
    if (showAddReceivable) {
        var clientName by remember { mutableStateOf("") }; var concept by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }; var clientExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddReceivable = false },
            title = { Text("Nueva Cuenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        OutlinedButton(onClick = { clientExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(clientName.ifEmpty { "Seleccionar cliente" }); Icon(Icons.Filled.ArrowDropDown, null) }
                        DropdownMenu(clientExpanded, { clientExpanded = false }) {
                            clients.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { clientName = c.name; clientExpanded = false }) }
                        }
                    }
                    OutlinedTextField(concept, { concept = it }, label = { Text("Concepto") })
                    OutlinedTextField(amount, { amount = it }, label = { Text("Monto") }, leadingIcon = { Text("$") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val a = amount.toDoubleOrNull() ?: 0.0
                        if (clientName.isNotBlank() && a > 0) {
                            db.receivableDao().insert(Receivable(clientName = clientName, concept = concept, totalAmount = a, balance = a, status = "pending"))
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
        var payAmount by remember { mutableStateOf("") }; var payMethod by remember { mutableStateOf("Efectivo") }
        AlertDialog(
            onDismissRequest = { showPayment = null },
            title = { Text("Registrar Abono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pendiente: $${Utils.formatCurrency(receivable.balance)}")
                    OutlinedTextField(payAmount, { payAmount = it }, label = { Text("Monto") }, leadingIcon = { Text("$") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val a = payAmount.toDoubleOrNull() ?: 0.0
                        if (a > 0 && a <= receivable.balance) {
                            val newBalance = receivable.balance - a
                            val newStatus = when { newBalance == 0.0 -> "paid"; newBalance < receivable.totalAmount -> "partial"; else -> "pending" }
db.receivableDao().update(receivable.copy(
    balance = newBalance, 
    status = newStatus, 
    payments = receivable.payments + Payment(amount = a, date = System.currentTimeMillis(), method = payMethod)
))
if (payMethod == "Efectivo") {
    db.cashMovementDao().insert(
        CashMovement(
            type = "Ingreso", 
            amount = a, 
            description = "Abono: ${receivable.clientName} - ${receivable.concept}",
            date = System.currentTimeMillis()
        )
    )
}
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showPayment = null }) { Text("Cancelar") } }
        )
    }
}
