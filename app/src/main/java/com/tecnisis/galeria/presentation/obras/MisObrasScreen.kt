package com.tecnisis.galeria.presentation.obras

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tecnisis.galeria.presentation.componentes.abrirPdf
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.MAX_OBRAS_POR_SOLICITUD
import com.tecnisis.galeria.domain.model.Obra
import com.tecnisis.galeria.domain.model.SolicitudExhibicion
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.EstadoPaso
import com.tecnisis.galeria.presentation.componentes.FilaPdf
import com.tecnisis.galeria.presentation.componentes.FotoObra
import com.tecnisis.galeria.presentation.componentes.LineaDeAvance
import com.tecnisis.galeria.presentation.componentes.PasoAvance
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoEnvio = SimpleDateFormat("d MMM · h:mm a", Locale.forLanguageTag("es-PE"))
private val formatoDia = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))

@Composable
fun MisObrasRoute(
    onRegistrar: () -> Unit,
    onEditar: (String) -> Unit,
    viewModel: MisObrasViewModel = viewModel(factory = MisObrasViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.mensaje) {
        val mensaje = uiState.mensaje ?: return@LaunchedEffect
        snackbar.showSnackbar(mensaje)
        viewModel.onMensajeMostrado()
    }

    // El documento ya está en la caché; se abre con la app de PDF del celular
    val contexto = LocalContext.current
    LaunchedEffect(uiState.pdfParaAbrir) {
        val ruta = uiState.pdfParaAbrir ?: return@LaunchedEffect
        viewModel.onPdfAbierto(abrirPdf(contexto, ruta))
    }

    // El permiso de notificaciones (Android 13+) se pide al confirmar el envío.
    // Si lo niega, todo sigue igual: la decisión igual aparece en Mis obras.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val confirmarEnvio = {
        viewModel.onConfirmarEnvio()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MisObrasScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onRegistrar = onRegistrar,
        onAbrirObra = onEditar,
        onEnviar = viewModel::onEnviar,
        onConfirmarEnvio = confirmarEnvio,
        onCancelarEnvio = viewModel::onCancelarEnvio,
        onVerDocumento = viewModel::onVerDocumento
    )
}

@Composable
fun MisObrasScreen(
    uiState: MisObrasUiState,
    snackbarHostState: SnackbarHostState,
    onRegistrar: () -> Unit,
    onAbrirObra: (String) -> Unit,
    onEnviar: () -> Unit,
    onConfirmarEnvio: () -> Unit,
    onCancelarEnvio: () -> Unit,
    onVerDocumento: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        when {
            uiState.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.sinObras -> EstadoVacio(onRegistrar)
            else -> ListaObras(uiState, onRegistrar, onAbrirObra, onEnviar, onVerDocumento)
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
    if (uiState.confirmandoEnvio) {
        DialogoEnvio(uiState.obras.size, onConfirmarEnvio, onCancelarEnvio)
    }
}

@Composable
private fun EstadoVacio(onRegistrar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Mis obras", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MarcoVacio()
            Spacer(Modifier.height(20.dp))
            Text("Aún no registras obras", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Registra cada obra con su foto, técnica y medidas. Se guardan en tu celular hasta que envíes tu solicitud de exhibición.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            BotonPrincipal(texto = "Registrar mi primera obra", cargando = false, onClick = onRegistrar, icono = Icons.Filled.Add)
        }
    }
}

/** Marco vacío con tres pigmentos, como el logo. */
@Composable
private fun MarcoVacio() {
    val c = MaterialTheme.colorScheme
    val g = MaterialTheme.galeria
    Canvas(
        Modifier
            .size(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.surfaceContainerLowest)
    ) {
        val lado = size.minDimension
        val margen = lado * 0.23f
        drawRect(
            color = c.onSurface,
            topLeft = Offset(margen, margen),
            size = Size(lado - 2 * margen, lado - 2 * margen),
            style = Stroke(width = lado * 0.045f)
        )
        drawCircle(c.primary, radius = lado * 0.09f, center = Offset(lado * 0.42f, lado * 0.44f))
        drawCircle(g.pigmentoAmarillo, radius = lado * 0.076f, center = Offset(lado * 0.56f, lado * 0.47f))
        drawCircle(g.pigmentoRojo, radius = lado * 0.076f, center = Offset(lado * 0.48f, lado * 0.58f))
    }
}

