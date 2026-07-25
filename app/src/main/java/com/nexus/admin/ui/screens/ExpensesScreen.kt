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
import com.nexus.admin.data.entity.CashMovement
import com.nexus.admin.data.entity.Expense
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var filterDate by remember { mutableStateOf("") }
    
    val categories = listOf(
        "Servicios", "Alquiler", "Salarios", "Insumos", "Marketing",
        "Transporte", "Impuestos", "Mantenimiento", "Seguros", "Otros"
    )
    
    var monthTotal by remember { mutableStateOf(0.0) }
    var fixedTotal by remember { mutableStateOf(0.0) }
    var variableTotal by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        db.expenseDao().getAllExpenses().collect { expenseList ->
            expenses = expenseList
            
            val (monthStart, monthEnd) = Utils.getMonthRange()
            val monthExpenses = expenseList.filter { it.date in monthStart..monthEnd }
            monthTotal = monthExpenses.sumOf { it.amount }
            fixedTotal = expenseList.filter { it.isFixed }.sumOf { it.amount }
            variableTotal = expenseList.filter { !it.isFixed }.sumOf { it.amount }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gastos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Gasto")
            }
        }
        
        // KPIs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Mes", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(monthTotal)}", fontWeight = FontWeight.Bold, color = Red)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Fijos", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(fixedTotal)}", fontWeight = FontWeight.Bold, color = Yellow)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Variables", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text("$${Utils.formatCurrency(variableTotal)}", fontWeight = FontWeight.Bold, color = Blue)
                }
            }
        }
        
        // Filters
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedCategory.ifEmpty { "Categoría" })
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Todas") }, onClick = { selectedCategory = ""; expanded = false })
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; expanded = false })
                    }
                }
            }
            
            OutlinedTextField(
                value = filterDate,
                onValueChange = { filterDate = it },
                label = { Text("Fecha") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        // Expenses List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses.filter { expense ->
                (selectedCategory.isEmpty() || expense.category == selectedCategory) &&
                (filterDate.isEmpty() || Utils.formatDate(expense.date).contains(filterDate))
            }) { expense ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (expense.isFixed) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = if (expense.isFixed) Yellow else Gray500,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.description.ifEmpty { expense.category }, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(expense.category, style = MaterialTheme.typography.labelSmall) }
                                )
                                Text(Utils.formatDate(expense.date), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            }
                        }
                        Text(
                            "$${Utils.formatCurrency(expense.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Red
                        )
                    }
                }
            }
        }
    }
    
    // Add Expense Dialog
    if (showAddDialog) {
        var category by remember { mutableStateOf(categories[0]) }
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isFixed by remember { mutableStateOf(false) }
        var fromCash by remember { mutableStateOf(false) }
        var categoryExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Gasto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(category)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false })
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Monto") },
                        leadingIcon = { Text("$") },
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        singleLine = true
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFixed, onCheckedChange = { isFixed = it })
                        Text("¿Es fijo?")
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = fromCash, onCheckedChange = { fromCash = it })
                        Text("Descontar de caja")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val expense = Expense(
                            category = category,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            description = description,
                            isFixed = isFixed,
                            fromCash = fromCash
                        )
                        db.expenseDao().insert(expense)
                        
                        if (fromCash) {
                            db.cashMovementDao().insert(
                                CashMovement(
                                    type = "Egreso",
                                    amount = expense.amount,
                                    description = "Gasto: $category - $description"
                                )
                            )
                        }
                        
                        showAddDialog = false
                    }
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}