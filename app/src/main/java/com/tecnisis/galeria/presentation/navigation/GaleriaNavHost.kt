package com.tecnisis.galeria.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.presentation.inicio.InicioRolRoute
import com.tecnisis.galeria.presentation.login.LoginRoute
import com.tecnisis.galeria.presentation.postulacion.PostulacionRoute
import com.tecnisis.galeria.presentation.registro.RegistroRoute

@Composable
fun GaleriaNavHost(navController: NavHostController = rememberNavController()) {
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

    NavHost(navController = navController, startDestination = Pantalla.Login.name) {
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
        // HU02
        composable(Pantalla.Postulacion.name) {
            PostulacionRoute(onEnviada = volver, onVolver = volver)
        }
        // HU03, HU04 y HU05: aquí se agregaran las pantallas
    }
}