package com.nexus.admin.ui.screens

import androidx.compose.foundation.clickable
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
import com.nexus.admin.data.entity.*
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Cotizaciones", "Proveedores", "Análisis")
    
    var suppliers by remember { mutableStateOf<List<Supplier>>(emptyList()) }
    var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showAddSupplier by remember { mutableStateOf(false) }
    var showAddQuote by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        db.supplierDao().getAllSuppliers().collect { suppliers = it }
        db.quoteDao().getAllQuotes().collect { quotes = it }
        db.productDao().getAllProducts().collect { products = it }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            "Proveedores y Cotizaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> QuotesTab(quotes, products, onAddQuote = { showAddQuote = true })
            1 -> SuppliersTab(suppliers, onAddSupplier = { showAddSupplier = true })
            2 -> AnalysisTab(quotes, products)
        }
    }
    
    // Add Supplier Dialog
    if (showAddSupplier) {
        var name by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var deliveryTime by remember { mutableStateOf("") }
        var paymentTerms by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddSupplier = false },
            title = { Text("Nuevo Proveedor") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }) }
                    item { OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Empresa") }) }
                    item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }) }
                    item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }) }
                    item { OutlinedTextField(value = deliveryTime, onValueChange = { deliveryTime = it }, label = { Text("Tiempo de entrega") }) }
                    item { OutlinedTextField(value = paymentTerms, onValueChange = { paymentTerms = it }, label = { Text("Condiciones de pago") }) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.supplierDao().insert(
                            Supplier(name = name, company = company, phone = phone, email = email,
                                deliveryTime = deliveryTime, paymentTerms = paymentTerms)
                        )
                        showAddSupplier = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplier = false }) { Text("Cancelar") }
            }
        )
    }
    
    // Add Quote Dialog
    if (showAddQuote) {
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var quoteSupplier by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var minQty by remember { mutableStateOf("1") }
        var deliveryDays by remember { mutableStateOf("0") }
        var terms by remember { mutableStateOf("") }
        var includesShipping by remember { mutableStateOf(false) }
        var productExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showAddQuote = false },
            title = { Text("Nueva Cotización") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Box {
                            OutlinedButton(onClick = { productExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedProduct?.name ?: "Seleccionar Producto")
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                                products.forEach { product ->
                                    DropdownMenuItem(
                                        text = { Text("${product.name} (Costo: $${Utils.formatCurrency(product.cost)})") },
                                        onClick = { selectedProduct = product; productExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    item { OutlinedTextField(value = quoteSupplier, onValueChange = { quoteSupplier = it }, label = { Text("Proveedor") }) }
                    item { OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio cotizado") }, leadingIcon = { Text("$") }) }
                    item { OutlinedTextField(value = minQty, onValueChange = { minQty = it }, label = { Text("Cantidad mínima") }) }
                    item { OutlinedTextField(value = deliveryDays, onValueChange = { deliveryDays = it }, label = { Text("Días de entrega") }) }
                    item { OutlinedTextField(value = terms, onValueChange = { terms = it }, label = { Text("Condiciones de pago") }) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includesShipping, onCheckedChange = { includesShipping = it })
                            Text("¿Incluye envío?")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        selectedProduct?.let { product ->
                            db.quoteDao().insert(
                                Quote(
                                    productId = product.id,
                                    supplier = quoteSupplier,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    minQuantity = minQty.toIntOrNull() ?: 1,
                                    deliveryDays = deliveryDays.toIntOrNull() ?: 0,
                                    paymentTerms = terms,
                                    includesShipping = includesShipping
                                )
                            )
                            showAddQuote = false
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddQuote = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun QuotesTab(quotes: List<Quote>, products: List<Product>, onAddQuote: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onAddQuote) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Nueva Cotización")
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quotes) { quote ->
                val product = products.find { it.id == quote.productId }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(product?.name ?: "Producto no encontrado", fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Proveedor: ${quote.supplier}")
                            Text("$${Utils.formatCurrency(quote.price)}", fontWeight = FontWeight.Bold, color = Green)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(onClick = {}, label = { Text("Mín: ${quote.minQuantity}") })
                            SuggestionChip(onClick = {}, label = { Text("${quote.deliveryDays} días") })
                            if (quote.includesShipping) {
                                SuggestionChip(onClick = {}, label = { Text("Envío incluido") })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersTab(suppliers: List<Supplier>, onAddSupplier: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onAddSupplier) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Nuevo Proveedor")
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suppliers) { supplier ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(supplier.name, fontWeight = FontWeight.SemiBold)
                        if (supplier.company.isNotEmpty()) Text(supplier.company, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (supplier.phone.isNotEmpty()) Text("📞 ${supplier.phone}", style = MaterialTheme.typography.bodySmall)
                            if (supplier.email.isNotEmpty()) Text("✉️ ${supplier.email}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (supplier.deliveryTime.isNotEmpty()) {
                            Text("⏱️ Entrega: ${supplier.deliveryTime}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisTab(quotes: List<Quote>, products: List<Product>) {
    val groupedQuotes = quotes.groupBy { it.productId }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(groupedQuotes.entries.toList()) { (productId, productQuotes) ->
            val product = products.find { it.id == productId } ?: return@items
            val bestQuote = productQuotes.minByOrNull { it.price }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Costo actual: $${Utils.formatCurrency(product.cost)}", color = Gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    productQuotes.forEach { quote ->
                        val isBest = quote.id == bestQuote?.id
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBest) GreenLight else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(quote.supplier, fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal)
                                    Text("$${Utils.formatCurrency(quote.price)}", color = Green)
                                }
                                if (isBest) {
                                    Text("🏆 Mejor opción", style = MaterialTheme.typography.bodySmall, color = GreenDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}