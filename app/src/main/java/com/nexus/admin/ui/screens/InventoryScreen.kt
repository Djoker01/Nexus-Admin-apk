package com.nexus.admin.ui.screens

import android.widget.Toast
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
import com.nexus.admin.ui.components.FloatingBarcodeScanner
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
    var showScanner by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo")
            }
        }

        // Barra de búsqueda con botón de escáner
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por nombre o SKU...") },
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
            
            // Botón de escáner con cámara
            FilledIconButton(
                onClick = { showScanner = true },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    "Escanear con cámara",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Lista de productos
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Inventory,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Gray300
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "No se encontraron productos"
                                else "No hay productos registrados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Gray500
                            )
                        }
                    }
                }
            }
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
                            Text(
                                "SKU: ${product.sku.ifEmpty { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (product.stock <= product.minStock && product.stock > 0) {
                                    Text(
                                        "⚠️ Stock bajo",
                                        color = Yellow,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (product.stock == 0) {
                                    Text(
                                        "❌ Agotado",
                                        color = Red,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Text(
                                "Ganancia: $${Utils.formatCurrency(product.price - product.cost)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Green
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                editingProduct = product
                                showAddDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, "Editar", tint = Blue)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    db.productDao().delete(product)
                                    Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Delete, "Eliminar", tint = Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Escáner flotante
    if (showScanner) {
        FloatingBarcodeScanner(
            onBarcodeScanned = { barcode ->
                searchQuery = barcode
                showScanner = false
                Toast.makeText(context, "🔍 Código escaneado: $barcode", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showScanner = false }
        )
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
        var showSkuScanner by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingProduct = null
            },
            title = {
                Text(if (editingProduct != null) "Editar Producto" else "Nuevo Producto")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text("Nombre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // SKU con botón de escanear
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            sku,
                            { sku = it },
                            label = { Text("SKU/Código") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = { showSkuScanner = true },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)
                        ) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                "Escanear SKU",
                                tint = White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        category,
                        { category = it },
                        label = { Text("Categoría") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            cost,
                            { cost = it },
                            label = { Text("Costo") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Text("$") }
                        )
                        OutlinedTextField(
                            price,
                            { price = it },
                            label = { Text("Precio") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Text("$") }
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            stock,
                            { stock = it },
                            label = { Text("Stock") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            minStock,
                            { minStock = it },
                            label = { Text("Stock Mín") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Vista previa de ganancia
                    val c = cost.toDoubleOrNull() ?: 0.0
                    val p = price.toDoubleOrNull() ?: 0.0
                    if (p > c) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GreenLight)
                        ) {
                            Text(
                                "Ganancia por unidad: $${Utils.formatCurrency(p - c)}",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                color = GreenDark
                            )
                        }
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
                        if (editingProduct != null) {
                            db.productDao().update(p)
                            Toast.makeText(context, "Producto actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            db.productDao().insert(p)
                            Toast.makeText(context, "Producto creado", Toast.LENGTH_SHORT).show()
                        }
                        showAddDialog = false
                        editingProduct = null
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingProduct = null
                }) {
                    Text("Cancelar")
                }
            }
        )

        // Escáner para SKU
        if (showSkuScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { barcode ->
                    sku = barcode
                    showSkuScanner = false
                },
                onDismiss = { showSkuScanner = false }
            )
        }
    }
}
