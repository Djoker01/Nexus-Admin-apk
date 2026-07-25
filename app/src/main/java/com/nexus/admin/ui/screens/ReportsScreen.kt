package com.nexus.admin.ui.screens

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
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var selectedPeriod by remember { mutableStateOf("daily") }
    var totalSales by remember { mutableStateOf(0.0) }
    var totalProfit by remember { mutableStateOf(0.0) }
    var transactions by remember { mutableStateOf(0) }
    var avgTicket by remember { mutableStateOf(0.0) }
    var totalExpenses by remember { mutableStateOf(0.0) }
    var paymentMethods by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var topProducts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    
    LaunchedEffect(selectedPeriod) {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = when (selectedPeriod) {
            "daily" -> Utils.getTodayStart() to Utils.getTodayEnd()
            "weekly" -> now - 7 * 24 * 60 * 60 * 1000 to now
            "monthly" -> Utils.getMonthRange()
            else -> now - 365L * 24 * 60 * 60 * 1000 to now
        }
        
        db.saleDao().getSalesByDateRange(startDate, endDate).collect { sales ->
            totalSales = sales.sumOf { it.total }
            totalProfit = sales.sumOf { it.total - it.cost }
            transactions = sales.size
            avgTicket = if (transactions > 0) totalSales / transactions else 0.0
            
            val methods = mutableMapOf<String, Double>()
            sales.forEach { sale ->
                methods[sale.paymentMethod] = (methods[sale.paymentMethod] ?: 0.0) + sale.total
            }
            paymentMethods = methods
            
            val productMap = mutableMapOf<String, Int>()
            sales.forEach { sale ->
                sale.products.forEach { sp ->
                    productMap[sp.name] = (productMap[sp.name] ?: 0) + sp.quantity
                }
            }
            topProducts = productMap.entries.sortedByDescending { it.value }.take(10).map { it.key to it.value }
        }
        
        val expenseTotal = db.expenseDao().getTotalExpensesByDateRange(startDate, endDate)
        totalExpenses = expenseTotal ?: 0.0
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Reportes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        // Period selector
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
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary KPIs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Ventas", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$${Utils.formatCurrency(totalSales)}", fontWeight = FontWeight.Bold, color = Green)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Ganancia", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$${Utils.formatCurrency(totalProfit)}", fontWeight = FontWeight.Bold, color = Blue)
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Transacciones", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$transactions", fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Ticket Promedio", style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text("$${Utils.formatCurrency(avgTicket)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Payment Methods
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ventas por Método de Pago", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        paymentMethods.forEach { (method, total) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(method)
                                Text("$${Utils.formatCurrency(total)}", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            
            // Top 10 Products
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top 10 Productos", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        topProducts.forEachIndexed { index, (name, qty) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${index + 1}. $name")
                                Text("$qty unid.", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            
            // Financial Summary
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen Financiero", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ingresos:"); Text("$${Utils.formatCurrency(totalSales)}")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gastos:"); Text("$${Utils.formatCurrency(totalExpenses)}")
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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