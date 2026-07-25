package com.nexus.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nexus.admin.ui.NexusApp
import com.nexus.admin.ui.theme.NexusAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusAdminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NexusApp()
                }
            }
        }
    }
}