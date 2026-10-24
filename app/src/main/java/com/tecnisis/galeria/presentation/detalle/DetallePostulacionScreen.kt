package com.tecnisis.galeria.presentation.detalle

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.DecisionPostulacion
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.model.decisionesPermitidas
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.ChipEstado
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM yyyy · h:mm a", Locale.forLanguageTag("es-PE"))

@Composable
fun DetallePostulacionRoute(
    onVolver: () -> Unit,
    viewModel: DetallePostulacionViewModel = viewModel(factory = DetallePostulacionViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.aviso) {
        val aviso = uiState.aviso ?: return@LaunchedEffect
        if (uiState.resuelta) {
            Toast.makeText(contexto, aviso, Toast.LENGTH_SHORT).show()
            onVolver()
        } else {
            snackbar.showSnackbar(aviso)
            viewModel.onAvisoMostrado()
        }
    }

    DetallePostulacionScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onVolver = onVolver,
        onReintentar = viewModel::cargar,
        onAbrirDecision = viewModel::onAbrirDecision,
        onMensajeChange = viewModel::onMensajeChange,
        onConfirmar = viewModel::onConfirmarDecision,
        onCancelar = viewModel::onCancelarDecision
    )
}

@Composable
fun DetallePostulacionScreen(
    uiState: DetallePostulacionUiState,
    snackbarHostState: SnackbarHostState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    onAbrirDecision: (DecisionPostulacion) -> Unit,
    onMensajeChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val c = MaterialTheme.colorScheme

    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Postulación", onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val p = uiState.postulacion
        when {
            uiState.cargando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            p == null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    uiState.errorCarga ?: "No se pudo cargar la postulación",
                    color = c.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onReintentar) { Text("Reintentar") }
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                Encabezado(p)

                if (p.estado == EstadoPostulacion.OBSERVADA && p.motivo != null) {
                    Aviso(
                        titulo = "Motivo de la observación",
                        texto = p.motivo,
                        icono = Icons.Outlined.Info,
                        fondo = c.secondaryContainer,
                        color = c.onSecondaryContainer
                    )
                }

                if (p.mensajeAnfitrion != null) {
                    Aviso(
                        titulo = "Información solicitada al postulante",
                        texto = p.mensajeAnfitrion,
                        icono = Icons.Outlined.Email,
                        fondo = c.primaryContainer,
                        color = c.onPrimaryContainer
                    )
                }

                TituloSeccion("Datos del postulante", Modifier.padding(top = 6.dp))
                DatosPostulante(p)

                Acciones(p, onAbrirDecision)
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val decision = uiState.decisionAbierta
    val postulacion = uiState.postulacion
    if (decision != null && postulacion != null) {
        DialogoDecision(
            decision = decision,
            estado = postulacion.estado,
            mensaje = uiState.mensaje,
            error = uiState.errorMensaje,
            enviando = uiState.enviando,
            onMensajeChange = onMensajeChange,
            onConfirmar = onConfirmar,
            onCancelar = onCancelar
        )
    }
}

@Composable
private fun Encabezado(p: Postulacion) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(p.nombreCompleto, style = MaterialTheme.typography.headlineSmall)
            Text(
                "DNI ${p.dni}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        ChipEstado(p.estado)
    }
}

