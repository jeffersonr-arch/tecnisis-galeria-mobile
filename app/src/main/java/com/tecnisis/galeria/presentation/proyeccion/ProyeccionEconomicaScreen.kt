package com.tecnisis.galeria.presentation.proyeccion

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.AgruparPor
import com.tecnisis.galeria.domain.model.FormatoReporte
import com.tecnisis.galeria.domain.model.ObraProyectada
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.calcularProyeccion
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.compartirArchivo
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun ProyeccionEconomicaRoute(
    viewModel: ProyeccionViewModel = viewModel(factory = ProyeccionViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    // Archivo listo: menú Compartir de Android
    LaunchedEffect(uiState.archivoParaCompartir) {
        val archivo = uiState.archivoParaCompartir ?: return@LaunchedEffect
        val abierto = compartirArchivo(contexto, archivo.ruta, archivo.formato.tipoMime, "Reporte de proyección económica")
        viewModel.onCompartido(abierto)
    }

    ProyeccionEconomicaScreen(
        uiState = uiState,
        onAgrupar = viewModel::onAgrupar,
        onExportar = viewModel::onExportar
    )
}

@Composable
fun ProyeccionEconomicaScreen(
    uiState: ProyeccionUiState,
    onAgrupar: (AgruparPor) -> Unit,
    onExportar: (FormatoReporte) -> Unit
) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
    ) {
        val proyeccion = uiState.proyeccion
        when {
            uiState.cargando ->
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Encabezado()
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }

            uiState.mensajeError != null || proyeccion == null ->
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Encabezado()
                    EstadoError(uiState.mensajeError ?: "No se pudo cargar la proyección. Inténtalo de nuevo")
                }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Encabezado(Modifier.padding(horizontal = 8.dp)) }
                item { TarjetaIndicador(uiState.textoTotal, proyeccion.resumen) }
                item { BotonesExportar(uiState.puedeExportar, onExportar) }
                uiState.avisoExportar?.let { aviso -> item { AvisoExportar(aviso) } }
                item { ChipsAgrupar(uiState.agrupar, onAgrupar) }
                item { TarjetaGrafico(uiState) }
            }
        }
    }
}

@Composable
private fun Encabezado(modifier: Modifier = Modifier) {
    Column(modifier) {
        Spacer(Modifier.height(24.dp))
        Text("Proyección", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ganancia de la galería por obra tasada · en tiempo real",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EstadoError(mensaje: String) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = c.outline, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("No se pudo cargar la proyección", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Ingreso total proyectado (grande) y la línea chica con obras, PVP total y lo que reciben los socios. */
@Composable
private fun TarjetaIndicador(total: String, resumen: String) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(horizontal = 16.dp, vertical = 18.dp)) {
        Text("INGRESO TOTAL PROYECTADO", style = MaterialTheme.typography.labelMedium, color = c.tertiary)
        Spacer(Modifier.height(6.dp))
        Text(total, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(resumen, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

@Composable
private fun BotonesExportar(habilitados: Boolean, onExportar: (FormatoReporte) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BotonExportar("Exportar PDF", Icons.Outlined.PictureAsPdf, habilitados, Modifier.weight(1f)) {
            onExportar(FormatoReporte.PDF)
        }
        BotonExportar("Exportar CSV", Icons.Outlined.TableChart, habilitados, Modifier.weight(1f)) {
            onExportar(FormatoReporte.CSV)
        }
    }
}

@Composable
private fun BotonExportar(texto: String, icono: ImageVector, habilitado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = habilitado, modifier = modifier.heightIn(min = 48.dp)) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto)
    }
}

@Composable
private fun AvisoExportar(aviso: String) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = c.onErrorContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(aviso, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
        }
    }
}

/** Por obra · Por socio */
@Composable
private fun ChipsAgrupar(agrupar: AgruparPor, onAgrupar: (AgruparPor) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipAgrupar("Por obra", agrupar == AgruparPor.OBRA) { onAgrupar(AgruparPor.OBRA) }
        ChipAgrupar("Por socio", agrupar == AgruparPor.SOCIO) { onAgrupar(AgruparPor.SOCIO) }
    }
}

@Composable
private fun ChipAgrupar(texto: String, elegido: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    FilterChip(
        selected = elegido,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (elegido) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = c.primaryContainer,
            selectedLabelColor = c.onPrimaryContainer,
            selectedLeadingIconColor = c.onPrimaryContainer
        )
    )
}

