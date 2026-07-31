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
import kotlinx.coroutines.launch

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
    var attempts by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val admin = db.userDao().getAdmin()
        if (admin == null) {
            db.userDao().insert(User(name = "Admin", pin = "0000", role = "admin"))
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
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("PIN de acceso") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Lock, null) }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val user = db.userDao().getUserByPin(pin)
                    if (user != null) {
                        attempts = 0
                        onLoginSuccess(user)
                    } else {
                        attempts++
                        Toast.makeText(context, "❌ PIN incorrecto (Intento $attempts)", Toast.LENGTH_SHORT).show()
                        pin = ""
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = pin.length == 4 && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else { Icon(Icons.Filled.Login, null); Spacer(Modifier.width(8.dp)); Text("Ingresar") }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onFirstTimeSetup) { Text("Configurar usuarios") }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showChangePin = true }) { Text("Cambiar mi PIN") }
    }

    // Diálogo para cambiar PIN
    if (showChangePin) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePin = false },
            title = { Text("Cambiar PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(currentPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it }, label = { Text("PIN actual") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    HorizontalDivider()
                    OutlinedTextField(newPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it }, label = { Text("Nuevo PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(confirmPin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("Confirmar PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val user = db.userDao().getUserByPin(currentPin)
                        if (user != null && newPin.length == 4 && newPin == confirmPin) {
                            db.userDao().update(user.copy(pin = newPin))
                            showChangePin = false
                            Toast.makeText(context, "✅ PIN actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ Verifique los datos", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Cambiar") }
            },
            dismissButton = { TextButton(onClick = { showChangePin = false }) { Text("Cancelar") } }
        )
    }
}
