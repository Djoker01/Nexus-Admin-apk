package com.nexus.admin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.Sale
import com.nexus.admin.ui.components.KpiCard
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(isAdmin: Boolean = true) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    var todaySales by remember { mutableDoubleStateOf(0.0) }
    var todayProfit by remember { mutableDoubleStateOf(0.0) }
    var cashBalance by remember { mutableDoubleStateOf(0.0) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var recentSales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var topProducts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var weeklyData by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val products = db.productDao().getAllProducts().first()
                val sales = db.saleDao().getAllSales().first()
                val movements = db.cashMovementDao().getAllMovements().first()

                lowStockCount = products.count { it.stock <= it.minStock && it.stock > 0 }

                val todayStart = Utils.getTodayStart()
                val todayEnd = Utils.getTodayEnd()
                val todayList = sales.filter { it.date in todayStart..todayEnd && !it.isReturned }
                todaySales = todayList.sumOf { it.total }
                todayProfit = todayList.sumOf { it.total - it.cost }

                recentSales = sales.filter { !it.isReturned }.sortedByDescending { it.date }.take(5)

                val productMap = mutableMapOf<String, Int>()
                sales.filter { !it.isReturned }.forEach { sale ->
                    sale.products.forEach { sp ->
                        productMap[sp.name] = (productMap[sp.name] ?: 0) + sp.quantity
                    }
                }
                topProducts = productMap.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }

                val sdf = SimpleDateFormat("EEE", Locale("es"))
                val cal = Calendar.getInstance()
                val weekData = mutableListOf<Triple<String, Double, Double>>()
                for (i in 6 downTo 0) {
                    cal.timeInMillis = System.currentTimeMillis()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    val daySales = sales.filter { it.date in start..end && !it.isReturned }
                    weekData.add(Triple(sdf.format(Date(start)), daySales.sumOf { it.total }, daySales.sumOf { it.total - it.cost }))
                }
                weeklyData = weekData

                cashBalance = movements.sumOf { if (it.type == "Ingreso") it.amount else -it.amount }
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(5000)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        // KPIs - Según rol
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Ventas Hoy", "$${Utils.formatCurrency(todaySales)}", Icons.Filled.TrendingUp, Green, Modifier.weight(1f))
                if (isAdmin) {
                    KpiCard("Ganancias", "$${Utils.formatCurrency(todayProfit)}", Icons.Filled.Savings, Blue, Modifier.weight(1f))
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    KpiCard("Caja", "$${Utils.formatCurrency(cashBalance)}", Icons.Filled.AccountBalance, Yellow, Modifier.weight(1f))
                }
                KpiCard("Stock Bajo", "$lowStockCount", Icons.Filled.Warning, if (lowStockCount > 0) Red else Green, Modifier.weight(if (isAdmin) 1f else 1f))
            }
        }

        // Gráfico semanal (solo admin)
        if (isAdmin && weeklyData.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ventas y Ganancias (7 días)", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        val maxVal = weeklyData.maxOf { maxOf(it.second, it.third) }.takeIf { it > 0 } ?: 1.0
                        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            val w = size.width; val h = size.height
                            val stepX = w / (weeklyData.size - 1).coerceAtLeast(1)
                            for (i in 0..4) { val y = h - (h * i / 4); drawLine(Color.Gray.copy(alpha = 0.2f), Offset(0f, y), Offset(w, y)) }
                            val salesPath = Path(); weeklyData.forEachIndexed { i, (_, s, _) -> val x = stepX * i; val y = h - (s / maxVal * h).toFloat(); if (i == 0) salesPath.moveTo(x, y) else salesPath.lineTo(x, y) }; drawPath(salesPath, Green, style = Stroke(3f))
                            val profitPath = Path(); weeklyData.forEachIndexed { i, (_, _, p) -> val x = stepX * i; val y = h - (p / maxVal * h).toFloat(); if (i == 0) profitPath.moveTo(x, y) else profitPath.lineTo(x, y) }; drawPath(profitPath, Blue, style = Stroke(3f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Canvas(Modifier.size(12.dp)) { drawCircle(Green, 6f) }; Spacer(Modifier.width(4.dp)); Text("Ventas", style = MaterialTheme.typography.bodySmall) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Canvas(Modifier.size(12.dp)) { drawCircle(Blue, 6f) }; Spacer(Modifier.width(4.dp)); Text("Ganancias", style = MaterialTheme.typography.bodySmall) }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { weeklyData.forEach { (day, _, _) -> Text(day, style = MaterialTheme.typography.labelSmall, color = Gray500) } }
                    }
                }
            }
        }

        // Top 5 Productos
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top 5 Productos", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (topProducts.isEmpty()) Text("No hay datos aún", color = Gray500)
                    else topProducts.forEachIndexed { i, (name, qty) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${i + 1}. $name"); Text("$qty vendidos", fontWeight = FontWeight.Medium, color = Green) }
                        if (i < topProducts.size - 1) HorizontalDivider()
                    }
                }
            }
        }

        // Últimas Ventas
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Últimas Ventas", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (recentSales.isEmpty()) Text("No hay ventas aún", color = Gray500)
                    else recentSales.forEach { sale ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(sale.client.ifEmpty { "General" }, fontWeight = FontWeight.Medium); Text(Utils.formatDate(sale.date), style = MaterialTheme.typography.bodySmall, color = Gray500) }
                            Text("$${Utils.formatCurrency(sale.total)}", fontWeight = FontWeight.Bold, color = Green)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
