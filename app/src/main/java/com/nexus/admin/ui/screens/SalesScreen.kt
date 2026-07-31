package com.nexus.admin.ui.screens

import android.widget.Toast
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
import com.nexus.admin.ui.components.FloatingBarcodeScanner
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var showSaleDialog by remember { mutableStateOf(false) }
    var editingSale by remember { mutableStateOf<Sale?>(null) }
    var filterDate by remember { mutableStateOf("") }
    var todaySales by remember { mutableDoubleStateOf(0.0) }
    var todayProfit by remember { mutableDoubleStateOf(0.0) }
    var transactionCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.saleDao().getAllSales().collect { saleList ->
            sales = saleList.sortedByDescending { it.date }
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val todayList = saleList.filter { it.date in todayStart..todayEnd }
            todaySales = todayList.filter { !it.isReturned }.sumOf { it.total }
            todayProfit = todayList.filter { !it.isReturned }.sumOf { it.total - it.cost }
            transactionCount = todayList.filter { !it.isReturned }.size
        }
    }

    val filteredSales = remember(sales, filterDate) {
        if (filterDate.isEmpty()) sales
        else sales.filter { Utils.formatDate(it.date).contains(filterDate, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { editingSale = null; showSaleDialog = true }) {
                Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva Venta")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Ventas Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(todaySales)}", fontWeight = FontWeight.Bold, color = Green, style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Ganancias", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(todayProfit)}", fontWeight = FontWeight.Bold, color = Blue, style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Trans.", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$transactionCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        OutlinedTextField(
            filterDate, { filterDate = it },
            label = { Text("Filtrar por fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.DateRange, null) },
            trailingIcon = { if (filterDate.isNotEmpty()) IconButton(onClick = { filterDate = "" }) { Icon(Icons.Filled.Clear, "Limpiar") } }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredSales.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PointOfSale, null, modifier = Modifier.size(64.dp), tint = Gray300)
                            Spacer(Modifier.height(16.dp))
                            Text("No hay ventas registradas", color = Gray500)
                        }
                    }
                }
            }
            items(filteredSales) { sale ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(sale.client.ifEmpty { "Cliente General" }, fontWeight = FontWeight.SemiBold)
                                    if (sale.isReceivable) { Spacer(Modifier.width(8.dp)); SuggestionChip(onClick = {}, label = { Text("Crédito", style = MaterialTheme.typography.labelSmall) }) }
                                    if (sale.isReturned) { Spacer(Modifier.width(8.dp)); SuggestionChip(onClick = {}, label = { Text("Devuelto", style = MaterialTheme.typography.labelSmall) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RedLight)) }
                                }
                                Text(Utils.formatDate(sale.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${Utils.formatCurrency(sale.total)}", fontWeight = FontWeight.Bold, color = if (sale.isReturned) Red else Green, style = MaterialTheme.typography.titleMedium)
                                Text(sale.paymentMethods.ifEmpty { sale.paymentMethod }, style = MaterialTheme.typography.labelSmall, color = Gray500)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(sale.products.joinToString(", ") { "${it.name} x${it.quantity}" }, style = MaterialTheme.typography.bodySmall, color = Gray500)
                        if (!sale.isReturned && sale.total > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editingSale = sale; showSaleDialog = true }) {
                                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Editar/Devolución", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Venta (Nueva/Edición/Devolución)
    if (showSaleDialog) {
        var client by remember { mutableStateOf(editingSale?.client ?: "") }
        var isReceivable by remember { mutableStateOf(editingSale?.isReceivable ?: false) }
        var isReturned by remember { mutableStateOf(editingSale?.isReturned ?: false) }
        var cashAmount by remember { mutableStateOf("0") }
        var transferAmount by remember { mutableStateOf("0") }
        var selectedProducts by remember { mutableStateOf<MutableMap<Long, Pair<Product, Int>>>(mutableMapOf()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }
        var showScanner by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it } }

        val total = selectedProducts.values.sumOf { (p, qty) -> p.price * qty }

        AlertDialog(
            onDismissRequest = { showSaleDialog = false; editingSale = null },
            title = { Text(when { isReturned -> "Devolución de Venta"; editingSale != null -> "Editar Venta"; else -> "Nueva Venta" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(isReceivable, { isReceivable = it }); Text("¿Es cuenta por cobrar?") }
                    OutlinedTextField(client, { client = it }, label = { Text(if (isReceivable) "Cliente *" else "Cliente (opcional)") }, singleLine = true)
                    if (editingSale != null) { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(isReturned, { isReturned = it }); Text("¿Registrar devolución?", color = Red) } }
                    
                    HorizontalDivider()
                    
                    selectedProducts.forEach { (id, pair) ->
                        val (product, qty) = pair
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.Medium); Text("$${Utils.formatCurrency(product.price)} | Stock: ${product.stock}") }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (qty > 1) selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty - 1) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, "Menos", modifier = Modifier.size(20.dp)) }
                                    Text("$qty", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty + 1) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "Más", modifier = Modifier.size(20.dp)) }
                                }
                                Text("$${Utils.formatCurrency(product.price * qty)}", fontWeight = FontWeight.Bold, color = Green)
                                IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it.remove(id) } }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Red, modifier = Modifier.size(22.dp)) }
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)); Text("Agregar") }
                        Button(onClick = { showScanner = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(18.dp)); Text("Escanear") }
                    }
                    
                    if (selectedProducts.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) {
                            Text("Total: $${Utils.formatCurrency(total)}", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = GreenDark)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (selectedProducts.isNotEmpty()) {
                            val productsList = selectedProducts.values.map { (p, q) -> SaleProduct(p.id, p.name, q, p.price, p.cost) }
                            val totalAmount = productsList.sumOf { it.price * it.quantity }
                            val totalCost = productsList.sumOf { it.cost * it.quantity }

                            if (isReturned && editingSale != null) {
                                // CORREGIDO: Devolución como venta separada con total negativo
                                val returnSale = Sale(
                                    client = editingSale!!.client,
                                    products = productsList,
                                    total = -totalAmount,
                                    cost = -totalCost,
                                    paymentMethod = "Devolución",
                                    paymentMethods = "Devolución",
                                    isReceivable = false,
                                    isReturned = true
                                )
                                db.saleDao().insert(returnSale)
                                db.saleDao().update(editingSale!!.copy(isReturned = true))
                                selectedProducts.values.forEach { (product, qty) ->
                                    db.productDao().update(product.copy(stock = product.stock + qty))
                                }
                                db.cashMovementDao().insert(
                                    CashMovement(
                                        type = "Egreso",
                                        amount = totalAmount,
                                        description = "Devolución venta #${editingSale!!.id} - ${editingSale!!.client.ifEmpty { "General" }}",
                                        date = System.currentTimeMillis()
                                    )
                                )
                                Toast.makeText(context, "✅ Devolución registrada", Toast.LENGTH_SHORT).show()
                            } else if (editingSale != null) {
                                val originalSale = editingSale!!
                                originalSale.products.forEach { sp ->
                                    val product = allProducts.find { it.id == sp.productId }
                                    if (product != null) db.productDao().update(product.copy(stock = product.stock + sp.quantity))
                                }
                                selectedProducts.values.forEach { (product, qty) ->
                                    db.productDao().update(product.copy(stock = product.stock - qty))
                                }
                                db.saleDao().update(originalSale.copy(
                                    client = client,
                                    products = productsList,
                                    total = totalAmount,
                                    cost = totalCost,
                                    isReceivable = isReceivable
                                ))
                                Toast.makeText(context, "✅ Venta actualizada", Toast.LENGTH_SHORT).show()
                            } else {
                                val sale = Sale(
                                    client = client,
                                    products = productsList,
                                    total = totalAmount,
                                    cost = totalCost,
                                    paymentMethod = "Efectivo",
                                    paymentMethods = "Efectivo",
                                    isReceivable = isReceivable
                                )
                                db.saleDao().insert(sale)
                                selectedProducts.values.forEach { (product, qty) ->
                                    db.productDao().update(product.copy(stock = product.stock - qty))
                                }
                                if (totalAmount > 0) {
                                    db.cashMovementDao().insert(
                                        CashMovement(
                                            type = "Ingreso",
                                            amount = totalAmount,
                                            description = "Venta - ${productsList.size} productos${if (client.isNotBlank()) " - $client" else ""}",
                                            date = System.currentTimeMillis()
                                        )
                                    )
                                }
                                Toast.makeText(context, "✅ Venta registrada", Toast.LENGTH_SHORT).show()
                            }
                            showSaleDialog = false
                            editingSale = null
                        }
                    }
                }, enabled = selectedProducts.isNotEmpty()) {
                    Text(when { isReturned -> "Registrar Devolución"; editingSale != null -> "Actualizar"; else -> "Completar Venta" })
                }
            },
            dismissButton = { TextButton(onClick = { showSaleDialog = false; editingSale = null }) { Text("Cancelar") } }
        )

        // Escáner flotante
        if (showScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { barcode ->
                    scope.launch {
                        val prods = db.productDao().getAllProducts().first()
                        prods.find { it.sku == barcode }?.let { product ->
                            if (product.stock > 0 && !selectedProducts.containsKey(product.id)) {
                                selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = product to 1 }
                                Toast.makeText(context, "✅ ${product.name} agregado", Toast.LENGTH_SHORT).show()
                            }
                        } ?: Toast.makeText(context, "❌ No encontrado: $barcode", Toast.LENGTH_SHORT).show()
                    }
                    showScanner = false
                },
                onDismiss = { showScanner = false }
            )
        }

        // Selector de productos
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
                        LazyColumn(Modifier.heightIn(max = 400.dp)) {
                            items(filtered) { product ->
                                ListItem(
                                    headlineContent = { Text(product.name) },
                                    supportingContent = { Text("Stock: ${product.stock} | $${Utils.formatCurrency(product.price)}") },
                                    modifier = Modifier.clickable {
                                        if (product.stock > 0 && !selectedProducts.containsKey(product.id)) {
                                            selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = product to 1 }
                                            showProductPicker = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showProductPicker = false }) { Text("Listo") } }
            )
        }
    }
}
