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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    db: AppDatabase,
    onLoginSuccess: (User) -> Unit,
    onFirstTimeSetup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }

    // Verificar si es primera instalación
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val admin = db.userDao().getAdmin()
                if (admin == null) {
                    withContext(Dispatchers.Main) {
                        showCreateAdminDialog = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla la verificación, mostrar diálogo de creación igualmente
            withContext(Dispatchers.Main) {
                showCreateAdminDialog = true
            }
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
            onValueChange = { 
                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                    pin = it
                    errorMessage = ""
                }
            },
            label = { Text("PIN de acceso") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            isError = errorMessage.isNotEmpty()
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (pin.length != 4) return@Button
                
                scope.launch {
                    isLoading = true
                    errorMessage = ""
                    
                    try {
                        // Ejecutar en hilo IO para no bloquear la UI
                        val user = withContext(Dispatchers.IO) {
                            db.userDao().getUserByPin(pin)
                        }
                        
                        if (user != null) {
                            attempts = 0
                            onLoginSuccess(user)
                        } else {
                            attempts++
                            errorMessage = "PIN incorrecto (Intento $attempts)"
                            pin = ""
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        attempts++
                        errorMessage = "Error al verificar. Intente de nuevo."
                        pin = ""
                    }
                    
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = pin.length == 4 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Login, null)
                Spacer(Modifier.width(8.dp))
                Text("Ingresar")
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onFirstTimeSetup) {
            Text("Configurar usuarios")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showChangePin = true }) {
            Text("Cambiar mi PIN")
        }
    }

    // Diálogo: Crear Administrador (primera instalación)
    if (showCreateAdminDialog) {
        var adminName by remember { mutableStateOf("") }
        var adminPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { /* No se puede cerrar */ },
            title = { Text("Configuración Inicial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Bienvenido a Nexus Admin. Crea tu cuenta de Administrador:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        adminName,
                        { adminName = it },
                        label = { Text("Tu nombre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                    Text("Crea tu PIN de acceso:", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        adminPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) adminPin = it },
                        label = { Text("PIN (4 dígitos) *") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        confirmPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirmar PIN *") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPin.isNotEmpty() && adminPin != confirmPin
                    )
                    if (confirmPin.isNotEmpty() && adminPin != confirmPin) {
                        Text(
                            "Los PIN no coinciden",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            "🔐 Guarda bien tu PIN. Es la llave de acceso como Administrador.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (adminName.isNotBlank() && adminPin.length == 4 && adminPin == confirmPin) {
                            try {
                                withContext(Dispatchers.IO) {
                                    db.userDao().insert(
                                        User(name = adminName, pin = adminPin, role = "admin")
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    showCreateAdminDialog = false
                                    Toast.makeText(
                                        context,
                                        "✅ Administrador creado. Ingresa con tu PIN",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "❌ Error al crear: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "❌ Completa todos los campos correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) { Text("Crear Administrador") }
            },
            dismissButton = null // No se puede cerrar sin crear admin
        )
    }

    // Diálogo: Cambiar PIN
    if (showChangePin) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmNewPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePin = false },
            title = { Text("Cambiar PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        currentPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("PIN actual") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    HorizontalDivider()
                    OutlinedTextField(
                        newPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("Nuevo PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        confirmNewPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                        label = { Text("Confirmar nuevo PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            val user = withContext(Dispatchers.IO) {
                                db.userDao().getUserByPin(currentPin)
                            }
                            if (user != null && newPin.length == 4 && newPin == confirmNewPin) {
                                val existingPin = withContext(Dispatchers.IO) {
                                    db.userDao().getUserByPin(newPin)
                                }
                                if (existingPin != null && existingPin.id != user.id) {
                                    Toast.makeText(
                                        context,
                                        "❌ Ese PIN ya está en uso",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                                withContext(Dispatchers.IO) {
                                    db.userDao().update(user.copy(pin = newPin))
                                }
                                showChangePin = false
                                Toast.makeText(
                                    context,
                                    "✅ PIN actualizado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "❌ Verifique los datos ingresados",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context,
                                "❌ Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) { Text("Cambiar PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePin = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
