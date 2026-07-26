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
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compras Mes", style = MaterialTheme.typography.bodySmall)
                    Text("$" + Utils.formatCurrency(monthRestocks), fontWeight = FontWeight.Bold, color = Blue)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Productos", style = MaterialTheme.typography.bodySmall)
                    Text("$productsRestocked", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compras", style = MaterialTheme.typography.bodySmall)
                    Text("$restockCount", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(restocks) { restock ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(restock.supplier, fontWeight = FontWeight.SemiBold)
                            Text(Utils.formatDate(restock.date), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            restock.products.joinToString(", ") { it.name + " x" + it.quantity },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total:")
                            Text("$" + Utils.formatCurrency(restock.total), fontWeight = FontWeight.Bold, color = Green)
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        var supplier by remember { mutableStateOf("") }
        var selectedProducts by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
        var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showProductPicker by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            db.productDao().getAllProducts().collect { allProducts = it }
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
                    
                    OutlinedButton(
                        onClick = { showProductPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Agregar Producto")
                    }
                    
                    selectedProducts.forEachIndexed { index, (product, qty) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Medium)
                                Text("Costo: $" + Utils.formatCurrency(product.cost) + " x $qty")
                            }
                            IconButton(onClick = {
                                selectedProducts = selectedProducts.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Filled.RemoveCircle, contentDescription = "Eliminar", tint = Red)
                            }
                        }
                    }
                    
                    val total = selectedProducts.sumOf { it.first.cost * it.second }
                    Text(
                        "Total: $" + Utils.formatCurrency(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
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
                            total = total
                        )
                        db.restockDao().insert(restock)
                        selectedProducts.forEach { (product, qty) ->
                            db.productDao().update(product.copy(stock = product.stock + qty))
                        }
                        showAddDialog = false
                    }
                }) { Text("Registrar Compra") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
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
                                supportingContent = { Text("Stock: " + product.stock + " | Costo: $" + Utils.formatCurrency(product.cost)) },
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
