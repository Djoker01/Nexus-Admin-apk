package com.nexus.admin.ui.screens

import android.util.Log
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
import kotlinx.coroutines.withTimeout

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

    // Verificar si hay admin al iniciar
    LaunchedEffect(Unit) {
        try {
            val admin = withTimeout(3000L) {
                withContext(Dispatchers.IO) {
                    db.userDao().getAdmin()
                }
            }
            if (admin == null) {
                showCreateAdminDialog = true
            }
        } catch (e: Exception) {
            Log.e("LoginScreen", "Error checking admin: ${e.message}", e)
            // Si timeout o error, asumir que no hay admin
            showCreateAdminDialog = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Storefront, null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("Nexus Admin", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Ingrese su PIN para acceder",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

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
            isError = errorMessage.isNotEmpty(),
            enabled = !isLoading
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
                if (pin.length != 4 || isLoading) return@Button
                
                isLoading = true
                errorMessage = ""
                
                scope.launch {
                    try {
                        // Intentar login con timeout de 5 segundos
                        val user = withTimeout(5000L) {
                            withContext(Dispatchers.IO) {
                                db.userDao().getUserByPin(pin)
                            }
                        }
                        
                        if (user != null) {
                            onLoginSuccess(user)
                        } else {
                            errorMessage = "PIN incorrecto"
                            pin = ""
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        Log.e("LoginScreen", "Login error: ${e.message}", e)
                        errorMessage = "Error. Intente de nuevo."
                        pin = ""
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = pin.length == 4 && !isLoading
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Verificando...")
                }
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

    // ========== DIÁLOGO: CREAR ADMINISTRADOR (PRIMERA INSTALACIÓN) ==========
    if (showCreateAdminDialog) {
        var adminName by remember { mutableStateOf("") }
        var adminPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { /* No se puede cerrar sin crear admin */ },
            title = { Text("Configuración Inicial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Bienvenido a Nexus Admin. Crea tu cuenta de Administrador:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        adminName, { adminName = it },
                        label = { Text("Tu nombre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreating
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
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreating
                    )
                    OutlinedTextField(
                        confirmPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirmar PIN *") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPin.isNotEmpty() && adminPin != confirmPin,
                        enabled = !isCreating
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
                Button(
                    onClick = {
                        if (adminName.isBlank() || adminPin.length != 4 || adminPin != confirmPin) {
                            Toast.makeText(context, "❌ Completa todos los campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isCreating = true
                        scope.launch {
                            try {
                                withTimeout(5000L) {
                                    withContext(Dispatchers.IO) {
                                        db.userDao().insert(
                                            User(name = adminName, pin = adminPin, role = "admin")
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    showCreateAdminDialog = false
                                    Toast.makeText(context, "✅ Admin creado. Ingresa con tu PIN", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Error creating admin: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = !isCreating && adminName.isNotBlank() && adminPin.length == 4 && adminPin == confirmPin
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Crear Administrador")
                    }
                }
            },
            dismissButton = null
        )
    }

    // ========== DIÁLOGO: CAMBIAR PIN ==========
    if (showChangePin) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmNewPin by remember { mutableStateOf("") }
        var isChanging by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isChanging) showChangePin = false },
            title = { Text("Cambiar PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        currentPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("PIN actual") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        enabled = !isChanging
                    )
                    HorizontalDivider()
                    OutlinedTextField(
                        newPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("Nuevo PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        enabled = !isChanging
                    )
                    OutlinedTextField(
                        confirmNewPin,
                        { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                        label = { Text("Confirmar nuevo PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        enabled = !isChanging
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPin.length != 4 || newPin.length != 4 || newPin != confirmNewPin) {
                            Toast.makeText(context, "❌ Verifique los datos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isChanging = true
                        scope.launch {
                            try {
                                val user = withTimeout(5000L) {
                                    withContext(Dispatchers.IO) {
                                        db.userDao().getUserByPin(currentPin)
                                    }
                                }
                                if (user != null) {
                                    val existingPin = withContext(Dispatchers.IO) {
                                        db.userDao().getUserByPin(newPin)
                                    }
                                    if (existingPin != null && existingPin.id != user.id) {
                                        Toast.makeText(context, "❌ Ese PIN ya está en uso", Toast.LENGTH_SHORT).show()
                                        isChanging = false
                                        return@launch
                                    }
                                    withContext(Dispatchers.IO) {
                                        db.userDao().update(user.copy(pin = newPin))
                                    }
                                    showChangePin = false
                                    Toast.makeText(context, "✅ PIN actualizado", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ PIN actual incorrecto", Toast.LENGTH_SHORT).show()
                                    isChanging = false
                                }
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Error changing PIN: ${e.message}", e)
                                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                isChanging = false
                            }
                        }
                    },
                    enabled = !isChanging && currentPin.length == 4 && newPin.length == 4 && newPin == confirmNewPin
                ) {
                    if (isChanging) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Cambiar PIN")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isChanging) showChangePin = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
