package com.nexus.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.*
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen() {
    val context = LocalContext.current; val db = remember { AppDatabase.getDatabase(context) }; val scope = rememberCoroutineScope()
    var restocks by remember { mutableStateOf<List<Restock>>(emptyList()) }; var showAddDialog by remember { mutableStateOf(false) }
    var monthRestocks by remember { mutableDoubleStateOf(0.0) }; var productsRestocked by remember { mutableIntStateOf(0) }; var restockCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { db.restockDao().getAllRestocks().collect { restocks = it.sortedByDescending { r -> r.date }; val (s, e) = Utils.getMonthRange(); val m = it.filter { r -> r.date in s..e }; monthRestocks = m.sumOf { r -> r.total }; productsRestocked = m.sumOf { r -> r.products.size }; restockCount = m.size } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Reabastecimiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Button(onClick = { showAddDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Compra", style = MaterialTheme.typography.bodySmall) } }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Card(Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) { Text("Compras Mes", style = MaterialTheme.typography.bodySmall, color = Gray500); Text("$${Utils.formatCurrency(monthRestocks)}", fontWeight = FontWeight.Bold, color = Blue) } }; Card(Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) { Text("Productos", style = MaterialTheme.typography.bodySmall, color = Gray500); Text("$productsRestocked", fontWeight = FontWeight.Bold) } }; Card(Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) { Text("Compras", style = MaterialTheme.typography.bodySmall, color = Gray500); Text("$restockCount", fontWeight = FontWeight.Bold) } } }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(restocks) { r -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(r.supplier, fontWeight = FontWeight.SemiBold); Text(Utils.formatDate(r.date), style = MaterialTheme.typography.bodySmall, color = Gray500) }; Text(r.products.joinToString(", ") { "${it.name} x${it.quantity}" }, style = MaterialTheme.typography.bodySmall); Text("Total: $${Utils.formatCurrency(r.total)}", fontWeight = FontWeight.Bold, color = Green) } } } }
    }

    if (showAddDialog) {
        var supplier by remember { mutableStateOf("") }; var selectedProducts by remember { mutableStateOf<MutableMap<Long, Triple<Product, Int, Double>>>(mutableMapOf()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }; var showProductPicker by remember { mutableStateOf(false) }; var discountFromCash by remember { mutableStateOf(false) }; var cashBalance by remember { mutableDoubleStateOf(0.0) }
        LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it }; cashBalance = db.cashMovementDao().getAllMovements().first().sumOf { if (it.type == "Ingreso") it.amount else -it.amount } }
        val total = selectedProducts.values.sumOf { (_, qty, cost) -> cost * qty }

        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("Nuevo Reabastecimiento") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(supplier, { supplier = it }, label = { Text("Proveedor *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                selectedProducts.forEach { (id, triple) -> val (product, qty, cost) = triple; Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.Medium); Text("Costo: $${Utils.formatCurrency(cost)} | Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall) }
                    var qtyText by remember { mutableStateOf(qty.toString()) }; OutlinedTextField(qtyText, { val f = it.filter { c -> c.isDigit() }; qtyText = f; val nq = f.toIntOrNull() ?: 1; if (nq > 0) selectedProducts = selectedProducts.toMutableMap().also { it[id] = Triple(product, nq, cost) } }, modifier = Modifier.width(70.dp), singleLine = true, keyboardOptions = KeyboardOptions(KeyboardType.Number), label = { Text("Cant") }, textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it[id] = Triple(product, qty + 1, cost) } }, modifier = Modifier.size(20.dp)) { Icon(Icons.Filled.KeyboardArrowUp, "Más", modifier = Modifier.size(16.dp)) }; IconButton(onClick = { if (qty > 1) selectedProducts = selectedProducts.toMutableMap().also { it[id] = Triple(product, qty - 1, cost) } }, modifier = Modifier.size(20.dp)) { Icon(Icons.Filled.KeyboardArrowDown, "Menos", modifier = Modifier.size(16.dp)) } }
                    Text("$${Utils.formatCurrency(cost * qty)}", fontWeight = FontWeight.Bold, color = Blue, modifier = Modifier.padding(start = 4.dp)); IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it.remove(id) } }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Red, modifier = Modifier.size(22.dp)) } } } }
                OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)); Text("Agregar Producto") }
                if (selectedProducts.isNotEmpty()) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) { Text("Total: $${Utils.formatCurrency(total)}", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = GreenDark) }; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(discountFromCash, { discountFromCash = it }); Text("Descontar de caja") }; Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (discountFromCash && total > cashBalance) RedLight else BlueLight)) { Column(Modifier.padding(12.dp)) { Text("Saldo: $${Utils.formatCurrency(cashBalance)}", fontWeight = FontWeight.Medium); if (discountFromCash && total > cashBalance) Text("⚠️ Saldo insuficiente", color = Red, style = MaterialTheme.typography.bodySmall) } } }
            }
        }, confirmButton = { Button(onClick = { scope.launch { if (selectedProducts.isNotEmpty() && supplier.isNotBlank()) { val list = selectedProducts.values.map { (p, q, c) -> RestockProduct(p.id, p.name, q, c) }; val t = list.sumOf { it.cost * it.quantity }; db.restockDao().insert(Restock(supplier = supplier, products = list, total = t, discountFromCash = discountFromCash)); selectedProducts.values.forEach { (p, q, _) -> db.productDao().update(p.copy(stock = p.stock + q)) }; if (discountFromCash) db.cashMovementDao().insert(CashMovement("Egreso", t, "Reabastecimiento: $supplier")); showAddDialog = false; Toast.makeText(context, "✅ Registrado", Toast.LENGTH_SHORT).show() } } }, enabled = selectedProducts.isNotEmpty() && supplier.isNotBlank()) { Text("Registrar Compra") } }, dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } })

        if (showProductPicker) {
            var s by remember { mutableStateOf("") }; val f = remember(allProducts, s) { if (s.isEmpty()) allProducts else allProducts.filter { it.name.contains(s, true) || it.sku.contains(s, true) } }
            AlertDialog(onDismissRequest = { showProductPicker = false }, title = { Text("Productos") }, text = { Column { OutlinedTextField(s, { s = it }, label = { Text("Buscar por nombre o código...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); LazyColumn(Modifier.heightIn(max = 400.dp)) { items(f) { p -> ListItem(headlineContent = { Row(verticalAlignment = Alignment.CenterVertically) { Text(p.name); if (p.stock <= p.minStock) { Spacer(Modifier.width(8.dp)); SuggestionChip(onClick = {}, label = { Text("Bajo") }) } } }, supportingContent = { Text("SKU: ${p.sku.ifEmpty { "N/A" }} | Stock: ${p.stock} | Costo: $${Utils.formatCurrency(p.cost)}") }, modifier = Modifier.clickable { if (!selectedProducts.containsKey(p.id)) { val sq = if (p.stock < p.minStock) (p.minStock * 2) - p.stock else 1; selectedProducts = selectedProducts.toMutableMap().also { it[p.id] = Triple(p, sq, p.cost) }; showProductPicker = false } }) } } }, confirmButton = { TextButton(onClick = { showProductPicker = false }) { Text("Listo") } })
        }
    }
}
