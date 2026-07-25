package com.nexus.admin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Buscar...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status.lowercase()) {
        "pendiente", "pending" -> BadgeColors.Error to "Pendiente"
        "parcial", "partial" -> BadgeColors.Warning to "Parcial"
        "pagado", "paid" -> BadgeColors.Success to "Pagado"
        "disponible", "available" -> BadgeColors.Success to "Disponible"
        "bajo", "low" -> BadgeColors.Warning to "Stock Bajo"
        "agotado", "out" -> BadgeColors.Error to "Agotado"
        else -> BadgeColors.Info to status
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.container
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color.content
        )
    }
}

object BadgeColors {
    data class BadgeColor(val container: androidx.compose.ui.graphics.Color, val content: androidx.compose.ui.graphics.Color)
    
    val Success = BadgeColor(
        com.nexus.admin.ui.theme.GreenLight,
        com.nexus.admin.ui.theme.GreenDark
    )
    val Error = BadgeColor(
        com.nexus.admin.ui.theme.RedLight,
        com.nexus.admin.ui.theme.Red
    )
    val Warning = BadgeColor(
        com.nexus.admin.ui.theme.YellowLight,
        com.nexus.admin.ui.theme.Black
    )
    val Info = BadgeColor(
        com.nexus.admin.ui.theme.BlueLight,
        com.nexus.admin.ui.theme.Blue
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun LoadingOverlay(isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = com.nexus.admin.ui.theme.Gray300
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = com.nexus.admin.ui.theme.Gray500
            )
        }
    }
}