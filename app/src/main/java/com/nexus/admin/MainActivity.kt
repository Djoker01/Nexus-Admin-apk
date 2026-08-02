package com.nexus.admin

import android.os.Bundle
import android.util.Log
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
        
        // Capturar TODOS los errores
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            Log.e("NexusAdmin", "Crash: ${e.message}", e)
            // No hacer nada más, dejar que Android maneje el crash
        }
        
        try {
            setContent {
                NexusAdminTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        try {
                            NexusApp()
                        } catch (e: Exception) {
                            Log.e("NexusAdmin", "Error en NexusApp: ${e.message}", e)
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NexusAdmin", "Error en setContent: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
