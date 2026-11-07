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
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.presentation.bandeja.BandejaRoute
import com.tecnisis.galeria.presentation.componentes.BarraInferior
import com.tecnisis.galeria.presentation.componentes.OpcionBarra
import com.tecnisis.galeria.presentation.detalle.DetallePostulacionRoute
import com.tecnisis.galeria.presentation.detalle.DetallePostulacionViewModel
import com.tecnisis.galeria.presentation.detallesolicitud.DetalleSolicitudRoute
import com.tecnisis.galeria.presentation.detallesolicitud.DetalleSolicitudViewModel
import com.tecnisis.galeria.presentation.estado.EstadoPostulacionRoute
import com.tecnisis.galeria.presentation.inicio.InicioRolRoute
import com.tecnisis.galeria.presentation.login.LoginRoute
import com.tecnisis.galeria.presentation.obras.MisObrasRoute
import com.tecnisis.galeria.presentation.obras.RegistrarObraRoute
import com.tecnisis.galeria.presentation.obras.RegistrarObraViewModel
import com.tecnisis.galeria.presentation.perfil.PerfilRoute
import com.tecnisis.galeria.presentation.postulacion.PostulacionRoute
import com.tecnisis.galeria.presentation.registro.RegistroRoute
import com.tecnisis.galeria.presentation.solicitudes.SolicitudesRoute

@Composable
fun GaleriaNavHost(
    navController: NavHostController = rememberNavController(),
    sesionViewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory),
    abrirMisObras: Boolean = false,          // Se tocó la notificación de la decisión
    onMisObrasAbierta: () -> Unit = {}       // Avisa a MainActivity que ya se atendió

) {
    val usuario by sesionViewModel.usuario.collectAsState()
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    val irAlLogin: () -> Unit = {
        navController.navigate(Pantalla.Login.name) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    val navegarSiExiste: (Pantalla) -> Unit = { destino ->
        if (navController.graph.findNode(destino.name) != null) {
            navController.navigate(destino.name)
        }
    }
    val volver: () -> Unit = { navController.popBackStack() }

    val destinos = if (entrada == null) emptyList() else
        usuario?.rol?.destinos().orEmpty()
            .filter { navController.graph.findNode(it.pantalla.name) != null }
    val mostrarBarra = destinos.size >= 2 && destinos.any { it.pantalla.name == rutaActual }

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

    LaunchedEffect(abrirMisObras, usuario?.rol, rutaActual) {
        if (abrirMisObras && usuario?.rol == Rol.SOCIO_ACTIVO &&
            rutaActual != null && rutaActual != Pantalla.Login.name
        ) {
            if (rutaActual != Pantalla.MisObras.name) {
                navController.navigate(Pantalla.MisObras.name) {
                    popUpTo(Rol.SOCIO_ACTIVO.pantallaInicio().name)
                    launchSingleTop = true
                }
            }
            onMisObrasAbierta()
        }
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
            composable(Pantalla.EstadoPostulacion.name) {
                EstadoPostulacionRoute(onCompletar = { navController.navigate(Pantalla.Postulacion.name) })
            }
            composable(Pantalla.Perfil.name) {
                PerfilRoute(onSesionCerrada = irAlLogin)
            }
            composable(Pantalla.MisObras.name) {
                MisObrasRoute(
                    onRegistrar = { navController.navigate(Pantalla.RegistrarObra.name) },
                    onEditar = { id ->
                        navController.navigate("${Pantalla.RegistrarObra.name}?${RegistrarObraViewModel.ARG_OBRA_ID}=$id")
                    }
                )
            }
            composable(
                route = "${Pantalla.RegistrarObra.name}?${RegistrarObraViewModel.ARG_OBRA_ID}={${RegistrarObraViewModel.ARG_OBRA_ID}}",
                arguments = listOf(
                    navArgument(RegistrarObraViewModel.ARG_OBRA_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                RegistrarObraRoute(onVolver = volver)
            }
            composable(Pantalla.Bandeja.name) {
                BandejaRoute(
                    onAbrir = { uid -> navController.navigate("${Pantalla.DetallePostulacion.name}/$uid") }
                )
            }
            composable(
                route = "${Pantalla.DetallePostulacion.name}/{${DetallePostulacionViewModel.ARG_UID}}",
                arguments = listOf(
                    navArgument(DetallePostulacionViewModel.ARG_UID) { type = NavType.StringType }
                )
            ) {
                DetallePostulacionRoute(onVolver = volver)
            }
            composable(Pantalla.SolicitudesObras.name) {
                SolicitudesRoute(
                    onAbrir = { id -> navController.navigate("${Pantalla.DetalleSolicitud.name}/$id") }
                )
            }
            composable(
                route = "${Pantalla.DetalleSolicitud.name}/{${DetalleSolicitudViewModel.ARG_ID}}",
                arguments = listOf(
                    navArgument(DetalleSolicitudViewModel.ARG_ID) { type = NavType.StringType }
                )
            ) {
                DetalleSolicitudRoute(onVolver = volver)
            }
        }
    }
}