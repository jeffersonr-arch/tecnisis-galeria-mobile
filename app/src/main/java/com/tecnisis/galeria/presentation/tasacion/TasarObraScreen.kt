package com.tecnisis.galeria.presentation.tasacion

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.ObraPorTasar
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.textoSoles
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.ChipEstadoObra
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.iniciales
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoDiaAceptada = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

private const val AYUDA_PVP = "Coma o punto para los decimales (hasta 2), sin separador de miles. Máximo S/ 1,000,000.00"
private const val AYUDA_PORCENTAJE = "Número entero de 0 a 100"

/**
 * Tasar obra. Al guardar, [onGuardada] recibe el aviso para Por tasar ("Tasación guardada…")
 * y la navegación vuelve a la lista.
 */
@Composable
fun TasarObraRoute(
    onVolver: () -> Unit,
    onGuardada: (String) -> Unit,
    viewModel: TasarObraViewModel = viewModel(factory = TasarObraViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.tituloGuardada) {
        val titulo = uiState.tituloGuardada ?: return@LaunchedEffect
        onGuardada("Tasación guardada. «$titulo» ya está lista para exhibición.")
    }

    // Mientras se guarda no se sale (se cortaría a la mitad)
    BackHandler(enabled = uiState.guardando) { }

    TasarObraScreen(
        uiState = uiState,
        onVolver = { if (!uiState.guardando) onVolver() },
        descargarFoto = viewModel::descargarFoto,
        onPvp = viewModel::onPvp,
        onPorcentaje = viewModel::onPorcentaje,
        onGuardar = viewModel::onGuardar
    )

    val obra = uiState.obra
    val montos = uiState.montos
    if (uiState.confirmando && obra != null && montos != null) {
        DialogoTasacion(
            tituloObra = obra.titulo,
            montos = montos,
            guardando = uiState.guardando,
            error = uiState.errorDialogo,
            yaTasada = uiState.yaTasada,
            onConfirmar = viewModel::onConfirmar,
            onCancelar = viewModel::onCancelar,
            onVolver = onVolver
        )
    }
}

@Composable
fun TasarObraScreen(
    uiState: TasarObraUiState,
    onVolver: () -> Unit,
    descargarFoto: suspend (String) -> ByteArray?,
    onPvp: (String) -> Unit,
    onPorcentaje: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Tasar obra", onVolver) },
        bottomBar = {
            if (uiState.obra != null) BarraGuardar(habilitado = uiState.puedeGuardar, onGuardar = onGuardar)
        }
    ) { padding ->
        val obra = uiState.obra
        when {
            uiState.cargando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            obra == null -> Text(
                uiState.errorCarga ?: "No se pudo cargar la obra",
                color = c.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FotoRemota(
                    rememberFotoRemota(obra.fotoRuta, descargarFoto),
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Cabecera(obra)
                TarjetaSocio(obra)
                TarjetaTasacion(uiState, onPvp, onPorcentaje)
                AvisoDocumentoVenta()
            }
        }
    }
}

@Composable
private fun Cabecera(obra: ObraPorTasar) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(obra.titulo, style = MaterialTheme.typography.headlineSmall)
            Text(obra.detalle, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        ChipEstadoObra(EstadoObra.ACEPTADA)
    }
}

/** Socio (solo nombre y número) y el comentario del Experto al aceptar, si lo dejó. */
@Composable
private fun TarjetaSocio(obra: ObraPorTasar) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(c.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (obra.nombreSocio.isNullOrBlank()) obra.nombreSocioVisible else iniciales(obra.nombreSocioVisible),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("SOCIO", style = MaterialTheme.typography.labelSmall, color = c.outline)
                Text(obra.nombreSocioVisible, style = MaterialTheme.typography.titleSmall)
                Text(obra.numeroSocioVisible, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
        val comentario = obra.comentarioVeredicto
        if (!comentario.isNullOrBlank()) {
            val fecha = obra.fechaVeredicto?.let { " · ACEPTADA ${formatoDiaAceptada.format(Date(it)).uppercase()}" }.orEmpty()
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.tertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("COMENTARIO DEL EXPERTO$fecha", style = MaterialTheme.typography.labelSmall, color = c.onTertiaryContainer)
                Spacer(Modifier.height(3.dp))
                Text(comentario, style = MaterialTheme.typography.bodyMedium, color = c.onTertiaryContainer)
            }
        }
    }
}

