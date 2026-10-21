package com.tecnisis.galeria.presentation.postulacion

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.CampoTexto
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.notificaciones.Notificaciones
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun PostulacionRoute(
    onEnviada: () -> Unit,
    onVolver: () -> Unit,
    viewModel: PostulacionViewModel = viewModel(factory = PostulacionViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

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

@Composable
fun PostulacionScreen(
    uiState: PostulacionUiState,
    onCambio: (Campo, String) -> Unit,
    onEnviarClick: () -> Unit,
    onVolver: () -> Unit
) {
    val habilitado = !uiState.cargando
    val c = MaterialTheme.colorScheme

    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver("Postulación de membresía", onVolver) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                uiState.mensajeError?.let {
                    Text(
                        it,
                        color = c.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                BotonPrincipal(
                    texto = "Enviar postulación",
                    cargando = uiState.cargando,
                    onClick = onEnviarClick,
                    icono = Icons.AutoMirrored.Filled.ArrowForward
                )
                Text(
                    "Recibirás una notificación al enviarla",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text("Mi postulación", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Escribe tus datos tal como figuran en tu DNI. Los verificaremos con RENIEC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }

            Seccion("Identidad", Icons.Outlined.Badge, c.primaryContainer, c.onPrimaryContainer) {
                CampoTexto(
                    valor = uiState.dni, onCambio = { onCambio(Campo.DNI, it) }, etiqueta = "DNI",
                    error = uiState.errores[Campo.DNI], habilitado = habilitado, tipoTeclado = KeyboardType.Number
                )
                CampoTexto(
                    valor = uiState.nombres, onCambio = { onCambio(Campo.NOMBRES, it) }, etiqueta = "Nombres",
                    error = uiState.errores[Campo.NOMBRES], habilitado = habilitado
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(
                        valor = uiState.apellidoPaterno, onCambio = { onCambio(Campo.APELLIDO_PATERNO, it) },
                        etiqueta = "Ap. paterno", error = uiState.errores[Campo.APELLIDO_PATERNO],
                        habilitado = habilitado, modifier = Modifier.weight(1f)
                    )
                    CampoTexto(
                        valor = uiState.apellidoMaterno, onCambio = { onCambio(Campo.APELLIDO_MATERNO, it) },
                        etiqueta = "Ap. materno", error = uiState.errores[Campo.APELLIDO_MATERNO],
                        habilitado = habilitado, modifier = Modifier.weight(1f)
                    )
                }
                SelectorSexo(
                    seleccion = uiState.sexo,
                    habilitado = habilitado,
                    error = uiState.errores[Campo.SEXO],
                    onElegir = { onCambio(Campo.SEXO, it) }
                )
            }

            Seccion("Contacto", Icons.Outlined.Phone, c.secondaryContainer, c.onSecondaryContainer) {
                CampoTexto(
                    valor = uiState.telefono, onCambio = { onCambio(Campo.TELEFONO, it) }, etiqueta = "Teléfono",
                    error = uiState.errores[Campo.TELEFONO], habilitado = habilitado,
                    tipoTeclado = KeyboardType.Phone, prefijo = "+51 "
                )
                CampoTexto(
                    valor = uiState.correo, onCambio = { onCambio(Campo.CORREO, it) }, etiqueta = "Correo electrónico",
                    error = uiState.errores[Campo.CORREO], habilitado = habilitado, tipoTeclado = KeyboardType.Email
                )
            }

            Seccion("Dirección", Icons.Outlined.LocationOn, c.tertiaryContainer, c.onTertiaryContainer) {
                CampoTexto(
                    valor = uiState.direccion, onCambio = { onCambio(Campo.DIRECCION, it) },
                    etiqueta = "Dirección de residencia", error = uiState.errores[Campo.DIRECCION],
                    habilitado = habilitado, ultimoCampo = true, onListo = onEnviarClick
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Seccion(
    titulo: String,
    icono: ImageVector,
    fondo: Color,
    color: Color,
    contenido: @Composable ColumnScope.() -> Unit
) {
    TarjetaGaleria {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fondo),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = contenido)
    }
}

@Composable
private fun SelectorSexo(seleccion: String, habilitado: Boolean, error: String?, onElegir: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column {
        Text(
            "Sexo",
            style = MaterialTheme.typography.labelMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(c.surfaceVariant)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Masculino", "Femenino").forEach { opcion ->
                val elegida = seleccion == opcion
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (elegida) c.surfaceContainerLowest else Color.Transparent)
                        .selectable(
                            selected = elegida,
                            enabled = habilitado,
                            role = Role.RadioButton,
                            onClick = { onElegir(opcion) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opcion,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (elegida) c.primary else c.onSurfaceVariant
                    )
                }
            }
        }
        if (error != null) {
            Text(
                error,
                color = c.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Preview(name = "Claro", showBackground = true, heightDp = 1100)
@Preview(name = "Nocturno", showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PostulacionScreenPreview() {
    TecnisisGaleriaTheme {
        PostulacionScreen(
            uiState = PostulacionUiState(
                dni = "88776655", nombres = "Pedro", sexo = "Masculino", telefono = "98765",
                errores = mapOf(Campo.TELEFONO to "9 dígitos, empieza con 9")
            ),
            onCambio = { _, _ -> }, onEnviarClick = {}, onVolver = {}
        )
    }
}