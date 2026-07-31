package com.nexus.admin.ui.screens

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
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

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

    // Launcher para exportar Excel - CORREGIDO
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        exportInventoryToExcel(context, db, it)
                    }
                }
            }
        }
    )

    // Launcher para importar Excel - CORREGIDO
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        importInventoryFromExcel(context, db, it)
                    }
                }
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón de opciones (Importar/Exportar)
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
                                importLauncher.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.ms-excel"
                                    )
                                )
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
                            Text(
                                "Ganancia: $${Utils.formatCurrency(product.price - product.cost)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Green
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
                Toast.makeText(context, "🔍 Código: $barcode", Toast.LENGTH_SHORT).show()
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
            onDismissRequest = { showAddDialog = false; editingProduct = null },
            title = { Text(if (editingProduct != null) "Editar Producto" else "Nuevo Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            sku, { sku = it },
                            label = { Text("SKU/Código") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = { showSkuScanner = true },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, "Escanear SKU", tint = White, modifier = Modifier.size(22.dp))
                        }
                    }
                    
                    OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            cost, { cost = it },
                            label = { Text("Costo") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Text("$") }
                        )
                        OutlinedTextField(
                            price, { price = it },
                            label = { Text("Precio") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Text("$") }
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            stock, { stock = it },
                            label = { Text("Stock") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            minStock, { minStock = it },
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
                            Toast.makeText(context, "✅ Producto actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            db.productDao().insert(p)
                            Toast.makeText(context, "✅ Producto creado", Toast.LENGTH_SHORT).show()
                        }
                        showAddDialog = false
                        editingProduct = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingProduct = null
                }) { Text("Cancelar") }
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

// ========== FUNCIÓN DE EXPORTACIÓN A EXCEL ==========

suspend fun exportInventoryToExcel(context: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val products = db.productDao().getAllProductsOnce()
        
        if (products.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "⚠️ No hay productos para exportar", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Inventario")

        // Estilo para encabezados
        val headerFont = workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index.toShort()
            fontHeightInPoints = 12
        }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.DARK_BLUE.index.toShort()
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }

        // Estilo para datos
        val dataFont = workbook.createFont().apply {
            fontHeightInPoints = 11
        }
        val dataStyle = workbook.createCellStyle().apply {
            setFont(dataFont)
        }

        // Encabezados
        val headers = arrayOf(
            "ID", "Nombre", "SKU", "Categoría", "Proveedor",
            "Costo", "Precio", "Stock", "Stock Mínimo", "Ganancia", "Descripción"
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // Datos
        products.forEachIndexed { rowIndex, product ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).apply { setCellValue(product.id.toDouble()); cellStyle = dataStyle }
            row.createCell(1).apply { setCellValue(product.name); cellStyle = dataStyle }
            row.createCell(2).apply { setCellValue(product.sku); cellStyle = dataStyle }
            row.createCell(3).apply { setCellValue(product.category); cellStyle = dataStyle }
            row.createCell(4).apply { setCellValue(product.supplier); cellStyle = dataStyle }
            row.createCell(5).apply { setCellValue(product.cost); cellStyle = dataStyle }
            row.createCell(6).apply { setCellValue(product.price); cellStyle = dataStyle }
            row.createCell(7).apply { setCellValue(product.stock.toDouble()); cellStyle = dataStyle }
            row.createCell(8).apply { setCellValue(product.minStock.toDouble()); cellStyle = dataStyle }
            row.createCell(9).apply { setCellValue(product.price - product.cost); cellStyle = dataStyle }
            row.createCell(10).apply { setCellValue(product.description); cellStyle = dataStyle }
        }

        // Auto-ajustar columnas
        for (i in 0..10) {
            sheet.autoSizeColumn(i)
        }

        // Congelar primera fila
        sheet.createFreezePane(0, 1)

        // Guardar archivo
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream != null) {
            workbook.write(outputStream)
            outputStream.flush()
            outputStream.close()
        }
        workbook.close()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "✅ Exportado: ${products.size} productos a Excel", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "❌ Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

// ========== FUNCIÓN DE IMPORTACIÓN DESDE EXCEL ==========

suspend fun importInventoryFromExcel(context: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo abrir el archivo")
        
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)

        var imported = 0
        var skipped = 0

        // Saltar encabezado (fila 0)
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            try {
                // Leer nombre (columna 1)
                val nameCell = row.getCell(1)
                val name = nameCell?.stringCellValue ?: continue
                if (name.isBlank()) continue

                val sku = row.getCell(2)?.stringCellValue ?: ""
                val category = row.getCell(3)?.stringCellValue ?: ""
                val supplier = row.getCell(4)?.stringCellValue ?: ""
                val cost = row.getCell(5)?.numericCellValue ?: 0.0
                val price = row.getCell(6)?.numericCellValue ?: 0.0
                val stock = row.getCell(7)?.numericCellValue?.toInt() ?: 0
                val minStock = row.getCell(8)?.numericCellValue?.toInt() ?: 5
                val description = row.getCell(10)?.stringCellValue ?: ""

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
            Toast.makeText(
                context,
                "✅ $imported productos importados${if (skipped > 0) " ($skipped omitidos)" else ""}",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "❌ Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
