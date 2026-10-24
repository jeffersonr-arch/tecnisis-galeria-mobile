package com.tecnisis.galeria.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.tecnisis.galeria.data.local.SocioDao
import com.tecnisis.galeria.data.local.SocioEntity
import com.tecnisis.galeria.data.local.aDominio
import com.tecnisis.galeria.data.work.SincronizacionPerfilWorker
import com.tecnisis.galeria.domain.model.Socio
import com.tecnisis.galeria.domain.model.SocioError
import com.tecnisis.galeria.domain.repository.SocioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** HU05: Firestore es el origen de los datos; Room es lo que lee la pantalla (funciona sin internet). */
class SocioRepositoryImpl(
    private val contexto: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val dao: SocioDao,
    private val alcance: CoroutineScope
) : SocioRepository {

    override fun observarMiPerfil(): Flow<Socio?> {
        val uid = auth.currentUser?.uid ?: return flowOf(null)
        return dao.observar(uid).map { it?.aDominio() }
    }

    override suspend fun sincronizar(): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: throw SocioError.SinSesion
        // Source.SERVER: solo cuenta como sincronizado si vino del servidor, no de la caché
        val doc = db.collection(COLECCION).document(uid).get(Source.SERVER).await()
        if (!doc.exists()) throw SocioError.SinFicha
        dao.guardar(doc.aEntidad(ahora = System.currentTimeMillis()))
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(mapear(e))
    }

    override fun programarSincronizacion() {
        val conInternet = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workManager = WorkManager.getInstance(contexto)

        // 1) Una vez, apenas haya internet (si ya hay, corre en segundos)
        workManager.enqueueUniqueWork(
            TRABAJO_AHORA,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SincronizacionPerfilWorker>()
                .setConstraints(conInternet)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        )
        // 2) Cada 6 horas, para que la ficha no quede desactualizada
        workManager.enqueueUniquePeriodicWork(
            TRABAJO_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SincronizacionPerfilWorker>(6, TimeUnit.HOURS)
                .setConstraints(conInternet)
                .build()
        )
    }

    override fun borrarPerfilLocal() {
        val uid = auth.currentUser?.uid ?: return
        WorkManager.getInstance(contexto).apply {
            cancelUniqueWork(TRABAJO_AHORA)
            cancelUniqueWork(TRABAJO_PERIODICO)
        }
        // Room no permite escribir en el hilo principal: se borra en segundo plano
        alcance.launch { dao.borrar(uid) }
    }

    private fun DocumentSnapshot.aEntidad(ahora: Long) = SocioEntity(
        uid = id,
        numeroSocio = getString("numeroSocio").orEmpty(),
        dni = getString("dni").orEmpty(),
        nombreCompleto = getString("nombreCompleto").orEmpty(),
        direccion = getString("direccion").orEmpty(),
        sexo = getString("sexo").orEmpty(),
        telefono = getString("telefono").orEmpty(),
        correo = getString("correo").orEmpty(),
        estadoMembresia = getString("estadoMembresia") ?: "activa",
        fechaAlta = getTimestamp("fechaAlta")?.toDate()?.time,
        sincronizadoEn = ahora
    )

    private fun mapear(e: Exception): SocioError = when (e) {
        is SocioError -> e
        is FirebaseFirestoreException ->
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) SocioError.SinConexion
            else SocioError.Desconocido
        is FirebaseNetworkException -> SocioError.SinConexion
        else -> SocioError.Desconocido
    }

    private companion object {
        const val COLECCION = "socios"
        const val TRABAJO_AHORA = "sincronizar_perfil_ahora"
        const val TRABAJO_PERIODICO = "sincronizar_perfil_periodico"
    }
}