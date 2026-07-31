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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.admin.ui.theme.*

data class HelpItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

val helpItems = listOf(
    HelpItem(Icons.Filled.Dashboard, "Dashboard", "Pantalla principal donde verás las ventas del día, productos más vendidos y últimas transacciones. Los administradores también ven ganancias y estado de caja."),
    HelpItem(Icons.Filled.Inventory, "Inventario", "Gestiona todos tus productos. Puedes buscar por nombre o SKU, escanear códigos de barras con la cámara, y exportar/importar a Excel. El stock se actualiza automáticamente con las ventas."),
    HelpItem(Icons.Filled.ShoppingCart, "Ventas", "Registra nuevas ventas seleccionando productos manualmente o escaneando códigos. Acepta pagos combinados (efectivo + transferencia). Puedes marcar ventas como cuentas por cobrar."),
    HelpItem(Icons.Filled.AccountBalanceWallet, "Caja", "Registra ingresos y egresos manuales. El saldo se actualiza automáticamente con las ventas en efectivo, transferencias, gastos y devoluciones."),
    HelpItem(Icons.Filled.TrendingDown, "Gastos", "Registra gastos del negocio por categorías. Puedes marcarlos como fijos (recurrentes) y descontarlos directamente de caja."),
    HelpItem(Icons.Filled.People, "Ctas por Cobrar", "Gestiona ventas a crédito. Registra clientes, crea cuentas por cobrar y recibe abonos parciales o totales. Los abonos en efectivo se registran en caja."),
    HelpItem(Icons.Filled.Delete, "Mermas", "Registra pérdidas de inventario por desperdicio, caducidad, robo u otros motivos. El stock se descuenta automáticamente."),
    HelpItem(Icons.Filled.Assessment, "Reportes", "Genera reportes diarios, semanales, mensuales o anuales con estadísticas de ventas, ganancias, métodos de pago y productos más vendidos. Puedes exportar a PDF."),
    HelpItem(Icons.Filled.CloudDownload, "Respaldos", "Crea copias de seguridad de todos tus datos en formato JSON. Puedes restaurar respaldos anteriores o eliminar todos los datos."),
    HelpItem(Icons.Filled.Sync, "Sincronización", "Comparte datos entre dispositivos sin internet. El trabajador exporta sus ventas del día como un código QR y el administrador lo escanea para importarlas."),
    HelpItem(Icons.Filled.QrCode, "Código QR", "Cada negocio tiene un código QR único. Compártelo con tus trabajadores para que se conecten a tu negocio. También se usa para exportar/importar datos de ventas."),
    HelpItem(Icons.Filled.Lock, "PIN de acceso", "Cada usuario tiene un PIN de 4 dígitos. El administrador puede crear usuarios trabajadores desde 'Configurar usuarios'. Puedes cambiar tu PIN en la pantalla de login.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onDismiss: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guía de Uso - Nexus Admin") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("📱 Bienvenido a Nexus Admin", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Esta guía te ayudará a entender todas las funciones de la aplicación. Desliza hacia abajo para ver todas las secciones.")
                    }
                }
            }
            items(helpItems) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(item.icon, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("💡 Consejos", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("• El PIN de administrador por defecto es 0000")
                        Text("• Puedes escanear códigos de barras con la cámara en Inventario y Ventas")
                        Text("• La sincronización por QR funciona sin internet")
                        Text("• Recuerda hacer respaldos periódicos de tus datos")
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