@Composable
private fun Aviso(titulo: String, texto: String, icono: ImageVector, fondo: Color, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fondo)
            .padding(16.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = color)
            Spacer(Modifier.height(2.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

@Composable
private fun DatosPostulante(p: Postulacion) {
    val datos = listOf(
        "Nombres" to p.nombres,
        "Apellido paterno" to p.apellidoPaterno,
        "Apellido materno" to p.apellidoMaterno,
        "Sexo" to p.sexo,
        "Dirección" to p.direccion,
        "Teléfono" to p.telefono,
        "Correo" to p.correo,
        "Fecha de registro" to (p.fechaRegistro?.let { formatoFecha.format(Date(it)) } ?: "Sin fecha")
    )
    TarjetaGaleria {
        datos.forEachIndexed { i, (etiqueta, valor) ->
            if (i > 0) HorizontalDivider(
                Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(valor.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Acciones(p: Postulacion, onAbrirDecision: (DecisionPostulacion) -> Unit) {
    val c = MaterialTheme.colorScheme
    val decisiones = p.estado.decisionesPermitidas()

    if (decisiones.isEmpty()) {
        TarjetaGaleria {
            Text(
                "Esta postulación ya fue resuelta.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        }
        return
    }

    TituloSeccion("Tu decisión", Modifier.padding(top = 6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (DecisionPostulacion.APROBAR in decisiones) {
            BotonPrincipal(
                texto = if (p.estado == EstadoPostulacion.OBSERVADA) "Corregir y aprobar" else "Aprobar membresía",
                cargando = false,
                onClick = { onAbrirDecision(DecisionPostulacion.APROBAR) },
                icono = Icons.Filled.Check
            )
        }
        if (DecisionPostulacion.PEDIR_INFORMACION in decisiones) {
            OutlinedButton(
                onClick = { onAbrirDecision(DecisionPostulacion.PEDIR_INFORMACION) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Pedir más información", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (DecisionPostulacion.RECHAZAR in decisiones) {
            OutlinedButton(
                onClick = { onAbrirDecision(DecisionPostulacion.RECHAZAR) },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, c.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Rechazar postulación", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private data class TextosDecision(
    val titulo: String,
    val explicacion: String,
    val etiqueta: String,
    val confirmar: String
)

private fun textosDe(decision: DecisionPostulacion, estado: EstadoPostulacion): TextosDecision = when (decision) {
    DecisionPostulacion.APROBAR ->
        if (estado == EstadoPostulacion.OBSERVADA) TextosDecision(
            titulo = "Corregir y aprobar",
            explicacion = "Anota qué revisaste o corregiste (por ejemplo, que verificaste el DNI físico). " +
                    "El postulante pasará a Socio Activo con su número de socio.",
            etiqueta = "Nota de la corrección",
            confirmar = "Aprobar"
        ) else TextosDecision(
            titulo = "Aprobar membresía",
            explicacion = "El postulante pasará a Socio Activo y recibirá su número de socio.",
            etiqueta = "Nota (opcional)",
            confirmar = "Aprobar"
        )
    DecisionPostulacion.PEDIR_INFORMACION -> TextosDecision(
        titulo = "Pedir más información",
        explicacion = "La postulación seguirá observada. El postulante verá tu mensaje en el estado de su postulación.",
        etiqueta = "Mensaje para el postulante",
        confirmar = "Enviar"
    )
    DecisionPostulacion.RECHAZAR -> TextosDecision(
        titulo = "Rechazar postulación",
        explicacion = "El rechazo es definitivo: el postulante verá este motivo y no podrá volver a postular.",
        etiqueta = "Motivo del rechazo",
        confirmar = "Rechazar"
    )
}

@Composable
private fun DialogoDecision(
    decision: DecisionPostulacion,
    estado: EstadoPostulacion,
    mensaje: String,
    error: String?,
    enviando: Boolean,
    onMensajeChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val textos = textosDe(decision, estado)

    AlertDialog(
        onDismissRequest = { if (!enviando) onCancelar() },
        containerColor = c.surfaceContainerLowest,
        title = { Text(textos.titulo, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(textos.explicacion, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                OutlinedTextField(
                    value = mensaje,
                    onValueChange = onMensajeChange,
                    label = { Text(textos.etiqueta) },
                    isError = error != null,
                    supportingText = if (error != null) { { Text(error) } } else null,
                    enabled = !enviando,
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.surfaceContainerLowest,
                        unfocusedContainerColor = c.surfaceContainerLowest,
                        unfocusedBorderColor = c.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = !enviando) {
                if (enviando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        textos.confirmar,
                        color = if (decision == DecisionPostulacion.RECHAZAR) c.error else c.primary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !enviando) { Text("Cancelar") }
        }
    )
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetallePostulacionScreenPreview() {
    TecnisisGaleriaTheme {
        DetallePostulacionScreen(
            uiState = DetallePostulacionUiState(
                cargando = false,
                postulacion = Postulacion(
                    uid = "1", dni = "11223344", nombres = "Rosa Maria", apellidoPaterno = "Quispe",
                    apellidoMaterno = "Mamani", direccion = "Av. Jose Olaya 133", sexo = "Femenino",
                    telefono = "999888777", correo = "rosa@tecnisis.pe",
                    estado = EstadoPostulacion.OBSERVADA,
                    motivo = "Los nombres no coinciden con RENIEC (ROSA QUISPE MAMANI)",
                    fechaRegistro = 1_791_000_000_000
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onVolver = {}, onReintentar = {}, onAbrirDecision = {},
            onMensajeChange = {}, onConfirmar = {}, onCancelar = {}
        )
    }
}