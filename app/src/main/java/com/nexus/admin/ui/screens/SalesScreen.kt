package com.nexus.admin.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
        else sales.filter { Utils.formatDate(it.date).contains(filterDate, ignoreCase = true) }
    }

    // Escáner
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("SCAN_RESULT")?.let { barcode ->
                // Buscar producto por SKU
                scope.launch {
                    db.productDao().getAllProducts().collect { products ->
                        val found = products.find { it.sku == barcode }
                        if (found != null) {
                            Toast.makeText(context, "Producto encontrado: ${found.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showSaleDialog = true }) {
                Icon(Icons.Filled.ShoppingCart, null)
                Spacer(Modifier.width(4.dp))
                Text("Nueva Venta")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Ventas Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(todaySales)}", fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Transacciones", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$transactionCount", fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            filterDate,
            { filterDate = it },
            label = { Text("Filtrar por fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSales) { sale ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sale.client.ifEmpty { "General" }, fontWeight = FontWeight.SemiBold)
                            Text(Utils.formatDate(sale.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text(
                            sale.products.joinToString(", ") { "${it.name} x${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall
                        )
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
        var selectedProducts by remember { mutableStateOf<MutableMap<Long, Pair<Product, Int>>>(mutableMapOf()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it } }

        val total = selectedProducts.values.sumOf { (p, qty) -> p.price * qty }

        AlertDialog(
            onDismissRequest = { showSaleDialog = false },
            title = { Text("Nueva Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(client, { client = it }, label = { Text("Cliente") }, singleLine = true)
                    
                    var payExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { payExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pago: $paymentMethod")
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                        DropdownMenu(payExpanded, { payExpanded = false }) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = { paymentMethod = it; payExpanded = false }
                                )
                            }
                        }
                    }

                    // Productos seleccionados con cantidades
                    selectedProducts.forEach { (id, pair) ->
                        val (product, qty) = pair
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Precio: $${Utils.formatCurrency(product.price)} | Stock: ${product.stock}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                // Selector de cantidad
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (qty > 1) {
                                                selectedProducts = selectedProducts.toMutableMap().also {
                                                    it[id] = product to (qty - 1)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, "Menos", modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        "$qty",
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            if (qty < product.stock) {
                                                selectedProducts = selectedProducts.toMutableMap().also {
                                                    it[id] = product to (qty + 1)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, "Más", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    "$${Utils.formatCurrency(product.price * qty)}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                IconButton(onClick = {
                                    selectedProducts = selectedProducts.toMutableMap().also { it.remove(id) }
                                }) {
                                    Icon(Icons.Filled.Delete, "Eliminar", tint = Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Botones Agregar y Escanear
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showProductPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Text("Agregar")
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                                        putExtra("SCAN_MODE", "PRODUCT_MODE")
                                    }
                                    scannerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "App de escáner no disponible", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null)
                            Text("Escanear")
                        }
                    }

                    Text(
                        "Total: $${Utils.formatCurrency(total)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (selectedProducts.isNotEmpty()) {
                                val productsList = selectedProducts.values.map { (p, q) ->
                                    SaleProduct(p.id, p.name, q, p.price, p.cost)
                                }
                                val totalAmount = productsList.sumOf { it.price * it.quantity }
                                val totalCost = productsList.sumOf { it.cost * it.quantity }

                                val sale = Sale(
                                    client = client,
                                    products = productsList,
                                    total = totalAmount,
                                    cost = totalCost,
                                    paymentMethod = paymentMethod
                                )
                                db.saleDao().insert(sale)

                                // Actualizar stock
                                selectedProducts.values.forEach { (product, qty) ->
                                    db.productDao().update(product.copy(stock = product.stock - qty))
                                }

                                // Registrar en caja si es efectivo
                                if (paymentMethod == "Efectivo") {
                                    db.cashMovementDao().insert(
                                        CashMovement(
                                            type = "Ingreso",
                                            amount = totalAmount,
                                            description = "Venta - ${productsList.size} productos"
                                        )
                                    )
                                }

                                showSaleDialog = false
                            }
                        }
                    },
                    enabled = selectedProducts.isNotEmpty()
                ) { Text("Completar Venta") }
            },
            dismissButton = { TextButton(onClick = { showSaleDialog = false }) { Text("Cancelar") } }
        )

        // Product Picker
        if (showProductPicker) {
            AlertDialog(
                onDismissRequest = { showProductPicker = false },
                title = { Text("Seleccionar Producto") },
                text = {
                    LazyColumn {
                        items(allProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = {
                                    Text("Stock: ${product.stock} | $${Utils.formatCurrency(product.price)}")
                                },
                                modifier = Modifier.clickable {
                                    if (product.stock > 0 && !selectedProducts.containsKey(product.id)) {
                                        selectedProducts = selectedProducts.toMutableMap().also {
                                            it[product.id] = product to 1
                                        }
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
