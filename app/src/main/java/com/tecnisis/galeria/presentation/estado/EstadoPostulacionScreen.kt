package com.tecnisis.galeria.presentation.estado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun EstadoPostulacionScreen(estado: EstadoPostulacionUi, onVolver: () -> Unit) {
    Scaffold(topBar = { BarraConVolver("Estado de mi postulación", onVolver) }) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (estado) {
                EstadoPostulacionUi.Cargando ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                EstadoPostulacionUi.SinPostulacion ->
                    Text("Aún no has enviado tu postulación.", Modifier.align(Alignment.Center))
                is EstadoPostulacionUi.Error ->
                    Text(
                        estado.mensaje,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                is EstadoPostulacionUi.ConPostulacion ->
                    DetalleEstado(estado.postulacion)
            }
        }
    }
}

@Composable
private fun DetalleEstado(p: Postulacion) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = colorDe(p.estado))) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(p.estado.etiqueta, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(explicacionDe(p.estado), color = Color.White)
            }
        }
        p.motivo?.let { Text("Observación: $it", style = MaterialTheme.typography.bodyMedium) }
        p.motivoRechazo?.let { Text("Motivo del rechazo: $it", style = MaterialTheme.typography.bodyMedium) }
        Text("Postulante: ${p.nombreCompleto}")
        Text("DNI: ${p.dni}")
    }
}

private fun colorDe(estado: EstadoPostulacion): Color = when (estado) {
    EstadoPostulacion.PENDIENTE_VERIFICACION -> Color(0xFF8A7F72)
    EstadoPostulacion.VERIFICADA -> Color(0xFF2E7D32)
    EstadoPostulacion.OBSERVADA -> Color(0xFFB8893A)
    EstadoPostulacion.APROBADA -> Color(0xFF1B5E20)
    EstadoPostulacion.RECHAZADA -> Color(0xFF6D1B2F)
}

private fun explicacionDe(estado: EstadoPostulacion): String = when (estado) {
    EstadoPostulacion.PENDIENTE_VERIFICACION -> "Estamos verificando tus datos con RENIEC."
    EstadoPostulacion.VERIFICADA -> "Tus datos son correctos. Falta la aprobación del Anfitrión."
    EstadoPostulacion.OBSERVADA -> "Encontramos una observación. El Anfitrión revisará tu caso."
    EstadoPostulacion.APROBADA -> "¡Bienvenido! Cierra sesión y vuelve a entrar como Socio Activo."
    EstadoPostulacion.RECHAZADA -> "Tu postulación no fue aprobada."
}

@Preview(showBackground = true)
@Composable
private fun EstadoPostulacionScreenPreview() {
    TecnisisGaleriaTheme {
        EstadoPostulacionScreen(
            estado = EstadoPostulacionUi.ConPostulacion(
                Postulacion(
                    uid = "1", dni = "11223344", nombres = "ROSA MARIA",
                    apellidoPaterno = "QUISPE", apellidoMaterno = "MAMANI",
                    direccion = "Av. Lima 123", sexo = "Femenino",
                    telefono = "987654321", correo = "rosa@correo.com",
                    estado = EstadoPostulacion.OBSERVADA,
                    motivo = "Los nombres no coinciden con RENIEC (ROSA QUISPE MAMANI)"
                )
            ),
            onVolver = {}
        )
    }
}

@Composable
fun EstadoPostulacionRoute(
    onVolver: () -> Unit,
    viewModel: EstadoPostulacionViewModel = viewModel(factory = EstadoPostulacionViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    EstadoPostulacionScreen(estado = estado, onVolver = onVolver)
}