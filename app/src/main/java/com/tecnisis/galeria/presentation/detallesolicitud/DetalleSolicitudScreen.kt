package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.CARGA_MAXIMA
import com.tecnisis.galeria.domain.model.CandidatoExperto
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.EstadoEspecialista
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.MOTIVO_MAXIMO
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.SolicitudEnRevision
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.seleccionInicial
import com.tecnisis.galeria.presentation.componentes.abrirPdf
import com.tecnisis.galeria.presentation.componentes.Aviso
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.ChipColor
import com.tecnisis.galeria.presentation.componentes.FilaExperto
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.componentes.iniciales
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM · h:mm a", Locale.forLanguageTag("es-PE"))

@Composable
fun DetalleSolicitudRoute(
    onVolver: () -> Unit,
    viewModel: DetalleSolicitudViewModel = viewModel(factory = DetalleSolicitudViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.aviso) {
        val aviso = uiState.aviso ?: return@LaunchedEffect
        snackbar.showSnackbar(aviso)
        viewModel.onAvisoMostrado()
    }

    val contexto = LocalContext.current
    LaunchedEffect(uiState.pdfParaAbrir) {
        val ruta = uiState.pdfParaAbrir ?: return@LaunchedEffect
        viewModel.onPdfAbierto(abrirPdf(contexto, ruta))
    }

    val pedirVolver = { if (viewModel.onVolverPedido()) onVolver() }
    // Botón Atrás: se intercepta si hay cambios de Experto sin aprobar o si se está guardando la decisión
    BackHandler(enabled = uiState.hayCambios || uiState.decidiendo) { pedirVolver() }

    DetalleSolicitudScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onVolver = pedirVolver,
        onReintentar = viewModel::analizar,
        onCambiar = viewModel::onCambiar,
        descargarFoto = viewModel::descargarFoto,
        onAprobar = viewModel::onAprobar,
        onRechazar = viewModel::onRechazar,
        onAbrirDocumento = viewModel::onAbrirDocumento
    )

    when (uiState.dialogo) {
        DialogoDecision.APROBAR -> DialogoAprobar(
            nombreSocio = uiState.solicitud?.nombreSocio.orEmpty(),
            filas = uiState.filasAprobacion,
            onAprobar = viewModel::onConfirmarAprobar,
            onCancelar = viewModel::onCerrarDialogo
        )
        DialogoDecision.RECHAZAR -> DialogoRechazar(
            cantidadObras = uiState.obras.size,
            motivo = uiState.motivo,
            maximo = MOTIVO_MAXIMO,
            mostrarError = uiState.mostrarErrorMotivo,
            sugerencia = uiState.sugerenciaMotivo,
            onMotivo = viewModel::onMotivo,
            onRechazar = viewModel::onConfirmarRechazo,
            onCancelar = viewModel::onCerrarDialogo
        )
        null -> Unit
    }

    uiState.obraEnHoja?.let { obra ->
        HojaCambiarExperto(
            obra = obra,
            opciones = uiState.opcionesDe(obra),
            elegidoUid = uiState.seleccion[obra.id],
            onElegir = { uid -> viewModel.onElegirExperto(obra.id, uid) },
            onCerrar = viewModel::onCerrarHoja
        )
    }
    if (uiState.confirmarSalida) {
        DialogoSalirSinAprobar(
            onSalir = {
                viewModel.onCancelarSalida()
                onVolver()
            },
            onQuedarse = viewModel::onCancelarSalida
        )
    }
}

