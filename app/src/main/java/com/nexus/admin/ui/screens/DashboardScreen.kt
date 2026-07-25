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
import com.nexus.admin.ui.components.KpiCard
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var todaySales by remember { mutableStateOf(0.0) }
    var todayProfit by remember { mutableStateOf(0.0) }
    var cashBalance by remember { mutableStateOf(0.0) }
    var lowStockCount by remember { mutableStateOf(0) }
    var recentSales by remember { mutableStateOf<List<com.nexus.admin.data.entity.Sale>>(emptyList()) }
    var topProducts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            val products = db.productDao().getAllProducts()
            val sales = db.saleDao().getAllSales()
            val movements = db.cashMovementDao().getAllMovements()
            
            products.collect { productList ->
                lowStockCount = productList.count { it.stock <= it.minStock && it.stock > 0 }
            }
            
            sales.collect { saleList ->
                val todayStart = Utils.getTodayStart()
                val todayEnd = Utils.getTodayEnd()
                val todaySalesList = saleList.filter { it.date in todayStart..todayEnd }
                todaySales = todaySalesList.sumOf { it.total }
                todayProfit = todaySalesList.sumOf { it.total - it.cost }
                recentSales = saleList.takeLast(5).reversed()
                
                val productMap = mutableMapOf<String, Int>()
                saleList.forEach { sale ->
                    sale.products.forEach { sp ->
                        productMap[sp.name] = (productMap[sp.name] ?: 0) + sp.quantity
                    }
                }
                topProducts = productMap.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
            }
            
            movements.collect { movementList ->
                cashBalance = movementList.sumOf { if (it.type == "Ingreso") it.amount else -it.amount }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // KPIs
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Ventas Hoy", "$${Utils.formatCurrency(todaySales)}", Icons.Filled.TrendingUp, Green, Modifier.weight(1f))
                KpiCard("Ganancias", "$${Utils.formatCurrency(todayProfit)}", Icons.Filled.Savings, Blue, Modifier.weight(1f))
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Caja", "$${Utils.formatCurrency(cashBalance)}", Icons.Filled.AccountBalance, Yellow, Modifier.weight(1f))
                KpiCard("Stock Bajo", "$lowStockCount", Icons.Filled.Warning, if (lowStockCount > 0) Red else Green, Modifier.weight(1f))
            }
        }
        
        // Top Products
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top 5 Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    topProducts.forEachIndexed { index, (name, qty) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. $name")
                            Text("$qty vendidos", fontWeight = FontWeight.Medium, color = Green)
                        }
                        if (index < topProducts.size - 1) HorizontalDivider()
                    }
                }
            }
        }
        
        // Recent Sales
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Últimas Ventas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    recentSales.forEach { sale ->
                        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sale.client.ifEmpty { "General" }, fontWeight = FontWeight.Medium)
                                Text(Utils.formatDate(sale.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            }
                            Text("$${Utils.formatCurrency(sale.total)}", fontWeight = FontWeight.Bold, color = Green)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}