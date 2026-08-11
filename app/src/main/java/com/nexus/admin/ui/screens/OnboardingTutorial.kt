package com.nexus.admin.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.admin.ui.theme.*

data class TutorialStep(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingTutorial(
    isAdmin: Boolean,
    onFinish: () -> Unit
) {
    val steps = if (isAdmin) {
        listOf(
            TutorialStep(Icons.Filled.Dashboard, "Dashboard",
                "Panel principal con ventas del día, ganancias, estado de caja y stock bajo. Gráficos de rendimiento y productos más vendidos."),
            TutorialStep(Icons.Filled.Inventory, "Inventario",
                "Gestiona productos: agrega, edita, elimina. Búsqueda por nombre o SKU, escaneo de códigos de barras con la cámara. Exporta/importa a Excel."),
            TutorialStep(Icons.Filled.ShoppingCart, "Ventas",
                "Registra nuevas ventas con cantidades manuales. Acepta pagos combinados (efectivo + transferencia) y cuentas por cobrar. Realiza devoluciones."),
            TutorialStep(Icons.Filled.AccountBalanceWallet, "Caja",
                "Controla ingresos y egresos. El saldo se actualiza automáticamente con ventas, transferencias, gastos y devoluciones."),
            TutorialStep(Icons.Filled.TrendingDown, "Gastos",
                "Registra gastos del negocio por categorías. Márcalos como fijos o variables y descuenta directamente de caja."),
            TutorialStep(Icons.Filled.Receipt, "Ctas por Cobrar",
                "Gestiona ventas a crédito. Crea clientes, cuentas por cobrar y registra abonos. Los abonos en efectivo se reflejan en caja."),
            TutorialStep(Icons.Filled.Delete, "Mermas",
                "Registra pérdidas de inventario: desperdicio, caducidad, robo u otros. El stock se descuenta automáticamente."),
            TutorialStep(Icons.Filled.BarChart, "Reportes",
                "Genera reportes diarios, semanales, mensuales o anuales. Exporta a PDF con estadísticas completas."),
            TutorialStep(Icons.Filled.CloudDownload, "Respaldos",
                "Crea copias de seguridad de todos los datos en formato JSON. Restaura respaldos anteriores o elimina todos los datos."),
            TutorialStep(Icons.Filled.Sync, "Sincronización",
                "Comparte datos entre dispositivos sin internet. El administrador exporta el stock actualizado, el trabajador exporta sus ventas. Escaneo QR para importar."),
            TutorialStep(Icons.Filled.FileDownload, "Exportación/Importación",
                "Exporta el inventario a Excel para análisis externo. Importa productos desde archivos Excel para cargar datos rápidamente.")
        )
    } else {
        listOf(
            TutorialStep(Icons.Filled.Dashboard, "Dashboard",
                "Panel principal con las ventas del día y transacciones. Visualiza el rendimiento de tu turno."),
            TutorialStep(Icons.Filled.Inventory, "Inventario",
                "Consulta el stock y precios de venta. Utiliza el escáner de códigos de barras para buscar productos rápidamente."),
            TutorialStep(Icons.Filled.ShoppingCart, "Ventas",
                "Registra nuevas ventas con cantidades manuales. Acepta pagos combinados y cuentas por cobrar."),
            TutorialStep(Icons.Filled.Sync, "Sincronización",
                "Exporta tus ventas del día como QR para el administrador. Escanea el QR del admin para actualizar tu stock y lista de productos.")
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bienvenido a Nexus Admin") },
                navigationIcon = {},
                actions = {
                    TextButton(onClick = onFinish) { Text("Saltar") }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicador de progreso
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        repeat(steps.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    // Botón de finalizar en la última página
                    if (pagerState.currentPage == steps.size - 1) {
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Comenzar")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onFinish) { Text("Saltar") }
                            Text("${pagerState.currentPage + 1}/${steps.size}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            val step = steps[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
