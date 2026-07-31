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

    LaunchedEffect(Unit) { db.productDao().getAllProducts().collect { allProducts = it } }

    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isEmpty()) allProducts
        else allProducts.filter { it.name.contains(searchQuery, true) || it.sku.contains(searchQuery, true) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri -> uri?.let { scope.launch { withContext(Dispatchers.IO) { exportToExcel(context, db, it) } } } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { scope.launch { withContext(Dispatchers.IO) { importFromExcel(context, db, it) } } } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    IconButton(onClick = { showImportExportMenu = true }) { Icon(Icons.Filled.MoreVert, "Opciones", tint = Blue) }
                    DropdownMenu(expanded = showImportExportMenu, onDismissRequest = { showImportExportMenu = false }) {
                        DropdownMenuItem(text = { Text("Exportar a Excel") }, onClick = { showImportExportMenu = false; exportLauncher.launch("inventario_${System.currentTimeMillis()}.xlsx") }, leadingIcon = { Icon(Icons.Filled.FileDownload, null) })
                        DropdownMenuItem(text = { Text("Importar desde Excel") }, onClick = { showImportExportMenu = false; importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) }, leadingIcon = { Icon(Icons.Filled.FileUpload, null) })
                    }
                }
                Button(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Nuevo") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Buscar...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, null) } }, modifier = Modifier.weight(1f), singleLine = true)
            FilledIconButton(onClick = { showScanner = true }, modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)) { Icon(Icons.Filled.QrCodeScanner, "Escanear", tint = White, modifier = Modifier.size(28.dp)) }
        }
        Spacer(Modifier.height(4.dp))
        Text("${filteredProducts.size} producto(s)", style = MaterialTheme.typography.bodySmall, color = Gray500, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filteredProducts.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(if (searchQuery.isNotEmpty()) "Sin resultados" else "No hay productos", color = Gray500) } }
            items(filteredProducts) { p ->
                val bg = when { p.stock == 0 -> RedLight; p.stock <= p.minStock -> YellowLight; else -> MaterialTheme.colorScheme.surface }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold); Text("SKU: ${p.sku.ifEmpty { "N/A" }} | Stock: ${p.stock}", style = MaterialTheme.typography.bodySmall); Text("Precio: $${Utils.formatCurrency(p.price)} | Gan: $${Utils.formatCurrency(p.price - p.cost)} (${Utils.formatCurrency(if(p.cost>0)((p.price-p.cost)/p.cost)*100 else 0.0)}%)", style = MaterialTheme.typography.labelSmall, color = Green) }
                        Row { IconButton(onClick = { editingProduct = p; showAddDialog = true }) { Icon(Icons.Filled.Edit, null, tint = Blue) }; IconButton(onClick = { scope.launch { db.productDao().delete(p); Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show() } }) { Icon(Icons.Filled.Delete, null, tint = Red) } }
                    }
                }
            }
        }
    }
    if (showScanner) FloatingBarcodeScanner(onBarcodeScanned = { searchQuery = it; showScanner = false }, onDismiss = { showScanner = false })

    if (showAddDialog) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }; var sku by remember { mutableStateOf(editingProduct?.sku ?: "") }; var cat by remember { mutableStateOf(editingProduct?.category ?: "") }
        var cost by remember { mutableStateOf(editingProduct?.cost?.toString() ?: "") }; var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
        var stock by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "0") }; var min by remember { mutableStateOf(editingProduct?.minStock?.toString() ?: "5") }
        AlertDialog(onDismissRequest = { showAddDialog = false; editingProduct = null }, title = { Text(if (editingProduct != null) "Editar" else "Nuevo Producto") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f), singleLine = true); IconButton(onClick = { }) { Icon(Icons.Filled.QrCodeScanner, null, tint = Blue) } }
                OutlinedTextField(cat, { cat = it }, label = { Text("Categoría") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(cost, { cost = it }, label = { Text("Costo") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("$") }); OutlinedTextField(price, { price = it }, label = { Text("Precio") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("$") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(min, { min = it }, label = { Text("Stock Mín") }, modifier = Modifier.weight(1f), singleLine = true) }
                val c = cost.toDoubleOrNull() ?: 0.0; val pr = price.toDoubleOrNull() ?: 0.0
                if (pr > c && c > 0) {
                    val gain = pr - c; val margin = (gain / c) * 100
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("💰 Ganancia: $${Utils.formatCurrency(gain)}", fontWeight = FontWeight.Bold, color = GreenDark)
                            Text("📊 Margen: ${Utils.formatCurrency(margin)}%", fontWeight = FontWeight.Bold, color = GreenDark)
                            Text("💵 Retorno: Por cada $$1.00 invertido ganas $${Utils.formatCurrency(gain/c)}", style = MaterialTheme.typography.bodySmall, color = GreenDark)
                        }
                    }
                }
            }
        }, confirmButton = { Button(onClick = { scope.launch { val p = Product(id = editingProduct?.id ?: 0, name = name, sku = sku, category = cat, cost = c, price = pr, stock = stock.toIntOrNull() ?: 0, minStock = min.toIntOrNull() ?: 5); if (editingProduct != null) db.productDao().update(p) else db.productDao().insert(p); showAddDialog = false; editingProduct = null } }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { showAddDialog = false; editingProduct = null }) { Text("Cancelar") } })
    }
}