@Composable
private fun ListaObras(
    uiState: MisObrasUiState,
    onRegistrar: () -> Unit,
    onAbrirObra: (String) -> Unit,
    onEnviar: () -> Unit,
    onVerDocumento: () -> Unit
) {
    val solicitud = uiState.solicitud ?: return
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Mis obras", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(16.dp))
        TarjetaSolicitud(solicitud, uiState.hayConexion, uiState.abriendoDocumento, onVerDocumento)
        Spacer(Modifier.height(22.dp))
        TituloSeccion("Obras · ${solicitud.obras.size}")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            solicitud.obras.forEach { obra ->
                TarjetaObra(
                    obra = obra,
                    estadoSolicitud = solicitud.estado,
                    editable = solicitud.esEditable,
                    onClick = { onAbrirObra(obra.id) }
                )
            }
        }
        if (solicitud.esEditable) {
            Spacer(Modifier.height(12.dp))
            if (solicitud.admiteNuevaObra) {
                OutlinedButton(
                    onClick = onRegistrar,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.5.dp, c.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Agregar otra obra")
                }
            } else {
                Text(
                    "Llegaste al máximo de $MAX_OBRAS_POR_SOLICITUD obras por solicitud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            BotonPrincipal(
                texto = "Enviar solicitud · ${textoObras(solicitud.obras.size)}",
                cargando = uiState.enviando,
                onClick = onEnviar,
                icono = Icons.AutoMirrored.Filled.Send
            )
            Text(
                "Después de enviarla no podrás editar ni agregar obras.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        } else if (solicitud.admiteNuevaObra) {
            // HU07: la solicitud anterior ya se resolvió, así que puede armar una nueva
            Spacer(Modifier.height(24.dp))
            BotonPrincipal(texto = "Armar una nueva solicitud", cargando = false, onClick = onRegistrar, icono = Icons.Filled.Add)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TarjetaSolicitud(solicitud: SolicitudExhibicion, hayConexion: Boolean, abriendoDocumento: Boolean, onVerDocumento: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val cantidad = solicitud.obras.size
    TarjetaGaleria {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Solicitud de exhibición", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            val (fondo, texto) = coloresSolicitud(solicitud.estado)
            ChipTexto(solicitud.estado.etiqueta, fondo, texto)
        }
        Spacer(Modifier.height(12.dp))
        when (solicitud.estado) {
            EstadoSolicitud.BORRADOR -> Text(
                "${textoObras(cantidad)} ${if (cantidad == 1) "guardada" else "guardadas"} en tu celular. " +
                        "Cuando estén completas, envía la solicitud al Anfitrión.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
            EstadoSolicitud.POR_ENVIAR -> AvisoEnvio(hayConexion)
            EstadoSolicitud.ENVIADA -> LineaDeAvance(pasosEnviada(solicitud))
            EstadoSolicitud.APROBADA, EstadoSolicitud.RECHAZADA ->
                Resolucion(solicitud, abriendoDocumento, onVerDocumento)
        }
    }
}

/** Avance, motivo (si se rechazó) y su copia del documento. */
@Composable
private fun Resolucion(solicitud: SolicitudExhibicion, abriendoDocumento: Boolean, onVerDocumento: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val aprobada = solicitud.estado == EstadoSolicitud.APROBADA
    LineaDeAvance(pasosResuelta(solicitud))
    if (!aprobada && !solicitud.motivoRechazo.isNullOrBlank()) {
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.errorContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text("MOTIVO DEL ANFITRIÓN", style = MaterialTheme.typography.labelSmall, color = c.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text(solicitud.motivoRechazo, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
        }
    }
    Spacer(Modifier.height(12.dp))
    FilaPdf(
        titulo = if (aprobada) "Mi documento de aprobación" else "Mi documento de rechazo",
        subtitulo = null,
        onClick = onVerDocumento,
        cargando = abriendoDocumento,
        icono = Icons.Outlined.FileDownload,
        fondo = c.surfaceVariant
    )
}

private fun pasosResuelta(s: SolicitudExhibicion): List<PasoAvance> {
    val enviada = PasoAvance(
        "Solicitud enviada",
        listOfNotNull(s.enviadaEn?.let { formatoEnvio.format(Date(it)) }, textoObras(s.obras.size)).joinToString(" · "),
        EstadoPaso.COMPLETO
    )
    val fecha = s.resueltaEn?.let { " el ${formatoDia.format(Date(it))}" }.orEmpty()
    return if (s.estado == EstadoSolicitud.APROBADA) listOf(
        enviada,
        PasoAvance("Asignación de expertos", "Aprobada$fecha", EstadoPaso.COMPLETO),
        PasoAvance("Evaluación artística", "Tus obras están con los Expertos", EstadoPaso.ACTUAL)
    ) else listOf(
        enviada,
        PasoAvance("Revisión del Anfitrión", "Rechazada$fecha", EstadoPaso.COMPLETO)
    )
}

@Composable
private fun AvisoEnvio(hayConexion: Boolean) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            if (hayConexion) Icons.Outlined.CloudUpload else Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = c.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            if (hayConexion) "Enviando tu solicitud: las fotos suben en segundo plano. Ya no puedes editar las obras."
            else "Sin conexión. Tu solicitud se enviará sola cuando vuelva internet; puedes cerrar la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSecondaryContainer
        )
    }
}

private fun pasosEnviada(s: SolicitudExhibicion) = listOf(
    PasoAvance(
        "Solicitud enviada",
        listOfNotNull(s.enviadaEn?.let { formatoEnvio.format(Date(it)) }, textoObras(s.obras.size)).joinToString(" · "),
        EstadoPaso.COMPLETO
    ),
    PasoAvance("Asignación de expertos", "En revisión del Anfitrión", EstadoPaso.ACTUAL),
    PasoAvance("Aprobación de la solicitud", "Pendiente", EstadoPaso.PENDIENTE)
)

@Composable
private fun TarjetaObra(obra: Obra, estadoSolicitud: EstadoSolicitud, editable: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = editable,
        shape = MaterialTheme.shapes.medium,
        color = c.surfaceContainerLowest,
        border = BorderStroke(1.dp, c.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FotoObra(obra.fotoLocal, Modifier.size(72.dp), ladoMaximo = 240)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${obra.tecnica.etiqueta} · ${medidasDe(obra)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (texto, fondo, color) = chipDeObra(obra, estadoSolicitud)
                    ChipTexto(texto, fondo, color)
                    if (obra.nota.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Outlined.Mic, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Nota", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                    }
                }
            }
            if (editable) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Editar", tint = c.outline)
            }
        }
    }
}

