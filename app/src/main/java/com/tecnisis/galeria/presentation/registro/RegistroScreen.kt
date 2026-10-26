package com.tecnisis.galeria.presentation.registro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.CampoTexto
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun RegistroScreen(
    uiState: RegistroUiState,
    onNombreChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onConfirmacionChange: (String) -> Unit,
    onRegistrarClick: () -> Unit,
    onVolver: () -> Unit
) {
    val habilitado = !uiState.cargando

    Scaffold(topBar = { BarraConVolver("Crear cuenta", onVolver) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Crea tu cuenta para postular como socio de la galería.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            CampoTexto(
                valor = uiState.nombre, onCambio = onNombreChange, etiqueta = "Nombre completo",
                error = uiState.errorNombre, habilitado = habilitado
            )
            CampoTexto(
                valor = uiState.correo, onCambio = onCorreoChange, etiqueta = "Correo electrónico",
                error = uiState.errorCorreo, habilitado = habilitado,
                tipoTeclado = KeyboardType.Email
            )
            CampoTexto(
                valor = uiState.contrasena, onCambio = onContrasenaChange, etiqueta = "Contraseña",
                error = uiState.errorContrasena, habilitado = habilitado,
                tipoTeclado = KeyboardType.Password, esContrasena = true
            )
            CampoTexto(
                valor = uiState.confirmacion, onCambio = onConfirmacionChange, etiqueta = "Repite la contraseña",
                error = uiState.errorConfirmacion, habilitado = habilitado,
                tipoTeclado = KeyboardType.Password, esContrasena = true
            )

            uiState.mensajeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            BotonPrincipal(texto = "Crear cuenta", cargando = uiState.cargando, onClick = onRegistrarClick)
        }
    }
}

@Composable
fun RegistroRoute(
    onRegistrado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistroViewModel = viewModel(factory = RegistroViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registrado) {
        if (uiState.registrado) onRegistrado()
    }

    RegistroScreen(
        uiState = uiState,
        onNombreChange = viewModel::onNombreChange,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onConfirmacionChange = viewModel::onConfirmacionChange,
        onRegistrarClick = viewModel::onRegistrarClick,
        onVolver = onVolver
    )
}

@Preview(showBackground = true)
@Composable
private fun RegistroScreenPreview() {
    TecnisisGaleriaTheme {
        RegistroScreen(RegistroUiState(), {}, {}, {}, {}, {}, {})
    }
}