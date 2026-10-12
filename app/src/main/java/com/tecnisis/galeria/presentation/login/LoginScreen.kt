package com.tecnisis.galeria.presentation.login

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.R
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.presentation.biometric.BiometricAuthenticator
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun LoginRoute(
    onLoginExitoso: (Usuario) -> Unit,
    onCrearCuenta: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity =  LocalActivity.current as FragmentActivity
    val biometria = remember(activity) { BiometricAuthenticator(activity) }

    LaunchedEffect(Unit) { viewModel.configurarHuella(biometria.estaDisponible()) }
    LaunchedEffect(uiState.usuarioAutenticado) {
        uiState.usuarioAutenticado?.let(onLoginExitoso)
    }

    LoginScreen(
        uiState = uiState,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onIngresarClick = viewModel::onIngresarClick,
        onCrearCuentaClick = onCrearCuenta,
        onHuellaClick = {
            biometria.autenticar(
                onExito = viewModel::onHuellaExitosa,
                onError = viewModel::onHuellaError
            )
        }
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onIngresarClick: () -> Unit,
    onHuellaClick: () -> Unit,
    onCrearCuentaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarContrasena by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Image(
                painter = painterResource(R.drawable.logo_tecnisis),
                contentDescription = "Logo de TECNISIS Galería",
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "TECNISIS",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Galería de Arte",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = uiState.correo,
                onValueChange = onCorreoChange,
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                isError = uiState.errorCorreo != null,
                supportingText = { uiState.errorCorreo?.let { Text(it) } },
                singleLine = true,
                enabled = !uiState.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Icon(
                            if (mostrarContrasena) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (mostrarContrasena) VisualTransformation.None
                else PasswordVisualTransformation(),
                isError = uiState.errorContrasena != null,
                supportingText = { uiState.errorContrasena?.let { Text(it) } },
                singleLine = true,
                enabled = !uiState.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onIngresarClick() }),
                modifier = Modifier.fillMaxWidth()
            )

            uiState.mensajeError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onIngresarClick,
                enabled = !uiState.cargando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (uiState.cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Ingresar")
                }
            }

            if (uiState.puedeUsarHuella) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onHuellaClick,
                    enabled = !uiState.cargando,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ingresar con huella")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCrearCuentaClick, enabled = !uiState.cargando) {
                Text("¿Quieres ser socio? Crea tu cuenta")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    TecnisisGaleriaTheme {
        LoginScreen(
            uiState = LoginUiState(puedeUsarHuella = true),
            onCorreoChange = {},
            onContrasenaChange = {},
            onIngresarClick = {},
            onHuellaClick = {},
            onCrearCuentaClick = {}
        )
    }
}