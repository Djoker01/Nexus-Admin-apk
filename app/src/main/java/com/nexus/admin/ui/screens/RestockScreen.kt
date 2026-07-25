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
    
    var monthRestocks by remember { mutableStateOf(0.0) }
    var productsRestocked by remember { mutableStateOf(0) }
    var restockCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        db.restockDao().getAllRestocks().collect { restockList ->
            restocks = restockList
            
            val (monthStart, monthEnd) = Utils.getMonthRange()
            val monthList = restockList.filter { it.date in monthStart..monthEnd }
            monthRestocks = monthList.sumOf { it.total }
            productsRestocked = monthList.sumOf { it.products.size }
            restockCount = monthList.size
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reabastecimiento", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva Compra")
            }
        }
        
        // KPIs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compras Mes", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(monthRestocks)}", fontWeight = FontWeight.Bold, color = Blue)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Productos", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$productsRestocked", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compras", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$restockCount", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(restocks) { restock ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(restock.supplier, fontWeight = FontWeight.SemiBold)
                            Text(Utils.formatDate(restock.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text(
                            restock.products.joinToString(", ") { "${it.name} x${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total:", color = Gray500)
                            Text("$${Utils.formatCurrency(restock.total)}", fontWeight = FontWeight.Bold, color = Green)
                        }
                        if (restock.discountFromCash) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Descontado de caja", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Restock Dialog
    if (showAddDialog) {
        var supplier by remember { mutableStateOf("") }
        var selectedProducts by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var lowStockProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }
        var discountFromCash by remember { mutableStateOf(false) }
        var cashBalance by remember { mutableStateOf(0.0) }
        
        LaunchedEffect(Unit) {
            db.productDao().getAllProducts().collect {
                allProducts = it
                lowStockProducts = it.filter { p -> p.stock <= p.minStock }
            }
            db.cashMovementDao().getAllMovements().collect {
                cashBalance = it.sumOf { m -> if (m.type == "Ingreso") m.amount else -m.amount }
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Reabastecimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Proveedor") },
                        singleLine = true
                    )
                    
                    // Selected products
                    selectedProducts.forEachIndexed { index, (product, qty) ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium)
                                    Text("Costo: $${Utils.formatCurrency(product.cost)} x $qty")
                                }
                                Text("$${Utils.formatCurrency(product.cost * qty)}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    selectedProducts = selectedProducts.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Filled.RemoveCircle, contentDescription = "Eliminar", tint = Red)
                                }
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
                    
                    // Add all low stock button
                    if (lowStockProducts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                selectedProducts = lowStockProducts.map { it to (it.minStock * 2 - it.stock) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Yellow)
                            Text("Agregar todos los bajos (${lowStockProducts.size})")
                        }
                    }
                    
                    val total = selectedProducts.sumOf { it.first.cost * it.second }
                    Text(
                        "Total: $${Utils.formatCurrency(total)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = discountFromCash, onCheckedChange = { discountFromCash = it })
                        Text("Descontar de caja")
                    }
                    
                    if (discountFromCash) {
                        Text(
                            "Saldo en caja: $${Utils.formatCurrency(cashBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (total > cashBalance) Red else Green
                        )
                        if (total > cashBalance) {
                            Text(
                                "⚠️ El total excede el saldo en caja",
                                color = Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val total = selectedProducts.sumOf { it.first.cost * it.second }
                        
                        val restock = Restock(
                            supplier = supplier,
                            products = selectedProducts.map { (product, qty) ->
                                RestockProduct(product.id, product.name, qty, product.cost)
                            },
                            total = total,
                            discountFromCash = discountFromCash
                        )
                        
                        db.restockDao().insert(restock)
                        
                        // Update stock
                        selectedProducts.forEach { (product, qty) ->
                            db.productDao().update(product.copy(stock = product.stock + qty))
                        }
                        
                        if (discountFromCash) {
                            db.cashMovementDao().insert(
                                CashMovement(
                                    type = "Egreso",
                                    amount = total,
                                    description = "Reabastecimiento: $supplier"
                                )
                            )
                        }
                        
                        showAddDialog = false
                    }
                }) { Text("Registrar Compra") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
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
                                headlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(product.name)
                                        if (product.stock <= product.minStock) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("Bajo", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                },
                                supportingContent = {
                                    Text("Stock: ${product.stock} | Costo: $${Utils.formatCurrency(product.cost)}")
                                },
                                modifier = Modifier {
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
