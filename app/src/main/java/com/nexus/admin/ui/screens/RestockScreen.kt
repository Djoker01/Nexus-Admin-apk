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
fun RestockScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var restocks by remember { mutableStateOf<List<Restock>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var monthRestocks by remember { mutableDoubleStateOf(0.0) }
    var productsRestocked by remember { mutableIntStateOf(0) }
    var restockCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.restockDao().getAllRestocks().collect { list ->
            restocks = list
            val (start, end) = Utils.getMonthRange()
            val month = list.filter { it.date in start..end }
            monthRestocks = month.sumOf { it.total }
            productsRestocked = month.sumOf { it.products.size }
            restockCount = month.size
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Reabastecimiento", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) { Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.width(4.dp)); Text("Nueva Compra") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Compras Mes"); Text("$${Utils.formatCurrency(monthRestocks)}", fontWeight = FontWeight.Bold, color = Blue) } }
            Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Productos"); Text("$productsRestocked", fontWeight = FontWeight.Bold) } }
            Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Compras"); Text("$restockCount", fontWeight = FontWeight.Bold) } }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(restocks) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.supplier, fontWeight = FontWeight.SemiBold)
                        Text(r.products.joinToString(", ") { "${it.name} x${it.quantity}" }, style = MaterialTheme.typography.bodySmall)
                        Text("Total: $${Utils.formatCurrency(r.total)}", fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var supplier by remember { mutableStateOf("") }
        var selectedProducts by remember { mutableStateOf<MutableMap<Long, Pair<Product, Int>>>(mutableMapOf()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it } }

        val total = selectedProducts.values.sumOf { (p, q) -> p.cost * q }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Reabastecimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("Proveedor") }, singleLine = true)

                    selectedProducts.forEach { (id, pair) ->
                        val (product, qty) = pair
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium)
                                    Text("Costo: $${Utils.formatCurrency(product.cost)}", style = MaterialTheme.typography.bodySmall)
                                }
                                // Cantidad manual
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (qty > 1) selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty - 1) } }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Remove, "Menos", modifier = Modifier.size(18.dp))
                                    }
                                    Text("$qty", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty + 1) } }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Add, "Más", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text("$${Utils.formatCurrency(product.cost * qty)}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                                IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it.remove(id) } }) {
                                    Icon(Icons.Filled.Delete, "Eliminar", tint = Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null); Text("Agregar Producto")
                    }

                    Text("Total: $${Utils.formatCurrency(total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (selectedProducts.isNotEmpty() && supplier.isNotBlank()) {
                            val list = selectedProducts.values.map { (p, q) -> RestockProduct(p.id, p.name, q, p.cost) }
                            val t = list.sumOf { it.cost * it.quantity }
                            db.restockDao().insert(Restock(supplier = supplier, products = list, total = t))
                            selectedProducts.forEach { (_, pair) -> db.productDao().update(pair.first.copy(stock = pair.first.stock + pair.second)) }
                            showAddDialog = false
                        }
                    }
                }, enabled = selectedProducts.isNotEmpty()) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )

        if (showProductPicker) {
            AlertDialog(
                onDismissRequest = { showProductPicker = false },
                title = { Text("Seleccionar Producto") },
                text = {
                    LazyColumn {
                        items(allProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = { Text("Stock: ${product.stock} | Costo: $${Utils.formatCurrency(product.cost)}") },
                                modifier = Modifier.clickable {
                                    if (!selectedProducts.containsKey(product.id)) {
                                        selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = product to 1 }
                                    }
                                    showProductPicker = false
                                }
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showProductPicker = false }) { Text("Cancelar") } }
            )
        }
    }
}
