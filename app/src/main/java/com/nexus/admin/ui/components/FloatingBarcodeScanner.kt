package com.nexus.admin.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun FloatingBarcodeScanner(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) onDismiss()
    }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            if (isScanning) {
                isScanning = false
                onBarcodeScanned(barcode)
                onDismiss()
            }
        }
    }

    // Diálogo que ocupa toda la pantalla con fondo semi-transparente
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            if (hasCameraPermission) {
                // Cámara de fondo
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(previewView.surfaceProvider)

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(1920, 1080))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val barcodeScanner = BarcodeScanning.getClient()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    try {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && isScanning) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            barcodeScanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    if (barcodes.isNotEmpty() && isScanning) {
                                                        barcodes.first().rawValue?.let { value ->
                                                            scannedBarcode = value
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            imageProxy.close()
                                        }
                                    } catch (e: Exception) {
                                        imageProxy.close()
                                    }
                                }

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Capa oscura semi-transparente con recorte para el escáner
                Box(modifier = Modifier.fillMaxSize()) {
                    // Marco de escaneo centrado
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.85f)
                            .height(220.dp)
                    ) {
                        // Esquinas del marco
                        // Esquina superior izquierda
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(50.dp)
                                .border(4.dp, Color(0xFF10B981), RoundedCornerShape(topStart = 16.dp))
                        )
                        // Esquina superior derecha
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(50.dp)
                                .border(4.dp, Color(0xFF10B981), RoundedCornerShape(topEnd = 16.dp))
                        )
                        // Esquina inferior izquierda
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(50.dp)
                                .border(4.dp, Color(0xFF10B981), RoundedCornerShape(bottomStart = 16.dp))
                        )
                        // Esquina inferior derecha
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(50.dp)
                                .border(4.dp, Color(0xFF10B981), RoundedCornerShape(bottomEnd = 16.dp))
                        )

                        // Línea de escaneo animada
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.8f))
                                .align(Alignment.Center)
                        )
                    }

                    // Header con botón cerrar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Escanear producto",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            onClick = onDismiss
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, "Cerrar", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Instrucciones abajo
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Coloca el código de barras dentro del recuadro",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Se detectará automáticamente",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Botón para ingresar manualmente
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        onClick = {
                            isScanning = false
                            onDismiss()
                        }
                    ) {
                        Text(
                            "Ingresar código manualmente",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Sin permiso
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                "Sin permiso",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Permiso de cámara requerido",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Necesitamos acceso a la cámara para escanear códigos",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Otorgar permiso")
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onDismiss) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }
}
