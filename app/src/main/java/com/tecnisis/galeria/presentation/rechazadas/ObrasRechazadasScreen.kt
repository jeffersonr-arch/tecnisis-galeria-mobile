package com.tecnisis.galeria.presentation.rechazadas

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.ObraRechazada
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.FilaPdf
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.abrirPdf
import com.tecnisis.galeria.presentation.theme.galeria
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

private val formatoDia = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

@Composable
fun ObrasRechazadasRoute(
    onVolver: () -> Unit,
    viewModel: ObrasRechazadasViewModel = viewModel(factory = ObrasRechazadasViewModel.Factory)
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

    ObrasRechazadasScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onVolver = onVolver,
        onVerDocumento = viewModel::onVerDocumento
    )
}

@Composable
fun ObrasRechazadasScreen(
    uiState: ObrasRechazadasUiState,
    snackbarHostState: SnackbarHostState,
    onVolver: () -> Unit,
    onVerDocumento: (String) -> Unit
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Obras rechazadas", onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.cargando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.mensajeError != null -> Text(
                uiState.mensajeError,
                color = c.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )

            uiState.vacia -> EstadoVacio(Modifier.padding(padding))

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        uiState.resumen,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                items(uiState.obras, key = { it.id }) { obra ->
                    TarjetaRechazada(
                        obra = obra,
                        abriendo = uiState.abriendo == obra.id,
                        onVerDocumento = { onVerDocumento(obra.id) }
                    )
                }
            }
        }
    }
}

/** Título, técnica y medidas, fecha, socio, Especialista, motivo y el PDF (solo si existe). */
@Composable
private fun TarjetaRechazada(obra: ObraRechazada, abriendo: Boolean, onVerDocumento: () -> Unit) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
                Text(obra.detalle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            obra.fechaVeredicto?.let {
                Text(formatoDia.format(Date(it)), style = MaterialTheme.typography.labelLarge, color = c.error)
            }
        }
        Spacer(Modifier.height(10.dp))
        Dato("Socio", obra.socioVisible)
        Dato("Especialista", obra.especialistaVisible)
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.errorContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("MOTIVO", style = MaterialTheme.typography.labelSmall, color = c.onErrorContainer)
            Spacer(Modifier.height(3.dp))
            Text(obra.motivo.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
        }
        if (obra.tieneDocumento) {
            Spacer(Modifier.height(10.dp))
            FilaPdf(
                titulo = "Documento de rechazo",
                subtitulo = null,
                onClick = onVerDocumento,
                cargando = abriendo,
                fondo = c.surfaceVariant
            )
        }
    }
}

@Composable
private fun Dato(rotulo: String, valor: String) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(valor, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
    }
}

/** Marco vacío con tres pigmentos, como el logo. */
@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
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
        Text("No hay obras rechazadas", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Cuando un Especialista de Arte rechace una obra, aparecerá aquí al instante.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun rechazadaEjemplo(
    id: String, titulo: String, tecnica: Tecnica, socio: String?, numero: String?,
    especialista: String?, motivo: String, fecha: Long, documento: Boolean = true
) = ObraRechazada(
    id = id, titulo = titulo, tecnica = tecnica, altoCm = 120.0, anchoCm = 90.0, profundidadCm = null,
    nombreSocio = socio, numeroSocio = numero, evaluadaPorNombre = especialista, motivo = motivo, fechaVeredicto = fecha,
    documentoRechazo = if (documento) listOf("documentos_obras/$id/archivo.pdf", "documentos_obras/$id/artista.pdf") else emptyList()
)

private val rechazadasEjemplo = listOf(
    rechazadaEjemplo(
        "1", "Telar de Ocongate", Tecnica.TEXTIL, "María Elena López Díaz", "SOC-0002", "Jorge Huamán Ccori",
        "Los bordes del tejido están deshilachados; no está en condiciones de exhibirse.", 1_794_700_000_000
    ),
    rechazadaEjemplo(
        "2", "Esquina de Barranco", Tecnica.ACRILICO, "Rosa Quispe Mamani", "SOC-0001", "Andrés Salazar Ríos",
        "La firma tapa parte de la composición y el lienzo está manchado.", 1_794_600_000_000
    ),
    rechazadaEjemplo(
        "3", "Telar de prueba", Tecnica.TEXTIL, "María Elena López Díaz", "SOC-0002", null,
        "Obra rechazada en la prueba de la HU08, antes del documento.", 1_794_100_000_000, documento = false
    )
)

@Composable
private fun VistaPrevia(uiState: ObrasRechazadasUiState) {
    TecnisisGaleriaTheme {
        ObrasRechazadasScreen(uiState, remember { SnackbarHostState() }, onVolver = {}, onVerDocumento = {})
    }
}

@Preview(name = "Con obras · claro", showBackground = true, heightDp = 1100)
@Preview(name = "Con obras · nocturno", showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObrasRechazadasPreview() =
    VistaPrevia(ObrasRechazadasUiState(cargando = false, obras = rechazadasEjemplo))

@Preview(name = "Abriendo el PDF", showBackground = true, heightDp = 1100)
@Composable
private fun ObrasRechazadasAbriendoPreview() =
    VistaPrevia(ObrasRechazadasUiState(cargando = false, obras = rechazadasEjemplo, abriendo = "1"))

@Preview(name = "Sin obras · claro", showBackground = true)
@Preview(name = "Sin obras · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObrasRechazadasVaciaPreview() =
    VistaPrevia(ObrasRechazadasUiState(cargando = false))