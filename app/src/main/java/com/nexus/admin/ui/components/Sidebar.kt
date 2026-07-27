package com.nexus.admin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedWidth: Dp by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 72.dp,
        animationSpec = tween(durationMillis = 300),
        label = "sidebar_width"
    )

    Surface(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight(),
        shadowElevation = if (isExpanded) 8.dp else 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header con logo y toggle
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nexus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Admin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.MenuOpen, "Colapsar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Menu, "Expandir", tint = Green, modifier = Modifier.size(28.dp))
                    }
                }
            }

            HorizontalDivider()

            // Navigation items
            LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                items(navItems) { item ->
                    val isSelected = selectedItem == item.screen
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Green.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(200)
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable {
                                onItemSelected(item.screen)
                                onClose()
                            }
                            .padding(horizontal = if (isExpanded) 16.dp else 0.dp, vertical = 12.dp),
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                if (isSelected) item.selectedIcon else item.icon,
                                item.screen.title,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -20 },
                            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -20 }
                        ) {
                            Text(
                                item.screen.title,
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider()
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("v1.0", style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
        }
    }
}
