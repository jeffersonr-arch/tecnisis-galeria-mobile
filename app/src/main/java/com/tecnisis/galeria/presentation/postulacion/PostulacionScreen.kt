package com.tecnisis.galeria.presentation.postulacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.CampoTexto
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun PostulacionScreen(
    uiState: PostulacionUiState,
    onCambio: (Campo, String) -> Unit,
    onEnviarClick: () -> Unit,
    onVolver: () -> Unit
) {
    val habilitado = !uiState.cargando

    Scaffold(topBar = { BarraConVolver("Mi postulación", onVolver) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Completa tus datos tal como figuran en tu DNI. Los verificaremos con RENIEC.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))

            CampoTexto(
                valor = uiState.dni, onCambio = { onCambio(Campo.DNI, it) }, etiqueta = "DNI",
                error = uiState.errores[Campo.DNI], habilitado = habilitado,
                tipoTeclado = KeyboardType.Number
            )
            CampoTexto(
                valor = uiState.nombres, onCambio = { onCambio(Campo.NOMBRES, it) }, etiqueta = "Nombres",
                error = uiState.errores[Campo.NOMBRES], habilitado = habilitado
            )
            CampoTexto(
                valor = uiState.apellidoPaterno, onCambio = { onCambio(Campo.APELLIDO_PATERNO, it) },
                etiqueta = "Apellido paterno", error = uiState.errores[Campo.APELLIDO_PATERNO],
                habilitado = habilitado
            )
            CampoTexto(
                valor = uiState.apellidoMaterno, onCambio = { onCambio(Campo.APELLIDO_MATERNO, it) },
                etiqueta = "Apellido materno", error = uiState.errores[Campo.APELLIDO_MATERNO],
                habilitado = habilitado
            )
            CampoTexto(
                valor = uiState.direccion, onCambio = { onCambio(Campo.DIRECCION, it) },
                etiqueta = "Dirección", error = uiState.errores[Campo.DIRECCION],
                habilitado = habilitado
            )

            Text("Sexo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Masculino", "Femenino").forEach { opcion ->
                    FilterChip(
                        selected = uiState.sexo == opcion,
                        onClick = { onCambio(Campo.SEXO, opcion) },
                        label = { Text(opcion) },
                        enabled = habilitado
                    )
                }
            }
            uiState.errores[Campo.SEXO]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))

            CampoTexto(
                valor = uiState.telefono, onCambio = { onCambio(Campo.TELEFONO, it) },
                etiqueta = "Teléfono", error = uiState.errores[Campo.TELEFONO],
                habilitado = habilitado, tipoTeclado = KeyboardType.Phone
            )
            CampoTexto(
                valor = uiState.correo, onCambio = { onCambio(Campo.CORREO, it) },
                etiqueta = "Correo electrónico", error = uiState.errores[Campo.CORREO],
                habilitado = habilitado, tipoTeclado = KeyboardType.Email
            )

            uiState.mensajeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            BotonPrincipal(texto = "Enviar postulación", cargando = uiState.cargando, onClick = onEnviarClick)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostulacionScreenPreview() {
    TecnisisGaleriaTheme {
        PostulacionScreen(PostulacionUiState(sexo = "Femenino"), { _, _ -> }, {}, {})
    }
}