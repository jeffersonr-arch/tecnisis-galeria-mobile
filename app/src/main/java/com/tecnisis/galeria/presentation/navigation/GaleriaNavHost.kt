package com.tecnisis.galeria.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.presentation.inicio.InicioRolRoute
import com.tecnisis.galeria.presentation.login.LoginRoute

@Composable
fun GaleriaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Pantalla.Login.name) {
        composable(Pantalla.Login.name) {
            LoginRoute(onLoginExitoso = { usuario ->
                navController.navigate(usuario.rol.pantallaInicio().name) {
                    popUpTo(Pantalla.Login.name) { inclusive = true }
                }
            })
        }
        Rol.entries.forEach { rol ->
            composable(rol.pantallaInicio().name) {
                InicioRolRoute(
                    rol = rol,
                    onSesionCerrada = {
                        navController.navigate(Pantalla.Login.name) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}