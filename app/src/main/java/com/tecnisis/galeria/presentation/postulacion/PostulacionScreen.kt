package com.tecnisis.galeria.presentation.postulacion

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.CampoTexto
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.notificaciones.Notificaciones

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

@Composable
fun PostulacionRoute(
    onEnviada: () -> Unit,
    onVolver: () -> Unit,
    viewModel: PostulacionViewModel = viewModel(factory = PostulacionViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Pide el permiso de notificaciones al abrir el formulario (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.enviada) {
        if (uiState.enviada) {
            Notificaciones.mostrarPostulacionRecibida(contexto)
            onEnviada()
        }
    }

    PostulacionScreen(
        uiState = uiState,
        onCambio = viewModel::onCambio,
        onEnviarClick = viewModel::onEnviarClick,
        onVolver = onVolver
    )
}

@Preview(showBackground = true)
@Composable
private fun PostulacionScreenPreview() {
    TecnisisGaleriaTheme {
        PostulacionScreen(PostulacionUiState(sexo = "Femenino"), { _, _ -> }, {}, {})
    }
}