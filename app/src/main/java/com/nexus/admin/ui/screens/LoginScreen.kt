package com.nexus.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.User
import kotlinx.coroutines.runBlocking

@Composable
fun LoginScreen(
    db: AppDatabase,
    onLoginSuccess: (User) -> Unit,
    onFirstTimeSetup: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val admin = runBlocking { db.userDao().getAdmin() }
            if (admin == null) showCreateAdminDialog = true
        } catch (e: Exception) {
            showCreateAdminDialog = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Storefront, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Nexus Admin", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Ingrese su PIN para acceder", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pin = it; errorMessage = "" } },
            label = { Text("PIN de acceso") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            isError = errorMessage.isNotEmpty()
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (pin.length != 4) return@Button
                try {
                    val user = runBlocking { db.userDao().getUserByPin(pin) }
                    if (user != null) {
                        onLoginSuccess(user)
                    } else {
                        errorMessage = "PIN incorrecto"
                        pin = ""
                    }
                } catch (e: Exception) {
                    errorMessage = "Error. Intente de nuevo."
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = pin.length == 4
        ) {
            Icon(Icons.Filled.Login, null)
            Spacer(Modifier.width(8.dp))
            Text("Ingresar")
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onFirstTimeSetup) { Text("Configurar usuarios") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showChangePin = true }) { Text("Cambiar mi PIN") }
    }

    // Crear Admin
    if (showCreateAdminDialog) {
        var adminName by remember { mutableStateOf("") }
        var adminPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {},
            title = { Text("Configuración Inicial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bienvenido. Crea tu cuenta de Administrador:")
                    OutlinedTextField(adminName, { adminName = it }, label = { Text("Tu nombre *") }, singleLine = true)
                    OutlinedTextField(adminPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) adminPin = it }, label = { Text("PIN (4 dígitos) *") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(confirmPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("Confirmar PIN *") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    if (confirmPin.isNotEmpty() && adminPin != confirmPin) Text("Los PIN no coinciden", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (adminName.isBlank() || adminPin.length != 4 || adminPin != confirmPin) {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    try {
                        runBlocking { db.userDao().insert(User(name = adminName, pin = adminPin, role = "admin")) }
                        showCreateAdminDialog = false
                        Toast.makeText(context, "✅ Admin creado. Ingresa con tu PIN", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Crear Administrador") }
            },
            dismissButton = null
        )
    }

    // Cambiar PIN
    if (showChangePin) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmNewPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePin = false },
            title = { Text("Cambiar PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(currentPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it }, label = { Text("PIN actual") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(newPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it }, label = { Text("Nuevo PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(confirmNewPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPin = it }, label = { Text("Confirmar") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val user = runBlocking { db.userDao().getUserByPin(currentPin) }
                        if (user != null && newPin.length == 4 && newPin == confirmNewPin) {
                            runBlocking { db.userDao().update(user.copy(pin = newPin)) }
                            showChangePin = false
                            Toast.makeText(context, "✅ PIN actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ Datos incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Cambiar") }
            },
            dismissButton = { TextButton(onClick = { showChangePin = false }) { Text("Cancelar") } }
        )
    }
}
