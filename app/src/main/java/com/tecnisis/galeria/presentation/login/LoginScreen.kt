package com.tecnisis.galeria.presentation.login

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.presentation.biometric.BiometricAuthenticator
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.CampoTexto
import com.tecnisis.galeria.presentation.componentes.LogoGaleria
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria

@Composable
fun LoginRoute(
    onLoginExitoso: (Usuario) -> Unit,
    onCrearCuenta: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current as FragmentActivity
    val biometria = remember(activity) { BiometricAuthenticator(activity) }

    val pedirHuella: () -> Unit = {
        biometria.autenticar(
            onExito = viewModel::onHuellaExitosa,
            onError = viewModel::onHuellaError
        )
    }

    LaunchedEffect(Unit) { viewModel.configurarHuella(biometria.estaDisponible()) }

    // HU05: con sesión abierta, la huella aparece sola al abrir la app
    LaunchedEffect(uiState.pedirHuella) {
        if (uiState.pedirHuella) {
            pedirHuella()
            viewModel.onHuellaPedida()
        }
    }

    LaunchedEffect(uiState.usuarioAutenticado) {
        uiState.usuarioAutenticado?.let(onLoginExitoso)
    }

    LoginScreen(
        uiState = uiState,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onIngresarClick = viewModel::onIngresarClick,
        onHuellaClick = pedirHuella,
        onCrearCuentaClick = onCrearCuenta
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
    val c = MaterialTheme.colorScheme
    val g = MaterialTheme.galeria

    Box(
        modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = (-120).dp).size(300.dp).clip(CircleShape).background(g.pigmentoAmarillo))
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-120).dp, y = 96.dp).size(70.dp).clip(CircleShape).background(g.pigmentoRojo))
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-150).dp, y = 20.dp).size(26.dp).clip(CircleShape).background(c.tertiary))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(64.dp))
            LogoGaleria(64.dp, fondo = c.primary, color = c.secondary, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "Bienvenido a\nTECNISIS Galería",
                style = MaterialTheme.typography.displaySmall,
                color = c.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Ingresa para gestionar tus exposiciones y postulaciones.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.height(32.dp))

            TarjetaGaleria {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CampoTexto(
                        valor = uiState.correo,
                        onCambio = onCorreoChange,
                        etiqueta = "Correo electrónico",
                        error = uiState.errorCorreo,
                        habilitado = !uiState.cargando,
                        tipoTeclado = KeyboardType.Email,
                        iconoInicial = Icons.Outlined.Email
                    )
                    CampoTexto(
                        valor = uiState.contrasena,
                        onCambio = onContrasenaChange,
                        etiqueta = "Contraseña",
                        error = uiState.errorContrasena,
                        habilitado = !uiState.cargando,
                        tipoTeclado = KeyboardType.Password,
                        esContrasena = !mostrarContrasena,
                        iconoInicial = Icons.Outlined.Lock,
                        accionFinal = {
                            IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                                Icon(
                                    if (mostrarContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        ultimoCampo = true,
                        onListo = onIngresarClick
                    )

                    uiState.mensajeError?.let {
                        Text(it, color = c.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    BotonPrincipal(texto = "Ingresar", cargando = uiState.cargando, onClick = onIngresarClick)

                    if (uiState.puedeUsarHuella) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f), color = c.outlineVariant)
                            Text(
                                "o",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(Modifier.weight(1f), color = c.outlineVariant)
                        }
                        OutlinedButton(
                            onClick = onHuellaClick,
                            enabled = !uiState.cargando,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.5.dp, c.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = c.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text("Ingresar con huella", color = c.onSurface, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "¿Eres artista y quieres exponer?",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = onCrearCuentaClick,
                enabled = !uiState.cargando,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Crea tu cuenta de postulante", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() {
    TecnisisGaleriaTheme {
        LoginScreen(
            uiState = LoginUiState(correo = "pedro.castillo@correo.com", puedeUsarHuella = true),
            onCorreoChange = {},
            onContrasenaChange = {},
            onIngresarClick = {},
            onHuellaClick = {},
            onCrearCuentaClick = {}
        )
    }
}