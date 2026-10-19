package com.tecnisis.galeria.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.presentation.componentes.BarraInferior
import com.tecnisis.galeria.presentation.componentes.OpcionBarra
import com.tecnisis.galeria.presentation.estado.EstadoPostulacionRoute
import com.tecnisis.galeria.presentation.inicio.InicioRolRoute
import com.tecnisis.galeria.presentation.login.LoginRoute
import com.tecnisis.galeria.presentation.postulacion.PostulacionRoute
import com.tecnisis.galeria.presentation.registro.RegistroRoute

@Composable
fun GaleriaNavHost(
    navController: NavHostController = rememberNavController(),
    sesionViewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)
) {
    val usuario by sesionViewModel.usuario.collectAsState()
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    val irAlLogin: () -> Unit = {
        navController.navigate(Pantalla.Login.name) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    // Solo navega si la pantalla ya existe en el grafo (las de sprints futuros aún no)
    val navegarSiExiste: (Pantalla) -> Unit = { destino ->
        if (navController.graph.findNode(destino.name) != null) {
            navController.navigate(destino.name)
        }
    }
    val volver: () -> Unit = { navController.popBackStack() }

    // HU14: pestañas del rol que ya tienen pantalla programada
    val destinos = if (entrada == null) emptyList() else
        usuario?.rol?.destinos().orEmpty()
            .filter { navController.graph.findNode(it.pantalla.name) != null }
    val mostrarBarra = destinos.size >= 2 && destinos.any { it.pantalla.name == rutaActual }

    // HU14: si el rol cambia con la sesión abierta (el Anfitrión aprobó), ir al inicio del rol nuevo
    var rolAnterior by remember { mutableStateOf<Rol?>(null) }
    LaunchedEffect(usuario?.rol) {
        val rolNuevo = usuario?.rol
        if (rolAnterior != null && rolNuevo != null && rolNuevo != rolAnterior) {
            navController.navigate(rolNuevo.pantallaInicio().name) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        rolAnterior = rolNuevo
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (mostrarBarra) {
                BarraInferior(
                    opciones = destinos.map { OpcionBarra(it.etiqueta, it.icono, it.pantalla.name == rutaActual) },
                    onSeleccion = { indice ->
                        navController.navigate(destinos[indice].pantalla.name) {
                            popUpTo(destinos.first().pantalla.name) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Pantalla.Login.name,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {

            composable(Pantalla.Login.name) {
                LoginRoute(
                    onLoginExitoso = { usuario ->
                        navController.navigate(usuario.rol.pantallaInicio().name) {
                            popUpTo(Pantalla.Login.name) { inclusive = true }
                        }
                    },
                    onCrearCuenta = { navController.navigate(Pantalla.Registro.name) }
                )
            }

            composable(Pantalla.Registro.name) {
                RegistroRoute(
                    onRegistrado = {
                        navController.navigate(Pantalla.Postulante.name) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onVolver = volver
                )
            }

            Rol.entries.forEach { rol ->
                composable(rol.pantallaInicio().name) {
                    InicioRolRoute(rol = rol, onSesionCerrada = irAlLogin, onNavegar = navegarSiExiste)
                }
            }

            // HU02: al enviar, va directo a ver el estado
            composable(Pantalla.Postulacion.name) {
                PostulacionRoute(
                    onEnviada = {
                        navController.navigate(Pantalla.EstadoPostulacion.name) {
                            popUpTo(Pantalla.Postulacion.name) { inclusive = true }
                        }
                    },
                    onVolver = volver
                )
            }

            // HU03
            composable(Pantalla.EstadoPostulacion.name) {
                EstadoPostulacionRoute(onVolver = volver)
            }

            // HU04, HU05 y HU14 (Perfil): aquí se agregan sus pantallas
        }
    }
}