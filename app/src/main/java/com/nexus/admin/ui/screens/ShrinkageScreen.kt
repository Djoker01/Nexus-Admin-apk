package com.nexus.admin.ui.screens

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
import com.nexus.admin.data.entity.Shrinkage
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShrinkageScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var shrinkages by remember { mutableStateOf<List<Shrinkage>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("") }
    
    val types = listOf(
        "Merma/Desperdicio", "Consumo Personal", "Producto Caducado",
        "Robo/Pérdida", "Error de Inventario", "Otro"
    )
    
    var todayLoss by remember { mutableStateOf(0.0) }
    var monthLoss by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        db.shrinkageDao().getAllShrinkages().collect { shrinkageList ->
            shrinkages = shrinkageList
            
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val (monthStart, monthEnd) = Utils.getMonthRange()
            
            todayLoss = shrinkageList.filter { it.date in todayStart..todayEnd }.sumOf { it.loss }
            monthLoss = shrinkageList.filter { it.date in monthStart..monthEnd }.sumOf { it.loss }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mermas y Consumo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar Merma")
            }
        }
        
        // KPIs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Today, contentDescription = null, tint = Red, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pérdida Hoy", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(todayLoss)}", fontWeight = FontWeight.Bold, color = Red)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Yellow, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pérdida del Mes", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(monthLoss)}", fontWeight = FontWeight.Bold, color = Yellow)
                }
            }
        }
        
        // Type filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType.isEmpty(),
                onClick = { selectedType = "" },
                label = { Text("Todas") }
            )
            types.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type) }
                )
            }
        }
        
        // List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shrinkages.filter { selectedType.isEmpty() || it.type == selectedType }) { shrinkage ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shrinkage.productName, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(shrinkage.type, style = MaterialTheme.typography.labelSmall) }
                                )
                                Text("Cant: ${shrinkage.quantity}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Utils.formatDate(shrinkage.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Text(
                            "-$${Utils.formatCurrency(shrinkage.loss)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Red
                        )
                    }
                }
            }
        }
    }
    
    // Add Shrinkage Dialog
    if (showAddDialog) {
        var products by remember { mutableStateOf<List<Product>>(emptyList()) }
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var selectedShrinkageType by remember { mutableStateOf(types[0]) }
        var quantity by remember { mutableStateOf("") }
        var typeExpanded by remember { mutableStateOf(false) }
        var productExpanded by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            db.productDao().getAllProducts().collect { products = it }
        }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Merma") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        OutlinedButton(onClick = { productExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedProduct?.name ?: "Seleccionar Producto")
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text("${product.name} (Stock: ${product.stock})") },
                                    onClick = { selectedProduct = product; productExpanded = false }
                                )
                            }
                        }
                    }
                    
                    Box {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedShrinkageType)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = { selectedShrinkageType = type; typeExpanded = false }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Cantidad") },
                        singleLine = true
                    )
                    
                    selectedProduct?.let { product ->
                        val loss = product.cost * (quantity.toIntOrNull() ?: 0)
                        Text(
                            "Pérdida estimada: $${Utils.formatCurrency(loss)}",
                            color = Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        selectedProduct?.let { product ->
                            val qty = quantity.toIntOrNull() ?: 0
                            if (qty > 0 && qty <= product.stock) {
                                db.shrinkageDao().insert(
                                    Shrinkage(
                                        productId = product.id,
                                        productName = product.name,
                                        type = selectedShrinkageType,
                                        quantity = qty,
                                        loss = product.cost * qty
                                    )
                                )
                                
                                db.productDao().update(product.copy(stock = product.stock - qty))
                                showAddDialog = false
                            }
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}