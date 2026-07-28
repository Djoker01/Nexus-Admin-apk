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

    var selectedTab by remember { mutableIntStateOf(0) }
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
        Text("Proveedores", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selectedTab == 0, { selectedTab = 0 }, text = { Text("Proveedores") })
            Tab(selectedTab == 1, { selectedTab = 1 }, text = { Text("Productos") })
            Tab(selectedTab == 2, { selectedTab = 2 }, text = { Text("Análisis") })
        }

        when (selectedTab) {
            0 -> SuppliersListTab(suppliers, onAdd = { showAddSupplier = true })
            1 -> QuotesListTab(quotes, products, onAdd = { showAddQuote = true })
            2 -> AnalysisTab(quotes, products)
        }
    }

    // Add Supplier Dialog
    if (showAddSupplier) {
        var name by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var delivery by remember { mutableStateOf("") }
        var terms by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSupplier = false },
            title = { Text("Nuevo Proveedor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true)
                    OutlinedTextField(company, { company = it }, label = { Text("Empresa") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, singleLine = true)
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(delivery, { delivery = it }, label = { Text("Tiempo de entrega") }, singleLine = true)
                    OutlinedTextField(terms, { terms = it }, label = { Text("Condiciones de pago") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (name.isNotBlank()) {
                            db.supplierDao().insert(Supplier(name = name, company = company, phone = phone, email = email, deliveryTime = delivery, paymentTerms = terms))
                            showAddSupplier = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddSupplier = false }) { Text("Cancelar") } }
        )
    }

    // Add Quote Dialog
    if (showAddQuote) {
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
        var qPrice by remember { mutableStateOf("") }
        var qMin by remember { mutableStateOf("1") }
        var qDays by remember { mutableStateOf("0") }
        var qTerms by remember { mutableStateOf("") }
        var qShipping by remember { mutableStateOf(false) }
        var prodExpanded by remember { mutableStateOf(false) }
        var suppExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddQuote = false },
            title = { Text("Agregar Producto a Proveedor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Selector de producto
                    ExposedDropdownMenuBox(prodExpanded, { prodExpanded = it }) {
                        OutlinedTextField(
                            selectedProduct?.name ?: "",
                            {},
                            readOnly = true,
                            label = { Text("Producto *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(prodExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(prodExpanded, { prodExpanded = false }) {
                            if (products.isEmpty()) {
                                DropdownMenuItem(text = { Text("No hay productos") }, onClick = { prodExpanded = false })
                            } else {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (Costo: $${Utils.formatCurrency(p.cost)})") },
                                        onClick = { selectedProduct = p; prodExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Selector de proveedor
                    ExposedDropdownMenuBox(suppExpanded, { suppExpanded = it }) {
                        OutlinedTextField(
                            selectedSupplier?.name ?: "",
                            {},
                            readOnly = true,
                            label = { Text("Proveedor *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(suppExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(suppExpanded, { suppExpanded = false }) {
                            if (suppliers.isEmpty()) {
                                DropdownMenuItem(text = { Text("No hay proveedores") }, onClick = { suppExpanded = false })
                            } else {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = { selectedSupplier = s; suppExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(qPrice, { qPrice = it }, label = { Text("Precio cotizado *") }, leadingIcon = { Text("$") }, singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(qMin, { qMin = it }, label = { Text("Cant. mín") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(qDays, { qDays = it }, label = { Text("Días") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(qTerms, { qTerms = it }, label = { Text("Condiciones") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(qShipping, { qShipping = it })
                        Text("¿Incluye envío?")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (selectedProduct != null && selectedSupplier != null && qPrice.isNotBlank()) {
                            db.quoteDao().insert(
                                Quote(
                                    productId = selectedProduct!!.id,
                                    supplier = selectedSupplier!!.name,
                                    price = qPrice.toDoubleOrNull() ?: 0.0,
                                    minQuantity = qMin.toIntOrNull() ?: 1,
                                    deliveryDays = qDays.toIntOrNull() ?: 0,
                                    paymentTerms = qTerms,
                                    includesShipping = qShipping
                                )
                            )
                            showAddQuote = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddQuote = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun SuppliersListTab(suppliers: List<Supplier>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("+ Proveedor", style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suppliers) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s.name, fontWeight = FontWeight.SemiBold)
                        if (s.company.isNotEmpty()) Text(s.company, style = MaterialTheme.typography.bodySmall)
                        if (s.phone.isNotEmpty()) Text("📞 ${s.phone}", style = MaterialTheme.typography.bodySmall)
                        if (s.email.isNotEmpty()) Text("✉️ ${s.email}", style = MaterialTheme.typography.bodySmall)
                        if (s.deliveryTime.isNotEmpty()) Text("⏱️ ${s.deliveryTime}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun QuotesListTab(quotes: List<Quote>, products: List<Product>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("+ Producto", style = MaterialTheme.typography.bodySmall)
            }
        }
        val grouped = quotes.groupBy { it.supplier }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(grouped.entries.toList()) { (supplier, supplierQuotes) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(supplier, fontWeight = FontWeight.Bold, color = Blue)
                        Spacer(Modifier.height(4.dp))
                        supplierQuotes.forEach { q ->
                            val p = products.find { it.id == q.productId }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p?.name ?: "Producto", style = MaterialTheme.typography.bodySmall)
                                Text("$${Utils.formatCurrency(q.price)}", fontWeight = FontWeight.Medium, color = Green)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisTab(quotes: List<Quote>, products: List<Product>) {
    val grouped = quotes.groupBy { it.productId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(grouped.entries.toList()) { (productId, productQuotes) ->
            val product = products.find { it.id == productId } ?: return@items
            val best = productQuotes.minByOrNull { it.price }
            val savings = best?.let { product.cost - it.price }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(product.name, fontWeight = FontWeight.Bold)
                    Text("Costo actual: $${Utils.formatCurrency(product.cost)}", color = Gray500)
                    Spacer(Modifier.height(8.dp))
                    Text("Cotizaciones:", fontWeight = FontWeight.SemiBold)
                    productQuotes.forEach { q ->
                        val isBest = q.id == best?.id
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isBest) GreenLight else MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(q.supplier, fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal)
                                    if (isBest) Text("🏆 Mejor", color = GreenDark, style = MaterialTheme.typography.labelSmall)
                                }
                                Text("$${Utils.formatCurrency(q.price)}", color = Green, fontWeight = FontWeight.Medium)
                                Text("Mín: ${q.minQuantity} | ${q.deliveryDays} días | Envío: ${if (q.includesShipping) "Sí" else "No"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (savings != null && savings > 0) {
                        Spacer(Modifier.height(8.dp))
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("💰 Ahorro: $${Utils.formatCurrency(savings)}/unidad", fontWeight = FontWeight.Bold, color = GreenDark)
                                Text("📈 Margen: $${Utils.formatCurrency(product.price - (best?.price ?: product.cost))}", color = GreenDark)
                            }
                        }
                    }
                }
            }
        }
    }
}
