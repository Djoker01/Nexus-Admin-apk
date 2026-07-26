package com.nexus.admin.ui.components

import androidx.compose.animation.*
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
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 72.dp,
        animationSpec = tween(durationMillis = 300),
        label = "sidebar_width"
    )

    Surface(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Nexus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.MenuOpen,
                            contentDescription = "Colapsar menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Expandir menu",
                            tint = Green,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Divider()

            // Navigation items
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                items(navItems) { item ->
                    val isSelected = selectedItem == item.screen
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Green.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(durationMillis = 200),
                        label = "nav_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 200),
                        label = "nav_content"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable(onClick = { onItemSelected(item.screen) })
                            .padding(
                                horizontal = if (isExpanded) 16.dp else 0.dp,
                                vertical = 12.dp
                            ),
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = item.screen.title,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                                animationSpec = tween(200),
                                initialOffsetX = { -20 }
                            ),
                            exit = fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                animationSpec = tween(200),
                                targetOffsetX = { -20 }
                            )
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.screen.title,
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
            }

            // Footer
            if (isExpanded) {
                Divider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "v1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
        }
    }
}