@Composable
private fun ChipTexto(texto: String, fondo: Color, color: Color) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun coloresSolicitud(estado: EstadoSolicitud): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (estado) {
        EstadoSolicitud.BORRADOR -> c.surfaceVariant to c.onSurfaceVariant
        EstadoSolicitud.POR_ENVIAR, EstadoSolicitud.ENVIADA -> c.secondaryContainer to c.onSecondaryContainer
        EstadoSolicitud.APROBADA -> c.tertiaryContainer to c.onTertiaryContainer
        EstadoSolicitud.RECHAZADA -> c.errorContainer to c.onErrorContainer
    }
}

/** Mientras espera el envío, cada obra dice si su foto ya subió (lo que hace WorkManager). */
@Composable
private fun chipDeObra(obra: Obra, estadoSolicitud: EstadoSolicitud): Triple<String, Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (estadoSolicitud) {
        EstadoSolicitud.BORRADOR -> Triple("Borrador", c.surfaceVariant, c.onSurfaceVariant)
        EstadoSolicitud.POR_ENVIAR ->
            if (obra.fotoSubida) Triple("Foto subida", c.tertiaryContainer, c.onTertiaryContainer)
            else Triple("Foto en espera", c.secondaryContainer, c.onSecondaryContainer)
        else -> when (obra.estado) {
            EstadoObra.NO_ADMITIDA, EstadoObra.RECHAZADA ->
                Triple(obra.estado.etiqueta, c.errorContainer, c.onErrorContainer)
            EstadoObra.ACEPTADA, EstadoObra.LISTA_EXHIBICION ->
                Triple(obra.estado.etiqueta, c.tertiaryContainer, c.onTertiaryContainer)
            else -> Triple(obra.estado.etiqueta, c.primaryContainer, c.onPrimaryContainer)
        }
    }
}

