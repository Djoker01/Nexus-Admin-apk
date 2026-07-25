package com.nexus.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.admin.ui.navigation.Screen
import com.nexus.admin.ui.theme.*

data class NavItemData(
    val screen: Screen,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val navItems = listOf(
    NavItemData(Screen.Dashboard, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    NavItemData(Screen.Inventory, Icons.Outlined.Inventory, Icons.Filled.Inventory),
    NavItemData(Screen.Sales, Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart),
    NavItemData(Screen.Cash, Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    NavItemData(Screen.Expenses, Icons.Outlined.TrendingDown, Icons.Filled.TrendingDown),
    NavItemData(Screen.Receivables, Icons.Outlined.People, Icons.Filled.People),
    NavItemData(Screen.Shrinkage, Icons.Outlined.Delete, Icons.Filled.Delete),
    NavItemData(Screen.Restock, Icons.Outlined.Refresh, Icons.Filled.Refresh),
    NavItemData(Screen.Suppliers, Icons.Outlined.Business, Icons.Filled.Business),
    NavItemData(Screen.Reports, Icons.Outlined.Assessment, Icons.Filled.Assessment),
    NavItemData(Screen.Backup, Icons.Outlined.CloudDownload, Icons.Filled.CloudDownload)
)

@Composable
fun Sidebar(
    selectedItem: Screen,
    onItemSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(260.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row {
                    Text(
                        text = "Nexus",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                }
            }
            
            HorizontalDivider()
            
            // Navigation
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(navItems) { item ->
                    val isSelected = selectedItem == item.screen
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemSelected(item.screen) }
                            .background(
                                if (isSelected) Green.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.screen.title,
                            tint = if (isSelected) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = item.screen.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .background(Green)
                            )
                        }
                    }
                }
            }
        }
    }
}