package com.tecnisis.galeria.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = Cobalto, onPrimary = Blanco,
    primaryContainer = CobaltoVelo, onPrimaryContainer = CobaltoProfundo,
    secondary = Cadmio, onSecondary = Tinta,
    secondaryContainer = CadmioVelo, onSecondaryContainer = CadmioTexto,
    tertiary = EsmeraldaProfunda, onTertiary = Blanco,
    tertiaryContainer = EsmeraldaVelo, onTertiaryContainer = EsmeraldaTexto,
    error = BermellonTexto, onError = Blanco,
    errorContainer = BermellonVelo, onErrorContainer = BermellonTexto,
    background = Lienzo, onBackground = Tinta,
    surface = Lienzo, onSurface = Tinta,
    surfaceVariant = Papel, onSurfaceVariant = TintaSuave,
    surfaceContainerLowest = Blanco, surfaceContainerLow = Blanco, surfaceContainer = Blanco,
    surfaceContainerHigh = Papel, surfaceContainerHighest = Papel,
    outline = TintaTenue, outlineVariant = LineaClara
)

private val EsquemaOscuro = darkColorScheme(
    primary = CobaltoLuz, onPrimary = Noche,
    primaryContainer = CobaltoNocheVelo, onPrimaryContainer = CobaltoNocheTexto,
    secondary = CadmioNoche, onSecondary = Noche,
    secondaryContainer = CadmioNocheVelo, onSecondaryContainer = CadmioNocheTexto,
    tertiary = EsmeraldaNoche, onTertiary = Noche,
    tertiaryContainer = EsmeraldaNocheVelo, onTertiaryContainer = EsmeraldaNocheTexto,
    error = BermellonNocheTexto, onError = Noche,
    errorContainer = BermellonNocheVelo, onErrorContainer = BermellonNocheTexto,
    background = Noche, onBackground = Hueso,
    surface = Noche, onSurface = Hueso,
    surfaceVariant = NocheElevada, onSurfaceVariant = HuesoSuave,
    surfaceContainerLowest = NocheSuperficie, surfaceContainerLow = NocheSuperficie,
    surfaceContainer = NocheSuperficie,
    surfaceContainerHigh = NocheElevada, surfaceContainerHighest = NocheElevada,
    outline = HuesoTenue, outlineVariant = NocheLinea
)

/** Colores propios de la galería que Material 3 no trae (encabezado y manchas de pigmento). */
@Immutable
data class ColoresGaleria(
    val encabezado: Color,
    val sobreEncabezado: Color,
    val sobreEncabezadoSuave: Color,
    val pigmentoAmarillo: Color,
    val pigmentoRojo: Color,
    val exito: Color,
    val sobreExito: Color
)

private val GaleriaClara = ColoresGaleria(
    encabezado = Cobalto, sobreEncabezado = Blanco, sobreEncabezadoSuave = CobaltoNiebla,
    pigmentoAmarillo = Cadmio, pigmentoRojo = Bermellon,
    exito = EsmeraldaProfunda, sobreExito = Blanco
)

private val GaleriaOscura = ColoresGaleria(
    encabezado = CobaltoNocheEncabezado, sobreEncabezado = Hueso, sobreEncabezadoSuave = CobaltoNocheNiebla,
    pigmentoAmarillo = OroNoche, pigmentoRojo = TerracotaNoche,
    exito = EsmeraldaNocheProfunda, sobreExito = Hueso
)

private val LocalColoresGaleria = staticCompositionLocalOf { GaleriaClara }

/** Se usa igual que colorScheme: MaterialTheme.galeria.encabezado */
val MaterialTheme.galeria: ColoresGaleria
    @Composable @ReadOnlyComposable get() = LocalColoresGaleria.current

private val FormasGaleria = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TecnisisGaleriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalColoresGaleria provides if (darkTheme) GaleriaOscura else GaleriaClara
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            typography = Typography,
            shapes = FormasGaleria,
            content = content
        )
    }
}