// ========== EXPORTAR EXCEL (CORREGIDO) ==========
suspend fun exportToExcel(ctx: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val products = db.productDao().getAllProductsOnce()
        if (products.isEmpty()) { withContext(Dispatchers.Main) { Toast.makeText(ctx, "⚠️ Sin productos", Toast.LENGTH_SHORT).show() }; return }
        val wb = XSSFWorkbook(); val sh = wb.createSheet("Inventario")
        val hs = wb.createCellStyle().apply { val f = wb.createFont().apply { bold = true; color = IndexedColors.WHITE.index.toShort() }; setFont(f); fillForegroundColor = IndexedColors.DARK_BLUE.index.toShort(); fillPattern = FillPatternType.SOLID_FOREGROUND }
        val ds = wb.createCellStyle().apply { setFont(wb.createFont().apply { fontHeightInPoints = 11 }) }
        val headers = arrayOf("ID","Nombre","SKU","Categoría","Proveedor","Costo","Precio","Stock","Stock Mín","Ganancia","Margen %","Descripción")
        val hr = sh.createRow(0); headers.forEachIndexed { i, h -> hr.createCell(i).apply { setCellValue(h); cellStyle = hs } }
        products.forEachIndexed { ri, p ->
            val r = sh.createRow(ri + 1)
            r.createCell(0).apply { setCellValue(p.id.toDouble()); cellStyle = ds }
            r.createCell(1).apply { setCellValue(p.name); cellStyle = ds }
            r.createCell(2).apply { setCellValue(p.sku); cellStyle = ds }
            r.createCell(3).apply { setCellValue(p.category); cellStyle = ds }
            r.createCell(4).apply { setCellValue(p.supplier); cellStyle = ds }
            r.createCell(5).apply { setCellValue(p.cost); cellStyle = ds }
            r.createCell(6).apply { setCellValue(p.price); cellStyle = ds }
            r.createCell(7).apply { setCellValue(p.stock.toDouble()); cellStyle = ds }
            r.createCell(8).apply { setCellValue(p.minStock.toDouble()); cellStyle = ds }
            r.createCell(9).apply { setCellValue(p.price - p.cost); cellStyle = ds }
            r.createCell(10).apply { setCellValue(if(p.cost>0) ((p.price-p.cost)/p.cost)*100 else 0.0); cellStyle = ds }
            r.createCell(11).apply { setCellValue(p.description); cellStyle = ds }
        }
        for (i in 0..11) sh.autoSizeColumn(i)
        sh.createFreezePane(0, 1)
        ctx.contentResolver.openOutputStream(uri)?.use { wb.write(it); it.flush() }
        wb.close()
        withContext(Dispatchers.Main) { Toast.makeText(ctx, "✅ ${products.size} productos exportados", Toast.LENGTH_SHORT).show() }
    } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(ctx, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show() } }
}

// ========== IMPORTAR EXCEL (CORREGIDO) ==========
suspend fun importFromExcel(ctx: android.content.Context, db: AppDatabase, uri: Uri) {
    try {
        val inp: InputStream = ctx.contentResolver.openInputStream(uri) ?: return
        val wb = WorkbookFactory.create(inp); val sh = wb.getSheetAt(0)
        var imp = 0; var skip = 0
        for (ri in 1..sh.lastRowNum) {
            val r = sh.getRow(ri) ?: continue
            try {
                val name = r.getCell(1)?.stringCellValue ?: continue; if (name.isBlank()) continue
                db.productDao().insert(Product(name = name, sku = r.getCell(2)?.stringCellValue ?: "", category = r.getCell(3)?.stringCellValue ?: "", supplier = r.getCell(4)?.stringCellValue ?: "", cost = r.getCell(5)?.numericCellValue ?: 0.0, price = r.getCell(6)?.numericCellValue ?: 0.0, stock = r.getCell(7)?.numericCellValue?.toInt() ?: 0, minStock = r.getCell(8)?.numericCellValue?.toInt() ?: 5, description = r.getCell(11)?.stringCellValue ?: "")); imp++
            } catch (e: Exception) { skip++ }
        }
        wb.close(); inp.close()
        withContext(Dispatchers.Main) { Toast.makeText(ctx, "✅ $imp importados${if(skip>0) " ($skip omitidos)" else ""}", Toast.LENGTH_SHORT).show() }
    } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(ctx, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show() } }
}
