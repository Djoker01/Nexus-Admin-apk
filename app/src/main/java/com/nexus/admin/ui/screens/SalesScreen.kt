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
fun SalesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var showSaleDialog by remember { mutableStateOf(false) }
    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    var filterDate by remember { mutableStateOf("") }
    
    // Sales KPIs
    var todaySales by remember { mutableStateOf(0.0) }
    var monthSales by remember { mutableStateOf(0.0) }
    var transactionCount by remember { mutableStateOf(0) }
    var avgTicket by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        db.saleDao().getAllSales().collect { saleList ->
            sales = saleList
            
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val (monthStart, monthEnd) = Utils.getMonthRange()
            
            val todayList = saleList.filter { it.date in todayStart..todayEnd }
            todaySales = todayList.sumOf { it.total }
            transactionCount = todayList.size
            avgTicket = if (transactionCount > 0) todaySales / transactionCount else 0.0
            
            val monthList = saleList.filter { it.date in monthStart..monthEnd }
            monthSales = monthList.sumOf { it.total }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showSaleDialog = true }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva Venta")
            }
        }
        
        // KPIs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(todaySales)}", fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Mes", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(monthSales)}", fontWeight = FontWeight.Bold, color = Blue)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Trans.", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$transactionCount", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Ticket", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(avgTicket)}", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date filter
        OutlinedTextField(
            value = filterDate,
            onValueChange = { filterDate = it },
            label = { Text("Filtrar por fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )
        
        // Sales list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sales.filter { filterDate.isEmpty() || Utils.formatDate(it.date).contains(filterDate) }) { sale ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedSale = sale }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sale.client.ifEmpty { "Cliente General" }, fontWeight = FontWeight.SemiBold)
                            Text(Utils.formatDate(sale.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            sale.products.joinToString(", ") { "${it.name} x${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total: $${Utils.formatCurrency(sale.total)}", fontWeight = FontWeight.Bold, color = Green)
                            SuggestionChip(
                                onClick = {},
                                label = { Text(sale.paymentMethod, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Sale Detail Dialog
    selectedSale?.let { sale ->
        AlertDialog(
            onDismissRequest = { selectedSale = null },
            title = { Text("Detalle de Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fecha: ${Utils.formatDate(sale.date)}")
                    Text("Cliente: ${sale.client.ifEmpty { "General" }}")
                    Text("Método de pago: ${sale.paymentMethod}")
                    HorizontalDivider()
                    sale.products.forEach { product ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${product.name} x${product.quantity}")
                            Text("$${Utils.formatCurrency(product.price * product.quantity)}")
                        }
                    }
                    HorizontalDivider()
                    Text("Total: $${Utils.formatCurrency(sale.total)}", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSale = null }) { Text("Cerrar") }
            }
        )
    }
    
    // New Sale Dialog
    if (showSaleDialog) {
        var client by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("Efectivo") }
        var paymentExpanded by remember { mutableStateOf(false) }
        var selectedProducts by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            db.productDao().getAllProducts().collect { allProducts = it }
        }
        
        AlertDialog(
            onDismissRequest = { showSaleDialog = false },
            title = { Text("Nueva Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = client,
                        onValueChange = { client = it },
                        label = { Text("Cliente") },
                        singleLine = true
                    )
                    
                    Box {
                        OutlinedButton(onClick = { paymentExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Pago: $paymentMethod")
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = { paymentMethod = method; paymentExpanded = false }
                                )
                            }
                        }
                    }
                    
                    // Selected products
                    selectedProducts.forEachIndexed { index, (product, qty) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Medium)
                                Text("$${Utils.formatCurrency(product.price)} x $qty", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("$${Utils.formatCurrency(product.price * qty)}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                selectedProducts = selectedProducts.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Filled.RemoveCircle, contentDescription = "Eliminar", tint = Red)
                            }
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { showProductPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Agregar Producto")
                    }
                    
                    // Total
                    val total = selectedProducts.sumOf { it.first.price * it.second }
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
                            val total = selectedProducts.sumOf { it.first.price * it.second }
                            val cost = selectedProducts.sumOf { it.first.cost * it.second }
                            
                            val sale = Sale(
                                client = client,
                                products = selectedProducts.map { (product, qty) ->
                                    SaleProduct(product.id, product.name, qty, product.price, product.cost)
                                },
                                total = total,
                                cost = cost,
                                paymentMethod = paymentMethod
                            )
                            
                            db.saleDao().insert(sale)
                            
                            // Update stock
                            selectedProducts.forEach { (product, qty) ->
                                db.productDao().update(product.copy(stock = product.stock - qty))
                            }
                            
                            // Register in cash if cash payment
                            if (paymentMethod == "Efectivo") {
                                db.cashMovementDao().insert(
                                    CashMovement(
                                        type = "Ingreso",
                                        amount = total,
                                        description = "Venta - ${selectedProducts.size} productos"
                                    )
                                )
                            }
                            
                            showSaleDialog = false
                        }
                    },
                    enabled = selectedProducts.isNotEmpty()
                ) { Text("Completar Venta") }
            },
            dismissButton = {
                TextButton(onClick = { showSaleDialog = false }) { Text("Cancelar") }
            }
        )
        
        // Product Picker Dialog
        if (showProductPicker) {
            AlertDialog(
                onDismissRequest = { showProductPicker = false },
                title = { Text("Seleccionar Producto") },
                text = {
                    LazyColumn {
                        items(allProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = { Text("Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}") },
                                modifier = Modifier.clickable {
                                    selectedProducts = selectedProducts + (product to 1)
                                    showProductPicker = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProductPicker = false }) { Text("Cancelar") }
                }
            )
        }
    }
}