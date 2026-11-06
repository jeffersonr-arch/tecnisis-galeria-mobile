package com.tecnisis.galeria.presentation.solicitudes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.EtapaRevision
import com.tecnisis.galeria.domain.model.ObraResumen
import com.tecnisis.galeria.domain.model.SolicitudResumen
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.etapa
import com.tecnisis.galeria.presentation.componentes.ChipColor
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

@Composable
fun SolicitudesRoute(
    onAbrir: (String) -> Unit,
    viewModel: SolicitudesViewModel = viewModel(factory = SolicitudesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    SolicitudesScreen(
        uiState = uiState,
        onPestanaChange = viewModel::onPestanaChange,
        onAbrir = onAbrir,
        descargarFoto = viewModel::descargarFoto
    )
}

@Composable
fun SolicitudesScreen(
    uiState: SolicitudesUiState,
    onPestanaChange: (PestanaSolicitudes) -> Unit,
    onAbrir: (String) -> Unit,
    descargarFoto: suspend (String) -> ByteArray?
) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Solicitudes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
        Text(
            "Solicitudes de exhibición de los socios",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            PestanaSolicitudes.entries.forEachIndexed { i, pestana ->
                val cantidad = if (pestana == PestanaSolicitudes.POR_REVISAR) " · ${uiState.porRevisar.size}" else ""
                SegmentedButton(
                    selected = pestana == uiState.pestana,
                    onClick = { onPestanaChange(pestana) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = PestanaSolicitudes.entries.size),
                    icon = {}
                ) { Text(pestana.titulo + cantidad, style = MaterialTheme.typography.labelLarge) }
            }
        }
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
            uiState.visibles.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    uiState.pestana.textoVacio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.visibles, key = { it.id }) { solicitud ->
                    TarjetaSolicitud(solicitud, descargarFoto, onClick = { onAbrir(solicitud.id) })
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(s: SolicitudResumen, descargarFoto: suspend (String) -> ByteArray?, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val etapa = s.etapa()
    val (fondo, texto) = coloresEtapa(etapa)
    val fecha = s.fechaEnvio?.let { formatoFecha.format(Date(it)) } ?: "sin fecha"
    val obras = if (s.obras.size == 1) "1 obra" else "${s.obras.size} obras"
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = c.surfaceContainerLowest, shadowElevation = 2.dp) {
        TarjetaContenido {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.nombreSocio, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOf(s.numeroSocio, obras, "enviada $fecha").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.obras.take(4).forEach { obra ->
                        FotoRemota(rememberFotoRemota(obra.fotoRuta, descargarFoto, ladoMaximo = 160), Modifier.size(40.dp))
                    }
                }
                ChipColor(etapa.etiqueta, fondo, texto)
            }
            HorizontalDivider(color = c.outlineVariant)
            Text(
                s.obras.map { it.tecnica.etiqueta }.distinct().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaContenido(contenido: @Composable () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { contenido() }
}

@Composable
fun coloresEtapa(etapa: EtapaRevision): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (etapa) {
        EtapaRevision.POR_ANALIZAR -> c.surfaceVariant to c.onSurfaceVariant
        EtapaRevision.ANALIZANDO -> c.secondaryContainer to c.onSecondaryContainer
        EtapaRevision.LISTA -> c.primaryContainer to c.onPrimaryContainer
        EtapaRevision.FALTA_EXPERTO, EtapaRevision.RECHAZADA -> c.errorContainer to c.onErrorContainer
        EtapaRevision.APROBADA -> c.tertiaryContainer to c.onTertiaryContainer
    }
}

private fun ejemplo(id: String, nombre: String, numero: String, analisis: EstadoAnalisis, sinExperto: Int?, vararg tecnicas: Tecnica) =
    SolicitudResumen(
        id = id, nombreSocio = nombre, numeroSocio = numero, estado = EstadoSolicitud.ENVIADA,
        analisis = analisis, obrasSinExperto = sinExperto, fechaEnvio = 1_794_500_000_000,
        obras = tecnicas.mapIndexed { i, t -> ObraResumen("$id$i", t, "") }
    )

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SolicitudesScreenPreview() {
    TecnisisGaleriaTheme {
        SolicitudesScreen(
            uiState = SolicitudesUiState(
                cargando = false,
                porRevisar = listOf(
                    ejemplo("a", "Rosa Quispe Mamani", "SOC-0001", EstadoAnalisis.LISTO, 0, Tecnica.OLEO, Tecnica.TEXTIL, Tecnica.CERAMICA),
                    ejemplo("b", "María López Díaz", "SOC-0002", EstadoAnalisis.PENDIENTE, null, Tecnica.GRABADO)
                )
            ),
            onPestanaChange = {},
            onAbrir = {},
            descargarFoto = { null }
        )
    }
}

@Preview(name = "Vacía", showBackground = true)
@Composable
private fun SolicitudesVaciaPreview() {
    TecnisisGaleriaTheme {
        SolicitudesScreen(SolicitudesUiState(cargando = false), {}, {}, { null })
    }
}