package com.tecnisis.galeria.presentation.detalle

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
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
    DetallePostulacionScreen(uiState = uiState, onVolver = onVolver, onReintentar = viewModel::cargar)
}

@Composable
fun DetallePostulacionScreen(
    uiState: DetallePostulacionUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit
) {
    val c = MaterialTheme.colorScheme

    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Postulación", onVolver) }
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

                TituloSeccion("Datos del postulante", Modifier.padding(top = 6.dp))
                DatosPostulante(p)
                Spacer(Modifier.height(24.dp))
            }
        }
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
            onVolver = {},
            onReintentar = {}
        )
    }
}