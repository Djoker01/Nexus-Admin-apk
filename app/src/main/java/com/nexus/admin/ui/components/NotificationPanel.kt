package com.nexus.admin.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.entity.AppNotification
import com.nexus.admin.ui.theme.*
import com.nexus.admin.utils.Utils

@Composable
fun NotificationPanel(
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(350.dp)
            .heightIn(max = 500.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onMarkAllRead) {
                    Text("Marcar todas")
                }
            }
            
            HorizontalDivider()
            
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay notificaciones", color = Gray500)
                }
            } else {
                LazyColumn {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) },
                            onMarkAsRead = { onMarkAsRead(notification) },
                            onDelete = { onDelete(notification) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (!notification.read) GreenLight else MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = when (notification.type) {
                "danger" -> Icons.Filled.Error
                "warning" -> Icons.Filled.Warning
                else -> Icons.Filled.Info
            },
            contentDescription = null,
            tint = when (notification.type) {
                "danger" -> Red
                "warning" -> Yellow
                else -> Blue
            },
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notification.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!notification.read) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                Utils.formatDate(notification.date),
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
        
        Column {
            if (!notification.read) {
                IconButton(
                    onClick = onMarkAsRead,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Marcar leída", modifier = Modifier.size(16.dp))
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
            }
        }
    }
}