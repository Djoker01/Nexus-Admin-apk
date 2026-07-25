package com.nexus.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = White,
    primaryContainer = GreenLight,
    secondary = Blue,
    tertiary = Yellow,
    background = Gray50,
    surface = White,
    onBackground = Gray900,
    onSurface = Gray900,
    error = Red,
    outline = Gray200
)

private val DarkColorScheme = darkColorScheme(
    primary = Green,
    onPrimary = Black,
    secondary = Blue,
    tertiary = Yellow,
    background = Gray900,
    surface = Gray700,
    onBackground = Gray50,
    onSurface = Gray50,
    error = Red,
    outline = Gray500
)

@Composable
fun NexusAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}