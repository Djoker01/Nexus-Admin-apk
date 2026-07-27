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
        Text("Proveedores y Cotizaciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selectedTab == 0, { selectedTab = 0 }, text = { Text("Proveedores") })
            Tab(selectedTab == 1, { selectedTab = 1 }, text = { Text("Cotizaciones") })
            Tab(selectedTab == 2, { selectedTab = 2 }, text = { Text("Análisis") })
        }

        when (selectedTab) {
            0 -> {
                // Suppliers tab
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { showAddSupplier = true }) { Text("+ Proveedor") }
                    }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suppliers) { s ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(s.name, fontWeight = FontWeight.SemiBold)
                                    if (s.company.isNotEmpty()) Text(s.company, style = MaterialTheme.typography.bodySmall)
                                    if (s.phone.isNotEmpty()) Text("📞 ${s.phone}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Quotes tab
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { showAddQuote = true }) { Text("+ Cotización") }
                    }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quotes) { q ->
                            val p = products.find { it.id == q.productId }
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(p?.name ?: "Producto", fontWeight = FontWeight.SemiBold)
                                    Text("Proveedor: ${q.supplier} | Precio: $${Utils.formatCurrency(q.price)}")
                                    Text("Mín: ${q.minQuantity} | Entrega: ${q.deliveryDays} días | Envío: ${if (q.includesShipping) "Sí" else "No"}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Analysis tab
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
                                    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = if (isBest) GreenLight else MaterialTheme.colorScheme.surface)) {
                                        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(q.supplier, fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal)
                                                Text("$${Utils.formatCurrency(q.price)}", color = Green)
                                            }
                                            if (isBest) Text("🏆 Mejor", color = GreenDark, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                if (savings != null && savings > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenLight)) {
                                        Text("Ahorro potencial: $${Utils.formatCurrency(savings)} por unidad", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = GreenDark)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Supplier Dialog
    if (showAddSupplier) {
        var name by remember { mutableStateOf("") }; var company by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
        var delivery by remember { mutableStateOf("") }; var terms by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSupplier = false },
            title = { Text("Nuevo Proveedor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre *") })
                    OutlinedTextField(company, { company = it }, label = { Text("Empresa") })
                    OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") })
                    OutlinedTextField(email, { email = it }, label = { Text("Email") })
                    OutlinedTextField(delivery, { delivery = it }, label = { Text("Tiempo entrega") })
                    OutlinedTextField(terms, { terms = it }, label = { Text("Cond. pago") })
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
        var qSupplier by remember { mutableStateOf("") }; var qPrice by remember { mutableStateOf("") }
        var qMin by remember { mutableStateOf("1") }; var qDays by remember { mutableStateOf("0") }
        var qTerms by remember { mutableStateOf("") }; var qShipping by remember { mutableStateOf(false) }
        var prodExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddQuote = false },
            title = { Text("Nueva Cotización") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        OutlinedButton(onClick = { prodExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedProduct?.name ?: "Producto"); Icon(Icons.Filled.ArrowDropDown, null) }
                        DropdownMenu(prodExpanded, { prodExpanded = false }) {
                            products.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedProduct = p; prodExpanded = false }) }
                        }
                    }
                    OutlinedTextField(qSupplier, { qSupplier = it }, label = { Text("Proveedor") })
                    OutlinedTextField(qPrice, { qPrice = it }, label = { Text("Precio") }, leadingIcon = { Text("$") })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(qMin, { qMin = it }, label = { Text("Cant. mín") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(qDays, { qDays = it }, label = { Text("Días") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(qTerms, { qTerms = it }, label = { Text("Condiciones") })
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(qShipping, { qShipping = it }); Text("Incluye envío") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        selectedProduct?.let {
                            db.quoteDao().insert(Quote(productId = it.id, supplier = qSupplier, price = qPrice.toDoubleOrNull() ?: 0.0, minQuantity = qMin.toIntOrNull() ?: 1, deliveryDays = qDays.toIntOrNull() ?: 0, paymentTerms = qTerms, includesShipping = qShipping))
                            showAddQuote = false
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showAddQuote = false }) { Text("Cancelar") } }
        )
    }
}
