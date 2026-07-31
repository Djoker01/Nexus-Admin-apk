package com.nexus.admin.ui.screens

import android.widget.Toast
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
import com.nexus.admin.data.entity.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(db: AppDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var adminPin by remember { mutableStateOf("") }
    var isAdminVerified by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { db.userDao().getAllUsers().collect { users = it } }

    if (!isAdminVerified) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Verificación de Admin") },
            text = {
                Column {
                    Text("Ingrese el PIN del administrador:")
                    OutlinedTextField(adminPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) adminPin = it }, label = { Text("PIN") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val admin = db.userDao().getUserByPin(adminPin)
                        if (admin != null && admin.role == "admin") isAdminVerified = true
                        else Toast.makeText(context, "❌ PIN incorrecto", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Verificar") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Volver") } }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gestión de Usuarios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) { Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(4.dp)); Text("Nuevo") }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.SemiBold)
                            Text("Rol: ${if (user.role == "admin") "Administrador" else "Trabajador"}", style = MaterialTheme.typography.bodySmall)
                            Text("PIN: ${user.pin}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (user.role != "admin") {
                            IconButton(onClick = { scope.launch { db.userDao().delete(user) } }) { Icon(Icons.Filled.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var role by remember { mutableStateOf("worker") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(pin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it }, label = { Text("PIN (4 dígitos)") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rol: "); Spacer(Modifier.width(8.dp))
                        FilterChip(role == "worker", { role = "worker" }, label = { Text("Trabajador") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(role == "admin", { role = "admin" }, label = { Text("Admin") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (name.isNotBlank() && pin.length == 4) {
                            db.userDao().insert(User(name = name, pin = pin, role = role))
                            showAddDialog = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}
