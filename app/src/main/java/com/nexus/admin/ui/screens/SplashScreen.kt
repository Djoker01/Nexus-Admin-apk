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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.admin.R
import com.nexus.admin.ui.theme.Green
import com.nexus.admin.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animación de escala (aparece suavemente)
    val scale = remember { Animatable(0.3f) }
    // Animación de opacidad (fade in)
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animar entrada
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }

        // Esperar 2 segundos antes de pasar al login
        delay(2000)

        // Animar salida (fade out)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )

        // Notificar que el splash terminó
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Azul marino oscuro
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo (usando el drawable del icono)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Nexus Admin Logo",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        this.scaleX = scale.value
                        this.scaleY = scale.value
                        this.alpha = alpha.value
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Título de la app
            Text(
                text = "Nexus Admin",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Sistema de Administración Empresarial",
                fontSize = 14.sp,
                color = White.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Indicador de carga animado
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
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value
                }
            )
        }
    }
}
