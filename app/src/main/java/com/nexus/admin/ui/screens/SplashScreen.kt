package com.nexus.admin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.admin.R
import com.nexus.admin.ui.theme.White

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animar entrada
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Nexus Admin Logo",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nexus Admin",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sistema de Administración Empresarial",
                fontSize = 14.sp,
                color = White.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )

            Spacer(modifier = Modifier.height(48.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val loadingAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "loading_alpha"
            )

            Text(
                text = "Cargando...",
                fontSize = 12.sp,
                color = White.copy(alpha = loadingAlpha),
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}
