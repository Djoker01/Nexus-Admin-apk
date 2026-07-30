package com.nexus.admin.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.Product
import com.nexus.admin.ui.components.FloatingBarcodeScanner
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream

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
    var showImportExportMenu by remember { mutableStateOf(false) }

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

    // Launcher para exportar Excel
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    exportInventoryToExcel(context, db, it)
                }
            }
        }
    }

    // Launcher para importar Excel
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    importInventoryFromExcel(context, db, it)
                }
            }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón Importar/Exportar
                Box {
                    IconButton(onClick = { showImportExportMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Opciones", tint = Blue)
                    }
                    DropdownMenu(
                        expanded = showImportExportMenu,
                        onDismissRequest = { showImportExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Exportar a Excel") },
                            onClick = {
                                showImportExportMenu = false
                                exportLauncher.launch("inventario_nexus_${System.currentTimeMillis()}.xlsx")
                            },
                            leadingIcon = { Icon(Icons.Filled.FileDownload, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Importar desde Excel") },
                            onClick = {
                                showImportExportMenu = false
                                importLauncher.launch(arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel"
                                ))
                            },
                            leadingIcon = { Icon(Icons.Filled.FileUpload, null) }
                        )
                    }
                }
                // Botón Nuevo Producto
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo")
                }
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
            
            FilledIconButton(
                onClick = { showScanner = true },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)
            ) {
                Icon(Icons.Filled.QrCodeScanner, "Escanear", tint = White, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Contador de productos
        Text(
            "${filteredProducts.size} producto(s)",
            style = MaterialTheme.typography.bodySmall,
            color = Gray500,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

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
                            Icon(Icons.Filled.Inventory, null, modifier = Modifier.size(64.dp), tint = Gray300)
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
                            Text("SKU: ${product.sku.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (product.stock <= product.minStock && product.stock > 0) {
                                    Text("⚠️ Stock bajo", color = Yellow, style = MaterialTheme.typography.labelSmall)
                                }
                                if (product.stock == 0) {
                                    Text("❌ Agotado", color = Red, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { editingProduct = product; showAddDialog = true }) {
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
            },
            onDismiss = { showScanner = false }
        )
    }

    // Add/Edit Dialog (igual que antes, omitido por brevedad)
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
            onDismissRequest = { showAddDialog = false; editingProduct = null },
            title = { Text(if (editingProduct != null) "Editar Producto" else "Nuevo Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(sku, { sku = it }, label = { Text("SKU/Código") }, modifier = Modifier.weight(1f), singleLine = true)
                        FilledIconButton(onClick = { showSkuScanner = true }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)) {
                            Icon(Icons.Filled.QrCodeScanner, "Escanear", tint = White, modifier = Modifier.size(22.dp))
                        }
                    }
                    
                    OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(cost, { cost = it }, label = { Text("Costo") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("$") })
                        OutlinedTextField(price, { price = it }, label = { Text("Precio") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("$") })
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
                            id = editingProduct?.id ?: 0, name = name, sku = sku, category = category,
                            cost = cost.toDoubleOrNull() ?: 0.0, price = price.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0, minStock = minStock.toIntOrNull() ?: 5
                        )
                        if (editingProduct != null) db.productDao().update(p) else db.productDao().insert(p)
                        showAddDialog = false; editingProduct = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; editingProduct = null }) { Text("Cancelar") } }
        )

        if (showSkuScanner) {
            FloatingBarcodeScanner(
                onBarcodeScanned = { barcode -> sku = barcode; showSkuScanner = false },
                onDismiss = { showSkuScanner = false }
            )
        }
    }
}

// ========== FUNCIONES DE EXPORTACIÓN/IMPORTACIÓN EXCEL ==========

suspend fun exportInventoryToExcel(context: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val products = db.productDao().getAllProducts().first()
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Inventario")

        // Estilo para encabezados
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.DARK_BLUE.index.toShort()
            setFont(workbook.createFont().apply {
                bold = true
                color = org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.WHITE.index.toShort()
            })
        }

        // Encabezados
        val headers = arrayOf("ID", "Nombre", "SKU", "Categoría", "Proveedor", "Costo", "Precio", "Stock", "Stock Mínimo", "Descripción")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // Datos
        products.forEachIndexed { rowIndex, product ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(product.id.toDouble())
            row.createCell(1).setCellValue(product.name)
            row.createCell(2).setCellValue(product.sku)
            row.createCell(3).setCellValue(product.category)
            row.createCell(4).setCellValue(product.supplier)
            row.createCell(5).setCellValue(product.cost)
            row.createCell(6).setCellValue(product.price)
            row.createCell(7).setCellValue(product.stock.toDouble())
            row.createCell(8).setCellValue(product.minStock.toDouble())
            row.createCell(9).setCellValue(product.description)
        }

        // Auto-ajustar columnas
        for (i in 0 until 10) {
            sheet.autoSizeColumn(i)
        }

        // Guardar
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "✅ Inventario exportado a Excel", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

suspend fun importInventoryFromExcel(context: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)

        var imported = 0
        var skipped = 0

        // Saltar encabezado (fila 0)
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            try {
                val name = row.getCell(1)?.stringCellValue ?: continue
                if (name.isBlank()) continue

                val sku = row.getCell(2)?.stringCellValue ?: ""
                val category = row.getCell(3)?.stringCellValue ?: ""
                val supplier = row.getCell(4)?.stringCellValue ?: ""
                val cost = row.getCell(5)?.numericCellValue ?: 0.0
                val price = row.getCell(6)?.numericCellValue ?: 0.0
                val stock = row.getCell(7)?.numericCellValue?.toInt() ?: 0
                val minStock = row.getCell(8)?.numericCellValue?.toInt() ?: 5
                val description = row.getCell(9)?.stringCellValue ?: ""

                val product = Product(
                    name = name,
                    sku = sku,
                    category = category,
                    supplier = supplier,
                    cost = cost,
                    price = price,
                    stock = stock,
                    minStock = minStock,
                    description = description
                )
                db.productDao().insert(product)
                imported++
            } catch (e: Exception) {
                skipped++
            }
        }

        workbook.close()
        inputStream.close()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "✅ $imported productos importados${if (skipped > 0) " ($skipped omitidos)" else ""}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "❌ Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
