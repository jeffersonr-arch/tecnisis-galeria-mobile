package com.tecnisis.galeria.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.SocioError

/** HU05: WorkManager lo ejecuta cuando hay internet; trae la ficha de Firestore y la guarda en Room. */
class SincronizacionPerfilWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val sincronizar = (applicationContext as GaleriaApplication).container.sincronizarPerfil

        return when (sincronizar().exceptionOrNull()) {
            null -> Result.success()
            // Se cayó la red a mitad de camino: WorkManager reintenta con espera creciente
            SocioError.SinConexion -> if (runAttemptCount < MAX_INTENTOS) Result.retry() else Result.failure()
            // Sin sesión o sin ficha todavía: no hay nada que reintentar
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_INTENTOS = 5
    }
}