package com.tecnisis.galeria.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tecnisis.galeria.R

// Fraunces: títulos con aire de catálogo de museo
val Fraunces = FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold))

// Manrope: textos, botones y campos
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

private val Titulo = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold)
private val Texto = TextStyle(fontFamily = Manrope)

val Typography = Typography(
    displaySmall = Titulo.copy(fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = Titulo.copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = Titulo.copy(fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = Titulo.copy(fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = Titulo.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = Texto.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    titleSmall = Texto.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Texto.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Texto.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = Texto.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = Texto.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = Texto.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = Texto.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
)