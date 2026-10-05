package com.tecnisis.galeria.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Vino, onPrimary = Color.White,
    secondary = Dorado, onSecondary = Carbon,
    tertiary = Dorado,
    background = Marfil, onBackground = Carbon,
    surface = Marfil, onSurface = Carbon
)

private val DarkColorScheme = darkColorScheme(
    primary = VinoClaro, onPrimary = Vino,
    secondary = DoradoClaro, onSecondary = Carbon,
    background = Carbon, onBackground = Marfil,
    surface = CarbonClaro, onSurface = Marfil
)
@Composable
fun TecnisisGaleriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}