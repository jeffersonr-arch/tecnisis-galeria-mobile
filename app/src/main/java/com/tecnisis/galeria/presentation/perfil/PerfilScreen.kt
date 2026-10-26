package com.tecnisis.galeria.presentation.perfil

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.BuildConfig
import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Socio
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"))

@Composable
fun PerfilRoute(
    onSesionCerrada: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.sesionCerrada) {
        if (uiState.sesionCerrada) onSesionCerrada()
    }

    // HU05: resultado de "deslizar para actualizar"
    LaunchedEffect(uiState.mensaje) {
        val mensaje = uiState.mensaje ?: return@LaunchedEffect
        snackbar.showSnackbar(mensaje)
        viewModel.onMensajeMostrado()
    }

    PerfilScreen(
        uiState = uiState,
        snackbarHostState = snackbar,
        onModoTemaChange = viewModel::onModoTemaChange,
        onActualizar = viewModel::onActualizar,
        onCerrarSesion = viewModel::onCerrarSesion
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    uiState: PerfilUiState,
    snackbarHostState: SnackbarHostState,
    onModoTemaChange: (ModoTema) -> Unit,
    onActualizar: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Solo el Socio Activo tiene algo que actualizar desde Firestore
        if (uiState.esSocio) {
            PullToRefreshBox(
                isRefreshing = uiState.sincronizando,
                onRefresh = onActualizar,
                modifier = Modifier.fillMaxSize()
            ) {
                ContenidoPerfil(uiState, onModoTemaChange, onCerrarSesion)
            }
        } else {
            ContenidoPerfil(uiState, onModoTemaChange, onCerrarSesion)
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ContenidoPerfil(
    uiState: PerfilUiState,
    onModoTemaChange: (ModoTema) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val oscuro = when (uiState.modoTema) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Perfil", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(16.dp))

        uiState.usuario?.let { TarjetaUsuario(it) }

        // HU05: ficha del Socio Activo (sale de Room, así que se ve sin internet)
        if (uiState.esSocio) {
            Spacer(Modifier.height(22.dp))
            if (!uiState.hayConexion) {
                AvisoSinConexion()
                Spacer(Modifier.height(10.dp))
            }
            TituloSeccion("Mi membresía")
            val socio = uiState.socio
            if (socio != null) {
                TarjetaMembresia(socio)
            } else {
                TarjetaGaleria {
                    Text("Tu ficha de socio aún no está disponible", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Desliza hacia abajo para actualizar cuando tengas internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        TituloSeccion("Preferencias")
        TarjetaGaleria(relleno = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
            FilaAjuste(
                icono = Icons.Outlined.DarkMode,
                fondo = c.primaryContainer,
                color = c.onPrimaryContainer,
                titulo = "Modo nocturno",
                detalle = when (uiState.modoTema) {
                    ModoTema.SISTEMA -> "Según el tema del celular"
                    ModoTema.CLARO -> "Desactivado"
                    ModoTema.OSCURO -> "Activado"
                }
            ) {
                Switch(
                    checked = oscuro,
                    onCheckedChange = { activar ->
                        onModoTemaChange(if (activar) ModoTema.OSCURO else ModoTema.CLARO)
                    }
                )
            }
            if (uiState.modoTema != ModoTema.SISTEMA) {
                TextButton(
                    onClick = { onModoTemaChange(ModoTema.SISTEMA) },
                    modifier = Modifier.padding(start = 42.dp, bottom = 4.dp)
                ) { Text("Usar el tema del celular") }
            }
        }

        Spacer(Modifier.height(22.dp))
        TituloSeccion("Cuenta")
        TarjetaGaleria(relleno = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
            FilaAjuste(
                icono = Icons.Outlined.Info,
                fondo = c.surfaceVariant,
                color = c.onSurfaceVariant,
                titulo = "Acerca de TECNISIS",
                detalle = "Versión ${BuildConfig.VERSION_NAME}"
            )
            HorizontalDivider(color = c.outlineVariant)
            FilaAjuste(
                icono = Icons.AutoMirrored.Outlined.Logout,
                fondo = c.errorContainer,
                color = c.onErrorContainer,
                titulo = "Cerrar sesión",
                detalle = null,
                colorTitulo = c.error,
                modifier = Modifier.clickable(onClick = onCerrarSesion)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AvisoSinConexion() {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Sin conexión · mostrando datos guardados",
            style = MaterialTheme.typography.labelLarge,
            color = c.onSurfaceVariant
        )
    }
}

@Composable
private fun TarjetaMembresia(socio: Socio) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("N.° DE SOCIO", style = MaterialTheme.typography.labelSmall, color = c.outline)
                Text(socio.numeroSocio, style = MaterialTheme.typography.headlineMedium, color = c.primary)
            }
            Text(
                socio.estadoMembresia.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = c.onTertiaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.tertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        socio.fechaAlta?.let {
            Text(
                "Socio desde el ${formatoFecha.format(Date(it))}",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = c.outlineVariant)
        DatoMembresia("DNI", socio.dni)
        DatoMembresia("Dirección", socio.direccion)
        DatoMembresia("Teléfono", socio.telefono)
        DatoMembresia("Sexo", socio.sexo)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Sync, contentDescription = null, tint = c.outline, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                textoSincronizado(socio.sincronizadoEn),
                style = MaterialTheme.typography.labelSmall,
                color = c.outline
            )
        }
    }
}

@Composable
private fun DatoMembresia(etiqueta: String, valor: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(valor.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}

/** "Actualizado ahora mismo" o "Actualizado hace 5 minutos" (en el idioma del celular) */
private fun textoSincronizado(momento: Long?): String {
    if (momento == null) return "Sin sincronizar"
    val ahora = System.currentTimeMillis()
    if (ahora - momento < DateUtils.MINUTE_IN_MILLIS) return "Actualizado ahora mismo"
    val hace = DateUtils.getRelativeTimeSpanString(momento, ahora, DateUtils.MINUTE_IN_MILLIS)
    return "Actualizado $hace"
}

@Composable
private fun TarjetaUsuario(usuario: Usuario) {
    val c = MaterialTheme.colorScheme
    val iniciales = usuario.nombre.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    TarjetaGaleria {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(c.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    iniciales.ifBlank { "?" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.onSecondary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                Text(usuario.correo, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    usuario.rol.etiqueta.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onSecondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(c.secondary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FilaAjuste(
    icono: ImageVector,
    fondo: Color,
    color: Color,
    titulo: String,
    detalle: String?,
    modifier: Modifier = Modifier,
    colorTitulo: Color = MaterialTheme.colorScheme.onSurface,
    accion: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(fondo),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = colorTitulo)
            if (detalle != null) {
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        accion()
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PerfilScreenPreview() {
    TecnisisGaleriaTheme {
        PerfilScreen(
            uiState = PerfilUiState(
                usuario = Usuario("1", "Rosa Maria Quispe Mamani", "rosa@tecnisis.pe", Rol.SOCIO_ACTIVO),
                socio = Socio(
                    uid = "1", numeroSocio = "SOC-0001", dni = "11223344",
                    nombreCompleto = "Rosa Maria Quispe Mamani", direccion = "Av. Jose Olaya 133",
                    sexo = "Femenino", telefono = "999888777", correo = "rosa@tecnisis.pe",
                    estadoMembresia = "activa", fechaAlta = 1_792_000_000_000,
                    sincronizadoEn = System.currentTimeMillis() - 5 * 60_000
                ),
                hayConexion = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onModoTemaChange = {},
            onActualizar = {},
            onCerrarSesion = {}
        )
    }
}