@Composable
private fun DialogoEnvio(cantidad: Int, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = c.surfaceContainerLowest,
        title = { Text("¿Enviar tu solicitud?", style = MaterialTheme.typography.titleLarge) },
        text = {
            val cuerpo = if (cantidad == 1)
                "Se enviará 1 obra al Anfitrión. Después ya no podrás editarla, borrarla ni agregar otras."
            else
                "Se enviarán $cantidad obras al Anfitrión. Después ya no podrás editarlas, borrarlas ni agregar otras."
            Text(
                "$cuerpo Si no tienes internet, tu solicitud se enviará sola cuando vuelva la conexión.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

private fun textoObras(n: Int) = if (n == 1) "1 obra" else "$n obras"

/** 60.0 → "60"; 60.5 → "60.5". Lo usa también Registrar obra. */
internal fun Double.comoMedida(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

internal fun medidasDe(obra: Obra): String =
    listOfNotNull(obra.altoCm, obra.anchoCm, obra.profundidadCm).joinToString(" × ") { it.comoMedida() } + " cm"

// ---------- Previews ----------

private fun obraEjemplo(
    id: String, titulo: String, tecnica: Tecnica, alto: Double, ancho: Double,
    profundidad: Double? = null, nota: String = "", subida: Boolean = false,
    estado: EstadoObra = EstadoObra.BORRADOR
) = Obra(
    id = id, solicitudId = "s1", titulo = titulo, tecnica = tecnica, altoCm = alto, anchoCm = ancho,
    profundidadCm = profundidad, fotoLocal = "", fotoUrl = if (subida) "https://ejemplo" else null,
    nota = nota, estado = estado, creadaEn = 0L
)

private fun solicitudEjemplo(estado: EstadoSolicitud): SolicitudExhibicion {
    val estadoObra = when (estado) {
        EstadoSolicitud.ENVIADA -> EstadoObra.ENVIADA
        EstadoSolicitud.APROBADA -> EstadoObra.EN_EVALUACION
        EstadoSolicitud.RECHAZADA -> EstadoObra.NO_ADMITIDA
        else -> EstadoObra.BORRADOR
    }
    val enviada = estado != EstadoSolicitud.BORRADOR && estado != EstadoSolicitud.POR_ENVIAR
    return SolicitudExhibicion(
        id = "s1",
        estado = estado,
        creadaEn = 0L,
        enviadaEn = if (enviada) 1_794_000_000_000 else null,
        motivoRechazo = if (estado == EstadoSolicitud.RECHAZADA) "No hay Experto disponible para Textil en este momento." else null,
        resueltaEn = if (enviada && estado != EstadoSolicitud.ENVIADA) 1_794_100_000_000 else null,
        obras = listOf(
            obraEjemplo("1", "Mañana en Paracas", Tecnica.OLEO, 60.0, 80.0, nota = "Amanecer", subida = true, estado = estadoObra),
            obraEjemplo("2", "Telar de Ocongate", Tecnica.TEXTIL, 120.0, 90.0, estado = estadoObra),
            obraEjemplo("3", "Vasija nocturna", Tecnica.CERAMICA, 35.0, 22.0, 22.0, nota = "Barro", estado = estadoObra)
        )
    )
}

@Composable
private fun VistaPrevia(uiState: MisObrasUiState) {
    TecnisisGaleriaTheme {
        MisObrasScreen(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            onRegistrar = {}, onAbrirObra = {}, onEnviar = {},
            onConfirmarEnvio = {}, onCancelarEnvio = {}
        )
    }
}

@Preview(name = "Borrador · claro", showBackground = true, heightDp = 1000)
@Preview(name = "Borrador · nocturno", showBackground = true, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MisObrasBorradorPreview() =
    VistaPrevia(MisObrasUiState(cargando = false, solicitud = solicitudEjemplo(EstadoSolicitud.BORRADOR)))

@Preview(name = "Sin obras · claro", showBackground = true)
@Preview(name = "Sin obras · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MisObrasVaciaPreview() = VistaPrevia(MisObrasUiState(cargando = false))

@Preview(name = "Pendiente sin internet", showBackground = true, heightDp = 900)
@Composable
private fun MisObrasPendientePreview() = VistaPrevia(
    MisObrasUiState(cargando = false, hayConexion = false, solicitud = solicitudEjemplo(EstadoSolicitud.POR_ENVIAR))
)

@Preview(name = "Enviada · claro", showBackground = true, heightDp = 900)
@Preview(name = "Enviada · nocturno", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MisObrasEnviadaPreview() =
    VistaPrevia(MisObrasUiState(cargando = false, solicitud = solicitudEjemplo(EstadoSolicitud.ENVIADA)))

@Preview(name = "Aprobada · claro", showBackground = true, heightDp = 1000)
@Preview(name = "Aprobada · nocturno", showBackground = true, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MisObrasAprobadaPreview() =
    VistaPrevia(MisObrasUiState(cargando = false, solicitud = solicitudEjemplo(EstadoSolicitud.APROBADA)))

@Preview(name = "Rechazada · claro", showBackground = true, heightDp = 1000)
@Preview(name = "Rechazada · nocturno", showBackground = true, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MisObrasRechazadaPreview() =
    VistaPrevia(MisObrasUiState(cargando = false, solicitud = solicitudEjemplo(EstadoSolicitud.RECHAZADA)))