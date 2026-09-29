package com.tecnisis.galeria.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecnisis.galeria.data.repository.AuthRepositoryImpl
import com.tecnisis.galeria.domain.repository.AuthRepository
import com.tecnisis.galeria.domain.usecase.CerrarSesionUseCase
import com.tecnisis.galeria.domain.usecase.HaySesionActivaUseCase
import com.tecnisis.galeria.domain.usecase.IniciarSesionUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase

interface AppContainer {
    val iniciarSesion: IniciarSesionUseCase
    val obtenerUsuarioActual: ObtenerUsuarioActualUseCase
    val haySesionActiva: HaySesionActivaUseCase
    val cerrarSesion: CerrarSesionUseCase
}

class DefaultAppContainer : AppContainer {
    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }
    override val iniciarSesion by lazy { IniciarSesionUseCase(authRepository) }
    override val obtenerUsuarioActual by lazy { ObtenerUsuarioActualUseCase(authRepository) }
    override val haySesionActiva by lazy { HaySesionActivaUseCase(authRepository) }
    override val cerrarSesion by lazy { CerrarSesionUseCase(authRepository) }
}