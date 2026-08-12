package com.nexus.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.admin.ui.theme.*

data class HelpCategory(
    val icon: ImageVector,
    val title: String,
    val description: String
)

data class FaqItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    isAdmin: Boolean = true,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    val categories = if (isAdmin) {
        listOf(
            HelpCategory(Icons.Filled.Dashboard, "Dashboard", "Ventas del día, ganancias, caja y stock bajo"),
            HelpCategory(Icons.Filled.Inventory, "Inventario", "Productos, stock, precios y escaneo de códigos"),
            HelpCategory(Icons.Filled.ShoppingCart, "Ventas", "Registrar ventas, pagos combinados y devoluciones"),
            HelpCategory(Icons.Filled.AccountBalanceWallet, "Caja", "Ingresos, egresos y saldo actualizado"),
            HelpCategory(Icons.Filled.TrendingDown, "Gastos", "Control de gastos fijos y variables"),
            HelpCategory(Icons.Filled.Receipt, "Ctas por Cobrar", "Créditos, abonos y estados de pago"),
            HelpCategory(Icons.Filled.Delete, "Mermas", "Pérdidas de inventario y descuentos"),
            HelpCategory(Icons.Filled.BarChart, "Reportes", "Estadísticas y exportación a PDF"),
            HelpCategory(Icons.Filled.CloudDownload, "Respaldos", "Copia de seguridad y restauración"),
            HelpCategory(Icons.Filled.Sync, "Sincronización", "QR entre admin y trabajadores"),
            HelpCategory(Icons.Filled.FileDownload, "Excel", "Exportar e importar inventario")
        )
    } else {
        listOf(
            HelpCategory(Icons.Filled.Dashboard, "Dashboard", "Ventas del día y transacciones"),
            HelpCategory(Icons.Filled.Inventory, "Inventario", "Stock y precios de venta"),
            HelpCategory(Icons.Filled.ShoppingCart, "Ventas", "Registrar ventas con pagos combinados"),
            HelpCategory(Icons.Filled.Sync, "Sincronización", "Exportar ventas y actualizar desde admin")
        )
    }

    val faqs = if (isAdmin) {
        listOf(
            FaqItem("¿Cómo crear un producto?", "Ve a Inventario → toca 'Nuevo' → completa nombre, SKU, costo, precio y stock → Guardar."),
            FaqItem("¿Cómo registrar una venta?", "Ve a Ventas → toca 'Nueva Venta' → selecciona productos → elige método de pago → Completar Venta."),
            FaqItem("¿Cómo registrar una venta a crédito?", "En Nueva Venta, marca '¿Cuenta por cobrar?' → ingresa el cliente → no se registra en caja hasta que se abone."),
            FaqItem("¿Cómo exportar a Excel?", "Ve a Inventario → toca ⋮ (tres puntos) → 'Exportar a Excel' → guarda el archivo."),
            FaqItem("¿Cómo importar desde Excel?", "Ve a Inventario → toca ⋮ → 'Importar desde Excel' → selecciona el archivo .xlsx."),
            FaqItem("¿Cómo sincronizar con trabajadores?", "Toca 🔄 → 'Exportar datos' para generar QR → el trabajador escanea con 'Actualizar desde Admin'."),
            FaqItem("¿Cómo crear un respaldo?", "Ve a Respaldos → 'Crear Respaldo' → guarda el archivo JSON."),
            FaqItem("¿Cómo registrar una devolución?", "En Ventas, toca 'Devolución' en la venta → confirma → repone stock y descuenta de caja.")
        )
    } else {
        listOf(
            FaqItem("¿Cómo exportar mis ventas?", "Toca 🔄 → 'Exportar mis ventas' → genera QR → muéstraselo al administrador."),
            FaqItem("¿Cómo actualizar mi stock?", "Toca 🔄 → 'Actualizar desde Admin' → escanea el QR del administrador."),
            FaqItem("¿Cómo registrar una venta?", "Ve a Ventas → toca 'Nueva Venta' → selecciona productos → completa el pago."),
            FaqItem("¿Puedo ver las ganancias?", "No. Como trabajador solo ves ventas, stock y precios de venta.")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Ayuda") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Buscador
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar ayuda...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Categorías
            item {
                Text("CATEGORÍAS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            items(categories.filter { cat ->
                searchQuery.isEmpty() || cat.title.contains(searchQuery, true) || cat.description.contains(searchQuery, true)
            }) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(category.icon, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(category.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Preguntas frecuentes
            item {
                Spacer(Modifier.height(8.dp))
                Text("PREGUNTAS FRECUENTES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            items(faqs.filter { faq ->
                searchQuery.isEmpty() || faq.question.contains(searchQuery, true) || faq.answer.contains(searchQuery, true)
            }) { faq ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedFaq = if (selectedFaq == faq.question) null else faq.question
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.question, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                            Icon(
                                if (selectedFaq == faq.question) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (selectedFaq == faq.question) {
                            Spacer(Modifier.height(8.dp))
                            Text(faq.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Soporte
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📞 SOPORTE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("WhatsApp: +5355555555", style = MaterialTheme.typography.bodyMedium)
                        Text("Email: soporte@nexusadmin.app", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