@Composable
private fun TarjetaGrafico(uiState: ProyeccionUiState) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Text("Ganancia de la galería", style = MaterialTheme.typography.titleLarge)
        Text(uiState.subtituloGrafico, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        val barras = uiState.barras
        if (barras.isEmpty()) {
            SinObras()
        } else {
            GraficoBarras(
                etiquetas = barras.map { it.etiqueta },
                centimos = barras.map { it.gananciaCentimos },
                esOtras = barras.map { it.esOtras },
                descripcion = uiState.descripcionGrafico,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ALTO_FILA_BARRA * barras.size + 16.dp)
            )
        }
    }
}

@Composable
private fun SinObras() {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.BarChart, contentDescription = null, tint = c.outline, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(10.dp))
        Text("Todavía no hay obras tasadas", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text(
            "Aparecerán aquí cuando el Especialista de Ventas tase una obra.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// --- Vistas previas ---

private fun obraEjemplo(titulo: String, uid: String, pvp: Long, porcentaje: Int): ObraProyectada {
    val galeria = (pvp * porcentaje + 50) / 100
    val (nombre, numero) = when (uid) {
        "maria" -> "María Elena López Díaz" to "SOC-0002"
        "rosa" -> "Rosa Quispe Mamani" to "SOC-0001"
        else -> null to null
    }
    return ObraProyectada(titulo, uid, titulo, Tecnica.OLEO, nombre, numero, pvp, porcentaje, galeria, pvp - galeria, null)
}

private val proyeccionEjemplo = calcularProyeccion(
    listOf(
        obraEjemplo("Mañana, en Paracas", "maria", 450_000, 30),
        obraEjemplo("Retablo de Sarhua", "rosa", 320_000, 35),
        obraEjemplo("Nocturno limeño", "maria", 280_000, 30),
        obraEjemplo("Cerros de Ayacucho", "rosa", 240_000, 30),
        obraEjemplo("Tejido de Taquile", "sin", 180_000, 35),
        obraEjemplo("Bodegón con lúcuma", "maria", 195_000, 25),
        obraEjemplo("Huaco contemporáneo", "rosa", 150_000, 30),
        obraEjemplo("Lluvia en Barranco", "maria", 125_050, 30),
        obraEjemplo("Grabado del Rímac", "rosa", 110_000, 25),
        obraEjemplo("Retrato de la abuela", "maria", 90_000, 25),
        obraEjemplo("Estudio en azul", "rosa", 65_000, 20),
        obraEjemplo("Boceto I", "maria", 40_000, 0)
    )
)

@Composable
private fun VistaPrevia(uiState: ProyeccionUiState) {
    TecnisisGaleriaTheme {
        ProyeccionEconomicaScreen(uiState, {}, {})
    }
}

@Preview(name = "Por obra · claro", showBackground = true, heightDp = 1150)
@Preview(name = "Por obra · nocturno", showBackground = true, heightDp = 1150, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProyeccionPorObraPreview() =
    VistaPrevia(ProyeccionUiState(cargando = false, proyeccion = proyeccionEjemplo))

@Preview(name = "Por socio", showBackground = true, heightDp = 800)
@Composable
private fun ProyeccionPorSocioPreview() =
    VistaPrevia(ProyeccionUiState(cargando = false, proyeccion = proyeccionEjemplo, agrupar = AgruparPor.SOCIO))

@Preview(name = "Sin obras · claro", showBackground = true, heightDp = 800)
@Preview(name = "Sin obras · nocturno", showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProyeccionVaciaPreview() =
    VistaPrevia(ProyeccionUiState(cargando = false, proyeccion = calcularProyeccion(emptyList())))

@Preview(name = "Aviso al exportar", showBackground = true, heightDp = 900)
@Composable
private fun ProyeccionAvisoPreview() = VistaPrevia(
    ProyeccionUiState(cargando = false, proyeccion = proyeccionEjemplo, avisoExportar = ProyeccionViewModel.AVISO_NO_SE_GENERO)
)

@Preview(name = "Error · claro", showBackground = true)
@Preview(name = "Error · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProyeccionErrorPreview() = VistaPrevia(
    ProyeccionUiState(cargando = false, mensajeError = "Sin conexión. Revisa tu internet y vuelve a entrar.")
)