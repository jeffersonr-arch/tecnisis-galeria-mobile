package com.tecnisis.galeria.presentation.estado

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.LineaDeAvance
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria

@Composable
fun EstadoPostulacionRoute(
    onVolver: () -> Unit = {},
    onCompletar: () -> Unit = {},
    viewModel: EstadoPostulacionViewModel = viewModel(factory = EstadoPostulacionViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    EstadoPostulacionScreen(estado = estado, onCompletar = onCompletar)
}

@Composable
fun EstadoPostulacionScreen(estado: EstadoPostulacionUi, onCompletar: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Mi postulación",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))

        when (estado) {
            EstadoPostulacionUi.Cargando -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            EstadoPostulacionUi.SinPostulacion -> TarjetaGaleria {
                Text("Aún no envías tu postulación", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Completa tus datos de identidad para iniciar tu admisión como socio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                BotonPrincipal(texto = "Completar mi postulación", cargando = false, onClick = onCompletar)
            }

            is EstadoPostulacionUi.Error ->
                Text(estado.mensaje, color = MaterialTheme.colorScheme.error)

            is EstadoPostulacionUi.ConPostulacion -> DetalleEstado(estado.postulacion)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetalleEstado(p: Postulacion) {
    val c = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TarjetaResultado(p)

        val nota = p.motivoRechazo?.let { "Motivo del rechazo: $it" } ?: p.motivo?.let { "Observación: $it" }
        if (nota != null) {
            TarjetaGaleria { Text(nota, style = MaterialTheme.typography.bodyMedium) }
        }

        TarjetaGaleria {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloSeccion("Avance", Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.tertiary))
                    Spacer(Modifier.width(6.dp))
                    Text("En vivo", style = MaterialTheme.typography.labelMedium, color = c.onTertiaryContainer)
                }
            }
            Spacer(Modifier.height(4.dp))
            LineaDeAvance(p.pasosDeAvance())
        }

        TarjetaGaleria {
            Row {
                Dato("Postulante", p.nombreCompleto, Modifier.weight(1f))
                Dato("DNI", p.dni, Modifier.weight(0.6f))
            }
        }
    }
}

@Composable
private fun TarjetaResultado(p: Postulacion) {
    val g = MaterialTheme.galeria
    val c = MaterialTheme.colorScheme
    val (fondo, texto) = when (p.estado) {
        EstadoPostulacion.VERIFICADA, EstadoPostulacion.APROBADA -> g.exito to g.sobreExito
        EstadoPostulacion.OBSERVADA -> c.secondaryContainer to c.onSecondaryContainer
        EstadoPostulacion.RECHAZADA -> c.errorContainer to c.onErrorContainer
        EstadoPostulacion.PENDIENTE_VERIFICACION -> c.primaryContainer to c.onPrimaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(fondo)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(texto.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconoDe(p.estado), contentDescription = null, tint = texto, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(p.estado.etiqueta, style = MaterialTheme.typography.headlineSmall, color = texto)
            Text(explicacionDe(p.estado), style = MaterialTheme.typography.bodyMedium, color = texto)
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(valor, style = MaterialTheme.typography.titleSmall)
    }
}

private fun iconoDe(estado: EstadoPostulacion): ImageVector = when (estado) {
    EstadoPostulacion.VERIFICADA, EstadoPostulacion.APROBADA -> Icons.Filled.Check
    EstadoPostulacion.OBSERVADA -> Icons.Outlined.Info
    EstadoPostulacion.RECHAZADA -> Icons.Filled.Close
    EstadoPostulacion.PENDIENTE_VERIFICACION -> Icons.Outlined.Schedule
}

private fun explicacionDe(estado: EstadoPostulacion): String = when (estado) {
    EstadoPostulacion.PENDIENTE_VERIFICACION -> "Estamos verificando tus datos con RENIEC."
    EstadoPostulacion.VERIFICADA -> "Tus datos coinciden con RENIEC. Ahora el Anfitrión la revisará."
    EstadoPostulacion.OBSERVADA -> "Encontramos una observación. El Anfitrión revisará tu caso."
    EstadoPostulacion.APROBADA -> "¡Bienvenido! En unos segundos verás tu inicio de Socio Activo."
    EstadoPostulacion.RECHAZADA -> "Tu postulación no fue aprobada."
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EstadoPostulacionScreenPreview() {
    TecnisisGaleriaTheme {
        EstadoPostulacionScreen(
            estado = EstadoPostulacionUi.ConPostulacion(
                Postulacion(
                    uid = "1", dni = "88776655", nombres = "Pedro", apellidoPaterno = "Castillo",
                    apellidoMaterno = "Huaman", direccion = "Av. Lima 123", sexo = "Masculino",
                    telefono = "987654321", correo = "pedro.castillo@correo.com",
                    estado = EstadoPostulacion.VERIFICADA, fechaRegistro = 1_760_320_000_000
                )
            ),
            onCompletar = {}
        )
    }
}