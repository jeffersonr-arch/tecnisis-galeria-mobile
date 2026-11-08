package com.tecnisis.galeria.presentation.evaluacion

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.COMENTARIO_MAXIMO
import com.tecnisis.galeria.domain.model.EstadoEspecialista
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EtiquetaDetectada
import com.tecnisis.galeria.domain.model.ObraAsignada
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.TipoVeredicto
import com.tecnisis.galeria.presentation.asignadas.obraAsignadaEjemplo
import com.tecnisis.galeria.presentation.asignadas.textoMedidas
import com.tecnisis.galeria.presentation.componentes.Aviso
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.ChipColor
import com.tecnisis.galeria.presentation.componentes.ChipEstadoObra
import com.tecnisis.galeria.presentation.componentes.FilaEtiqueta
import com.tecnisis.galeria.presentation.componentes.FotoRemota
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.componentes.iniciales
import com.tecnisis.galeria.presentation.componentes.rememberFotoRemota
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoDia = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE"))
private val formatoFecha = SimpleDateFormat("d MMM · h:mm a", Locale.forLanguageTag("es-PE"))

@Composable
fun DetalleObraAsignadaRoute(
    onVolver: () -> Unit,
    viewModel: DetalleObraAsignadaViewModel = viewModel(factory = DetalleObraAsignadaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.aviso) {
        val aviso = uiState.aviso ?: return@LaunchedEffect
        snackbar.showSnackbar(aviso)
        viewModel.onAvisoMostrado()
    }

    // Mientras se guarda el veredicto no se sale (se cortaría a la mitad)
    BackHandler(enabled = uiState.guardando) { }

    DetalleObraAsignadaScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onVolver = { if (!uiState.guardando) onVolver() },
        descargarFoto = viewModel::descargarFoto,
        onAceptar = viewModel::onAceptar,
        onRechazar = viewModel::onRechazar
    )

    val tipo = uiState.dialogo
    val obra = uiState.obra
    if (tipo != null && obra != null) {
        DialogoVeredicto(
            rechazar = tipo == TipoVeredicto.RECHAZAR,
            tituloObra = obra.titulo,
            nombreSocio = obra.socioVisible,
            comentario = uiState.comentario,
            maximo = COMENTARIO_MAXIMO,
            mostrarError = uiState.mostrarErrorComentario,
            guardando = uiState.guardando,
            error = uiState.errorDialogo,
            onComentario = viewModel::onComentario,
            onConfirmar = viewModel::onConfirmar,
            onCancelar = viewModel::onCerrarDialogo
        )
    }
}

@Composable
fun DetalleObraAsignadaScreen(
    uiState: DetalleObraAsignadaUiState,
    snackbarHostState: SnackbarHostState,
    onVolver: () -> Unit,
    descargarFoto: suspend (String) -> ByteArray?,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Evaluar obra", onVolver) },
        bottomBar = {
            if (uiState.puedeEvaluar) BarraVeredicto(ocupado = uiState.guardando, onRechazar = onRechazar, onAceptar = onAceptar)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        .height(230.dp)
                )
                Cabecera(obra)
                if (obra.estado.tieneVeredicto) TarjetaVeredicto(obra)
                TarjetaEtiquetas(uiState.etiquetas)
                DescripcionIa(obra.descripcionIa)
                TarjetaPersonas(obra, uiState.ficha)
            }
        }
    }
}

@Composable
private fun Cabecera(obra: ObraAsignada) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(obra.titulo, style = MaterialTheme.typography.headlineSmall)
                Text("${obra.tecnica.etiqueta} · ${obra.textoMedidas()}", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            if (obra.porEvaluar) ChipColor("Por evaluar", c.secondaryContainer, c.onSecondaryContainer)
            else ChipEstadoObra(obra.estado)
        }
        if (obra.nota.isNotBlank()) {
            Column {
                TituloSeccion("Nota del artista")
                Text("“${obra.nota}”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
        }
    }
}

