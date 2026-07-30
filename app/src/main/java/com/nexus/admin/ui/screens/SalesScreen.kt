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

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = {
                editingSale = null
                showSaleDialog = true
            }) {
                Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva Venta")
            }
        }

        // KPIs
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
                    Text("Transacciones", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$transactionCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Filtro de fecha
        OutlinedTextField(
            filterDate, { filterDate = it },
            label = { Text("Filtrar por fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.DateRange, null) },
            trailingIcon = {
                if (filterDate.isNotEmpty()) {
                    IconButton(onClick = { filterDate = "" }) { Icon(Icons.Filled.Clear, "Limpiar") }
                }
            }
        )

        // Lista de ventas
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredSales.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PointOfSale, null, modifier = Modifier.size(64.dp), tint = Gray300)
                            Spacer(Modifier.height(16.dp))
                            Text("No hay ventas registradas", style = MaterialTheme.typography.bodyLarge, color = Gray500)
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
                                    if (sale.isReceivable) {
                                        Spacer(Modifier.width(8.dp))
                                        SuggestionChip(onClick = {}, label = { Text("Crédito", style = MaterialTheme.typography.labelSmall) })
                                    }
                                    if (sale.isReturned) {
                                        Spacer(Modifier.width(8.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Devuelto", style = MaterialTheme.typography.labelSmall) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RedLight)
                                        )
                                    }
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
                        
                        // Botones de acción
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            if (!sale.isReturned) {
                                TextButton(onClick = {
                                    editingSale = sale
                                    showSaleDialog = true
                                }) {
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
        
        // Pagos combinados
        var cashAmount by remember { mutableStateOf(if (editingSale == null) "0" else if (editingSale?.paymentMethod == "Efectivo") editingSale?.total?.toString() ?: "0" else "0") }
        var transferAmount by remember { mutableStateOf(if (editingSale == null) "0" else if (editingSale?.paymentMethod == "Transferencia") editingSale?.total?.toString() ?: "0" else "0") }
        
        var selectedProducts by remember {
            mutableStateOf<MutableMap<Long, Pair<Product, Int>>>(
                editingSale?.products?.associate { sp ->
                    sp.productId to (Product(id = sp.productId, name = sp.name, cost = sp.cost, price = sp.price, stock = 0) to sp.quantity)
                }?.toMutableMap() ?: mutableMapOf()
            )
        }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }
        var showScanner by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            db.productDao().getAllProducts().collect { allProducts = it }
        }

        val total = selectedProducts.values.sumOf { (p, qty) -> p.price * qty }
        val combinedTotal = (cashAmount.toDoubleOrNull() ?: 0.0) + (transferAmount.toDoubleOrNull() ?: 0.0)
        val isPaymentComplete = combinedTotal >= total

        AlertDialog(
            onDismissRequest = { showSaleDialog = false; editingSale = null },
            title = { 
                Text(
                    when {
                        isReturned -> "Devolución de Venta"
                        editingSale != null -> "Editar Venta"
                        else -> "Nueva Venta"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    
                    // Checkbox: ¿Es cuenta por cobrar?
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isReceivable,
                            onCheckedChange = { isReceivable = it }
                        )
                        Text("¿Es cuenta por cobrar?", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Cliente (obligatorio si es cuenta por cobrar)
                    if (isReceivable || editingSale?.isReceivable == true) {
                        OutlinedTextField(
                            client, { client = it },
                            label = { Text("Cliente *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = isReceivable && client.isBlank()
                        )
                        if (isReceivable && client.isBlank()) {
                            Text("El cliente es obligatorio para cuentas por cobrar", color = Red, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        OutlinedTextField(
                            client, { client = it },
                            label = { Text("Cliente (opcional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Checkbox: ¿Devolución? (solo en edición)
                    if (editingSale != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isReturned,
                                onCheckedChange = { isReturned = it }
                            )
                            Text("¿Registrar devolución?", color = Red, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (isReturned) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RedLight)
                            ) {
                                Text(
                                    "⚠️ La devolución descontará el monto de caja y repondrá el stock",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Red
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Productos seleccionados
                    if (selectedProducts.isNotEmpty()) {
                        Text("Productos:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    selectedProducts.forEach { (id, pair) ->
                        val (product, qty) = pair
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    Text("$${Utils.formatCurrency(product.price)} c/u | Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (qty > 1) selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty - 1) } },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Filled.Remove, "Reducir", modifier = Modifier.size(20.dp)) }

                                    Text("$qty", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                    IconButton(
                                        onClick = {
                                            val maxQty = if (isReturned) Int.MAX_VALUE else product.stock
                                            if (qty < maxQty) selectedProducts = selectedProducts.toMutableMap().also { it[id] = product to (qty + 1) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Filled.Add, "Aumentar", modifier = Modifier.size(20.dp)) }
                                }

                                Text("$${Utils.formatCurrency(product.price * qty)}", fontWeight = FontWeight.Bold, color = Green, modifier = Modifier.padding(start = 8.dp))

                                IconButton(onClick = { selectedProducts = selectedProducts.toMutableMap().also { it.remove(id) } }) {
                                    Icon(Icons.Filled.Delete, "Eliminar", tint = Red, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    // Botones Agregar y Escanear
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar")
                        }
                        Button(onClick = { showScanner = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Escanear")
                        }
                    }

                    HorizontalDivider()

                    // Total
                    if (selectedProducts.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Total: $${Utils.formatCurrency(total)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = GreenDark)
                            }
                        }
                    }

                    // Pagos combinados
                    if (selectedProducts.isNotEmpty() && !isReturned) {
                        Text("Métodos de pago:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        
                        OutlinedTextField(
                            cashAmount, { cashAmount = it },
                            label = { Text("Efectivo") },
                            leadingIcon = { Text("$") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            transferAmount, { transferAmount = it },
                            label = { Text("Transferencia") },
                            leadingIcon = { Text("$") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Resumen de pagos
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isPaymentComplete) GreenLight else YellowLight)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Total pagado: $${Utils.formatCurrency(combinedTotal)}", fontWeight = FontWeight.Bold)
                                if (combinedTotal < total) {
                                    Text("⚠️ Faltan: $${Utils.formatCurrency(total - combinedTotal)}", color = Red, style = MaterialTheme.typography.bodySmall)
                                } else if (combinedTotal > total) {
                                    Text("Cambio: $${Utils.formatCurrency(combinedTotal - total)}", color = Blue, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Text("✅ Pago completo", color = Green, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (selectedProducts.isNotEmpty()) {
                                // Validar cliente si es cuenta por cobrar
                                if (isReceivable && client.isBlank()) {
                                    Toast.makeText(context, "❌ El cliente es obligatorio para cuentas por cobrar", Toast.LENGTH_LONG).show()
                                    return@launch
                                }

                                val productsList = selectedProducts.values.map { (p, q) -> SaleProduct(p.id, p.name, q, p.price, p.cost) }
                                val totalAmount = productsList.sumOf { it.price * it.quantity }
                                val totalCost = productsList.sumOf { it.cost * it.quantity }

                                // Construir métodos de pago
                                val paymentMethodsList = mutableListOf<String>()
                                if ((cashAmount.toDoubleOrNull() ?: 0.0) > 0) paymentMethodsList.add("Efectivo")
                                if ((transferAmount.toDoubleOrNull() ?: 0.0) > 0) paymentMethodsList.add("Transferencia")
                                val paymentMethodsStr = paymentMethodsList.joinToString(" + ").ifEmpty { "Pendiente" }

                                if (isReturned && editingSale != null) {
                                    // PROCESAR DEVOLUCIÓN
                                    val originalSale = editingSale!!
                                    
                                    // Actualizar venta como devuelta
                                    db.saleDao().update(originalSale.copy(isReturned = true, total = -totalAmount))
                                    
                                    // Reponer stock
                                    selectedProducts.values.forEach { (product, qty) ->
                                        db.productDao().update(product.copy(stock = product.stock + qty))
                                    }
                                    
                                    // Descontar de caja
                                    db.cashMovementDao().insert(
                                        CashMovement(
                                            type = "Egreso",
                                            amount = totalAmount,
                                            description = "Devolución venta #${originalSale.id} - ${originalSale.client}",
                                            date = System.currentTimeMillis()
                                        )
                                    )
                                    
                                    Toast.makeText(context, "✅ Devolución registrada", Toast.LENGTH_SHORT).show()
                                } else if (editingSale != null) {
                                    // EDITAR VENTA EXISTENTE
                                    val originalSale = editingSale!!
                                    
                                    // Reponer stock original
                                    originalSale.products.forEach { sp ->
                                        val product = allProducts.find { it.id == sp.productId }
                                        if (product != null) {
                                            db.productDao().update(product.copy(stock = product.stock + sp.quantity))
                                        }
                                    }
                                    
                                    // Descontar nuevo stock
                                    selectedProducts.values.forEach { (product, qty) ->
                                        db.productDao().update(product.copy(stock = product.stock - qty))
                                    }
                                    
                                    db.saleDao().update(
                                        originalSale.copy(
                                            client = client,
                                            products = productsList,
                                            total = totalAmount,
                                            cost = totalCost,
                                            paymentMethod = paymentMethodsStr,
                                            paymentMethods = paymentMethodsStr,
                                            isReceivable = isReceivable
                                        )
                                    )
                                    
                                    Toast.makeText(context, "✅ Venta actualizada", Toast.LENGTH_SHORT).show()
                                } else {
                                    // NUEVA VENTA
                                    val sale = Sale(
                                        client = client,
                                        products = productsList,
                                        total = totalAmount,
                                        cost = totalCost,
                                        paymentMethod = paymentMethodsStr,
                                        paymentMethods = paymentMethodsStr,
                                        isReceivable = isReceivable
                                    )
                                    db.saleDao().insert(sale)

                                    // Actualizar stock
                                    selectedProducts.values.forEach { (product, qty) ->
                                        db.productDao().update(product.copy(stock = product.stock - qty))
                                    }

                                    // Registrar en caja (efectivo)
                                    val cashAmt = cashAmount.toDoubleOrNull() ?: 0.0
                                    if (cashAmt > 0) {
                                        db.cashMovementDao().insert(
                                            CashMovement(
                                                type = "Ingreso",
                                                amount = cashAmt,
                                                description = "Venta - ${productsList.size} productos${if (client.isNotBlank()) " - $client" else ""}",
                                                date = System.currentTimeMillis()
                                            )
                                        )
                                    }

                                    // Si es cuenta por cobrar y falta pago
                                    if (isReceivable && combinedTotal < totalAmount) {
                                        val pendingAmount = totalAmount - combinedTotal
                                        db.receivableDao().insert(
                                            Receivable(
                                                clientName = client,
                                                concept = "Venta - ${productsList.size} productos",
                                                totalAmount = pendingAmount,
                                                balance = pendingAmount,
                                                status = "pending"
                                            )
                                        )
                                    }

                                    Toast.makeText(context, "✅ Venta registrada", Toast.LENGTH_SHORT).show()
                                }

                                showSaleDialog = false
                                editingSale = null
                            }
                        }
                    },
                    enabled = selectedProducts.isNotEmpty()
                ) {
                    Text(
                        when {
                            isReturned -> "Registrar Devolución"
                            editingSale != null -> "Actualizar Venta"
                            else -> "Completar Venta"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaleDialog = false; editingSale = null }) { Text("Cancelar") }
            }
        )

        // Escáner flotante
        if (showScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { barcode ->
                    scope.launch {
                        val products = db.productDao().getAllProducts().first()
                        products.find { it.sku == barcode }?.let { product ->
                            if (product.stock > 0 && !selectedProducts.containsKey(product.id)) {
                                selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = product to 1 }
                                Toast.makeText(context, "✅ ${product.name} agregado", Toast.LENGTH_SHORT).show()
                            } else if (selectedProducts.containsKey(product.id)) {
                                val currentQty = selectedProducts[product.id]?.second ?: 1
                                if (currentQty < product.stock) {
                                    selectedProducts = selectedProducts.toMutableMap().also { it[product.id] = product to (currentQty + 1) }
                                    Toast.makeText(context, "➕ ${product.name} x${currentQty + 1}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "⚠️ Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "❌ Sin stock: ${product.name}", Toast.LENGTH_SHORT).show()
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
                        OutlinedTextField(searchProd, { searchProd = it }, label = { Text("Buscar...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            if (filtered.isEmpty()) {
                                item { Text("No se encontraron productos", modifier = Modifier.padding(16.dp), color = Gray500) }
                            }
                            items(filtered) { product ->
                                val isOutOfStock = product.stock == 0 && !isReturned
                                val isAlreadyAdded = selectedProducts.containsKey(product.id)
                                ListItem(
                                    headlineContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(product.name)
                                            if (isOutOfStock) { Spacer(Modifier.width(8.dp)); SuggestionChip(onClick = {}, label = { Text("Agotado") }) }
                                            if (isAlreadyAdded) { Spacer(Modifier.width(8.dp)); SuggestionChip(onClick = {}, label = { Text("Agregado") }) }
                                        }
                                    },
                                    supportingContent = { Text("Stock: ${product.stock} | $${Utils.formatCurrency(product.price)}") },
                                    modifier = Modifier.clickable {
                                        if (!isOutOfStock && !isAlreadyAdded) {
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
