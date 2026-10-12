package com.tecnisis.galeria.presentation.inicio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.presentation.navigation.Pantalla
import com.tecnisis.galeria.presentation.navigation.funciones
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun InicioRolRoute(
    rol: Rol,
    onSesionCerrada: () -> Unit,
    onNavegar: (Pantalla) -> Unit = {},
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(rol) { viewModel.verificarAcceso(rol) }

    when (val e = estado) {
        EstadoInicio.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        EstadoInicio.Denegado -> LaunchedEffect(Unit) {
            viewModel.onCerrarSesion()
            onSesionCerrada()
        }
        is EstadoInicio.Autorizado -> InicioRolScreen(
            usuario = e.usuario,
            onCerrarSesion = {
                viewModel.onCerrarSesion()
                onSesionCerrada()
            },
            onNavegar = onNavegar
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioRolScreen(usuario: Usuario, onCerrarSesion: () -> Unit, onNavegar: (Pantalla) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(usuario.rol.etiqueta) },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Hola, ${usuario.nombre}", style = MaterialTheme.typography.headlineSmall)
            Text(usuario.correo, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Text("Tus funciones", style = MaterialTheme.typography.titleMedium)
            usuario.rol.funciones().forEach { funcion ->
                Card(
                    onClick = { funcion.destino?.let(onNavegar) },
                    enabled = funcion.destino != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(funcion.titulo, style = MaterialTheme.typography.titleSmall)
                        if (funcion.destino == null) {
                            Text("Próximamente", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioRolScreenPreview() {
    TecnisisGaleriaTheme {
        InicioRolScreen(
            usuario = Usuario("1", "Postulante Prueba", "postulante@tecnisis.pe", Rol.POSTULANTE),
            onCerrarSesion = {},
            onNavegar = {}
        )
    }
}