/** Los dos campos y el cálculo en vivo ("—" mientras falte un dato válido). */
@Composable
private fun TarjetaTasacion(uiState: TasarObraUiState, onPvp: (String) -> Unit, onPorcentaje: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    val colores = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surfaceContainerLowest,
        unfocusedContainerColor = c.surfaceContainerLowest,
        disabledContainerColor = c.surfaceVariant,
        unfocusedBorderColor = c.outlineVariant
    )
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Text("TASACIÓN", style = MaterialTheme.typography.labelSmall, color = c.primary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.pvp,
            onValueChange = onPvp,
            label = { Text("Precio de venta al público (PVP)") },
            prefix = { Text("S/ ") },
            placeholder = { Text("0.00") },
            isError = uiState.errorPvp != null,
            supportingText = { Text(uiState.errorPvp ?: AYUDA_PVP) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.medium,
            colors = colores,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.porcentaje,
            onValueChange = onPorcentaje,
            label = { Text("Ganancia de la galería") },
            suffix = { Text("%") },
            placeholder = { Text("0") },
            isError = uiState.errorPorcentaje != null,
            supportingText = { Text(uiState.errorPorcentaje ?: AYUDA_PORCENTAJE) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.medium,
            colors = colores,
            modifier = Modifier.width(200.dp)
        )
        Spacer(Modifier.height(8.dp))
        val montos = uiState.montos
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            FilaMonto("Precio de venta al público", montos?.let { textoSoles(it.pvpCentimos) } ?: "—")
            HorizontalDivider(color = c.outlineVariant)
            FilaMonto(
                montos?.let { "Ganancia de la galería (${it.porcentaje} %)" } ?: "Ganancia de la galería",
                montos?.let { textoSoles(it.galeriaCentimos) } ?: "—"
            )
            HorizontalDivider(color = c.outlineVariant)
            FilaMonto("Monto neto para el socio", montos?.let { textoSoles(it.socioCentimos) } ?: "—", destacado = true)
        }
    }
}

/** Sello "PDF" esmeralda y el aviso de que se genera el documento de venta por duplicado. */
@Composable
private fun AvisoDocumentoVenta() {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surfaceContainerLowest, shape = MaterialTheme.shapes.small) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = c.tertiaryContainer, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(width = 30.dp, height = 34.dp)) {
                Text(
                    "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 9.dp)
                )
            }
            Text(
                "Al guardar se genera el documento de venta por duplicado: copia del archivo y copia del artista. La tasación es definitiva.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
        }
    }
}

/** Abajo: "Guardar tasación", apagado mientras falte un dato válido. */
@Composable
private fun BarraGuardar(habilitado: Boolean, onGuardar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surfaceContainerLowest, shadowElevation = 8.dp) {
        Button(
            onClick = onGuardar,
            enabled = habilitado,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(54.dp)
        ) { Text("Guardar tasación") }
    }
}

// ---------- Previews ----------

@Composable
private fun VistaPrevia(uiState: TasarObraUiState) {
    TecnisisGaleriaTheme {
        TasarObraScreen(uiState, onVolver = {}, descargarFoto = { null }, onPvp = {}, onPorcentaje = {}, onGuardar = {})
    }
}

private val obraEjemplo = obraPorTasarEjemplo("1", "Mañana en Paracas", Tecnica.OLEO)

@Preview(name = "Datos válidos · claro", showBackground = true, heightDp = 1300)
@Preview(name = "Datos válidos · nocturno", showBackground = true, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TasarObraValidaPreview() =
    VistaPrevia(TasarObraUiState(cargando = false, obra = obraEjemplo, pvp = "2500", porcentaje = "18"))

@Preview(name = "Datos inválidos", showBackground = true, heightDp = 1300)
@Composable
private fun TasarObraErrorPreview() =
    VistaPrevia(TasarObraUiState(cargando = false, obra = obraEjemplo, pvp = "1,250.50", porcentaje = "120"))

@Preview(name = "Vacía · nocturno", showBackground = true, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TasarObraVaciaPreview() =
    VistaPrevia(TasarObraUiState(cargando = false, obra = obraPorTasarEjemplo("1", "Vasija nocturna", Tecnica.CERAMICA, comentario = null)))