/** Ya evaluada: quién y cuándo, y el comentario (si lo hay). */
@Composable
private fun TarjetaVeredicto(obra: ObraAsignada) {
    val c = MaterialTheme.colorScheme
    val aceptada = obra.estado != EstadoObra.RECHAZADA
    val (fondo, texto) = if (aceptada) c.tertiaryContainer to c.onTertiaryContainer else c.errorContainer to c.onErrorContainer
    val fecha = obra.fechaVeredicto?.let { " el ${formatoFecha.format(Date(it))}" }.orEmpty()
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (aceptada) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = null,
                tint = if (aceptada) c.tertiary else c.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                (if (aceptada) "Aceptada por ti" else "Rechazada por ti") + fecha,
                style = MaterialTheme.typography.titleSmall,
                color = if (aceptada) c.tertiary else c.error
            )
        }
        obra.comentarioVeredicto?.let { comentario ->
            Spacer(Modifier.height(10.dp))
            Bloque(fondo, texto, "TU COMENTARIO", comentario)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "El veredicto es definitivo y ya lo ven el socio y el Anfitrión.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
    }
}

@Composable
private fun Bloque(fondo: Color, texto: Color, rotulo: String, contenido: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(fondo)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = texto)
        Spacer(Modifier.height(4.dp))
        Text(contenido, style = MaterialTheme.typography.bodyMedium, color = texto)
    }
}

/** Etiquetas · ML Kit: las de 50 % o más, analizando, sin resultados o error. */
@Composable
private fun TarjetaEtiquetas(estado: EstadoEtiquetas) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ImageSearch, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
            Text(
                " ETIQUETAS · ML KIT",
                style = MaterialTheme.typography.labelSmall,
                color = c.primary,
                modifier = Modifier.weight(1f)
            )
            Text("En tu celular · ≥ 50 %", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        when (estado) {
            EstadoEtiquetas.Analizando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Analizando la foto…", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
            is EstadoEtiquetas.Listas -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                estado.etiquetas.forEach { FilaEtiqueta(it.nombre, it.porcentaje, it.traducida) }
            }
            EstadoEtiquetas.SinResultados -> Aviso(
                "No se detectaron etiquetas con suficiente confianza",
                "Solo se muestran las de 50 % o más. Puedes evaluar la obra igual.",
                Icons.Outlined.Info, c.surfaceVariant, c.onSurfaceVariant
            )
            EstadoEtiquetas.Error -> Aviso(
                "No se pudo analizar la foto",
                "Revisa tu conexión y vuelve a abrir la obra. Puedes evaluarla igual.",
                Icons.Outlined.Info, c.surfaceVariant, c.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DescripcionIa(descripcion: String?) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
                Text(" DESCRIPCIÓN · IA", style = MaterialTheme.typography.labelSmall, color = c.primary)
            }
            Text(
                descripcion ?: "Sin descripción.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (descripcion != null) c.onSurface else c.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Socio (solo nombre y número; "—" en obras antiguas) y el Experto asignado, que es el Especialista de la sesión. */
@Composable
private fun TarjetaPersonas(obra: ObraAsignada, ficha: PerfilEspecialista?) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(0.dp)) {
        FilaPersona(
            iniciales = if (obra.nombreSocio.isNullOrBlank()) obra.socioVisible else iniciales(obra.socioVisible),
            fondo = c.primaryContainer, colorTexto = c.onPrimaryContainer,
            rotulo = "Socio", nombre = obra.socioVisible, detalle = obra.numeroSocioVisible
        )
        HorizontalDivider(color = c.outlineVariant)
        val nombre = ficha?.nombre?.takeIf { it.isNotBlank() } ?: "Tú"
        val tecnicas = ficha?.tecnicas.orEmpty().joinToString(" · ") { it.etiqueta }
        val asignada = obra.fechaAsignacion?.let { "asignada ${formatoDia.format(Date(it))}" }
        FilaPersona(
            iniciales = iniciales(nombre), fondo = c.secondaryContainer, colorTexto = c.onSecondaryContainer,
            rotulo = "Experto asignado · tú", nombre = nombre,
            detalle = listOfNotNull(tecnicas.takeIf { it.isNotBlank() }, asignada).joinToString(" · ")
        )
    }
}

