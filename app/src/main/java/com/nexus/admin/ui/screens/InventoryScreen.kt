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
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(isAdmin: Boolean = true) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var showImportExportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it } }

    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isEmpty()) allProducts
        else allProducts.filter { it.name.contains(searchQuery, true) || it.sku.contains(searchQuery, true) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri -> uri?.let { scope.launch { withContext(Dispatchers.IO) { exportToExcel(context, db, it) } } } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { scope.launch { withContext(Dispatchers.IO) { importFromExcel(context, db, it) } } } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    Box {
                        IconButton(onClick = { showImportExportMenu = true }) { Icon(Icons.Filled.MoreVert, "Opciones", tint = Blue) }
                        DropdownMenu(expanded = showImportExportMenu, onDismissRequest = { showImportExportMenu = false }) {
                            DropdownMenuItem(text = { Text("Exportar Excel") }, onClick = { showImportExportMenu = false; exportLauncher.launch("inventario_${System.currentTimeMillis()}.xlsx") }, leadingIcon = { Icon(Icons.Filled.FileDownload, null) })
                            DropdownMenuItem(text = { Text("Importar Excel") }, onClick = { showImportExportMenu = false; importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) }, leadingIcon = { Icon(Icons.Filled.FileUpload, null) })
                        }
                    }
                }
                if (isAdmin) {
                    Button(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Nuevo") }
                }
            }
        }

        // Barra de búsqueda con escáner
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Buscar...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, "Limpiar") } }, modifier = Modifier.weight(1f), singleLine = true)
            FilledIconButton(onClick = { showScanner = true }, modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)) { Icon(Icons.Filled.QrCodeScanner, "Escanear", tint = White, modifier = Modifier.size(28.dp)) }
        }

        Spacer(Modifier.height(8.dp))
        Text("${filteredProducts.size} producto(s)", style = MaterialTheme.typography.bodySmall, color = Gray500, modifier = Modifier.padding(horizontal = 16.dp))

        // Lista de productos
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filteredProducts.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(if (searchQuery.isNotEmpty()) "No se encontraron productos" else "No hay productos", color = Gray500) } } }
            items(filteredProducts) { product ->
                val bgColor = when { product.stock == 0 -> RedLight; product.stock <= product.minStock -> YellowLight; else -> MaterialTheme.colorScheme.surface }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Stock: ${product.stock} | Precio: $${Utils.formatCurrency(product.price)}", style = MaterialTheme.typography.bodySmall)
                            
                            // Solo admin ve costos y ganancias
                            if (isAdmin) {
                                Text("Costo: $${Utils.formatCurrency(product.cost)}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                                val margin = if (product.cost > 0) ((product.price - product.cost) / product.cost * 100) else 0.0
                                Text("Ganancia: $${Utils.formatCurrency(product.price - product.cost)} (${String.format("%.0f", margin)}%)", style = MaterialTheme.typography.labelSmall, color = Green)
                            }
                            
                            if (product.stock <= product.minStock && product.stock > 0) Text("⚠️ Stock bajo", color = Yellow, style = MaterialTheme.typography.labelSmall)
                            if (product.stock == 0) Text("❌ Agotado", color = Red, style = MaterialTheme.typography.labelSmall)
                        }
                        // Solo admin puede editar/eliminar
                        if (isAdmin) {
                            Row {
                                IconButton(onClick = { editingProduct = product; showAddDialog = true }) { Icon(Icons.Filled.Edit, "Editar", tint = Blue) }
                                IconButton(onClick = { scope.launch { db.productDao().delete(product) } }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Red) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Escáner flotante (disponible para todos)
    if (showScanner) { FloatingBarcodeScanner(onBarcodeScanned = { searchQuery = it; showScanner = false }, onDismiss = { showScanner = false }) }

    // Add/Edit Dialog (solo admin)
    if (showAddDialog && isAdmin) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }; var sku by remember { mutableStateOf(editingProduct?.sku ?: "") }; var category by remember { mutableStateOf(editingProduct?.category ?: "") }
        var cost by remember { mutableStateOf(editingProduct?.cost?.toString() ?: "") }; var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
        var stock by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "0") }; var minStock by remember { mutableStateOf(editingProduct?.minStock?.toString() ?: "5") }
        var showSkuScanner by remember { mutableStateOf(false) }
        val c = cost.toDoubleOrNull() ?: 0.0; val p = price.toDoubleOrNull() ?: 0.0; val margin = if (c > 0) ((p - c) / c * 100) else 0.0

        AlertDialog(onDismissRequest = { showAddDialog = false; editingProduct = null }, title = { Text(if (editingProduct != null) "Editar Producto" else "Nuevo Producto") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f), singleLine = true); FilledIconButton(onClick = { showSkuScanner = true }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)) { Icon(Icons.Filled.QrCodeScanner, "Escanear SKU", tint = White, modifier = Modifier.size(22.dp)) } }
                OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(cost, { cost = it }, label = { Text("Costo") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("$") }); OutlinedTextField(price, { price = it }, label = { Text("Precio") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("$") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(minStock, { minStock = it }, label = { Text("Stock Mín") }, modifier = Modifier.weight(1f), singleLine = true) }
                if (p > c) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) { Column(Modifier.padding(12.dp)) { Text("💰 Ganancia: $${Utils.formatCurrency(p - c)}", fontWeight = FontWeight.Bold, color = GreenDark); Text("📊 Margen: ${String.format("%.1f", margin)}%", color = GreenDark) } } }
            }
        }, confirmButton = { Button(onClick = { scope.launch { val prod = Product(id = editingProduct?.id ?: 0, name = name, sku = sku, category = category, cost = c, price = p, stock = stock.toIntOrNull() ?: 0, minStock = minStock.toIntOrNull() ?: 5); if (editingProduct != null) db.productDao().update(prod) else db.productDao().insert(prod); showAddDialog = false; editingProduct = null } }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { showAddDialog = false; editingProduct = null }) { Text("Cancelar") } })
        if (showSkuScanner) { FloatingBarcodeScanner(onBarcodeScanned = { sku = it; showSkuScanner = false }, onDismiss = { showSkuScanner = false }) }
    }
}

