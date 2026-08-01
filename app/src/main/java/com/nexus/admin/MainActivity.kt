package com.nexus.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nexus.admin.ui.NexusApp
import com.nexus.admin.ui.theme.NexusAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Capturar errores globales para evitar cierre de app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            e.printStackTrace()
            runOnUiThread {
                try {
                    Toast.makeText(
                        this,
                        "Error inesperado: ${e.message?.take(100) ?: "Desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
            }
            // Pasar al handler por defecto para que Android maneje el crash
            defaultHandler?.uncaughtException(thread, e)
        }
        
        try {
            setContent {
                NexusAdminTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NexusApp()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Error al iniciar la aplicación: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