@Composable
fun DetalleSolicitudScreen(
    uiState: DetalleSolicitudUiState,
    snackbarHostState: SnackbarHostState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    onCambiar: (String) -> Unit,
    descargarFoto: suspend (String) -> ByteArray?,
    onAprobar: () -> Unit = {},
    onRechazar: () -> Unit = {},
    onAbrirDocumento: (CopiaDocumento) -> Unit = {}
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Solicitud de exhibición", onVolver) },
        bottomBar = {
            if (uiState.enRevision) {
                BarraDecision(
                    puedeAprobar = uiState.puedeAprobar,
                    ayuda = uiState.ayudaDecision,
                    ocupado = uiState.decidiendo,
                    onRechazar = onRechazar,
                    onAprobar = onAprobar
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val solicitud = uiState.solicitud
        when {
            uiState.cargando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            solicitud == null -> Text(
                uiState.errorCarga ?: "No se pudo cargar la solicitud",
                color = c.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { TarjetaSocio(solicitud) }
                item { EstadoDelAnalisis(uiState, onReintentar, onAbrirDocumento) }
                item { TituloSeccion("Obras · ${solicitud.obras.size}", Modifier.padding(top = 4.dp)) }
                items(solicitud.obras, key = { it.id }) { obra ->
                    TarjetaObra(obra, uiState, descargarFoto, onCambiar = { onCambiar(obra.id) })
                }
            }
        }
    }
}

@Composable
private fun TarjetaSocio(s: SolicitudEnRevision) {
    val c = MaterialTheme.colorScheme
    val (fondo, texto) = when (s.estado) {
        EstadoSolicitud.APROBADA -> c.tertiaryContainer to c.onTertiaryContainer
        EstadoSolicitud.RECHAZADA -> c.errorContainer to c.onErrorContainer
        else -> c.secondaryContainer to c.onSecondaryContainer
    }
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(c.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text(iniciales(s.nombreSocio), style = MaterialTheme.typography.titleSmall, color = c.onPrimaryContainer) }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(s.nombreSocio, style = MaterialTheme.typography.titleMedium)
                val fecha = s.fechaEnvio?.let { "enviada ${formatoFecha.format(Date(it))}" }
                Text(
                    listOfNotNull(s.numeroSocio.takeIf { it.isNotBlank() }, fecha).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
            }
            ChipColor(s.estado.etiqueta, fondo, texto)
        }
    }
}

/**
 * La franja bajo el socio: analizando, error con Reintentar, el avance de obras con Experto
 * o, si ya se resolvió (Parte 2), la decisión con sus 2 documentos.
 */
