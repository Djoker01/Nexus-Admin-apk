package com.nexus.admin.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.widget.Toast
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        db.productDao().getAllProducts().collect { allProducts = it }
    }

    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isEmpty()) allProducts
        else allProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.sku.contains(searchQuery, ignoreCase = true)
        }
    }

    // Escáner de código de barras
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT") ?: ""
            if (barcode.isNotEmpty()) {
                searchQuery = barcode
                Toast.makeText(context, "Código escaneado: $barcode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Nuevo")
            }
        }

        // Barra de búsqueda con escáner
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Limpiar")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            // Botón de escanear
            IconButton(
                onClick = {
                    try {
                        val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                            putExtra("SCAN_MODE", "PRODUCT_MODE")
                        }
                        scannerLauncher.launch(intent)
                    } catch (e: Exception) {
                        // Si no hay app de escáner, mostrar entrada manual
                        Toast.makeText(context, "Ingrese el código manualmente en la búsqueda", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    "Escanear código",
                    tint = Blue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts) { product ->
                val bgColor = when {
                    product.stock == 0 -> RedLight
                    product.stock <= product.minStock -> YellowLight
                    else -> MaterialTheme.colorScheme.surface
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                            if (product.stock <= product.minStock && product.stock > 0)
                                Text("⚠️ Stock bajo", color = Yellow, style = MaterialTheme.typography.labelSmall)
                            if (product.stock == 0)
                                Text("❌ Agotado", color = Red, style = MaterialTheme.typography.labelSmall)
                        }
                        Row {
                            IconButton(onClick = { editingProduct = product; showAddDialog = true }) {
                                Icon(Icons.Filled.Edit, "Editar", tint = Blue)
                            }
                            IconButton(onClick = {
                                scope.launch { db.productDao().delete(product) }
                            }) {
                                Icon(Icons.Filled.Delete, "Eliminar", tint = Red)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                    
                    // SKU con botón de escanear
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            sku, { sku = it },
                            label = { Text("SKU/Código") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                                        putExtra("SCAN_MODE", "PRODUCT_MODE")
                                    }
                                    scannerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "App de escáner no disponible", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, "Escanear", tint = Blue)
                        }
                    }
                    
                    OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(cost, { cost = it }, label = { Text("Costo") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(price, { price = it }, label = { Text("Precio") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(minStock, { minStock = it }, label = { Text("Stock Mín") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val p = Product(
                            id = editingProduct?.id ?: 0,
                            name = name,
                            sku = sku,
                            category = category,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            price = price.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0,
                            minStock = minStock.toIntOrNull() ?: 5
                        )
                        if (editingProduct != null) db.productDao().update(p)
                        else db.productDao().insert(p)
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
