package com.tecnisis.galeria.presentation.tasacion

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.ObraTasada
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.textoSoles
import com.tecnisis.galeria.presentation.componentes.FilaPdf
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.abrirPdf
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoDiaTasada = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

@Composable
fun TasadasRoute(
    viewModel: TasadasViewModel = viewModel(factory = TasadasViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.aviso) {
        val aviso = uiState.aviso ?: return@LaunchedEffect
        snackbar.showSnackbar(aviso)
        viewModel.onAvisoMostrado()
    }

    // El documento ya está en la caché; se abre con la app de PDF del celular
    val contexto = LocalContext.current
    LaunchedEffect(uiState.pdfParaAbrir) {
        val ruta = uiState.pdfParaAbrir ?: return@LaunchedEffect
        viewModel.onPdfAbierto(abrirPdf(contexto, ruta))
    }

    TasadasScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onVerDocumento = viewModel::onVerDocumento,
        descargarFoto = viewModel::descargarFoto
    )
}

@Composable
fun TasadasScreen(
    uiState: TasadasUiState,
    snackbarHostState: SnackbarHostState,
    onVerDocumento: (String) -> Unit,
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
            Text("Tasadas", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
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
                    "Todavía no tasaste obras",
                    "Las obras que tases aparecerán aquí con su documento de venta."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.obras, key = { it.obraId }) { obra ->
                        TarjetaTasada(
                            obra = obra,
                            descargarFoto = descargarFoto,
                            abriendo = uiState.abriendo == obra.obraId,
                            onVerDocumento = { onVerDocumento(obra.obraId) }
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/** Foto, título, técnica y fecha; los tres montos y la fila del PDF (copia del archivo). */
@Composable
private fun TarjetaTasada(
    obra: ObraTasada,
    descargarFoto: suspend (String) -> ByteArray?,
    abriendo: Boolean,
    onVerDocumento: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val fecha = obra.fechaTasacion?.let { " · tasada el ${formatoDiaTasada.format(Date(it))}" }.orEmpty()
    TarjetaGaleria(relleno = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FotoRemota(rememberFotoRemota(obra.fotoRuta, descargarFoto, ladoMaximo = 240), Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
                Text(obra.tecnica.etiqueta + fecha, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            FilaMonto("PVP", textoSoles(obra.pvpCentimos))
            HorizontalDivider(color = c.outlineVariant)
            FilaMonto("Galería (${obra.porcentaje} %)", textoSoles(obra.montoGaleriaCentimos))
            HorizontalDivider(color = c.outlineVariant)
            FilaMonto("Socio recibe", textoSoles(obra.montoSocioCentimos), destacado = true)
        }
        Spacer(Modifier.height(10.dp))
        FilaPdf(
            titulo = "Documento de venta",
            subtitulo = "Copia · Archivo de la galería",
            onClick = onVerDocumento,
            cargando = abriendo,
            fondo = c.surfaceVariant,
            selloFondo = c.tertiaryContainer,
            selloTexto = c.onTertiaryContainer
        )
    }
}

/** Una fila "rótulo ............ S/ 0.00". La destacada (lo que recibe el socio) va en verde y negrita. */
@Composable
internal fun FilaMonto(rotulo: String, valor: String, destacado: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            rotulo,
            style = if (destacado) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (destacado) c.onSurface else c.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (destacado) c.tertiary else c.onSurface
        )
    }
}

// ---------- Previews ----------

private fun tasadaEjemplo(id: String, titulo: String, tecnica: Tecnica, pvp: Long, pct: Int, galeria: Long, fecha: Long) = ObraTasada(
    obraId = id, titulo = titulo, tecnica = tecnica, fotoRuta = "", nombreSocio = "María Elena López Díaz",
    numeroSocio = "SOC-0002", pvpCentimos = pvp, porcentaje = pct, montoGaleriaCentimos = galeria,
    montoSocioCentimos = pvp - galeria, fechaTasacion = fecha
)

private val tasadasEjemplo = listOf(
    tasadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO, 250_000, 18, 45_000, 1_794_900_000_000),
    tasadaEjemplo("2", "Retablo ayacuchano", Tecnica.TECNICA_MIXTA, 125_050, 30, 37_515, 1_794_800_000_000),
    tasadaEjemplo("3", "Huaco retrato", Tecnica.CERAMICA, 80_000, 0, 0, 1_794_700_000_000)
)

@Composable
private fun VistaPrevia(uiState: TasadasUiState) {
    TecnisisGaleriaTheme {
        TasadasScreen(uiState, remember { SnackbarHostState() }, onVerDocumento = {}, descargarFoto = { null })
    }
}

@Preview(name = "Tasadas · claro", showBackground = true, heightDp = 1000)
@Preview(name = "Tasadas · nocturno", showBackground = true, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TasadasPreview() = VistaPrevia(TasadasUiState(cargando = false, obras = tasadasEjemplo))

@Preview(name = "Abriendo el PDF", showBackground = true, heightDp = 1000)
@Composable
private fun TasadasAbriendoPreview() = VistaPrevia(TasadasUiState(cargando = false, obras = tasadasEjemplo, abriendo = "1"))

@Preview(name = "Sin obras · claro", showBackground = true)
@Preview(name = "Sin obras · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TasadasVaciaPreview() = VistaPrevia(TasadasUiState(cargando = false))