@Composable
private fun EstadoDelAnalisis(
    uiState: DetalleSolicitudUiState,
    onReintentar: () -> Unit,
    onAbrirDocumento: (CopiaDocumento) -> Unit
) {
    val c = MaterialTheme.colorScheme
    when {
        !uiState.enRevision -> TarjetaResolucion(
            aprobada = uiState.aprobada,
            fecha = uiState.solicitud?.fechaResolucion?.let { formatoFecha.format(Date(it)) },
            motivo = uiState.solicitud?.motivoRechazo,
            abriendoArchivo = uiState.abriendo == CopiaDocumento.ARCHIVO,
            abriendoArtista = uiState.abriendo == CopiaDocumento.ARTISTA,
            onAbrirArchivo = { onAbrirDocumento(CopiaDocumento.ARCHIVO) },
            onAbrirArtista = { onAbrirDocumento(CopiaDocumento.ARTISTA) }
        )
        uiState.analizando -> Aviso(
            "Analizando las obras con IA",
            "Se describe cada obra y se busca el Experto más afín. Puede tardar hasta un minuto.",
            Icons.Outlined.AutoAwesome, c.primaryContainer, c.onPrimaryContainer
        )
        uiState.mostrarReintentar -> Aviso(
            "No se pudo analizar la solicitud",
            "Revisa tu conexión a internet e inténtalo de nuevo.",
            Icons.Outlined.CloudOff, c.errorContainer, c.onErrorContainer
        ) {
            OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
        }
        uiState.analisisListo -> {
            val total = uiState.obras.size
            val conExperto = uiState.obrasConExperto
            TarjetaGaleria(relleno = PaddingValues(16.dp)) {
                Text("$conExperto de $total obras con Experto", style = MaterialTheme.typography.titleSmall)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else conExperto.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(8.dp),
                    color = if (conExperto == total) c.tertiary else c.secondary,
                    trackColor = c.surfaceVariant,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                if (conExperto < total) {
                    Aviso(
                        "Falta Experto en ${total - conExperto} obra(s)",
                        "No podrás aprobar: tendrás que rechazar la solicitud e indicar el motivo al socio.",
                        Icons.Outlined.ErrorOutline, c.errorContainer, c.onErrorContainer
                    )
                } else {
                    Text(
                        "Descripciones y recomendaciones generadas con IA. Revísalas antes de decidir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaObra(
    obra: ObraEnRevision,
    uiState: DetalleSolicitudUiState,
    descargarFoto: suspend (String) -> ByteArray?,
    onCambiar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = c.surfaceContainerLowest, shadowElevation = 2.dp) {
        Column {
            FotoRemota(
                rememberFotoRemota(obra.fotoRuta, descargarFoto),
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
                    Text("${obra.tecnica.etiqueta} · ${medidas(obra)}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                if (obra.nota.isNotBlank()) {
                    Column {
                        TituloSeccion("Nota del artista")
                        Text("“${obra.nota}”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                    }
                }
                Surface(color = c.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
                            Text(
                                " DESCRIPCIÓN · IA",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.primary
                            )
                        }
                        Text(
                            obra.descripcionIa ?: if (uiState.analizando) "Generando descripción…" else "Todavía sin descripción.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (obra.descripcionIa != null) c.onSurface else c.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                BloqueExperto(obra, uiState, onCambiar)
            }
        }
    }
}

@Composable
private fun BloqueExperto(obra: ObraEnRevision, uiState: DetalleSolicitudUiState, onCambiar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    if (!uiState.enRevision) {
        val asignado = uiState.expertoAsignado(obra) ?: return
        FilaExperto(
            rotulo = "Experto asignado",
            nombre = asignado.nombre,
            tecnicas = asignado.tecnicas.joinToString(" · ") { it.etiqueta },
            afinidad = obra.candidatos.firstOrNull { it.uid == asignado.uid }?.afinidad ?: 0,
            carga = asignado.carga,
            cargaMaxima = CARGA_MAXIMA,
            onCambiar = null
        )
        return
    }
    if (!uiState.analisisListo) {
        Text(
            if (uiState.analizando) "Buscando el Experto más afín…" else "Experto: pendiente de análisis",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
        return
    }
    val experto = uiState.expertoDe(obra)
    if (experto == null) {
        Aviso(
            "Sin Experto disponible para ${obra.tecnica.etiqueta}",
            "Los Expertos que dominan esta técnica están ocupados o inactivos.",
            Icons.Outlined.PersonOff, c.errorContainer, c.onErrorContainer
        )
        return
    }
    val afinidad = obra.candidatos.firstOrNull { it.uid == experto.uid }?.afinidad ?: 0
    val cambiado = uiState.seleccion[obra.id] != obra.recomendadoUid
    FilaExperto(
        rotulo = if (cambiado) "Experto elegido por ti" else "Experto recomendado",
        nombre = experto.nombre,
        tecnicas = experto.tecnicas.joinToString(" · ") { it.etiqueta },
        afinidad = afinidad,
        carga = experto.carga,
        cargaMaxima = CARGA_MAXIMA,
        onCambiar = onCambiar
    )
}

private fun cm(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString().replace('.', ',')

private fun medidas(obra: ObraEnRevision): String =
    listOfNotNull(obra.altoCm, obra.anchoCm, obra.profundidadCm).joinToString(" × ") { cm(it) } + " cm"

// ---------- Previews ----------

private val expertosEjemplo = listOf(
    PerfilEspecialista("c", "Carmen Villanueva", setOf(Tecnica.OLEO, Tecnica.ACRILICO), EstadoEspecialista.ACTIVO, 2),
    PerfilEspecialista("j", "Jorge Huamán Ccori", setOf(Tecnica.TEXTIL, Tecnica.CERAMICA), EstadoEspecialista.ACTIVO, 1)
)

private fun estadoEjemplo(analisis: EstadoAnalisis, sinExpertoTextil: Boolean = false, estado: EstadoSolicitud = EstadoSolicitud.ENVIADA): DetalleSolicitudUiState {
    val aprobada = estado == EstadoSolicitud.APROBADA
    val obras = listOf(
        ObraEnRevision(
            "1", "Mañana en Paracas", Tecnica.OLEO, 60.0, 80.0, null,
            "Pinté la bahía al amanecer, cuando los botes todavía no salen.", "",
            if (analisis == EstadoAnalisis.LISTO) "Paisaje marino al óleo: una bahía en calma con luz de amanecer, en azules intensos y ocres cálidos." else null,
            listOf(CandidatoExperto("c", 87)), "c", expertoAsignadoUid = if (aprobada) "c" else null
        ),
        ObraEnRevision(
            "2", "Telar de Ocongate", Tecnica.TEXTIL, 120.0, 90.0, null, "", "",
            if (analisis == EstadoAnalisis.LISTO) "Tejido de lana con rombos geométricos en rojo, verde y amarillo sobre fondo oscuro." else null,
            listOf(CandidatoExperto("j", 91)), if (sinExpertoTextil) null else "j", expertoAsignadoUid = if (aprobada) "j" else null
        )
    )
    val solicitud = SolicitudEnRevision(
        "s", "u", "Rosa Quispe Mamani", "SOC-0001", estado, analisis,
        analisisDesde = if (analisis == EstadoAnalisis.EN_CURSO) System.currentTimeMillis() else null,
        fechaEnvio = 1_794_500_000_000, obras = obras,
        motivoRechazo = if (estado == EstadoSolicitud.RECHAZADA) "No hay Experto disponible para Textil" else null,
        fechaResolucion = if (estado == EstadoSolicitud.ENVIADA) null else 1_794_560_000_000
    )
    val expertos = if (sinExpertoTextil) expertosEjemplo.take(1) else expertosEjemplo
    return DetalleSolicitudUiState(cargando = false, solicitud = solicitud, expertos = expertos, seleccion = seleccionInicial(obras))
}

@Preview(name = "Lista · claro", showBackground = true, heightDp = 1400)
@Preview(name = "Lista · nocturno", showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleListaPreview() {
    TecnisisGaleriaTheme {
        DetalleSolicitudScreen(estadoEjemplo(EstadoAnalisis.LISTO), remember { SnackbarHostState() }, {}, {}, {}, { null })
    }
}

@Preview(name = "Analizando", showBackground = true, heightDp = 1200)
@Composable
private fun DetalleAnalizandoPreview() {
    TecnisisGaleriaTheme {
        DetalleSolicitudScreen(estadoEjemplo(EstadoAnalisis.EN_CURSO), remember { SnackbarHostState() }, {}, {}, {}, { null })
    }
}

@Preview(name = "Sin Experto", showBackground = true, heightDp = 1400)
@Composable
private fun DetalleSinExpertoPreview() {
    TecnisisGaleriaTheme {
        DetalleSolicitudScreen(estadoEjemplo(EstadoAnalisis.LISTO, sinExpertoTextil = true), remember { SnackbarHostState() }, {}, {}, {}, { null })
    }
}

@Preview(name = "Error · nocturno", showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleErrorPreview() {
    TecnisisGaleriaTheme {
        DetalleSolicitudScreen(estadoEjemplo(EstadoAnalisis.ERROR), remember { SnackbarHostState() }, {}, {}, {}, { null })
    }
}

@Preview(name = "Aprobada · claro", showBackground = true, heightDp = 1400)
@Preview(name = "Aprobada · nocturno", showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleAprobadaPreview() {
    TecnisisGaleriaTheme {
        DetalleSolicitudScreen(
            estadoEjemplo(EstadoAnalisis.LISTO, estado = EstadoSolicitud.APROBADA),
            remember { SnackbarHostState() }, {}, {}, {}, { null }
        )
    }
}

@Preview(name = "Rechazada", showBackground = true, heightDp = 1400)
@Composable
private fun DetalleRechazadaPreview() {
    TecnisisGaleriaTheme {
        DetalleSolicitudScreen(
            estadoEjemplo(EstadoAnalisis.LISTO, sinExpertoTextil = true, estado = EstadoSolicitud.RECHAZADA),
            remember { SnackbarHostState() }, {}, {}, {}, { null }
        )
    }
}