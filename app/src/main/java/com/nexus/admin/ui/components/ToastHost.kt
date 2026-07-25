package com.nexus.admin.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.admin.ui.theme.*
import kotlinx.coroutines.delay

data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.INFO
)

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

@Composable
fun ToastHost(
    toastState: MutableState<ToastMessage?>,
    modifier: Modifier = Modifier
) {
    val message = toastState.value
    
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        message?.let {
            val backgroundColor = when (it.type) {
                ToastType.SUCCESS -> Green
                ToastType.ERROR -> Red
                ToastType.WARNING -> Yellow
                ToastType.INFO -> Blue
            }
            
            val textColor = when (it.type) {
                ToastType.WARNING -> Color.Black
                else -> Color.White
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it.message,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            LaunchedEffect(message) {
                delay(3000)
                toastState.value = null
            }
        }
    }
}