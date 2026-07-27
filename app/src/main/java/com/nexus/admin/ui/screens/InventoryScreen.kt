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

    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Cargar productos automáticamente
    LaunchedEffect(Unit) {
        db.productDao().getAllProducts().collect { productList ->
            allProducts = productList
            categories = productList.map { it.category }.filter { it.isNotEmpty() }.distinct()
        }
    }

    // Filtrar productos
    val filteredProducts = remember(searchQuery, selectedCategory, allProducts) {
        allProducts.filter { product ->
            (searchQuery.isEmpty() || product.name.contains(searchQuery, ignoreCase = true) || 
             product.sku.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory.isEmpty() || product.category == selectedCategory)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Nuevo") }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar producto...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton({ searchQuery = "" }) { Icon(Icons.Filled.Clear, "Limpiar") } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category filter chips
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredProducts) { product ->
                val bgColor = when {
                    product.stock == 0 -> RedLight
                    product.stock <= product.minStock -> YellowLight
                    else -> MaterialTheme.colorScheme.surface
                }
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.ifEmpty { "N/A" }} | Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall)
                            Text("Precio: $${Utils.formatCurrency(product.price)} | Costo: $${Utils.formatCurrency(product.cost)}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            if (product.stock <= product.minStock && product.stock > 0) Text("⚠️ Stock bajo", color = Yellow, style = MaterialTheme.typography.labelSmall)
                            if (product.stock == 0) Text("❌ Agotado", color = Red, style = MaterialTheme.typography.labelSmall)
                        }
                        Row {
                            IconButton(onClick = { editingProduct = product; showAddDialog = true }) { Icon(Icons.Filled.Edit, "Editar", tint = Blue) }
                            IconButton(onClick = { scope.launch { db.productDao().delete(product) } }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Red) }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit dialog
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { /* Barcode scanner - implementar */ }) { Icon(Icons.Filled.QrCodeScanner, "Escanear") }
                    }
                    OutlinedTextField(category, { category = it }, label = { Text("Categoría") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(cost, { cost = it }, label = { Text("Costo") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(price, { price = it }, label = { Text("Precio") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(minStock, { minStock = it }, label = { Text("Stock Mín") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val p = Product(
                            id = editingProduct?.id ?: 0,
                            name = name, sku = sku, category = category,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            price = price.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0,
                            minStock = minStock.toIntOrNull() ?: 5
                        )
                        if (editingProduct != null) db.productDao().update(p)
                        else db.productDao().insert(p)
                        showAddDialog = false; editingProduct = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; editingProduct = null }) { Text("Cancelar") } }
        )
    }
}