// Funciones de exportación/importación (solo admin las usa)
suspend fun exportToExcel(context: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val products = db.productDao().getAllProductsOnce()
        
        if (products.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "⚠️ No hay productos para exportar", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Inventario")
        
        val headerFont = wb.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index.toShort()
            fontHeightInPoints = 12
        }
        val headerStyle = wb.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.DARK_BLUE.index.toShort()
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }
        val dataStyle = wb.createCellStyle().apply {
            setFont(wb.createFont().apply { fontHeightInPoints = 11 })
        }
        
        val headers = arrayOf("ID", "Nombre", "SKU", "Categoría", "Costo", "Precio", "Stock", "Stock Mín", "Ganancia", "Margen %")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h -> val c = headerRow.createCell(i); c.setCellValue(h); c.cellStyle = headerStyle }
        
        products.forEachIndexed { ri, p ->
            val row = sheet.createRow(ri + 1)
            val margin = if (p.cost > 0) ((p.price - p.cost) / p.cost * 100) else 0.0
            row.createCell(0).apply { setCellValue(p.id.toDouble()); cellStyle = dataStyle }
            row.createCell(1).apply { setCellValue(p.name); cellStyle = dataStyle }
            row.createCell(2).apply { setCellValue(p.sku); cellStyle = dataStyle }
            row.createCell(3).apply { setCellValue(p.category); cellStyle = dataStyle }
            row.createCell(4).apply { setCellValue(p.cost); cellStyle = dataStyle }
            row.createCell(5).apply { setCellValue(p.price); cellStyle = dataStyle }
            row.createCell(6).apply { setCellValue(p.stock.toDouble()); cellStyle = dataStyle }
            row.createCell(7).apply { setCellValue(p.minStock.toDouble()); cellStyle = dataStyle }
            row.createCell(8).apply { setCellValue(p.price - p.cost); cellStyle = dataStyle }
            row.createCell(9).apply { setCellValue(margin); cellStyle = dataStyle }
        }
        
        for (i in 0..9) sheet.autoSizeColumn(i)
        sheet.createFreezePane(0, 1)
        
        // CORREGIDO: Manejar correctamente el OutputStream
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream != null) {
            try {
                wb.write(outputStream)
                outputStream.flush()
            } finally {
                outputStream.close()
                wb.close()
            }
        }
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "✅ Exportado: ${products.size} productos", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

suspend fun importFromExcel(context: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val inp = context.contentResolver.openInputStream(uri) ?: return; val wb = WorkbookFactory.create(inp); val sheet = wb.getSheetAt(0); var imp = 0; var skp = 0
        for (ri in 1..sheet.lastRowNum) { val row = sheet.getRow(ri) ?: continue; try { val name = row.getCell(1)?.stringCellValue ?: continue; if (name.isBlank()) continue; db.productDao().insert(Product(name = name, sku = row.getCell(2)?.stringCellValue ?: "", category = row.getCell(3)?.stringCellValue ?: "", cost = row.getCell(4)?.numericCellValue ?: 0.0, price = row.getCell(5)?.numericCellValue ?: 0.0, stock = row.getCell(6)?.numericCellValue?.toInt() ?: 0, minStock = row.getCell(7)?.numericCellValue?.toInt() ?: 5)); imp++ } catch (e: Exception) { skp++ } }
        wb.close(); inp.close()
        withContext(Dispatchers.Main) { Toast.makeText(context, "✅ $imp importados${if (skp > 0) " ($skp omitidos)" else ""}", Toast.LENGTH_SHORT).show() }
    } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show() } }
}
