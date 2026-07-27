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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var showSaleDialog by remember { mutableStateOf(false) }
    var filterDate by remember { mutableStateOf("") }
    var todaySales by remember { mutableDoubleStateOf(0.0) }
    var transactionCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.saleDao().getAllSales().collect { saleList ->
            sales = saleList.sortedByDescending { it.date }
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val todayList = saleList.filter { it.date in todayStart..todayEnd }
            todaySales = todayList.sumOf { it.total }
            transactionCount = todayList.size
        }
    }

    val filteredSales = remember(sales, filterDate) {
        if (filterDate.isEmpty()) sales
        else sales.filter { Utils.formatDate(it.date).contains(filterDate) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showSaleDialog = true }) { Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.width(4.dp)); Text("Nueva Venta") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) { Text("Ventas Hoy"); Text("$${Utils.formatCurrency(todaySales)}", fontWeight = FontWeight.Bold, color = Green) } }
            Card(modifier = Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) { Text("Transacciones"); Text("$transactionCount", fontWeight = FontWeight.Bold) } }
        }

        OutlinedTextField(filterDate, { filterDate = it }, label = { Text("Filtrar fecha (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true)

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredSales) { sale ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sale.client.ifEmpty { "General" }, fontWeight = FontWeight.SemiBold)
                            Text(Utils.formatDate(sale.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text(sale.products.joinToString(", ") { "${it.name} x${it.quantity}" }, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total: $${Utils.formatCurrency(sale.total)}", fontWeight = FontWeight.Bold, color = Green)
                            SuggestionChip(onClick = {}, label = { Text(sale.paymentMethod) })
                        }
                    }
                }
            }
        }
    }

    // New Sale Dialog
    if (showSaleDialog) {
        var client by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("Efectivo") }
        var selectedProducts by remember { mutableStateOf<MutableMap<Long, Triple<Product, Int, Double>>>(mutableMapOf()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it } }

        val total = selectedProducts.values.sumOf { (_, qty, price) -> price * qty }

        AlertDialog(
            onDismissRequest = { showSaleDialog = false },
            title = { Text("Nueva Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(client, { client = it }, label = { Text("Cliente") })
                    var payExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { payExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(paymentMethod); Icon(Icons.Filled.ArrowDropDown, null) }
                        DropdownMenu(payExpanded, { payExpanded = false }) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { paymentMethod = it; payExpanded = false })
                            }
                        }
                    }

                    selectedProducts.forEach { (_, triple) ->
                        val (product, qty, _) = triple
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Medium)
                                Text("Precio: $${Utils.formatCurrency(product.price)}", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedTextField(qty.toString(), { newQty ->
                                val q = newQty.toIntOrNull() ?: 1
                                if (q > 0 && q <= product.stock) {
                                    selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = Triple(product, q, product.price) }
                                }
                            }, modifier = Modifier.width(70.dp), singleLine = true)
                            Spacer(Modifier.width(8.dp))
                            Text("$${Utils.formatCurrency(product.price * qty)}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it.remove(product.id) } }) {
                                Icon(Icons.Filled.RemoveCircle, "Eliminar", tint = Red)
                            }
                        }
                    }

                    OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null); Text("Agregar Producto")
                    }
                    OutlinedButton(onClick = { /* Scanner */ }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.QrCodeScanner, null); Text("Escanear Producto")
                    }

                    Text("Total: $${Utils.formatCurrency(total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (selectedProducts.isNotEmpty()) {
                            val productsList = selectedProducts.values.map { (p, q, pr) -> SaleProduct(p.id, p.name, q, pr, p.cost) }
                            val totalAmount = productsList.sumOf { it.price * it.quantity }
                            val totalCost = productsList.sumOf { it.cost * it.quantity }
                            val sale = Sale(client = client, products = productsList, total = totalAmount, cost = totalCost, paymentMethod = paymentMethod)
                            db.saleDao().insert(sale)

                            // Update stock
                            selectedProducts.values.forEach { (product, qty, _) ->
                                db.productDao().update(product.copy(stock = product.stock - qty))
                            }

                            // Register in cash if cash
                            if (paymentMethod == "Efectivo") {
                                db.cashMovementDao().insert(CashMovement(type = "Ingreso", amount = totalAmount, description = "Venta - ${productsList.size} productos"))
                            }

                            showSaleDialog = false
                        }
                    }
                }, enabled = selectedProducts.isNotEmpty()) { Text("Completar Venta") }
            },
            dismissButton = { TextButton(onClick = { showSaleDialog = false }) { Text("Cancelar") } }
        )

        // Product picker
        if (showProductPicker) {
            AlertDialog(
                onDismissRequest = { showProductPicker = false },
                title = { Text("Seleccionar Producto") },
                text = {
                    LazyColumn {
                        items(allProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = { Text("Stock: ${product.stock} | $${Utils.formatCurrency(product.price)}") },
                                modifier = Modifier.clickable {
                                    if (product.stock > 0 && !selectedProducts.containsKey(product.id)) {
                                        selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = Triple(product, 1, product.price) }
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
