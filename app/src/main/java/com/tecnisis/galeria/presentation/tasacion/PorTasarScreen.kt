package com.tecnisis.galeria.presentation.tasacion

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.ObraPorTasar
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.presentation.componentes.ChipColor
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoDia = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

/**
 * Pestaña Por tasar. [aviso] llega de Tasar obra al guardar ("Tasación guardada…"); se muestra una vez
 * y se avisa con [onAvisoMostrado] para que no vuelva a salir.
 */
@Composable
fun PorTasarRoute(
    onAbrir: (String) -> Unit,
    aviso: String?,
    onAvisoMostrado: () -> Unit,
    viewModel: PorTasarViewModel = viewModel(factory = PorTasarViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(aviso) {
        val texto = aviso ?: return@LaunchedEffect
        snackbar.showSnackbar(texto)
        onAvisoMostrado()
    }

    PorTasarScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onAbrir = onAbrir,
        descargarFoto = viewModel::descargarFoto
    )
}

@Composable
fun PorTasarScreen(
    uiState: PorTasarUiState,
    snackbarHostState: SnackbarHostState,
    onAbrir: (String) -> Unit,
    descargarFoto: suspend (String) -> ByteArray?
) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(24.dp))
            Text("Por tasar", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
            Text(
                uiState.resumen,
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            when {
                uiState.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.mensajeError != null -> Text(
                    uiState.mensajeError,
                    color = c.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp)
                )
                uiState.vacia -> EstadoVacioTasacion(
                    "No hay obras por tasar",
                    "Cuando un Especialista de Arte acepte una obra, aparecerá aquí al instante."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.obras, key = { it.id }) { obra ->
                        TarjetaPorTasar(obra, descargarFoto, onClick = { onAbrir(obra.id) })
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/** Foto, título, técnica y medidas; abajo el socio y la fecha en que se aceptó. Toca → Tasar obra. */
@Composable
private fun TarjetaPorTasar(obra: ObraPorTasar, descargarFoto: suspend (String) -> ByteArray?, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val fecha = obra.fechaVeredicto?.let { "Aceptada ${formatoDia.format(Date(it))}" } ?: EstadoObra.ACEPTADA.etiqueta
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = c.surfaceContainerLowest, shadowElevation = 2.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FotoRemota(rememberFotoRemota(obra.fotoRuta, descargarFoto, ladoMaximo = 240), Modifier.size(60.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(obra.detalle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tasar", tint = c.outline)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = c.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    obra.socioVisible,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                ChipColor(fecha, c.tertiaryContainer, c.onTertiaryContainer)
            }
        }
    }
}

/** Marco vacío con tres pigmentos, como el logo. Lo usan Por tasar y Tasadas. */
@Composable
internal fun EstadoVacioTasacion(titulo: String, texto: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val g = MaterialTheme.galeria
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceContainerLowest)
        ) {
            val lado = size.minDimension
            val margen = lado * 0.27f
            drawRect(
                color = c.onSurface,
                topLeft = Offset(margen, margen),
                size = Size(lado - 2 * margen, lado - 2 * margen),
                style = Stroke(width = lado * 0.045f)
            )
            drawCircle(c.primary, radius = lado * 0.06f, center = Offset(lado * 0.43f, lado * 0.44f))
            drawCircle(g.pigmentoAmarillo, radius = lado * 0.05f, center = Offset(lado * 0.55f, lado * 0.5f))
            drawCircle(g.pigmentoRojo, radius = lado * 0.045f, center = Offset(lado * 0.48f, lado * 0.6f))
        }
        Spacer(Modifier.height(20.dp))
        Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ---------- Previews ----------

internal fun obraPorTasarEjemplo(
    id: String,
    titulo: String,
    tecnica: Tecnica,
    socio: String? = "María Elena López Díaz",
    comentario: String? = "Buen manejo de la luz y de la textura del agua.",
    fecha: Long = 1_794_700_000_000
) = ObraPorTasar(
    id = id, solicitudId = "s", uid = "u", titulo = titulo, tecnica = tecnica, altoCm = 60.0, anchoCm = 80.0,
    profundidadCm = null, fotoRuta = "", nombreSocio = socio, numeroSocio = socio?.let { "SOC-0002" },
    comentarioVeredicto = comentario, fechaVeredicto = fecha, estado = EstadoObra.ACEPTADA
)

private val porTasarEjemplo = listOf(
    obraPorTasarEjemplo("1", "Mañana en Paracas", Tecnica.OLEO),
    obraPorTasarEjemplo("2", "Vasija nocturna", Tecnica.CERAMICA, fecha = 1_794_790_000_000),
    obraPorTasarEjemplo("3", "Huacos al sol", Tecnica.OLEO, socio = "Rosa Quispe Mamani", fecha = 1_794_880_000_000)
)

@Composable
private fun VistaPrevia(uiState: PorTasarUiState) {
    TecnisisGaleriaTheme {
        PorTasarScreen(uiState, remember { SnackbarHostState() }, onAbrir = {}, descargarFoto = { null })
    }
}

@Preview(name = "Por tasar · claro", showBackground = true, heightDp = 800)
@Preview(name = "Por tasar · nocturno", showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PorTasarPreview() = VistaPrevia(PorTasarUiState(cargando = false, obras = porTasarEjemplo))

@Preview(name = "Sin obras · claro", showBackground = true)
@Preview(name = "Sin obras · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PorTasarVaciaPreview() = VistaPrevia(PorTasarUiState(cargando = false))