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
        Text("Sistema de administración", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onLoginSuccess(user)
                    } else {
                        Toast.makeText(context, "❌ PIN incorrecto", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = pin.length == 4 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.Login, null); Spacer(Modifier.width(8.dp)); Text("Ingresar")
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onFirstTimeSetup) { Text("Configurar usuarios") }
    }
}
