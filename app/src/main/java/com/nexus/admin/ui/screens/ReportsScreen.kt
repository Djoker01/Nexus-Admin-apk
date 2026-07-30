package com.nexus.admin.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nexus.admin.data.entity.Sale
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf("daily") }
    var totalSales by remember { mutableDoubleStateOf(0.0) }
    var totalProfit by remember { mutableDoubleStateOf(0.0) }
    var transactions by remember { mutableIntStateOf(0) }
    var avgTicket by remember { mutableDoubleStateOf(0.0) }
    var totalExpenses by remember { mutableDoubleStateOf(0.0) }
    var paymentMethods by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var topProducts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var salesList by remember { mutableStateOf<List<Sale>>(emptyList()) }

    // Launcher para exportar PDF
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    exportReportToPdf(
                        context, db, it, selectedPeriod,
                        totalSales, totalProfit, transactions, avgTicket,
                        totalExpenses, paymentMethods, topProducts, salesList
                    )
                }
            }
        }
    }

    // Cargar datos según período
    LaunchedEffect(selectedPeriod) {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = when (selectedPeriod) {
            "daily" -> Utils.getTodayStart() to Utils.getTodayEnd()
            "weekly" -> now - 7 * 24 * 60 * 60 * 1000 to now
            "monthly" -> Utils.getMonthRange()
            else -> now - 365L * 24 * 60 * 60 * 1000 to now
        }

        val sales = db.saleDao().getSalesByDateRange(startDate, endDate).first()
        salesList = sales
        totalSales = sales.sumOf { it.total }
        totalProfit = sales.sumOf { it.total - it.cost }
        transactions = sales.size
        avgTicket = if (transactions > 0) totalSales / transactions else 0.0

        val methods = mutableMapOf<String, Double>()
        sales.forEach { sale ->
            val method = sale.paymentMethod
            methods[method] = (methods[method] ?: 0.0) + sale.total
        }
        paymentMethods = methods

        val productMap = mutableMapOf<String, Int>()
        sales.forEach { sale ->
            sale.products.forEach { sp ->
                productMap[sp.name] = (productMap[sp.name] ?: 0) + sp.quantity
            }
        }
        topProducts = productMap.entries.sortedByDescending { it.value }.take(10).map { it.key to it.value }

        totalExpenses = db.expenseDao().getTotalExpensesByDateRange(startDate, endDate) ?: 0.0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Reportes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

        // Selector de período
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("daily" to "Diario", "weekly" to "Semanal", "monthly" to "Mensual", "yearly" to "Anual").forEach { (period, label) ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Botón Exportar PDF
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    pdfLauncher.launch("reporte_${selectedPeriod}_${System.currentTimeMillis()}.pdf")
                }
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Exportar PDF")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPIs
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Ventas", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$${Utils.formatCurrency(totalSales)}", fontWeight = FontWeight.Bold, color = Green)
                        }
                    }
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Ganancia", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$${Utils.formatCurrency(totalProfit)}", fontWeight = FontWeight.Bold, color = Blue)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Transacciones", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$transactions", fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Ticket Promedio", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$${Utils.formatCurrency(avgTicket)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Métodos de pago
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ventas por Método de Pago", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        paymentMethods.forEach { (method, total) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(method)
                                Text("$${Utils.formatCurrency(total)}", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Top 10 Productos
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Top 10 Productos", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        topProducts.forEachIndexed { index, (name, qty) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${index + 1}. $name")
                                Text("$qty unid.", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Resumen Financiero
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Resumen Financiero", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ingresos:"); Text("$${Utils.formatCurrency(totalSales)}")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gastos:"); Text("$${Utils.formatCurrency(totalExpenses)}")
                        }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Balance Neto:", fontWeight = FontWeight.Bold)
                            Text(
                                "$${Utils.formatCurrency(totalProfit - totalExpenses)}",
                                fontWeight = FontWeight.Bold,
                                color = if (totalProfit - totalExpenses >= 0) Green else Red
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== FUNCIÓN DE EXPORTACIÓN PDF ==========

suspend fun exportReportToPdf(
    context: Context,
    db: AppDatabase,
    uri: Uri,
    period: String,
    totalSales: Double,
    totalProfit: Double,
    transactions: Int,
    avgTicket: Double,
    totalExpenses: Double,
    paymentMethods: Map<String, Double>,
    topProducts: List<Pair<String, Int>>,
    salesList: List<Sale>
) {
    try {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // Título
        val periodName = when (period) {
            "daily" -> "Diario"
            "weekly" -> "Semanal"
            "monthly" -> "Mensual"
            else -> "Anual"
        }

        document.add(Paragraph("NEXUS ADMIN - REPORTE $periodName".uppercase())
            .setBold()
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Fecha: ${Utils.formatDate(System.currentTimeMillis())}")
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("\n"))

        // KPIs
        document.add(Paragraph("RESUMEN").setBold().setFontSize(14f))
        document.add(Paragraph("Ventas Totales: $${Utils.formatCurrency(totalSales)}"))
        document.add(Paragraph("Ganancia Neta: $${Utils.formatCurrency(totalProfit)}"))
        document.add(Paragraph("Transacciones: $transactions"))
        document.add(Paragraph("Ticket Promedio: $${Utils.formatCurrency(avgTicket)}"))
        document.add(Paragraph("Gastos Totales: $${Utils.formatCurrency(totalExpenses)}"))
        document.add(Paragraph("Balance Neto: $${Utils.formatCurrency(totalProfit - totalExpenses)}"))
        document.add(Paragraph("\n"))

        // Métodos de pago
        if (paymentMethods.isNotEmpty()) {
            document.add(Paragraph("VENTAS POR MÉTODO DE PAGO").setBold().setFontSize(14f))
            val methodsTable = Table(2)
            methodsTable.setWidth(UnitValue.createPercentValue(100))
            methodsTable.addCell(Cell().add(Paragraph("Método").setBold()))
            methodsTable.addCell(Cell().add(Paragraph("Total").setBold()))
            paymentMethods.forEach { (method, total) ->
                methodsTable.addCell(Cell().add(Paragraph(method)))
                methodsTable.addCell(Cell().add(Paragraph("$${Utils.formatCurrency(total)}")))
            }
            document.add(methodsTable)
            document.add(Paragraph("\n"))
        }

        // Top 10 Productos
        if (topProducts.isNotEmpty()) {
            document.add(Paragraph("TOP 10 PRODUCTOS").setBold().setFontSize(14f))
            val productsTable = Table(3)
            productsTable.setWidth(UnitValue.createPercentValue(100))
            productsTable.addCell(Cell().add(Paragraph("#").setBold()))
            productsTable.addCell(Cell().add(Paragraph("Producto").setBold()))
            productsTable.addCell(Cell().add(Paragraph("Cantidad").setBold()))
            topProducts.forEachIndexed { index, (name, qty) ->
                productsTable.addCell(Cell().add(Paragraph("${index + 1}")))
                productsTable.addCell(Cell().add(Paragraph(name)))
                productsTable.addCell(Cell().add(Paragraph("$qty")))
            }
            document.add(productsTable)
            document.add(Paragraph("\n"))
        }

        // Últimas ventas
        if (salesList.isNotEmpty()) {
            document.add(Paragraph("ÚLTIMAS VENTAS").setBold().setFontSize(14f))
            val salesTable = Table(4)
            salesTable.setWidth(UnitValue.createPercentValue(100))
            salesTable.addCell(Cell().add(Paragraph("Fecha").setBold()))
            salesTable.addCell(Cell().add(Paragraph("Cliente").setBold()))
            salesTable.addCell(Cell().add(Paragraph("Productos").setBold()))
            salesTable.addCell(Cell().add(Paragraph("Total").setBold()))
            salesList.take(20).forEach { sale ->
                salesTable.addCell(Cell().add(Paragraph(Utils.formatDate(sale.date)).setFontSize(8f)))
                salesTable.addCell(Cell().add(Paragraph(sale.client.ifEmpty { "General" }).setFontSize(8f)))
                salesTable.addCell(Cell().add(Paragraph(sale.products.joinToString { "${it.name} x${it.quantity}" }).setFontSize(8f)))
                salesTable.addCell(Cell().add(Paragraph("$${Utils.formatCurrency(sale.total)}").setFontSize(8f)))
            }
            document.add(salesTable)
        }

        // Pie de página
        document.add(Paragraph("\n"))
        document.add(Paragraph("Reporte generado por Nexus Admin v1.0")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER))

        document.close()
        outputStream.close()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "✅ Reporte PDF exportado exitosamente", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "❌ Error al exportar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
