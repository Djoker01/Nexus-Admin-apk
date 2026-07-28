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
import com.nexus.admin.data.entity.Product
import com.nexus.admin.data.entity.Shrinkage
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShrinkageScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var shrinkages by remember { mutableStateOf<List<Shrinkage>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("") }

    val types = listOf(
        "Merma/Desperdicio", "Consumo Personal", "Producto Caducado",
        "Robo/Pérdida", "Error de Inventario", "Otro"
    )

    var todayLoss by remember { mutableDoubleStateOf(0.0) }
    var monthLoss by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        db.shrinkageDao().getAllShrinkages().collect { list ->
            shrinkages = list.sortedByDescending { it.date }
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val (monthStart, monthEnd) = Utils.getMonthRange()
            todayLoss = list.filter { it.date in todayStart..todayEnd }.sumOf { it.loss }
            monthLoss = list.filter { it.date in monthStart..monthEnd }.sumOf { it.loss }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header responsivo
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mermas y Consumo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Registrar", style = MaterialTheme.typography.bodySmall)
            }
        }

        // KPIs
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Today, null, tint = Red, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(todayLoss)}", fontWeight = FontWeight.Bold, color = Red)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = Yellow, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Mes", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(monthLoss)}", fontWeight = FontWeight.Bold, color = Yellow)
                }
            }
        }

        // Filtros de tipo
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(selected = selectedType.isEmpty(), onClick = { selectedType = "" }, label = { Text("Todas", style = MaterialTheme.typography.labelSmall) })
            types.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = if (selectedType == type) "" else type },
                    label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Lista
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shrinkages.filter { selectedType.isEmpty() || it.type == selectedType }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.productName, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(onClick = {}, label = { Text(s.type, style = MaterialTheme.typography.labelSmall) })
                                Text("Cant: ${s.quantity}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Utils.formatDate(s.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text("-$${Utils.formatCurrency(s.loss)}", fontWeight = FontWeight.Bold, color = Red)
                    }
                }
            }
        }
    }

    // Add Shrinkage Dialog
    if (showAddDialog) {
        var products by remember { mutableStateOf<List<Product>>(emptyList()) }
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var selectedShrinkageType by remember { mutableStateOf(types[0]) }
        var quantity by remember { mutableStateOf("1") }
        var typeExpanded by remember { mutableStateOf(false) }
        var prodExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { products = it } }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Merma") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Selector de producto
                    ExposedDropdownMenuBox(prodExpanded, { prodExpanded = it }) {
                        OutlinedTextField(
                            selectedProduct?.name ?: "",
                            {},
                            readOnly = true,
                            label = { Text("Producto *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(prodExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(prodExpanded, { prodExpanded = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (Stock: ${p.stock})") },
                                    onClick = { selectedProduct = p; prodExpanded = false }
                                )
                            }
                        }
                    }

                    // Selector de tipo
                    ExposedDropdownMenuBox(typeExpanded, { typeExpanded = it }) {
                        OutlinedTextField(
                            selectedShrinkageType,
                            {},
                            readOnly = true,
                            label = { Text("Tipo *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(typeExpanded, { typeExpanded = false }) {
                            types.forEach { t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = { selectedShrinkageType = t; typeExpanded = false })
                            }
                        }
                    }

                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Cantidad *") }, singleLine = true)

                    selectedProduct?.let { product ->
                        val loss = product.cost * (quantity.toIntOrNull() ?: 1)
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RedLight)) {
                            Text(
                                "Pérdida estimada: $${Utils.formatCurrency(loss)}",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                color = Red
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        selectedProduct?.let { product ->
                            val qty = quantity.toIntOrNull() ?: 1
                            if (qty > 0 && qty <= product.stock) {
                                db.shrinkageDao().insert(
                                    Shrinkage(
                                        productId = product.id,
                                        productName = product.name,
                                        type = selectedShrinkageType,
                                        quantity = qty,
                                        loss = product.cost * qty
                                    )
                                )
                                db.productDao().update(product.copy(stock = product.stock - qty))
                                showAddDialog = false
                            }
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}
