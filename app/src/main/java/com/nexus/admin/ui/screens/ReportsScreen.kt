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
        // Título
        Text(
            "Reportes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Selector de período
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "daily" to "Diario",
                "weekly" to "Semanal",
                "monthly" to "Mensual",
                "yearly" to "Anual"
            ).forEach { (period, label) ->
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
                Spacer(Modifier.width(8.dp))
                Text("Exportar PDF")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Contenido del reporte
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPIs principales
            item {
                Text(
                    "Resumen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Ventas Totales", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                "$${Utils.formatCurrency(totalSales)}",
                                fontWeight = FontWeight.Bold,
                                color = Green,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Ganancia Neta", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                "$${Utils.formatCurrency(totalProfit)}",
                                fontWeight = FontWeight.Bold,
                                color = Blue,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Transacciones", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                "$transactions",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Ticket Promedio", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                "$${Utils.formatCurrency(avgTicket)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Gastos Totales", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                "$${Utils.formatCurrency(totalExpenses)}",
                                fontWeight = FontWeight.Bold,
                                color = Red,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Balance Neto", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            val balance = totalProfit - totalExpenses
                            Text(
                                "$${Utils.formatCurrency(balance)}",
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) Green else Red,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Métodos de pago
            if (paymentMethods.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Ventas por Método de Pago",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            paymentMethods.forEach { (method, total) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(method, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$${Utils.formatCurrency(total)}",
                                        fontWeight = FontWeight.Medium,
                                        color = Green
                                    )
                                }
                                if (method != paymentMethods.keys.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Top 10 Productos
            if (topProducts.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Top 10 Productos Más Vendidos",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            topProducts.forEachIndexed { index, (name, qty) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${index + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(30.dp)
                                        )
                                        Text(name, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        "$qty unid.",
                                        fontWeight = FontWeight.Medium,
                                        color = Blue
                                    )
                                }
                                if (index < topProducts.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Espacio final
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}


            // ========== FUNCIÓN DE EXPORTACIÓN A PDF (CORREGIDA) ==========

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

        val periodName = when (period) {
            "daily" -> "Diario"
            "weekly" -> "Semanal"
            "monthly" -> "Mensual"
            else -> "Anual"
        }

        // Título principal
        val titleParagraph = Paragraph("NEXUS ADMIN - REPORTE $periodName".uppercase())
            .setBold()
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(titleParagraph as com.itextpdf.layout.element.IBlockElement)

        val dateParagraph = Paragraph("Fecha: ${Utils.formatDate(System.currentTimeMillis())}")
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(dateParagraph as com.itextpdf.layout.element.IBlockElement)

        val spacer = Paragraph("\n")
        document.add(spacer as com.itextpdf.layout.element.IBlockElement)

        // Sección Resumen
        val summaryTitle = Paragraph("RESUMEN")
            .setBold()
            .setFontSize(14f)
        document.add(summaryTitle as com.itextpdf.layout.element.IBlockElement)

        val lines = listOf(
            "Ventas Totales: $$totalSales",
            "Ganancia Neta: $$totalProfit",
            "Transacciones: $transactions",
            "Ticket Promedio: $$avgTicket",
            "Gastos Totales: $$totalExpenses",
            "Balance Neto: $${totalProfit - totalExpenses}"
        )
        lines.forEach { line ->
            document.add(Paragraph(line).setFontSize(11f) as com.itextpdf.layout.element.IBlockElement)
        }
        document.add(Paragraph("\n") as com.itextpdf.layout.element.IBlockElement)

        // Sección Métodos de Pago
        if (paymentMethods.isNotEmpty()) {
            val methodsTitle = Paragraph("VENTAS POR MÉTODO DE PAGO")
                .setBold()
                .setFontSize(14f)
            document.add(methodsTitle as com.itextpdf.layout.element.IBlockElement)

            val methodsTable = Table(2)  // ← Int, no Float
            methodsTable.setWidth(UnitValue.createPercentValue(100f))

            methodsTable.addCell(Cell().add(Paragraph("Método").setBold().setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
            methodsTable.addCell(Cell().add(Paragraph("Total").setBold().setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))

            paymentMethods.forEach { (method, total) ->
                methodsTable.addCell(Cell().add(Paragraph(method).setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
                methodsTable.addCell(Cell().add(Paragraph("$$total").setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
            }
            document.add(methodsTable)
            document.add(Paragraph("\n") as com.itextpdf.layout.element.IBlockElement)
        }

        // Sección Top 10 Productos
        if (topProducts.isNotEmpty()) {
            val productsTitle = Paragraph("TOP 10 PRODUCTOS MÁS VENDIDOS")
                .setBold()
                .setFontSize(14f)
            document.add(productsTitle as com.itextpdf.layout.element.IBlockElement)

            val productsTable = Table(3)  // ← Int, no Float
            productsTable.setWidth(UnitValue.createPercentValue(100f))

            productsTable.addCell(Cell().add(Paragraph("#").setBold().setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
            productsTable.addCell(Cell().add(Paragraph("Producto").setBold().setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
            productsTable.addCell(Cell().add(Paragraph("Cantidad").setBold().setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))

            topProducts.forEachIndexed { index, (name, qty) ->
                productsTable.addCell(Cell().add(Paragraph("${index + 1}").setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
                productsTable.addCell(Cell().add(Paragraph(name).setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
                productsTable.addCell(Cell().add(Paragraph("$qty").setFontSize(10f) as com.itextpdf.layout.element.IBlockElement))
            }
            document.add(productsTable)
        }

        // Pie de página
        document.add(Paragraph("\n") as com.itextpdf.layout.element.IBlockElement)
        val footer = Paragraph("Reporte generado por Nexus Admin v1.0 - ${Utils.formatDate(System.currentTimeMillis())}")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic()
        document.add(footer as com.itextpdf.layout.element.IBlockElement)

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
