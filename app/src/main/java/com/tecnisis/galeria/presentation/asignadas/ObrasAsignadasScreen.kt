package com.tecnisis.galeria.presentation.asignadas

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.CARGA_MAXIMA
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.ObraAsignada
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.presentation.componentes.ChipColor
import com.tecnisis.galeria.presentation.componentes.ChipEstadoObra
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

@Composable
fun ObrasAsignadasRoute(
    onAbrir: (String) -> Unit,
    viewModel: ObrasAsignadasViewModel = viewModel(factory = ObrasAsignadasViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    ObrasAsignadasScreen(
        uiState = uiState,
        onPestanaChange = viewModel::onPestanaChange,
        onAbrir = onAbrir,
        descargarFoto = viewModel::descargarFoto
    )
}

@Composable
fun ObrasAsignadasScreen(
    uiState: ObrasAsignadasUiState,
    onPestanaChange: (PestanaAsignadas) -> Unit,
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
        Text("Obras asignadas", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
        Text(
            "Las obras que el Anfitrión te asignó para evaluar",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        uiState.carga?.let { carga ->
            Spacer(Modifier.height(16.dp))
            TarjetaCarga(carga, Modifier.padding(horizontal = 16.dp))
        }
        Spacer(Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            PestanaAsignadas.entries.forEachIndexed { i, pestana ->
                val cantidad = if (pestana == PestanaAsignadas.POR_EVALUAR) " · ${uiState.porEvaluar.size}" else ""
                SegmentedButton(
                    selected = pestana == uiState.pestana,
                    onClick = { onPestanaChange(pestana) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = PestanaAsignadas.entries.size),
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
                items(uiState.visibles, key = { it.id }) { obra ->
                    TarjetaObraAsignada(obra, descargarFoto, onClick = { onAbrir(obra.id) })
                }
            }
        }
    }
}

/** "Tu carga · 3 de 5 obras sin veredicto", con una barra cadmio. */
@Composable
private fun TarjetaCarga(carga: Int, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = c.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tu carga", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "$carga de $CARGA_MAXIMA obras sin veredicto",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onSecondaryContainer
                )
            }
            LinearProgressIndicator(
                progress = { (carga.toFloat() / CARGA_MAXIMA).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.galeria.pigmentoAmarillo,
                trackColor = c.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun TarjetaObraAsignada(obra: ObraAsignada, descargarFoto: suspend (String) -> ByteArray?, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val fecha = if (obra.porEvaluar) {
        obra.fechaAsignacion?.let { "asignada ${formatoFecha.format(Date(it))}" }
    } else {
        obra.fechaVeredicto?.let { "evaluada ${formatoFecha.format(Date(it))}" }
    }
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = c.surfaceContainerLowest, shadowElevation = 2.dp) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FotoRemota(rememberFotoRemota(obra.fotoRuta, descargarFoto, ladoMaximo = 240), Modifier.size(64.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
                Text("${obra.tecnica.etiqueta} · ${obra.textoMedidas()}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(obra.socioVisible, fecha).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (obra.porEvaluar) {
                        ChipColor("Por evaluar", c.secondaryContainer, c.onSecondaryContainer)
                    } else {
                        ChipEstadoObra(obra.estado)
                    }
                }
            }
        }
    }
}

/** 60 × 80 cm (con profundidad si la tiene). Lo usa también el detalle de la obra. */
internal fun ObraAsignada.textoMedidas(): String =
    listOfNotNull(altoCm, anchoCm, profundidadCm).joinToString(" × ") {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString().replace('.', ',')
    } + " cm"

// ---------- Previews ----------

internal fun obraAsignadaEjemplo(
    id: String,
    titulo: String,
    tecnica: Tecnica,
    estado: EstadoObra = EstadoObra.EN_EVALUACION,
    socio: String? = "Rosa Quispe Mamani",
    comentario: String? = null
) = ObraAsignada(
    id = id, solicitudId = "s", titulo = titulo, tecnica = tecnica, altoCm = 60.0, anchoCm = 80.0,
    profundidadCm = null, nota = "Pinté la bahía al amanecer, cuando los botes todavía no salen.", fotoRuta = "",
    descripcionIa = "Paisaje marino al óleo: una bahía en calma con luz de amanecer, en azules intensos y ocres cálidos.",
    nombreSocio = socio, numeroSocio = socio?.let { "SOC-0001" }, estado = estado,
    fechaAsignacion = 1_794_560_000_000, fechaEnvio = 1_794_500_000_000,
    comentarioVeredicto = comentario, fechaVeredicto = if (estado.tieneVeredicto) 1_794_700_000_000 else null
)

@Preview(name = "Por evaluar · claro", showBackground = true, heightDp = 800)
@Preview(name = "Por evaluar · nocturno", showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObrasAsignadasPreview() {
    TecnisisGaleriaTheme {
        ObrasAsignadasScreen(
            uiState = ObrasAsignadasUiState(
                cargando = false,
                carga = 3,
                porEvaluar = listOf(
                    obraAsignadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO),
                    obraAsignadaEjemplo("2", "Retrato de mi abuela", Tecnica.ACRILICO, socio = "María López Díaz"),
                    obraAsignadaEjemplo("3", "Ciudad en capas", Tecnica.TECNICA_MIXTA, socio = null)
                )
            ),
            onPestanaChange = {}, onAbrir = {}, descargarFoto = { null }
        )
    }
}

@Preview(name = "Evaluadas", showBackground = true, heightDp = 700)
@Composable
private fun ObrasEvaluadasPreview() {
    TecnisisGaleriaTheme {
        ObrasAsignadasScreen(
            uiState = ObrasAsignadasUiState(
                cargando = false,
                carga = 1,
                pestana = PestanaAsignadas.EVALUADAS,
                evaluadas = listOf(
                    obraAsignadaEjemplo("4", "Huacos al sol", Tecnica.OLEO, EstadoObra.ACEPTADA),
                    obraAsignadaEjemplo("5", "Esquina de Barranco", Tecnica.ACRILICO, EstadoObra.RECHAZADA, comentario = "Craquelado")
                )
            ),
            onPestanaChange = {}, onAbrir = {}, descargarFoto = { null }
        )
    }
}

@Preview(name = "Sin obras", showBackground = true)
@Composable
private fun ObrasAsignadasVaciaPreview() {
    TecnisisGaleriaTheme {
        ObrasAsignadasScreen(ObrasAsignadasUiState(cargando = false, carga = 0), {}, {}, { null })
    }
}