@Composable
private fun FilaPersona(iniciales: String, fondo: Color, colorTexto: Color, rotulo: String, nombre: String, detalle: String) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(fondo),
            contentAlignment = Alignment.Center
        ) { Text(iniciales, style = MaterialTheme.typography.labelLarge, color = colorTexto) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(rotulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.outline)
            Text(nombre, style = MaterialTheme.typography.titleSmall)
            if (detalle.isNotBlank()) Text(detalle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}

/** Abajo: Rechazar (rojo, con borde) y Aceptar. Mientras se guarda, los dos se apagan. */
@Composable
private fun BarraVeredicto(ocupado: Boolean, onRechazar: () -> Unit, onAceptar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surfaceContainerLowest, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRechazar,
                enabled = !ocupado,
                border = BorderStroke(1.5.dp, if (ocupado) c.outlineVariant else c.error),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) { Text("Rechazar", color = if (ocupado) c.onSurfaceVariant else c.error) }
            Button(
                onClick = onAceptar,
                enabled = !ocupado,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) { Text("Aceptar") }
        }
    }
}

// ---------- Previews ----------

private val fichaEjemplo = PerfilEspecialista(
    "c", "Carmen Villanueva", setOf(Tecnica.OLEO, Tecnica.ACRILICO, Tecnica.TECNICA_MIXTA), EstadoEspecialista.ACTIVO, 3
)

private val etiquetasEjemplo = EstadoEtiquetas.Listas(
    listOf(
        EtiquetaDetectada("Cielo", 91, true),
        EtiquetaDetectada("Barco", 78, true),
        EtiquetaDetectada("Atardecer", 66, true),
        EtiquetaDetectada("Patrón", 57, true),
        EtiquetaDetectada("Brig", 52, false)
    )
)

@Composable
private fun VistaPrevia(obra: ObraAsignada, etiquetas: EstadoEtiquetas = etiquetasEjemplo) {
    TecnisisGaleriaTheme {
        DetalleObraAsignadaScreen(
            uiState = DetalleObraAsignadaUiState(cargando = false, obra = obra, ficha = fichaEjemplo, etiquetas = etiquetas),
            snackbarHostState = remember { SnackbarHostState() },
            onVolver = {}, descargarFoto = { null }, onAceptar = {}, onRechazar = {}
        )
    }
}

@Preview(name = "Por evaluar · claro", showBackground = true, heightDp = 1500)
@Preview(name = "Por evaluar · nocturno", showBackground = true, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetallePorEvaluarPreview() = VistaPrevia(obraAsignadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO))

@Preview(name = "Analizando la foto", showBackground = true, heightDp = 1300)
@Composable
private fun DetalleAnalizandoPreview() =
    VistaPrevia(obraAsignadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO), EstadoEtiquetas.Analizando)

@Preview(name = "Sin etiquetas y obra antigua", showBackground = true, heightDp = 1300)
@Composable
private fun DetalleSinEtiquetasPreview() =
    VistaPrevia(obraAsignadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO, socio = null), EstadoEtiquetas.SinResultados)

@Preview(name = "Ya aceptada", showBackground = true, heightDp = 1500)
@Composable
private fun DetalleAceptadaPreview() = VistaPrevia(
    obraAsignadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO, EstadoObra.ACEPTADA, comentario = "Buen manejo de la luz y de la textura del agua.")
)

@Preview(name = "Ya rechazada · nocturno", showBackground = true, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleRechazadaPreview() = VistaPrevia(
    obraAsignadaEjemplo("1", "Mañana en Paracas", Tecnica.OLEO, EstadoObra.RECHAZADA, comentario = "La pintura presenta craquelado en la esquina inferior.")
)