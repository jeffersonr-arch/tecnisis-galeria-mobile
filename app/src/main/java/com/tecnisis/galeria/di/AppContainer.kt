package com.tecnisis.galeria.di

import com.tecnisis.galeria.data.work.VerificacionSchedulerImpl
import com.tecnisis.galeria.BuildConfig
import com.tecnisis.galeria.data.remote.RetrofitDni
import com.tecnisis.galeria.data.repository.ConsultaDniMock
import com.tecnisis.galeria.data.repository.ConsultaDniRepositoryImpl
import com.tecnisis.galeria.domain.repository.ConsultaDniRepository
import com.tecnisis.galeria.domain.usecase.VerificarPostulacionUseCase
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.tecnisis.galeria.data.repository.AuthRepositoryImpl
import com.tecnisis.galeria.data.repository.PostulacionRepositoryImpl
import com.tecnisis.galeria.domain.repository.AuthRepository
import com.tecnisis.galeria.domain.repository.PostulacionRepository
import com.tecnisis.galeria.domain.repository.VerificacionScheduler
import com.tecnisis.galeria.domain.usecase.CerrarSesionUseCase
import com.tecnisis.galeria.domain.usecase.EnviarPostulacionUseCase
import com.tecnisis.galeria.domain.usecase.HaySesionActivaUseCase
import com.tecnisis.galeria.domain.usecase.IniciarSesionUseCase
import com.tecnisis.galeria.domain.usecase.ObservarMiPostulacionUseCase
import com.tecnisis.galeria.domain.usecase.ObservarPostulacionesPorEstadoUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerPostulacionUseCase
import com.tecnisis.galeria.domain.usecase.ObtenerUsuarioActualUseCase
import com.tecnisis.galeria.domain.usecase.RegistrarCuentaUseCase
import com.tecnisis.galeria.domain.usecase.ResolverPostulacionUseCase
import com.tecnisis.galeria.data.local.PreferenciasRepositoryImpl
import com.tecnisis.galeria.domain.repository.PreferenciasRepository
import com.tecnisis.galeria.domain.usecase.GuardarModoTemaUseCase
import com.tecnisis.galeria.domain.usecase.ObservarModoTemaUseCase
import com.tecnisis.galeria.domain.usecase.ObservarUsuarioActualUseCase
import com.tecnisis.galeria.data.remote.ConectividadRepositoryImpl
import com.tecnisis.galeria.domain.repository.ConectividadRepository
import com.tecnisis.galeria.domain.usecase.ObservarConexionUseCase

interface AppContainer {
    // Sprint 1
    val iniciarSesion: IniciarSesionUseCase
    val obtenerUsuarioActual: ObtenerUsuarioActualUseCase
    val haySesionActiva: HaySesionActivaUseCase
    val cerrarSesion: CerrarSesionUseCase
    // Sprint 2
    val registrarCuenta: RegistrarCuentaUseCase
    val enviarPostulacion: EnviarPostulacionUseCase
    val observarMiPostulacion: ObservarMiPostulacionUseCase
    val observarPostulacionesPorEstado: ObservarPostulacionesPorEstadoUseCase
    val obtenerPostulacion: ObtenerPostulacionUseCase
    val resolverPostulacion: ResolverPostulacionUseCase

    val verificarPostulacion: VerificarPostulacionUseCase

    // Sprint 2 · HU14
    val observarModoTema: ObservarModoTemaUseCase
    val guardarModoTema: GuardarModoTemaUseCase

    val observarUsuarioActual: ObservarUsuarioActualUseCase
    // Sprint 2 · HU05
    val observarConexion: ObservarConexionUseCase
}

class DefaultAppContainer(private val contexto: Context) : AppContainer {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance("southamerica-east1") }

    private val authRepository: AuthRepository by lazy { AuthRepositoryImpl(auth, db) }
    private val postulacionRepository: PostulacionRepository by lazy {
        PostulacionRepositoryImpl(auth, db, functions)
    }
    // Con DNI_API_MOCK=true usa la API simulada; con falso, la real de Decolecta
    private val consultaDni: ConsultaDniRepository by lazy {
        if (BuildConfig.DNI_API_MOCK) ConsultaDniMock()
        else ConsultaDniRepositoryImpl(RetrofitDni.crearApi(), BuildConfig.DNI_API_TOKEN)
    }

    private val verificacionScheduler: VerificacionScheduler by lazy { VerificacionSchedulerImpl(contexto) }

    override val iniciarSesion by lazy { IniciarSesionUseCase(authRepository) }
    override val obtenerUsuarioActual by lazy { ObtenerUsuarioActualUseCase(authRepository) }
    override val haySesionActiva by lazy { HaySesionActivaUseCase(authRepository) }
    override val cerrarSesion by lazy { CerrarSesionUseCase(authRepository) }

    override val registrarCuenta by lazy { RegistrarCuentaUseCase(authRepository) }
    override val enviarPostulacion by lazy {
        EnviarPostulacionUseCase(postulacionRepository, verificacionScheduler)
    }
    override val observarMiPostulacion by lazy { ObservarMiPostulacionUseCase(postulacionRepository) }
    override val observarPostulacionesPorEstado by lazy {
        ObservarPostulacionesPorEstadoUseCase(postulacionRepository)
    }
    override val obtenerPostulacion by lazy { ObtenerPostulacionUseCase(postulacionRepository) }
    override val resolverPostulacion by lazy { ResolverPostulacionUseCase(postulacionRepository) }
    override val verificarPostulacion by lazy {
        VerificarPostulacionUseCase(postulacionRepository, consultaDni)
    }

    private val preferenciasRepository: PreferenciasRepository by lazy {
        PreferenciasRepositoryImpl(contexto)
    }

    override val observarModoTema by lazy { ObservarModoTemaUseCase(preferenciasRepository) }
    override val guardarModoTema by lazy { GuardarModoTemaUseCase(preferenciasRepository) }
    override val observarUsuarioActual by lazy { ObservarUsuarioActualUseCase(authRepository) }
    // HU05: conectividad
    private val conectividadRepository: ConectividadRepository by lazy { ConectividadRepositoryImpl(contexto) }
    override val observarConexion by lazy { ObservarConexionUseCase(conectividadRepository) }
}