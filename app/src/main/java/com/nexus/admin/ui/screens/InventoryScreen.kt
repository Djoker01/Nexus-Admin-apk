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
import com.nexus.admin.data.entity.Product
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    
    LaunchedEffect(Unit) {
        db.productDao().getAllProducts().collect { productList ->
            products = if (searchQuery.isNotEmpty()) {
                productList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true) }
            } else productList
            
            if (selectedCategory.isNotEmpty()) {
                products = products.filter { it.category == selectedCategory }
            }
            
            categories = productList.map { it.category }.filter { it.isNotEmpty() }.distinct()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Producto")
            }
        }
        
        // Search and Filter
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedCategory.ifEmpty { "Categoría" })
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Todas") }, onClick = { selectedCategory = ""; expanded = false })
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; expanded = false })
                    }
                }
            }
        }
        
        // Product List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            product.stock == 0 -> RedLight
                            product.stock <= product.minStock -> YellowLight
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}", 
                                style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { editingProduct = product; showAddDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Blue)
                            }
                            IconButton(onClick = {
                                scope.launch { db.productDao().delete(product) }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Red)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var sku by remember { mutableStateOf(editingProduct?.sku ?: "") }
        var category by remember { mutableStateOf(editingProduct?.category ?: "") }
        var cost by remember { mutableStateOf(editingProduct?.cost?.toString() ?: "") }
        var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
        var stock by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "0") }
        var minStock by remember { mutableStateOf(editingProduct?.minStock?.toString() ?: "5") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingProduct = null },
            title = { Text(if (editingProduct != null) "Editar Producto" else "Nuevo Producto") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") })
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría") })
                        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Costo") })
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio") })
                        OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") })
                        OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Stock Mínimo") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val product = Product(
                            id = editingProduct?.id ?: 0,
                            name = name,
                            sku = sku,
                            category = category,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            price = price.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0,
                            minStock = minStock.toIntOrNull() ?: 5
                        )
                        if (editingProduct != null) db.productDao().update(product)
                        else db.productDao().insert(product)
                        showAddDialog = false
                        editingProduct = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; editingProduct = null }) { Text("Cancelar") }
            }
        )
    }
}