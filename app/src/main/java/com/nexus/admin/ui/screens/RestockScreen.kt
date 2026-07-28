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
            restocks = list.sortedByDescending { it.date }
            val (start, end) = Utils.getMonthRange()
            val month = list.filter { it.date in start..end }
            monthRestocks = month.sumOf { it.total }
            productsRestocked = month.sumOf { it.products.size }
            restockCount = month.size
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header responsivo
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reabastecimiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Compra", style = MaterialTheme.typography.bodySmall)
            }
        }

        // KPIs
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Compras Mes", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(monthRestocks)}", fontWeight = FontWeight.Bold, color = Blue, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Productos", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$productsRestocked", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Compras", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$restockCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Historial
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(restocks) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.supplier, fontWeight = FontWeight.SemiBold)
                            Text(Utils.formatDate(r.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text(r.products.joinToString(", ") { "${it.name} x${it.quantity}" }, style = MaterialTheme.typography.bodySmall)
                        Text("Total: $${Utils.formatCurrency(r.total)}", fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }
        }
    }

    // Add Restock Dialog
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
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("Proveedor *") }, singleLine = true)

                    // Productos seleccionados con cantidades manuales
                    selectedProducts.forEach { (id, pair) ->
                        val (product, qty) = pair
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    Text("Costo: $${Utils.formatCurrency(product.cost)} | Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall)
                                }
                                // Selector de cantidad manual
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (qty > 1) selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty - 1) } },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Filled.Remove, "Menos", modifier = Modifier.size(18.dp)) }
                                    Text("$qty", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty + 1) } },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Filled.Add, "Más", modifier = Modifier.size(18.dp)) }
                                }
                                Text("$${Utils.formatCurrency(product.cost * qty)}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                                IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it.remove(id) } }) {
                                    Icon(Icons.Filled.Delete, "Eliminar", tint = Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Text("Agregar Producto")
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

                            // ACTUALIZAR STOCK DEL INVENTARIO
                            selectedProducts.values.forEach { (product, qty) ->
                                db.productDao().update(product.copy(stock = product.stock + qty))
                            }

                            showAddDialog = false
                        }
                    }
                }, enabled = selectedProducts.isNotEmpty() && supplier.isNotBlank()) { Text("Registrar Compra") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )

        // Product Picker
        if (showProductPicker) {
            var searchProd by remember { mutableStateOf("") }
            val filtered = remember(allProducts, searchProd) {
                if (searchProd.isEmpty()) allProducts
                else allProducts.filter { it.name.contains(searchProd, true) || it.sku.contains(searchProd, true) }
            }
            AlertDialog(
                onDismissRequest = { showProductPicker = false },
                title = { Text("Seleccionar Producto") },
                text = {
                    Column {
                        OutlinedTextField(searchProd, { searchProd = it }, label = { Text("Buscar...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        LazyColumn {
                            items(filtered) { product ->
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
                    }
                },
                confirmButton = { TextButton(onClick = { showProductPicker = false }) { Text("Cancelar") } }
            )
        }
    }
}
