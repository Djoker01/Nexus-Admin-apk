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
        else sales.filter {
            Utils.formatDate(it.date).contains(filterDate, ignoreCase = true)
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
                    Text(
                        "$${Utils.formatCurrency(todaySales)}",
                        fontWeight = FontWeight.Bold,
                        color = Green,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Transacciones", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        "$transactionCount",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Filtro de fecha
        OutlinedTextField(
            filterDate,
            { filterDate = it },
            label = { Text("Filtrar por fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.DateRange, null) },
            trailingIcon = {
                if (filterDate.isNotEmpty()) {
                    IconButton(onClick = { filterDate = "" }) {
                        Icon(Icons.Filled.Clear, "Limpiar filtro")
                    }
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
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.PointOfSale,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Gray300
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No hay ventas registradas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Gray500
                            )
                        }
                    }
                }
            }
            items(filteredSales) { sale ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sale.client.ifEmpty { "Cliente General" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    Utils.formatDate(sale.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray500
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$${Utils.formatCurrency(sale.total)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Green,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(sale.paymentMethod, style = MaterialTheme.typography.labelSmall)
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            sale.products.joinToString(", ") { "${it.name} x${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Nueva Venta
    if (showSaleDialog) {
        var client by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("Efectivo") }
        var selectedProducts by remember {
            mutableStateOf<MutableMap<Long, Pair<Product, Int>>>(mutableMapOf())
        }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }
        var showScanner by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            db.productDao().getAllProducts().collect { allProducts = it }
        }

        val total = selectedProducts.values.sumOf { (p, qty) -> p.price * qty }

        AlertDialog(
            onDismissRequest = { showSaleDialog = false },
            title = { Text("Nueva Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        client,
                        { client = it },
                        label = { Text("Cliente") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var payExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { payExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pago: $paymentMethod")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                        DropdownMenu(payExpanded, { payExpanded = false }) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        paymentMethod = m
                                        payExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Productos seleccionados
                    if (selectedProducts.isNotEmpty()) {
                        Text(
                            "Productos:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    selectedProducts.forEach { (id, pair) ->
                        val (product, qty) = pair
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        product.name,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "$${Utils.formatCurrency(product.price)} c/u | Stock: ${product.stock}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (qty > 1) {
                                                selectedProducts = selectedProducts.toMutableMap()
                                                    .also { it[id] = product to (qty - 1) }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, "Reducir", modifier = Modifier.size(20.dp))
                                    }

                                    Text(
                                        "$qty",
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    IconButton(
                                        onClick = {
                                            if (qty < product.stock) {
                                                selectedProducts = selectedProducts.toMutableMap()
                                                    .also { it[id] = product to (qty + 1) }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, "Aumentar", modifier = Modifier.size(20.dp))
                                    }
                                }

                                Text(
                                    "$${Utils.formatCurrency(product.price * qty)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Green,
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                IconButton(
                                    onClick = {
                                        selectedProducts = selectedProducts.toMutableMap()
                                            .also { it.remove(id) }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        "Eliminar",
                                        tint = Red,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Botones Agregar y Escanear
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showProductPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar")
                        }

                        Button(
                            onClick = { showScanner = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Escanear")
                        }
                    }

                    // Total
                    if (selectedProducts.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GreenLight)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total a pagar:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "$${Utils.formatCurrency(total)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                )
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

                                selectedProducts.values.forEach { (product, qty) ->
                                    db.productDao().update(
                                        product.copy(stock = product.stock - qty)
                                    )
                                }

                                if (paymentMethod == "Efectivo") {
                                    db.cashMovementDao().insert(
                                        CashMovement(
                                            type = "Ingreso",
                                            amount = totalAmount,
                                            description = "Venta - ${productsList.size} productos",
                                            date = System.currentTimeMillis()
                                        )
                                    )
                                }

                                showSaleDialog = false
                                Toast.makeText(context, "✅ Venta registrada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = selectedProducts.isNotEmpty()
                ) {
                    Text("Completar Venta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaleDialog = false }) {
                    Text("Cancelar")
                }
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
                                selectedProducts = selectedProducts.toMutableMap().also {
                                    it[product.id] = product to 1
                                }
                                Toast.makeText(context, "✅ ${product.name} agregado", Toast.LENGTH_SHORT).show()
                            } else if (selectedProducts.containsKey(product.id)) {
                                val currentQty = selectedProducts[product.id]?.second ?: 1
                                if (currentQty < product.stock) {
                                    selectedProducts = selectedProducts.toMutableMap().also {
                                        it[product.id] = product to (currentQty + 1)
                                    }
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
                else allProducts.filter {
                    it.name.contains(searchProd, true) || it.sku.contains(searchProd, true)
                }
            }

            AlertDialog(
                onDismissRequest = { showProductPicker = false },
                title = { Text("Seleccionar Producto") },
                text = {
                    Column {
                        OutlinedTextField(
                            searchProd,
                            { searchProd = it },
                            label = { Text("Buscar producto...") },
                            leadingIcon = { Icon(Icons.Filled.Search, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            if (filtered.isEmpty()) {
                                item {
                                    Text(
                                        "No se encontraron productos",
                                        modifier = Modifier.padding(16.dp),
                                        color = Gray500
                                    )
                                }
                            }
                            items(filtered) { product ->
                                val isOutOfStock = product.stock == 0
                                val isAlreadyAdded = selectedProducts.containsKey(product.id)

                                ListItem(
                                    headlineContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(product.name)
                                            if (isOutOfStock) {
                                                Spacer(Modifier.width(8.dp))
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Agotado") }
                                                )
                                            }
                                            if (isAlreadyAdded) {
                                                Spacer(Modifier.width(8.dp))
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Agregado") }
                                                )
                                            }
                                        }
                                    },
                                    supportingContent = {
                                        Text("Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}")
                                    },
                                    modifier = Modifier.clickable {
                                        if (!isOutOfStock && !isAlreadyAdded) {
                                            selectedProducts = selectedProducts.toMutableMap().also {
                                                it[product.id] = product to 1
                                            }
                                            showProductPicker = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProductPicker = false }) {
                        Text("Listo")
                    }
                }
            )
        }
    }
}
